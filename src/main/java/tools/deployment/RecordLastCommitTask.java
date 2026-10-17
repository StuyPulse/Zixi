/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package tools.deployment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecordLastCommitTask {
    public static void main() throws IOException, InterruptedException {
        String[] parts = getCommitInfo();
        String hash = parts.length > 0 ? parts[0] : "";
        String message = parts.length > 1 ? parts[1] : "";
        String description = parts.length > 2 ? parts[2] : "";

        // should be fine to hardcode line endings since the systemcore is on linux
        String content = hash + "\n" + message + "\n" + description + "\n";
        Path commitPath = Paths.get("src", "main", "deploy", "commit.txt");

        try {
            Files.writeString(commitPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            IO.println("Failed to write git commit to deploy file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String[] getCommitInfo() throws IOException, InterruptedException {
        Process process =
                new ProcessBuilder("git", "log", "-1", "--format=%h%x00%s%x00%b")
                        .redirectErrorStream(true)
                        .start();

        String output =
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Git failed: " + output);
        }

        String[] parts = output.split("\u0000", 3);

        return parts;
    }
}
