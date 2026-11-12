/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.opmodes.characterization;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.commands.subsystems.DriveCommands;
import com.stuypulse.robot.subsystems.swerve.Swerve;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Utility;

@Utility(
        name = "Swerve FF Characterization",
        group = "Characterize",
        description = "Swerve FeedForward characterization routine made by 6328")
public class SwerveFFCharacterization implements OpMode {
    private final Swerve swerve;

    private final Command command;

    public SwerveFFCharacterization(Robot robot) {
        swerve = robot.swerve;

        command = DriveCommands.feedforwardCharacterization(swerve);
    }

    @Override
    public void start() {
        Scheduler.getDefault().schedule(command);
    }

    @Override
    public void end() {
        Scheduler.getDefault().cancel(command);
    }
}
