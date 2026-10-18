package com.stuypulse.robot.commands.auton;

import org.wpilib.command3.Command;

/** Interface for disdplaying auton paths on elastic 
 * <p> Default implementions used for do nothing auton and characterization routines</p>
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
