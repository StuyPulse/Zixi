/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package tools.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

        Path commitPath = Paths.get("src", "main", "deploy", "commit.json");

        ObjectMapper json = new ObjectMapper();
        ObjectNode object = json.createObjectNode();
        object.put("hash", hash);
        object.put("message", message);
        if (!description.isBlank()) {
            object.put("description", description);
        }

        String content = json.writerWithDefaultPrettyPrinter().writeValueAsString(object);

        try {
            Files.writeString(commitPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            IO.println("Failed to write git commit to deploy file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Use NUL character deliminator so that characters in commit message don't conflict with
    // separators
    private static String constructFormat(String... formatters) {
        String NUL = "%x00";
        String format = "--format=";
        for (int i = 0; i < formatters.length; i++) {
            if (i == formatters.length - 1) {
                format += formatters[i];
            } else {
                format += formatters[i] + NUL;
            }
        }
        return format;
    }

    private static String[] getCommitInfo() throws IOException, InterruptedException {
        String hash = "%h";
        String message = "%s";
        String description = "%b";
        Process process =
                new ProcessBuilder("git", "log", "-1", constructFormat(hash, message, description))
                        .redirectErrorStream(true)
                        .start();

        String output =
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Git failed: " + output);
        }

        // \u0000 is java unicode escape character for the NUL character
        String[] parts = output.split("\u0000", 3);

        return parts;
    }
}
