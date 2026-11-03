import { createHmac, timingSafeEqual } from "node:crypto";
import type { VercelRequest, VercelResponse } from '@vercel/node';

import { Octokit } from "@octokit/rest";

import { GithubPushSchema } from "../schemas/github-push.js";

const config = {
    api: {
        bodyParser: false,
    },
};

const handler = async (req: VercelRequest, res: VercelResponse) => {
    if (req.method !== "POST") {
        return res.status(405).json({ error: "Only POST requests are allowed" });
    }

    const secret = process.env.GITHUB_WEBHOOK_SECRET;
    if (!secret) {
        return res.status(500).json({ error: "Secret not setup" });
    }

    const chunks: Buffer[] = [];

    for await (const chunk of req) {
        chunks.push(Buffer.from(chunk));
    }

    const body = Buffer.concat(chunks);

    if (req.headers["x-github-event"] !== "push") {
        return res.status(400).json({ error: "Unexpected event" });
    }

    const signature = req.headers["x-hub-signature-256"];

    if (typeof signature !== "string") {
        return res.status(401).json({ error: "Missing signature" });
    }

    const expected = "sha256=" + createHmac("sha256", secret).update(body).digest("hex");

    const givenBuffer = Buffer.from(signature);
    const correctBuffer = Buffer.from(expected);

    if (givenBuffer.length !== correctBuffer.length || !timingSafeEqual(givenBuffer, correctBuffer)) {
        return res.status(401).json({ error: "Invalid signature" });
    }

    let json: unknown;

    try {
        json = JSON.parse(body.toString("utf8"));
    } catch {
        return res.status(400).json({ error: "Invalid JSON" });
    }

    const parsed = GithubPushSchema.safeParse(json);

    if (!parsed.success) {
        console.error(parsed.error);

        return res.status(400).json({ error: "Invalid GitHub payload" });
    }

    const payload = parsed.data;

    if (payload.repository.full_name !== "StuyPulse/Zixi") {
        return res.status(403).json({ error: "Expected StuyPulse/Zixi repository" });
    }

    if (payload.ref !== "refs/heads/main") {
        return res.status(200).json({ message: "Only main pushes are detected" });
    }

    const octokit = new Octokit({
        auth: process.env.GITHUB_TOKEN
    });

    const owner = "StuyPulse";
    const repo = "Zixi";
    const path = ".automation/main-push.json";
    const branch = "docs";

    const headCommit = payload.head_commit;
    if (!headCommit) {
        return res.status(200).json({ message: "Head commit not present" });
    }
    try {
        let sha: string | undefined;

        try {
            const { data: existingFile } = await octokit.repos.getContent({
                owner,
                repo,
                path,
                ref: branch,
            });

            if (!Array.isArray(existingFile) && existingFile.type === 'file') {
                sha = existingFile.sha;
            }
        } catch {
            // File does not exist
        }

        const updatedData = {
            message: headCommit.message,
            hash: headCommit.id,
            updatedAt: new Date().toISOString()
        };

        const response = await octokit.repos.createOrUpdateFileContents({
            owner,
            repo,
            path,
            message: `chore: update main push info for commit ${headCommit.id.substring(0, 7)}`,
            content: Buffer.from(JSON.stringify(updatedData, null, 2)).toString('base64'),
            sha,
            branch,
        });

        return res.status(200).json({ success: true, sha: response.data.content?.sha });
    } catch (error: any) {
        console.error('Failed to commit to docs branch:', error.message);
        return res.status(500).json({ error: error.message });
    }
}

export { config };
export default handler;
