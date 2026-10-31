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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class PatchBuildConstants {
    private static Git git;
    private static RevCommit lastCommit;

    public static void main(String[] args) throws IOException, GitAPIException {
        String gitDirectory = args.length > 4 ? args[4] : null;
        if (gitDirectory != null) {
            git = Git.open(new File(gitDirectory));
            lastCommit = git.log().call().iterator().next();
        }

        String srcDir = args.length > 0 ? args[0] : "src/main/java/";
        String classPackage = args.length > 1 ? args[1] : "com.stuypulse.robot";
        String className = args.length > 2 ? args[2] : "BuildConstants";
        String indent = args.length > 3 ? args[3] : "  ";

        String message = "unknown";
        String commitLink = "unknown";
        PushStatus pushStatus = PushStatus.UNKNOWN;

        String packagePath = classPackage.replace(".", "/");
        Path targetPath = Paths.get(srcDir, packagePath, className + ".java");

        if (gitDirectory != null) {
            try {
                message = getCommitMessage();
                message = escapeJavaString(message);
                commitLink = "";
                pushStatus = getCommitPushStatus();
                if (pushStatus == PushStatus.PUSHED) {
                    commitLink = getCommitLink();
                }

            } catch (Exception e) {
                IO.println("Failed to get git commit info: ");
                e.printStackTrace();
            }
        }

        if (Files.exists(targetPath)) {
            try {
                Map<String, String> fields = new LinkedHashMap<>();
                fields.put("GIT_MESSAGE", message);
                fields.put("GIT_LINK", commitLink);
                fields.put("GIT_PUSH_STATUS", pushStatus.toString());
                patchWithNewFields(targetPath, indent, fields);
            } catch (Exception e) {
                IO.println("Failed to write commit info to " + targetPath.getFileName());
                e.printStackTrace();
            }
        } else {
            IO.println("Could not find file at " + targetPath.getFileName());
        }
    }

    private static void patchWithNewFields(Path targetPath, String indent, Map<String, String> fields) throws IOException {
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

        boolean hasDescription =
                longMessage != null
                        && longMessage.length() > shortMessage.length()
                        && !longMessage.substring(shortMessage.length()).isBlank();

        return lastCommit.getShortMessage() + (hasDescription ? "..." : "");
    }

    private static String getCommitLink() {
        Config config = git.getRepository().getConfig();
        String repoLink = config.getString("remote", "origin", "url");

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

            List<Ref> remoteBranches =
                    git.branchList().setListMode(ListMode.REMOTE).call();

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
