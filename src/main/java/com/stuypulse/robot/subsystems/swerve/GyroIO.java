/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.swerve;

import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.AngularVelocity;

import static org.wpilib.units.Units.Radians;
import static org.wpilib.units.Units.RadiansPerSecond;

import org.littletonrobotics.junction.AutoLog;

public interface GyroIO {
    @AutoLog
    public static class GyroIOInputs {
        public boolean connected = false;
        public Angle yawPosition = Radians.zero();
        public AngularVelocity yawVelocityRadPerSec = RadiansPerSecond.zero();
        public double[] odometryYawTimestamps = new double[] {}; // double is better for timestamps
        public Rotation2d[] odometryYawPositions = new Rotation2d[] {}; 
        // rotation2d expected by swerve & other downstreams, would require too much allocation to be worth it
    }

    public default void updateInputs(GyroIOInputs inputs) {}
}
