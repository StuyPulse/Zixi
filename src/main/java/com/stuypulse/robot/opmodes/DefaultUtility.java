/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.opmodes;

import com.stuypulse.robot.Robot;

import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Utility;

@Utility(name = "Default Utility", description = "Does nothing")
public final class DefaultUtility implements OpMode {
    private final Robot robot;

    public DefaultUtility(Robot robot) {
        this.robot = robot;
    }

    @Override
    public void start() {
        robot.defaultScheduler.cancelAll();
    }
}
