/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util.auton;

import org.wpilib.driverstation.DriverStationErrors;
import org.wpilib.system.Filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 *
 * <h2>AutonUtil</h2>
 *
 * Shared utilities for discovering and comparing autonomous path names.
 */
public final class AutonUtil {
    private AutonUtil() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated");
    }

    /** Represents a supported autonomous path format and its location in the deploy directory. */
    public enum AutonType {
        PATHPLANNER("pathplanner/paths", "path"),
        BLINE("bline/autos/paths", "json");

        private final String directory;
        private final String extension;

        private AutonType(String directory, String extension) {
            this.directory = directory;
            this.extension = extension;
        }

        /**
         * Gets the path directory relative to the deploy directory.
         *
         * @return The path directory relative to the deploy directory
         */
        public String getDirectory() {
            return directory;
        }

        /**
         * Gets the file extension used by this autonomous format, without a leading dot.
         *
         * @return The file extension used by this autonomous format, without a leading dot
         */
        public String getExtension() {
            return extension;
        }
    }

    /**
     * Lists the available path names for an autonomous format.
     *
     * @param autonType The auton type to search for determining which auton directory and file
     *     extension to search
     * @return Sorted path names without their file extensions. Returns an empty list if the
     *     directory cannot be read, in which case an error is reported to the Driver Station
     */
    public static List<String> getPathFileNames(AutonType autonType) {
        File pathsDir = new File(Filesystem.getDeployDirectory(), autonType.getDirectory());

        ArrayList<String> fileList = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(pathsDir.toPath(), "*." + autonType.getExtension())) {
            for (Path file : stream) {
                fileList.add(
                        file.getFileName()
                                .toString()
                                .replaceFirst("." + autonType.getExtension(), ""));
            }
        } catch (IOException err) {
            DriverStationErrors.reportError(err.getMessage(), false);
        }
        Collections.sort(fileList);
        return fileList;
    }

    /**
     * Finds the listed path name with the smallest character-based distance from an input.
     *
     * @param paths The possible path names to compare
     * @param input The path name to match
     * @return The closest candidate, or an empty string when no candidate is selected
     */
    public static String findClosestMatch(List<String> paths, String input) {
        double closestValue = 10.0;
        String matching = "";

        for (String fileName : paths) {
            HashMap<Character, Integer> fileChars = countChars(fileName.toCharArray());
            HashMap<Character, Integer> inputChars = countChars(input.toCharArray());

            double proximity = compareNameProximity(fileChars, inputChars);
            closestValue = Math.min(proximity, closestValue);

            if (proximity == closestValue) {
                matching = fileName;
            }
        }

        return matching;
    }

    /**
     * Counts the occurrences of each character in an array.
     *
     * <p>This is mainly used for {@link #findClosestMatch(List, String)}.
     *
     * @param chars Characters to count
     * @return A map from each encountered character to its amount of occurences
     */
    public static HashMap<Character, Integer> countChars(char[] chars) {
        HashMap<Character, Integer> letterMap = new HashMap<>();
        for (char i = 'a'; i <= 'z'; i++) letterMap.put(i, 0);
        for (char i = 'a'; i <= 'z'; i++) letterMap.put(i, 0);
        letterMap.put('(', 0);
        letterMap.put(' ', 0);
        letterMap.put(')', 0);
        for (char letter : chars) {
            if (letterMap.containsKey(letter)) {
                letterMap.put(letter, letterMap.get(letter));
            } else {
                letterMap.put(letter, 1);
            }
        }
        return letterMap;
    }

    /**
     * Computes a character-count distance between two names.
     *
     * <p>Smaller values indicate closer names, while larger values indicate more distant names.
     *
     * <p>This is mainly used for {@link #findClosestMatch(List, String)}.
     *
     * @param list1 Character counts for the first name
     * @param list2 Character counts for the second name
     * @return The calculated distance between the names
     */
    public static double compareNameProximity(
            HashMap<Character, Integer> list1, HashMap<Character, Integer> list2) {
        double proximity = 0.0;
        int list1sum = 0, list2sum = 0;
        for (char key : list1.keySet()) {
            if (!list2.containsKey(key)) {
                proximity += 0.1;
                continue;
            }
            proximity += 0.05 * Math.abs(list1.get(key) - list2.get(key));
        }
        for (char key : list2.keySet()) {
            if (!list1.containsKey(key)) {
                proximity += 0.1;
                continue;
            }
            proximity += 0.05 * Math.abs(list1.get(key) - list2.get(key));
        }
        for (int count : list1.values()) list1sum += count;
        for (int count : list2.values()) list2sum += count;
        proximity += 0.4 * Math.abs(list2sum - list1sum);
        return proximity;
    }
}
