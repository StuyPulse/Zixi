/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot;

import com.stuypulse.robot.commands.auton.AutonomousRoutines;
import com.stuypulse.robot.constants.GlobalPorts;

import org.wpilib.command3.Command;
import org.wpilib.command3.button.CommandGamepad;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.tunable.Selectable;

public class RobotContainer {

    // Gamepads
    public final CommandGamepad driver =
            new CommandGamepad(GlobalPorts.Gamepad.DRIVER); // model-agnostic controller
    // public final CommandGamepad operator = new CommandGamepad(GlobalPorts.Gamepad.OPERATOR);

    // Subsystems

    // Autons
    private static Selectable<Command> autonChooser = new Selectable<>();

    // Robot container

    public RobotContainer() {
        configureLogging();
        configureDefaultCommands();
        configureButtonBindings();
        configureAutons();
    }

    /***************/
    /*** LOGGING ***/
    /***************/

    private void configureLogging() {}

    /****************/
    /*** DEFAULTS ***/
    /****************/

    private void configureDefaultCommands() {}

    /***************/
    /*** BUTTONS ***/
    /***************/

    private void configureButtonBindings() {}

    /**************/
    /*** AUTONS ***/
    /**************/

    public void configureAutons() {
        autonChooser.addDefault("Do Nothing", AutonomousRoutines.doNothingAuton());

        Telemetry.log("Autonomous", autonChooser);
    }

    /**
     * Use this to pass the autonomous command to the main {@link Robot} class.
     *
     * @return The command to run in autonomous
     */
    public Command getAutonomousCommand() {
        return autonChooser.getSelected();
    }
}
