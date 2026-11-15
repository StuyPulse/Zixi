/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util.auton;

import com.stuypulse.robot.util.auton.AutonUtil.AutonType;

import com.stuypulse.robot.commands.auton.Auton;
import com.stuypulse.robot.commands.auton.BLineAuton;
import com.stuypulse.robot.opmodes.AutonOpMode;

import org.wpilib.driverstation.DriverStationErrors;

import bline.path.Path;
import java.util.function.Function;
import org.littletonrobotics.junction.networktables.LoggedNetworkChooser;

/**
 * @author Taryn Huang (https://github.com/7arymisu)
 */
public class BLineUtil {
    public static class BLineConfig {

        private final String name;
        private final Function<Path[], BLineAuton> auton;
        private final String[] paths;

        public BLineConfig(String name, Function<Path[], BLineAuton> auton, String... paths) {
            this.name = name;
            this.auton = auton;
            this.paths = paths;

            for (String path : paths) {
                try {
                    new Path(path);
                } catch (Exception e) {
                    DriverStationErrors.reportError(
                            "BLine path \""
                                    + path
                                    + "\" not found. Did you mean \""
                                    + AutonUtil.findClosestMatch(
                                            AutonUtil.getPathFileNames(AutonType.BLINE),
                                            path)
                                    + "\"?",
                            false);
                }
            }
        }

        private BLineAuton buildBLineAuton() {
            return auton.apply(loadPaths(paths)); // pass names straight through, no pre-loading
        }

        public BLineConfig register(LoggedNetworkChooser<Auton> chooser) {
            chooser.add(name, buildBLineAuton());
            return this;
        }

        public BLineConfig register() {
            AutonOpMode.addAuton(name, buildBLineAuton());
            return this;
        }

        public BLineConfig registerDefault(LoggedNetworkChooser<Auton> chooser) {
            chooser.addDefault(name, buildBLineAuton());
            return this;
        }

        public BLineConfig registerDefault() {
            AutonOpMode.addDefaultAuton(name, buildBLineAuton());
            return this;
        }
    }

    /*** PATH LOADING ***/

    public static Path[] loadPaths(String... names) {
        Path[] output = new Path[names.length];
        for (int i = 0; i < names.length; i++) {
            output[i] = load(names[i]);
        }
        return output;
    }

    public static Path load(String name) {
        try {
            return new Path(name);
        } catch (Exception e) {
            DriverStationErrors.reportError("BLine path \"" + name + "\" not found.", false);
            return null;
        }
    }
}
