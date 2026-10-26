/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.swerve;

import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.kinematics.SwerveModulePosition;
import org.wpilib.math.kinematics.SwerveModuleVelocity;
import org.wpilib.util.Alert;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.swerve.SwerveModuleConstants;
import com.stuypulse.robot.subsystems.swerve.ModuleIO.DriveOutputMode;
import com.stuypulse.robot.subsystems.swerve.ModuleIO.ModuleIOOutputs;
import com.stuypulse.robot.subsystems.swerve.ModuleIO.TurnOutputMode;

import static org.wpilib.units.Units.Radians;
import static org.wpilib.units.Units.RadiansPerSecond;
import static org.wpilib.units.Units.RotationsPerSecond;

import org.littletonrobotics.junction.Logger;

public class Module {
    private final ModuleIO io;
    private final ModuleIOInputsAutoLogged inputs = new ModuleIOInputsAutoLogged();
    private final ModuleIOOutputs outputs = new ModuleIOOutputs();
    private final int index;
    private final SwerveModuleConstants<
                    TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>
            constants;

    private final Alert driveDisconnectedAlert;
    private final Alert turnDisconnectedAlert;
    private final Alert turnEncoderDisconnectedAlert;
    private SwerveModulePosition[] odometryPositions = new SwerveModulePosition[] {};

    public Module(
            ModuleIO io,
            int index,
            SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>
                    constants) {
        this.io = io;
        this.index = index;
        this.constants = constants;
        driveDisconnectedAlert =
                new Alert(
                        "ModuleDriveDisconnect_" + Integer.toString(index),
                        "Disconnected drive motor on module " + Integer.toString(index) + ".",
                        Alert.Level.HIGH);
        turnDisconnectedAlert =
                new Alert(
                        "ModuleTurnDisconnect_" + Integer.toString(index),
                        "Disconnected turn motor on module " + Integer.toString(index) + ".",
                        Alert.Level.HIGH);
        turnEncoderDisconnectedAlert =
                new Alert(
                        "ModuleEncoderDisconnect_" + Integer.toString(index),
                        "Disconnected turn encoder on module " + Integer.toString(index) + ".",
                        Alert.Level.HIGH);
    }

    public void periodic() {
        io.updateInputs(inputs);
        Logger.processInputs("Drive/Module" + Integer.toString(index), inputs);

        // Calculate positions for odometry
        int sampleCount = inputs.odometryTimestamps.length; // All signals are sampled together
        odometryPositions = new SwerveModulePosition[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double positionMeters = inputs.odometryDrivePositions[i].in(Radians) * constants.WheelRadius;
            Rotation2d angle = inputs.odometryTurnPositions[i];
            odometryPositions[i] = new SwerveModulePosition(positionMeters, angle);
        }

        // Update alerts
        driveDisconnectedAlert.set(!inputs.driveConnected);
        turnDisconnectedAlert.set(!inputs.turnConnected);
        turnEncoderDisconnectedAlert.set(!inputs.turnEncoderConnected);
    }

    /** Runs the module with the specified setpoint state. Mutates the state to optimize it. */
    public void runSetpoint(SwerveModuleVelocity state) {
        // Optimize velocity setpoint
        state = state.optimize(getAngle());
        state = state.cosineScale(inputs.turnPosition);

        // Apply setpoints
        outputs.driveOutputMode = DriveOutputMode.VELOCITY;
        outputs.driveTargetVelocity = RadiansPerSecond.of(state.velocity / constants.WheelRadius);
        outputs.turnOutputMode = TurnOutputMode.POSITION;
        outputs.turnTargetPosition = state.angle.getMeasure();
    }

    /** Runs the module with the specified output while controlling to zero degrees. */
    public void runCharacterization(double output) {
        outputs.driveOutputMode = DriveOutputMode.OPEN_LOOP;
        outputs.driveTargetOpenLoop = output;
        outputs.turnOutputMode = TurnOutputMode.POSITION;
        outputs.turnTargetPosition = Radians.zero();
    }

    /** Disables all outputs to motors. */
    public void stop() {
        outputs.driveOutputMode = DriveOutputMode.OPEN_LOOP;
        outputs.driveTargetOpenLoop = 0.0;
        outputs.turnOutputMode = TurnOutputMode.OPEN_LOOP;
        outputs.turnTargetOpenLoop = 0.0;
    }

    /** Returns the current turn angle of the module. */
    public Rotation2d getAngle() {
        return inputs.turnPosition;
    }

    /** Returns the current drive position of the module in meters. */
    public double getPositionMeters() {
        return inputs.drivePosition.in(Radians) * constants.WheelRadius;
    }

    /** Returns the current drive velocity of the module in meters per second. */
    public double getVelocityMetersPerSec() {
        return inputs.driveVelocity.in(RadiansPerSecond) * constants.WheelRadius;
    }

    /** Returns the module position (turn angle and drive position). */
    public SwerveModulePosition getPosition() {
        return new SwerveModulePosition(getPositionMeters(), getAngle());
    }

    /** Returns the module velocity (turn angle and drive velocity). */
    public SwerveModuleVelocity getVelocity() {
        return new SwerveModuleVelocity(getVelocityMetersPerSec(), getAngle());
    }

    /** Returns the module positions received this cycle. */
    public SwerveModulePosition[] getOdometryPositions() {
        return odometryPositions;
    }

    /** Returns the timestamps of the samples received this cycle. */
    public double[] getOdometryTimestamps() {
        return inputs.odometryTimestamps;
    }

    /** Returns the module position in radians. */
    public double getWheelRadiusCharacterizationPosition() {
        return inputs.drivePosition.in(Radians);
    }

    /** Returns the module velocity in rotations/sec (Phoenix native units). */
    public double getFFCharacterizationVelocity() {
        return inputs.driveVelocity.in(RotationsPerSecond);
    }

    public void periodicAfterScheduler() {
        io.applyOutputs(outputs);
    }
}
