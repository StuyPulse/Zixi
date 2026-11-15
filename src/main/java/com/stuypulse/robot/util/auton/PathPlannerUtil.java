/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util.auton;

import com.stuypulse.robot.commands.auton.Auton;
import com.stuypulse.robot.commands.auton.PathPlannerAuton;
import com.stuypulse.robot.opmodes.AutonOpMode;
import com.stuypulse.robot.util.auton.AutonUtil.AutonType;

import org.wpilib.driverstation.DriverStationErrors;

import com.pathplanner.lib.path.PathPlannerPath;
import java.util.function.Function;
import org.littletonrobotics.junction.networktables.LoggedNetworkChooser;

/**
 *
 *
 * <h2>PathPlanner Utility</h2>
 *
 * <p>Loads PathPlanner path (.path) files from the deploy directory and builds named autonomous
 * routines from those paths.
 *
 * <h3>Usage</h3>
 *
 * <p>For example, inside {@link com.stuypulse.robot.Robot#configureAutons()}, you can create a
 * {@link PathPlannerConfig}, pass in the auton name, factory method, and path names, then register
 * it with {@link AutonOpMode} via {@link PathPlannerConfig#register()}.
 *
 * <pre>{@code
 * AutonFactory factory = new AutonFactory(swerve);
 *
 * PathPlannerConfig scoreAutonConfig = new PathPlannerConfig(
 *                 "Score",
 *                 factory::score,
 *                 "Score Path");
 *
 * scoreAutonConfig.register();
 * }</pre>
 *
 * <p>The factory method receives the loaded paths in the order listed and returns a {@link
 * PathPlannerAuton}. Use {@link PathPlannerConfig#register()} for a regular option, or {@link
 * PathPlannerConfig#registerDefault()} for the chooser's default option. A config can also register
 * directly with a {@link LoggedNetworkChooser} when a custom chooser is used. The methods without a
 * chooser register the routine with {@link AutonOpMode}.
 *
 * <p>Path names are checked when the configuration is created. Missing paths are reported to the
 * Driver Station with a suggested closest match when one is available.
 */
public final class PathPlannerUtil {
    private PathPlannerUtil() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated");
    }

    /**
     *
     *
     * <h2>PathPlanner Configuration</h2>
     *
     * <p>Associates an autonomous routine name and one or more PathPlanner path names with a
     * factory method that creates the corresponding {@link PathPlannerAuton}. Create one
     * configuration for each routine, then call one of the {@code register} methods to add it to an
     * autonomous chooser.
     */
    public static class PathPlannerConfig {

        private final String name;

        private final Function<PathPlannerPath[], PathPlannerAuton> auton;

        private final String[] paths;

        /**
         *
         *
         * <h4>Constructor for a PathPlannerConfig</h4>
         *
         * <p>Stores the routine name, path names, and factory. Checks if every path exists and
         * reports missing paths and suggested closest matches to the Driver Station.
         *
         * @param name The name to display for this autonomous routine
         * @param auton Factory that creates the routine from the loaded paths
         * @param paths PathPlanner path names used by the routine, in the order expected by the
         *     factory
         */
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

        /**
         * Loads the configured paths and passes them to the routine factory.
         *
         * @return The autonomous routine created from the configured paths
         */
        private PathPlannerAuton buildPathPlannerAuton() {
            return auton.apply(loadPaths(paths));
        }

        /**
         * Adds this routine to a {@link LoggedNetworkChooser} under the configured name.
         *
         * @param chooser The chooser that will display this routine
         * @return This instance of the class, for method chaining
         */
        public PathPlannerConfig register(LoggedNetworkChooser<Auton> chooser) {
            chooser.add(name, buildPathPlannerAuton());
            return this;
        }

        /**
         * Registers this routine with {@link AutonOpMode}'s internal chooser.
         *
         * @return This instance of the class, for method chaining
         */
        public PathPlannerConfig register() {
            AutonOpMode.addAuton(name, buildPathPlannerAuton());
            return this;
        }

        /**
         * Adds this routine to a {@link LoggedNetworkChooser} as its default option.
         *
         * @param chooser The chooser that will display this routine.
         * @return This instance of the class, for method chaining
         */
        public PathPlannerConfig registerDefault(LoggedNetworkChooser<Auton> chooser) {
            chooser.addDefault(name, buildPathPlannerAuton());
            return this;
        }

        /**
         * Registers this routine as the default with {@link AutonOpMode}'s internal chooser.
         *
         * @return This instance of the class, for method chaining
         */
        public PathPlannerConfig registerDefault() {
            AutonOpMode.addDefaultAuton(name, buildPathPlannerAuton());
            return this;
        }
    }

    // PATH LOADING METHODS

    /**
     * Loads multiple PathPlanner paths in the order given. Each name is passed to {@link
     * #load(String)}.
     *
     * <p>If a path cannot be loaded or found, an error is reported to the Driver Station, and null
     * is returned in the corresponding position.
     *
     * @param names PathPlanner path names to load
     * @return The corresponding paths where entries that fail to load are {@code null}
     */
    public static PathPlannerPath[] loadPaths(String... names) {
        PathPlannerPath[] output = new PathPlannerPath[names.length];
        for (int i = 0; i < names.length; i++) {
            output[i] = load(names[i]);
        }
        return output;
    }

    /**
     * Loads a PathPlanner path by name.
     *
     * <p>If the path cannot be loaded or found, an error is reported to the Driver Station, and
     * null is returned.
     *
     * @param name PathPlanner path name
     * @return The loaded path, or {@code null} if not found
     */
    public static PathPlannerPath load(String name) {
        try {
            return PathPlannerPath.fromPathFile(name);
        } catch (Exception e) {
            DriverStationErrors.reportError("Path not found.", false);
            return null;
        }
    }
}
