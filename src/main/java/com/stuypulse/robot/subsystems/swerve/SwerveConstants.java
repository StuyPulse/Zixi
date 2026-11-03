/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.swerve;

import static org.wpilib.units.Units.RadiansPerSecond;
import static org.wpilib.units.Units.RadiansPerSecondPerSecond;
import static org.wpilib.units.Units.Seconds;

import org.wpilib.units.measure.AngularAcceleration;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Time;

public interface SwerveConstants {
    public interface Characterization {
        Time FF_START_DELAY = Seconds.of(1);
        double FF_RAMP_RATE = 1;

        AngularVelocity WHEEL_RADIUS_MAX_VELOCITY = RadiansPerSecond.of(0.25);
        AngularAcceleration WHEEL_RADIUS_RAMP_RATE = RadiansPerSecondPerSecond.of(0.05);
    }
}
