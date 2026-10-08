package com.stuypulse.robot.commands.auton;

import org.wpilib.command3.Command;

/**Class that stores all auton commands */
public class AutonFactory {
    /**Add robot subsystems into this constructor for easy access when making autons */
    public AutonFactory() {}

    public Command doNothingAuton() {
        return Command.sequence(
            // Does nothing
        ).named("Do Nothing Auton");
    }
}
