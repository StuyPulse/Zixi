/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.commands.auton;

import com.stuypulse.robot.RobotContainer;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

/**
 *
 *
 * <h2>Auton </h2>
 *
 * <p>Used for previewing the selected auton's paths in elastic
 *
 * <p>The selected path disappears when a different auton is selected or when the robot is enabled
 *
 * <p>Define an auton in {@link AutonFactory} and add it to the auton chooser as shown in the
 * example auton in {@link RobotContainer}
 *
 * <p>Auton routines without paths like characterization routines can be constructed using the
 * {@link #noPaths} method
 */
public interface Auton {
    /** Display auton path trajectories on elastic */
    public default void displayPaths() {}

    /** Clear auton path trajectories from elastic */
    public default void clearPaths() {}

    /**
     * @return The auton command to be scheduled
     */
    public default Command getCommand() {
        return Commands.sequence();
    }

    /**
     * Used to construct autons with no paths such as sysid characterization routines
     *
     * @param command The command to run in the auton
     */
    public static Auton noPaths(Command command) {
        return new Auton() {
            @Override
            public Command getCommand() {
                return command;
            }
        };
    }
}
