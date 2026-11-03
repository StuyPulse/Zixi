/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.constants;

import static org.wpilib.units.Units.DegreesPerSecond;
import static org.wpilib.units.Units.MetersPerSecond;
import static org.wpilib.units.Units.MetersPerSecondPerSecond;

import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.LinearAcceleration;
import org.wpilib.units.measure.LinearVelocity;

public interface DriverConstants {

    public interface Driver {
        int DRIVER_INDEX = 0;
        double BUZZ_TIME = 1.0;
        double BUZZ_INTENSITY = 1.0;

        public interface Drive {
            double DEADBAND = 0.05;
            double RC = 0.05;
            int POWER = 2;
        }

        public interface Turn {
            double DEADBAND = 0.05;
            double RC = 0.05;
            int POWER = 2;
        }
    }

    public interface DriveConstraints {
        LinearVelocity MAX_VELOCITY = MetersPerSecond.of(4.16);

        AngularVelocity MAX_ANGULAR_VEL = DegreesPerSecond.of(300.0);

        LinearAcceleration MAX_ACCEL = MetersPerSecondPerSecond.of(15.0);
    }
}
