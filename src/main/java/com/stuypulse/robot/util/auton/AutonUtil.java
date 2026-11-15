package com.stuypulse.robot.util.auton;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.wpilib.driverstation.DriverStationErrors;
import org.wpilib.system.Filesystem;

public final class AutonUtil {
    private AutonUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public enum AutonType {
        PATHPLANNER("pathplanner/paths", "path"),
        BLINE("bline/autos/paths", "json");
        
        private final String directory;
        private final String extension;

        private AutonType(String directory, String extension) {
            this.directory = directory;
            this.extension = extension;
        }

        public String getDirectory() {
            return directory;
        }

        public String getExtension() {
            return extension;
        }
    }

    public static List<String> getPathFileNames(AutonType autonType) {
        File pathsDir = new File(Filesystem.getDeployDirectory(), autonType.getDirectory());

        ArrayList<String> fileList = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pathsDir.toPath(), "*." + autonType.getExtension())) {
            for (Path file : stream) {
                fileList.add(file.getFileName().toString().replaceFirst("." + autonType.getExtension(), ""));
            }
        } catch (IOException err) {
            DriverStationErrors.reportError(err.getMessage(), false);
        }
        Collections.sort(fileList);
        return fileList;
    }

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
