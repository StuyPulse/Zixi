package com.stuypulse.robot.commands.auton;

import org.wpilib.command3.Command;

/**
 *
 *
 * <h2>Auton </h2>
 *
 * <p> Used for previewing the selected auton's paths in elastic
 *
 * <p>The selected path disappears when a different auton is selected or when the robot is enabled
 *
 * <p>Define an auton in {@link AutonFactory} and add it to the auton chooser as shown in the
 * example auton in {@link RobotContainer}
 */
public interface Auton {
    /** Display auton path trajectories on elastic */
    public default void displayPaths() {}

    /** Clear auton path trajectories from elastic */
    public default void clearPaths() {}

    /** @return The auton command to be scheduled */
    public default Command getCommand() {
        return Command.sequence().named("Do Nothing");
    }

    /** Used to construct characterization routines like sysid as autons */
    public static Auton characterization(Command command) {
        return new Auton() {
            @Override
            public Command getCommand() {
                return command;
            }
        };
    }
}
