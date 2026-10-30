/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.swerve;

import static org.wpilib.units.Units.Amps;
import static org.wpilib.units.Units.Radians;
import static org.wpilib.units.Units.RadiansPerSecond;
import static org.wpilib.units.Units.Volts;

import org.wpilib.math.controller.PIDController;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.system.Models;
import org.wpilib.math.util.Units;
import org.wpilib.simulation.DCMotorSim;
import org.wpilib.system.Timer;
import org.wpilib.units.measure.Angle;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.swerve.SwerveModuleConstants;

/**
 * Physics sim implementation of module IO. The sim models are configured using a set of module
 * constants from Phoenix. Simulation is always based on voltage control.
 */
public class ModuleIOSim implements ModuleIO {
    // TunerConstants doesn't support separate sim constants, so they are declared
    // locally
    private static final double DRIVE_KP = 0.05;
    private static final double DRIVE_KD = 0.0;
    private static final double DRIVE_KS = 0.0;
    private static final double DRIVE_KV_ROT =
            0.91035; // Same units as TunerConstants: (volt * secs) / rotation
    private static final double DRIVE_KV = 1.0 / Units.rotationsToRadians(1.0 / DRIVE_KV_ROT);
    private static final double TURN_KP = 8.0;
    private static final double TURN_KD = 0.0;
    private static final DCMotor DRIVE_GEARBOX = DCMotor.getKrakenX60Foc(1);
    private static final DCMotor TURN_GEARBOX = DCMotor.getKrakenX60Foc(1);

    private final DCMotorSim driveSim;
    private final DCMotorSim turnSim;

    private boolean driveClosedLoop = false;
    private boolean turnClosedLoop = false;
    private PIDController driveController = new PIDController(DRIVE_KP, 0, DRIVE_KD);
    private PIDController turnController = new PIDController(TURN_KP, 0, TURN_KD);
    private double driveFFVolts = 0.0;
    private double driveAppliedVolts = 0.0;
    private double turnAppliedVolts = 0.0;

    public ModuleIOSim(
            SwerveModuleConstants<TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>
                    constants) {
        // Create drive and turn sim models
        driveSim =
                new DCMotorSim(
                        Models.singleJointedArmFromPhysicalConstants( // Recommended model by
                                // documentation
                                DRIVE_GEARBOX,
                                constants.DriveInertia,
                                constants.DriveMotorGearRatio),
                        DRIVE_GEARBOX);
        turnSim =
                new DCMotorSim(
                        Models.singleJointedArmFromPhysicalConstants(
                                TURN_GEARBOX,
                                constants.SteerInertia,
                                constants.SteerMotorGearRatio),
                        TURN_GEARBOX);

        // Enable wrapping for turn PID
        turnController.enableContinuousInput(-Math.PI, Math.PI);
    }

    @Override
    public void updateInputs(ModuleIOInputs inputs) {
        // Run closed-loop control
        if (driveClosedLoop) {
            driveAppliedVolts =
                    driveFFVolts + driveController.calculate(driveSim.getAngularVelocity());
        } else {
            driveController.reset();
        }
        if (turnClosedLoop) {
            turnAppliedVolts = turnController.calculate(turnSim.getAngularPosition());
        } else {
            turnController.reset();
        }

        // Update simulation state
        driveSim.setInputVoltage(Math.clamp(driveAppliedVolts, -12.0, 12.0));
        turnSim.setInputVoltage(Math.clamp(turnAppliedVolts, -12.0, 12.0));
        driveSim.update(0.02);
        turnSim.update(0.02);

        // Update drive inputs
        inputs.driveConnected = true;
        inputs.drivePosition = Radians.of(driveSim.getAngularPosition());
        inputs.driveVelocity = RadiansPerSecond.of(driveSim.getAngularVelocity());
        inputs.driveAppliedVolts = Volts.of(driveAppliedVolts);
        inputs.driveCurrentAmps = Amps.of(Math.abs(driveSim.getCurrentDraw()));

        // Update turn inputs
        inputs.turnConnected = true;
        inputs.turnEncoderConnected = true;
        inputs.turnAbsolutePosition = new Rotation2d(turnSim.getAngularPosition());
        inputs.turnPosition = new Rotation2d(turnSim.getAngularPosition());
        inputs.turnVelocityRadPerSec = turnSim.getAngularVelocity();
        inputs.turnAppliedVolts = turnAppliedVolts;
        inputs.turnCurrentAmps = Math.abs(turnSim.getCurrentDraw());

        // Update odometry inputs (50Hz because high-frequency odometry in sim doesn't
        // matter)
        inputs.odometryTimestamps = new double[] {Timer.getMonotonicTimestamp()};
        inputs.odometryDrivePositions = new Angle[] {inputs.drivePosition};
        inputs.odometryTurnPositions = new Rotation2d[] {inputs.turnPosition};
    }

    @Override
    public void applyOutputs(ModuleIOOutputs outputs) {
        switch (outputs.driveOutputMode) {
            case VELOCITY -> {
                driveClosedLoop = true;
                double velocityRadPerSec = outputs.driveTargetVelocity.in(RadiansPerSecond);
                driveFFVolts =
                        DRIVE_KS * Math.signum(velocityRadPerSec) + DRIVE_KV * velocityRadPerSec;
                driveController.setSetpoint(velocityRadPerSec);
            }
            case OPEN_LOOP -> {
                driveClosedLoop = false;
                driveAppliedVolts = outputs.driveTargetOpenLoop;
            }
        }
        switch (outputs.turnOutputMode) {
            case POSITION -> {
                turnClosedLoop = true;
                turnController.setSetpoint(outputs.turnTargetPosition.in(Radians));
            }
            case OPEN_LOOP -> {
                turnClosedLoop = false;
                turnAppliedVolts = outputs.turnTargetOpenLoop;
            }
        }
    }
}
