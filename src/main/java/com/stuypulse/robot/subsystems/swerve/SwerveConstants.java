/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.swerve;

import static org.wpilib.units.Units.*;

import org.wpilib.units.*;
import org.wpilib.units.measure.*;

public interface SwerveConstants {
    public interface Characterization {
        Time FF_START_DELAY = Seconds.of(1);
        double FF_RAMP_RATE = 1;

        AngularVelocity WHEEL_RADIUS_MAX_VELOCITY = RadiansPerSecond.of(0.25);
        AngularAcceleration WHEEL_RADIUS_RAMP_RATE = RadiansPerSecondPerSecond.of(0.05);
    }

    public interface SwerveCharacterization {
        Time FF_START_DELAY = Seconds.of(2.0);
        double FF_RAMP_RATE = 0.1;

        double WHEEL_RADIUS_MAX_VELOCITY = 0.25;
        double WHEEL_RADIUS_RAMP_RATE = 0.05;

        Time SYSID_REST = Seconds.of(1.5);

        public interface DriveSysId {
            Velocity<VoltageUnit> RAMP_RATE = Volts.of(1).per(Second);
            Voltage STEP_VOLTAGE = Volts.of(4);
            Time QUASISTATIC_TIMEOUT = Seconds.of(5);
            Time DYNAMIC_TIMEOUT = Seconds.of(2);
        }

        public interface SteerSysId {
            Velocity<VoltageUnit> RAMP_RATE = Volts.of(1).per(Second);
            Voltage STEP_VOLTAGE = Volts.of(7);
            Time QUASISTATIC_TIMEOUT = Seconds.of(6);
            Time DYNAMIC_TIMEOUT = Seconds.of(2);
        }
    }
}
