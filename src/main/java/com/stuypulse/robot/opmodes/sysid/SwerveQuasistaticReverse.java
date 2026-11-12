/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.opmodes.sysid;

import com.stuypulse.robot.Robot;

import org.wpilib.command3.sysid.SysIdRoutine;
import org.wpilib.command3.sysid.SysIdRoutine.Direction;
import org.wpilib.opmode.Utility;

@Utility(name = "Quasistatic Reverse", group = "SysId")
public class SwerveQuasistaticReverse extends SwerveDriveSysId {
    public SwerveQuasistaticReverse(Robot robot) {
        super(robot.swerve, Direction.REVERSE, SysIdRoutine::quasistatic);
    }
}
