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
import java.util.List;
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
                String content = Files.readString(targetPath, StandardCharsets.UTF_8);
                String newFields =
                        indent
                                + "public static final String GIT_MESSAGE = \""
                                + message
                                + "\";\n"
                                + indent
                                + "public static final String GIT_LINK = \""
                                + commitLink
                                + "\";\n"
                                + "}";

                content = content.replaceAll("}\\s*$", newFields);
                Files.writeString(targetPath, content, StandardCharsets.UTF_8);
                System.out.println(
                        "[Patch] Successfully injected Git metadata into "
                                + targetPath.getFileName());
            } catch (Exception e) {
                IO.println("Failed to write commit info to " + targetPath.getFileName());
                e.printStackTrace();
            }
        } else {
            System.out.println("Could not find file at " + targetPath.getFileName());
        }
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
        NO_NETWORK,
        UNKNOWN
    }

    private static PushStatus getCommitPushStatus() throws Exception {
        try {
            git.fetch().setRemote("origin").setTimeout(5).call();
        } catch (TransportException e) {
            return PushStatus.NO_NETWORK;
        }

        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit localCommitContext = walk.parseCommit(lastCommit.getId());

            List<Ref> remoteBranches = git.branchList().setListMode(ListMode.REMOTE).call();
            for (Ref remoteRef : remoteBranches) {
                RevCommit tipCommit = walk.parseCommit(remoteRef.getObjectId());

                if (walk.isMergedInto(localCommitContext, tipCommit)) {
                    return PushStatus.PUSHED;
                }
            }
        }

        return PushStatus.NOT_PUSHED;
    }
}
