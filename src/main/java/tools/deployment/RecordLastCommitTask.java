/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package tools.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecordLastCommitTask {
    public static void main() throws IOException, InterruptedException {
        String hash = "unknown";
        String message = "unknown";
        String description = "unknown";

        try {
            String[] parts = getCommitInfo();
            if (parts.length > 0 && !parts[0].isBlank()) {
                hash = parts[0];
            }
            if (parts.length > 1 && !parts[1].isBlank()) {
                message = parts[1];
            }
            if (parts.length > 2 && !parts[2].isBlank()) {
                description = parts[2];
            }
        } catch (Exception e) {
            System.out.println("Failed to get git commit info: ");
            e.printStackTrace();
        }

        Path commitPath = Paths.get("src", "main", "deploy", "commit.json");

        ObjectMapper json = new ObjectMapper();
        ObjectNode object = json.createObjectNode();
        object.put("hash", hash);
        object.put("message", message);
        if (!description.isBlank()) {
            object.put("description", description);
        }

        try {
            Files.createDirectories(commitPath.getParent());
            String content = json.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            Files.writeString(commitPath, content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("Failed to write git commit info to commit.json: ");
            e.printStackTrace();
        }
    }

    // Use NUL character deliminator so that characters in commit message don't conflict with
    // separators
    private static String constructFormat(String... formatters) {
        String NUL = "%x00";
        return "--format=" + String.join(NUL, formatters);
    }

    private static String[] getCommitInfo() throws IOException, InterruptedException {
        String hash = "%h";
        String message = "%s";
        String description = "%b";
        Process process =
                new ProcessBuilder("git", "log", "-1", constructFormat(hash, message, description))
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
