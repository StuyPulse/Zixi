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
        String description = "unknown";

        try {
            String[] parts = getCommitInfo();
            if (parts.length >= 1 && !parts[0].isBlank()) {
                message = parts[0];
            }
            if (parts.length >= 2) {
                description = !parts[1].isBlank() ? parts[1] : "";
            }
        } catch (Exception e) {
            IO.println("Failed to get git commit info: ");
            e.printStackTrace();
        }

        message = escapeJavaString(message);

        // dude
        description =
                description
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .replaceAll("\n{2,}", "\n")
                        .trim();
        description = escapeJavaString(description);

        String packagePath = classPackage.replace(".", "/");
        Path targetPath = Paths.get(srcDir, packagePath, className + ".java");

        if (Files.exists(targetPath)) {
            try {
                String content = Files.readString(targetPath, StandardCharsets.UTF_8);
                String newFields =
                        indent
                                + "public static final String GIT_MESSAGE = \""
                                + message
                                + "\";\n"
                                + indent
                                + "public static final String GIT_DESCRIPTION = \""
                                + description
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
        return value.replace("\\", "").replace("\"", "").replace("\n", "").replace("\t", "");
    }

    // Use NUL character deliminator so that characters in commit message don't conflict with
    // separators
    private static String constructFormat(String... formatters) {
        String NUL = "%x00";
        return "--format=" + String.join(NUL, formatters);
    }

    private static String[] getCommitInfo() throws IOException, InterruptedException {
        String message = "%s";
        String description = "%b";
        Process process =
                new ProcessBuilder("git", "log", "-1", constructFormat(message, description))
                        .redirectErrorStream(true)
                        .start();

        String output;
        try (InputStream stream = process.getInputStream()) {
            output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Git failed: " + output);
        }

        // \u0000 is java unicode escape character for the NUL character
        String[] parts = output.strip().split("\u0000", 3);
        return parts;
    }
}
