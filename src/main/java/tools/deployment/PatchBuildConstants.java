/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package tools.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class PatchBuildConstants {
    private static Git git;
    private static RevCommit lastCommit;

    public static void main(String[] args) {
        String indent = "    ";
        Path targetPath = null;

        String message = "unknown";
        String commitLink = "unknown";
        PushStatus pushStatus = PushStatus.UNKNOWN;

        try {
            String[] argSchema = new String[] { "srcDir", "classPackage", "className", "indent", "gitDir" };
            argSchema = Arrays.stream(argSchema).map(str -> String.valueOf("<" + str + ">")).toArray(String[]::new);
            if (args == null || args.length < argSchema.length) {
                throw new IllegalArgumentException("Expected arguments: " + String.join(" ", argSchema));
            }

            for (int i = 0; i < argSchema.length; i++) {
                if (args[i] == null) {
                    throw new IllegalArgumentException("Missing required argument #" + i + ": " + argSchema[i]);
                }
            }

            String srcDir = args[0];
            String classPackage = args[1];
            String className = args[2];
            indent = args[3];
            String gitDirectory = args[4];

            String packagePath = classPackage.replace(".", "/");
            targetPath = Paths.get(srcDir, packagePath, className + ".java");

            try (Git _git = Git.open(new File(gitDirectory))) {
                git = _git;
                Iterator<RevCommit> commits = git.log().call().iterator();

                if (commits.hasNext()) {
                    lastCommit = commits.next();

                    message = getCommitMessage();
                    message = escapeJavaString(message);
                    pushStatus = getCommitPushStatus();

                    if (pushStatus == PushStatus.PUSHED) {
                        commitLink = getCommitLink();
                    }
                } else {
                    throw new IllegalStateException("Git repository contains no commits");
                }
            }
        } catch (Exception e) {
            IO.println("Failed to generate Git information: ");
            e.printStackTrace();

            message = "unknown";
            commitLink = "unknown";
            pushStatus = PushStatus.UNKNOWN;
        } finally {
            try {
                if (targetPath == null) {
                    IO.println("Cannot write BuildConstants: targetPath missing");
                } else if (!Files.exists(targetPath)) {
                    IO.println("Could not find file at: " + targetPath);
                } else {
                    Map<String, String> fields = new LinkedHashMap<>();
                    fields.put("GIT_MESSAGE", message);
                    fields.put("GIT_LINK", commitLink);
                    fields.put("GIT_PUSH_STATUS", pushStatus.toString());
                    patchWithNewFields(targetPath, indent, fields);
                }
            } catch (Exception e) {
                IO.println("Failed to write BuildConstants:");
                e.printStackTrace();
            }
        }
    }

    private static void patchWithNewFields(
        Path targetPath, String indent, Map<String, String> fields) throws IOException {
        String content = Files.readString(targetPath);

        StringBuilder newFields = new StringBuilder();

        for (Entry<String, String> entry : fields.entrySet()) {
            newFields
                    .append(indent)
                    .append("public static final String ")
                    .append(entry.getKey())
                    .append(" = \"")
                    .append(entry.getValue())
                    .append("\";\n");
        }
        newFields.append("}");

        content = content.replaceAll("}\\s*$", newFields.toString());
        Files.writeString(targetPath, content, StandardCharsets.UTF_8);
    }

    private static String escapeJavaString(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    private static String getCommitMessage() {
        if (lastCommit == null) {
            return "";
        }

        String shortMessage = lastCommit.getShortMessage();
        String longMessage = lastCommit.getFullMessage();

        boolean hasDescription = longMessage != null
                && longMessage.length() > shortMessage.length()
                && !longMessage.substring(shortMessage.length()).isBlank();

        return lastCommit.getShortMessage() + (hasDescription ? "..." : "");
    }

    private static String getCommitLink() {
        Config config = git.getRepository().getConfig();
        String repoLink = config.getString("remote", "origin", "url");

        if (repoLink == null || repoLink.isBlank()) {
            return "unknown";
        }

        if (repoLink.startsWith("git@github.com:")) {
            repoLink = repoLink.replace("git@github.com:", "https://github.com");
        }

        if (repoLink.endsWith(".git")) {
            repoLink = repoLink.substring(0, repoLink.lastIndexOf(".git"));
        }

        String commitHash = lastCommit.getName();

        return repoLink + "/commit/" + commitHash;
    }

    enum PushStatus {
        PUSHED,
        NOT_PUSHED,
        STALE,
        UNKNOWN
    }

    private static PushStatus getCommitPushStatus() {
        try {
            git.fetch().setRemote("origin").setTimeout(5).call();

        } catch (TransportException e) {
            return PushStatus.STALE;
        } catch (Exception e) {
            IO.println("Failed to fetch remote Git information: ");
            e.printStackTrace();
            return PushStatus.UNKNOWN;
        }

        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit localCommit = walk.parseCommit(lastCommit.getId());

            List<Ref> remoteBranches = git.branchList().setListMode(ListMode.REMOTE).call();

            for (Ref remoteRef : remoteBranches) {
                if (remoteRef.getObjectId() == null) {
                    continue;
                }

                RevCommit remoteCommit = walk.parseCommit(remoteRef.getObjectId());

                if (walk.isMergedInto(localCommit, remoteCommit)) {
                    return PushStatus.PUSHED;
                }
            }

            return PushStatus.NOT_PUSHED;

        } catch (Exception e) {
            IO.println("Failed to determine Git push status: ");
            e.printStackTrace();
            return PushStatus.UNKNOWN;
        }
    }
}
