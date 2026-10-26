/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.swerve;

import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Current;
import org.wpilib.units.measure.Voltage;

import com.ctre.phoenix6.swerve.SwerveModuleConstants;

import static org.wpilib.units.Units.Amps;
import static org.wpilib.units.Units.Radians;
import static org.wpilib.units.Units.RotationsPerSecond;
import static org.wpilib.units.Units.Volts;

import org.littletonrobotics.junction.AutoLog;
import org.littletonrobotics.junction.AutoLogOutput;

public interface ModuleIO {
    @AutoLog
    public static class ModuleIOInputs {
        public boolean driveConnected = false;
        public Angle drivePosition = Radians.zero();
        public AngularVelocity driveVelocity = RotationsPerSecond.zero();
        public Voltage driveAppliedVolts = Volts.zero();
        public Current driveCurrentAmps = Amps.zero();

        public boolean turnConnected = false;
        public boolean turnEncoderConnected = false;
        public Rotation2d turnAbsolutePosition = Rotation2d.ZERO;
        public Rotation2d turnPosition = Rotation2d.ZERO;
        public double turnVelocityRadPerSec = 0.0;
        public double turnAppliedVolts = 0.0;
        public double turnCurrentAmps = 0.0;

        public double[] odometryTimestamps = new double[] {};
        public Angle[] odometryDrivePositions = new Angle[] {};
        public Rotation2d[] odometryTurnPositions = new Rotation2d[] {};
    }

    /** Updates the set of loggable inputs. */
    public default void updateInputs(ModuleIOInputs inputs) {}

    enum DriveOutputMode {
        VELOCITY,
        OPEN_LOOP
    }

    enum TurnOutputMode {
        POSITION,
        OPEN_LOOP
    }

    class ModuleIOOutputs {
        DriveOutputMode driveOutputMode = DriveOutputMode.VELOCITY;
        /**
         * Either amps or volts, depending on {@link SwerveModuleConstants#DriveMotorClosedLoopOutput}
         */
        double driveTargetOpenLoop = 0.0;
        AngularVelocity driveTargetVelocity = RotationsPerSecond.zero();

        TurnOutputMode turnOutputMode = TurnOutputMode.POSITION;
        /**
         * Either amps or volts, depending on {@link SwerveModuleConstants#DriveMotorClosedLoopOutput}
         */
        double turnTargetOpenLoop = 0.0;
        Angle turnTargetPosition = Radians.zero();
    }

    public default void applyOutputs(ModuleIOOutputs outputs) {}
}
