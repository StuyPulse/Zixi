/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util.auton;

import com.stuypulse.robot.util.auton.AutonUtil.AutonType;

import com.stuypulse.robot.commands.auton.Auton;
import com.stuypulse.robot.commands.auton.PathPlannerAuton;
import com.stuypulse.robot.opmodes.AutonOpMode;

import org.wpilib.driverstation.DriverStationErrors;

import com.pathplanner.lib.path.PathPlannerPath;

import java.util.function.Function;
import org.littletonrobotics.junction.networktables.LoggedNetworkChooser;

public class PathPlannerUtil {

    public static class PathPlannerConfig {

        private final String name;

        private final Function<PathPlannerPath[], PathPlannerAuton> auton;

        private final String[] paths;

        public PathPlannerConfig(
                String name, Function<PathPlannerPath[], PathPlannerAuton> auton, String... paths) {
            this.name = name;
            this.auton = auton;
            this.paths = paths;
            for (String path : paths) {
                try {
                    PathPlannerPath.fromPathFile(path);
                } catch (Exception e) {
                    DriverStationErrors.reportError(
                            "Path \""
                                    + path
                                    + "\" not found. Did you mean \""
                                    + AutonUtil.findClosestMatch(
                                            AutonUtil.getPathFileNames(AutonType.PATHPLANNER), path)
                                    + "\"?",
                            false);
                }
            }
        }

        private PathPlannerAuton buildPathPlannerAuton() {
            return auton.apply(loadPaths(paths));
        }

        public PathPlannerConfig register(LoggedNetworkChooser<Auton> chooser) {
            chooser.add(name, buildPathPlannerAuton());
            return this;
        }

        public PathPlannerConfig register() {
            AutonOpMode.addAuton(name, buildPathPlannerAuton());
            return this;
        }

        public PathPlannerConfig registerDefault(LoggedNetworkChooser<Auton> chooser) {
            chooser.add(name, buildPathPlannerAuton());
            return this;
        }

        public PathPlannerConfig registerDefault() {
            AutonOpMode.addDefaultAuton(name, buildPathPlannerAuton());
            return this;
        }
    }

    /** PATH LOADING ** */
    public static PathPlannerPath[] loadPaths(String... names) {
        PathPlannerPath[] output = new PathPlannerPath[names.length];
        for (int i = 0; i < names.length; i++) {
            output[i] = load(names[i]);
        }
        return output;
    }

    public static PathPlannerPath load(String name) {
        try {
            return PathPlannerPath.fromPathFile(name);
        } catch (Exception e) {
            DriverStationErrors.reportError("Path not found.", false);
            return null;
        }
    }
}
