import os
import json
import uuid
import hmac
import shutil
import tempfile
import subprocess
import threading

from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from concurrent.futures import ThreadPoolExecutor

PORT = int(os.environ.get("PORT", "10000"))

WORKER_SECRET = os.environ["WORKER_SECRET"]
GITHUB_TOKEN = os.environ["GITHUB_TOKEN"]

REPOSITORY = "StuyPulse/Zixi"
SOURCE_BRANCH = "main"
PUBLISH_BRANCH = "docs"

ASSETS_DIR = Path("/app/assets")
PATCHER = Path("/app/workflowscripts/JavadocPatcher.mjs")

EXECUTOR = ThreadPoolExecutor(max_workers=1)

JOBS = {}
JOBS_LOCK = threading.Lock()

def run_command(command, cwd, env=None, timeout=900):
    print(f"Running: {' '.join(map(str, command))}", flush=True)

    result = subprocess.run(
        command,
        cwd=cwd,
        env=env,
        capture_output=True,
        text=True,
        timeout=timeout,
    )

    if (result.stdout):
        print(result.stdout, flush=True)

    if (result.stderr):
        print(result.stderr, flush=True)

    if (result.returncode != 0):
        raise RuntimeError(
            f"Command failed with exit code {result.returncode}: "
            f"{' '.join(map(str, command))}"
        )

    return result


def generate_docs(job_id, commit_sha):
    with (JOBS_LOCK):
        JOBS[job_id]["status"] = "running"

    try:
        with (tempfile.TemporaryDirectory(prefix="zixi-") as temp):
            temp = Path(temp)

            source = temp / "source"
            publishing = temp / "publishing"

            repository_url = f"https://github.com/{REPOSITORY}.git"

            print("Cloning source repository...", flush=True)

            run_command(
                [
                    "git", "clone",
                    "--depth", "1",
                    "--branch", SOURCE_BRANCH,
                    repository_url,
                    str(source),
                ],
                temp,
            )

            run_command(["chmod", "+x", "gradlew"], source,)

            print("Building Java project", flush=True)

            run_command(["./gradlew", "build", "--no-daemon"], source, timeout=900)

            print("Generating Javadocs...", flush=True)

            run_command(["./gradlew", "javadoc", "--no-daemon"], source, timeout=900)

            javadoc_directory = source / "build" / "docs" / "javadoc"

            if (not javadoc_directory.is_dir()):
                raise RuntimeError("Javadoc output directory missing")

            print("Patching Javadocs", flush=True)

            run_command(
                [
                    "node",
                    str(PATCHER),
                    str(source),
                ],
                source,
                timeout=120,
            )

            askpass = temp / "askpass.sh"

            askpass.write_text(
                '#!/bin/sh\n'
                'case "$1" in\n'
                '  *Username*) echo "x-access-token" ;;\n'
                '  *Password*) echo "$GITHUB_TOKEN" ;;\n'
                '  *) echo "" ;;\n'
                'esac\n'
            )

            askpass.chmod(0o700)

            git_env = os.environ.copy()
            git_env["GIT_ASKPASS"] = str(askpass)
            git_env["GIT_TERMINAL_PROMPT"] = "0"

            print("Cloning publishing branch", flush=True)

            run_command(
                [
                    "git", "clone",
                    "--depth", "1",
                    "--branch", PUBLISH_BRANCH,
                    repository_url,
                    str(publishing),
                ],
                temp,
                env=git_env,
            )

            destination = publishing / "docs"

            shutil.rmtree(destination, ignore_errors=True)

            shutil.copytree(javadoc_directory, destination)

            run_command(
                ["git", "config", "user.name", "Zixi Docs Bot"],
                publishing,
            )

            run_command(
                [
                    "git", "config", "user.email",
                    "zixi-docs-bot@users.noreply.github.com",
                ],
                publishing,
            )

            run_command(["git", "add", "docs/"], publishing)

            diff = subprocess.run(
                ["git", "diff", "--cached", "--quiet"],
                cwd=publishing,
                env=git_env,
            )

            if (diff.returncode == 0):
                print("No documentation changes to publish", flush=True)
            elif (diff.returncode == 1):
                run_command(
                    [
                        "git", "commit", "-m",
                        f"docs: generate Javadocs for {commit_sha[:12]}",
                    ],
                    publishing,
                )

                run_command(
                    ["git", "push", "origin", PUBLISH_BRANCH],
                    publishing,
                    env=git_env,
                )

                print("Documentation published!", flush=True)

            else:
                raise RuntimeError("Could not inspect Git changes")

        with (JOBS_LOCK):
            JOBS[job_id]["status"] = "completed"

    except Exception as error:
        print(f"Generation failed: {error}", flush=True)

        with (JOBS_LOCK):
            JOBS[job_id]["status"] = "failed"
            JOBS[job_id]["error"] = str(error)


class Handler(BaseHTTPRequestHandler):
    def respond(self, status, data):
        body = json.dumps(data).encode()

        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()

        self.wfile.write(body)

    def do_GET(self):
        if (self.path == "/health"):
            return self.respond(200, {"status": "healthy"})

        if (self.path.startswith("/jobs/")):
            job_id = self.path.removeprefix("/jobs/")

            with (JOBS_LOCK):
                job = JOBS.get(job_id)

            if (job is None):
                return self.respond(404, {"error": "Job not found"})

            return self.respond(200, job)

        return self.respond(404, {"error": "Not found"})

    def do_POST(self):
        if (self.path != "/generate"):
            return self.respond(404, {"error": "Not found"})

        authorization = self.headers.get("Authorization", "")
        expected = f"Bearer {WORKER_SECRET}"

        if (not hmac.compare_digest(authorization, expected)):
            return self.respond(401, {"error": "Unauthorized"})

        try:
            length = int(self.headers.get("Content-Length", "0"))

            if (length > 4096):
                return self.respond(413, {"error": "Payload too large"})

            body = self.rfile.read(length) if length else b"{}"
            payload = json.loads(body)

            commit_sha = payload.get("after", "unknown")

            if (not isinstance(commit_sha, str)):
                return self.respond(400, {"error": "Invalid commit SHA"})

            job_id = str(uuid.uuid4())

            with (JOBS_LOCK):
                JOBS[job_id] = {
                    "id": job_id,
                    "status": "queued",
                    "commit": commit_sha,
                }

            EXECUTOR.submit(generate_docs, job_id, commit_sha)

            return self.respond(202, {
                "success": True,
                "jobId": job_id,
                "status": "queued",
            })

        except (ValueError, json.JSONDecodeError):
            return self.respond(400, {"error": "Invalid JSON"})

        except (Exception as error):
            print(error, flush=True)
            return self.respond(500, {"error": "Internal server error"})

server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)

print(f"Zixi documentation worker listening on {PORT}", flush=True)

server.serve_forever()
