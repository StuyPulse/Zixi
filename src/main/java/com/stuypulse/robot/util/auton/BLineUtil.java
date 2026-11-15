/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util.auton;

import com.stuypulse.robot.commands.auton.Auton;
import com.stuypulse.robot.commands.auton.BLineAuton;
import com.stuypulse.robot.opmodes.AutonOpMode;
import com.stuypulse.robot.util.auton.AutonUtil.AutonType;

import org.wpilib.driverstation.DriverStationErrors;

import bline.path.Path;
import java.util.function.Function;
import org.littletonrobotics.junction.networktables.LoggedNetworkChooser;

/**
 *
 *
 * <h2>BLine Utility</h2>
 *
 * <p>Loads BLine path (.json) files from the deploy directory and builds named autonomous routines
 * from those paths.
 *
 * <h3>Usage</h3>
 *
 * <p>For example, inside {@link com.stuypulse.robot.Robot#configureAutons()}, you can create a
 * {@link BLineConfig}, pass in the auton name, factory method, and path names, then register it
 * with {@link AutonOpMode} via {@link BLineConfig#register()}.
 *
 * <pre>{@code
 * AutonFactory factory = new AutonFactory(swerve);
 *
 * BLineConfig scoreAutonConfig = new BLineConfig(
 *                 "Score",
 *                 factory::score,
 *                 "Score Path");
 *
 * scoreAutonConfig.register();
 * }</pre>
 *
 * <p>The factory method receives the loaded paths in the order listed and returns a {@link
 * BLineAuton}. Use {@link BLineConfig#register()} for a regular option, or {@link
 * BLineConfig#registerDefault()} for the chooser's default option. A config can also register
 * directly with a {@link LoggedNetworkChooser} when a custom chooser is used. The methods without a
 * chooser register the routine with {@link AutonOpMode}.
 *
 * <p>Path names are checked when the configuration is created. Missing paths are reported to the
 * Driver Station with a suggested closest match when one is available.
 *
 * @author Taryn Huang (https://github.com/7arymisu)
 */
public final class BLineUtil {
    private BLineUtil() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated");
    }

    /**
     *
     *
     * <h2>BLine Configuration</h2>
     *
     * <p>Associates an autonomous routine name and one or more BLine path names with a factory
     * method that creates the corresponding {@link BLineAuton}. Create one configuration for each
     * routine, then call one of the {@code register} methods to add it to an autonomous chooser.
     */
    public static class BLineConfig {

        private final String name;

        private final Function<Path[], BLineAuton> auton;

        private final String[] paths;

        /**
         *
         *
         * <h4>Constructor for a BLineConfig</h4>
         *
         * <p>Stores the routine name, path names, and factory. Checks if every path exists and
         * reports missing paths and suggested closest matches to the Driver Station.
         *
         * @param name The name to display for this autonomous routine
         * @param auton Factory that creates the routine from the loaded paths
         * @param paths BLine path names used by the routine, in the order expected by the factory
         */
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
                                            AutonUtil.getPathFileNames(AutonType.BLINE), path)
                                    + "\"?",
                            false);
                }
            }
        }

        /**
         * Loads the configured paths and passes them to the routine factory.
         *
         * @return The autonomous routine created from the configured paths
         */
        private BLineAuton buildBLineAuton() {
            return auton.apply(loadPaths(paths)); // pass names straight through, no pre-loading
        }

        /**
         * Adds this routine to a {@link LoggedNetworkChooser} under the configured name.
         *
         * @param chooser The chooser that will display this routine
         * @return This instance of the class, for method chaining
         */
        public BLineConfig register(LoggedNetworkChooser<Auton> chooser) {
            chooser.add(name, buildBLineAuton());
            return this;
        }

        /**
         * Registers this routine with {@link AutonOpMode}'s internal chooser.
         *
         * @return This instance of the class, for method chaining
         */
        public BLineConfig register() {
            AutonOpMode.addAuton(name, buildBLineAuton());
            return this;
        }

        /**
         * Adds this routine as the default option in a {@link LoggedNetworkChooser}.
         *
         * @param chooser The chooser that will display this routine
         * @return This instance of the class, for method chaining
         */
        public BLineConfig registerDefault(LoggedNetworkChooser<Auton> chooser) {
            chooser.addDefault(name, buildBLineAuton());
            return this;
        }

        /**
         * Registers this routine as the default with {@link AutonOpMode}'s internal chooser.
         *
         * @return This instance of the class, for method chaining
         */
        public BLineConfig registerDefault() {
            AutonOpMode.addDefaultAuton(name, buildBLineAuton());
            return this;
        }
    }

    // PATH LOADING METHODS

    /**
     * Loads multiple BLine paths in the order given. Each name is passed to {@link #load(String)}.
     *
     * <p>If a path cannot be loaded or found, an error is reported to the Driver Station, and null
     * is returned in the corresponding position.
     *
     * @param names BLine path names to load
     * @return The corresponding paths where entries that fail to load are {@code null}
     */
    public static Path[] loadPaths(String... names) {
        Path[] output = new Path[names.length];
        for (int i = 0; i < names.length; i++) {
            output[i] = load(names[i]);
        }
        return output;
    }

    /**
     * Loads a BLine path by name.
     *
     * <p>If the path cannot be loaded or found, an error is reported to the Driver Station, and
     * null is returned.
     *
     * @param name BLine path name
     * @return The loaded path, or {@code null} if not found
     */
    public static Path load(String name) {
        try {
            return new Path(name);
        } catch (Exception e) {
            DriverStationErrors.reportError("BLine path \"" + name + "\" not found.", false);
            return null;
        }
    }
}
