/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package tools.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PatchBuildConstants {
    public static void main(String[] args) throws IOException, InterruptedException {
        String srcDir = args.length > 0 ? args[0] : "src/main/java/";
        String classPackage = args.length > 1 ? args[1] : "com.stuypulse.robot";
        String className = args.length > 2 ? args[2] : "BuildConstants";
        String indent = args.length > 3 ? args[3] : "  ";

        String message = "unknown";
        String commitLink = "unknown";

        String packagePath = classPackage.replace(".", "/");
        Path targetPath = Paths.get(srcDir, packagePath, className + ".java");

        try {
            message = getCommitMessage();
            message = escapeJavaString(message);
            commitLink = isCommitPushed() ? getCommitLink() : "";
        } catch (Exception e) {
            IO.println("Failed to get git commit info: ");
            e.printStackTrace();
        }

        message = escapeJavaString(message);

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

    // Use NUL character deliminator so that characters in commit message don't conflict with
    // separators
    private static String constructFormat(String... formatters) {
        String NUL = "%x00";
        return "--format=" + String.join(NUL, formatters);
    }

    private static String getCommitMessage() throws IOException, InterruptedException {
        String output =
                runCommand(
                        "git",
                        "log",
                        "-1",
                        constructFormat("%s", "%b")); // %s is message, %b is description
        if (output.isBlank()) {
            throw new RuntimeException("Could not determine last commit message and description");
        }

        String[] parts = output.split("\u0000", 2);

        String message = parts[0].trim();
        boolean hasDescription = parts.length > 1 && !parts[1].isBlank();

        return message + (hasDescription ? "..." : "");
    }

    private static String getCommitLink() throws IOException, InterruptedException {
        String remoteURL = runCommand("git", "config", "--get", "remote.origin.url");
        if (remoteURL.isBlank()) {
            throw new RuntimeException("Could not determine Git remote URL");
        }

        // i prefer not to do reflection to get the hash from BuildConstants might
        // as well just calculate it again
        String longHash = runCommand("git", "log", "-1", constructFormat("%H")); // %H is long hash
        if (longHash.isBlank()) {
            throw new RuntimeException("Could not determine Git hash of last commit");
        }

        if (remoteURL.startsWith("git@github.com:")) {
            remoteURL = remoteURL.replace("git@github.com:", "https://github.com/");
        }

        if (remoteURL.endsWith(".git")) {
            remoteURL = remoteURL.substring(0, remoteURL.length() - 4);
        }

        return remoteURL + "/commit/" + longHash;
    }

    private static String runCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        try (InputStream stream = process.getInputStream()) {
            String output = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();

            if (process.waitFor() != 0) {
                return "";
            }

            return output;
        }
    }

    private static boolean isCommitPushed() throws IOException, InterruptedException {
        runCommand("git", "fetch", "origin");

        String result = runCommand("git", "branch", "-r", "--contains", "HEAD");

        return !result.isBlank();
    }
}
