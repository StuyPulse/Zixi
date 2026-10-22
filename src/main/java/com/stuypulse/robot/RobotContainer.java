/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot;

import com.stuypulse.robot.commands.auton.Auton;
import com.stuypulse.robot.commands.auton.AutonFactory;
import com.stuypulse.robot.constants.Field;
import com.stuypulse.robot.constants.GlobalPorts;

import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;

public class RobotContainer {

    // Gamepads
    public final CommandXboxController driver =
            new CommandXboxController(GlobalPorts.Gamepad.DRIVER); // model-agnostic controller
    // public final CommandGamepad operator = new CommandGamepad(GlobalPorts.Gamepad.OPERATOR);

    // Subsystems

    // Autons
    private SendableChooser<Auton> autonChooser = new SendableChooser<>();

    // Robot container

    public RobotContainer() {
        configureLogging();
        configureDefaultCommands();
        configureButtonBindings();
        configureAutons();

        SmartDashboard.putData("Field", Field.FIELD_2D);
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
        AutonFactory autonFactory = new AutonFactory();

        autonChooser.addOption("Do Nothing", autonFactory.doNothingAuton());
    }

    /**
     * Use this to pass the autonomous command to the main {@link Robot} class.
     *
     * @return The command to run in autonomous
     */
    public Auton getAutonomousCommand() {
        return autonChooser.getSelected();
    }
}
