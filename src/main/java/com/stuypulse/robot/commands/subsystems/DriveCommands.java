/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.commands.subsystems;

import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.Radians;
import static org.wpilib.units.Units.RadiansPerSecond;
import static org.wpilib.units.Units.RadiansPerSecondPerSecond;
import static org.wpilib.units.Units.Seconds;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.constants.DriverConstants;
import com.stuypulse.robot.subsystems.swerve.Swerve;
import com.stuypulse.robot.subsystems.swerve.SwerveConstants;
import com.stuypulse.robot.util.swerve.DriveInputProcessor;
import com.stuypulse.robot.util.swerve.DriveTurnInputProcessor;

import org.wpilib.command3.*;
import org.wpilib.command3.button.CommandGamepad;
import org.wpilib.math.filter.SlewRateLimiter;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.kinematics.ChassisVelocities;
import org.wpilib.system.Timer;
import org.wpilib.units.measure.AngularVelocity;

import java.util.LinkedList;
import java.util.List;
import org.littletonrobotics.junction.Logger;

public interface DriveCommands {
    public static Command resetPose(Swerve swerve, Pose2d pose) {
        return Command.noRequirements(
                        coroutine -> {
                            swerve.resetPose(pose);
                        })
                .named("Swerve ResetPose");
    }

    public static Command resetHeading(Swerve swerve, Rotation2d heading) {
        return Command.noRequirements(
                        coroutine -> {
                            swerve.resetRotation(heading);
                        })
                .named("Swerve Reset Heading");
    }

    public static Command xMode(Swerve swerve) {
        return swerve.run(coroutine -> swerve.stopWithX()).named("Swerve X Mode");
    }

    /**
     * Field relative drive command using two joysticks (controlling linear and angular velocities).
     */
    public static Command joystickDrive(Swerve swerve, CommandGamepad driver) {
        DriveInputProcessor driveInputProcessor =
                new DriveInputProcessor(
                        driver,
                        DriverConstants.Driver.Drive.DEADBAND,
                        DriverConstants.Driver.Drive.POWER,
                        DriverConstants.DriveConstraints.MAX_VELOCITY,
                        DriverConstants.DriveConstraints.MAX_ACCEL,
                        DriverConstants.Driver.Drive.RC);
        DriveTurnInputProcessor driveTurnInputProcessor =
                new DriveTurnInputProcessor(
                        driver,
                        DriverConstants.Driver.Turn.DEADBAND,
                        DriverConstants.Driver.Turn.POWER,
                        DriverConstants.DriveConstraints.MAX_ANGULAR_VEL,
                        DriverConstants.Driver.Turn.RC);

        return swerve.runRepeatedly(
                        () -> {
                            driveInputProcessor.update();
                            driveTurnInputProcessor.update();
                            // Get linear velocity
                            Translation2d linearVelocity = driveInputProcessor.get();

                            // Get angular velocity
                            AngularVelocity angularVelocity = driveTurnInputProcessor.get();

                            // Convert to field relative speeds & send command
                            ChassisVelocities speeds =
                                    new ChassisVelocities(
                                            linearVelocity.getX(),
                                            linearVelocity.getY(),
                                            angularVelocity.in(RadiansPerSecond));
                            swerve.runVelocity(
                                    speeds.toRobotRelative(
                                            !Robot.isBlue()
                                                    ? swerve.getRotation()
                                                            .plus(new Rotation2d(Math.PI))
                                                    : swerve.getRotation()));
                        })
                .named("Joystick Drive");
    }

    /**
     * Measures the velocity feedforward constants for the drive motors.
     *
     * <p>This command should only be used in voltage control mode.
     */
    public static Command feedforwardCharacterization(Swerve drive) {
        List<Double> velocitySamples = new LinkedList<>();
        List<Double> voltageSamples = new LinkedList<>();
        Timer timer = new Timer();

        Command resetData =
                Command.noRequirements(
                                coroutine -> {
                                    velocitySamples.clear();
                                    voltageSamples.clear();
                                })
                        .named("Reset data");

        Command orientModules =
                drive.runRepeatedly(() -> drive.runCharacterization(0.0))
                        .named("Orient modules")
                        .withTimeout(SwerveConstants.Characterization.FF_START_DELAY);

        Command startTimer =
                Command.noRequirements(coroutine -> timer.restart()).named("Start timer");

        Command accelerateAndGatherData =
                drive.runRepeatedly(
                                () -> {
                                    double voltage =
                                            timer.get()
                                                    * SwerveConstants.Characterization.FF_RAMP_RATE;
                                    drive.runCharacterization(voltage);
                                    velocitySamples.add(drive.getFFCharacterizationVelocity());
                                    voltageSamples.add(voltage);
                                })
                        .whenCanceled(
                                () -> {
                                    int n = velocitySamples.size();
                                    double sumX = 0.0;
                                    double sumY = 0.0;
                                    double sumXY = 0.0;
                                    double sumX2 = 0.0;
                                    for (int i = 0; i < n; i++) {
                                        sumX += velocitySamples.get(i);
                                        sumY += voltageSamples.get(i);
                                        sumXY += velocitySamples.get(i) * voltageSamples.get(i);
                                        sumX2 += velocitySamples.get(i) * velocitySamples.get(i);
                                    }
                                    double kS =
                                            (sumY * sumX2 - sumX * sumXY)
                                                    / (n * sumX2 - sumX * sumX);
                                    double kV =
                                            (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

                                    Logger.recordOutput("Swerve/FF Char/kS", kS);
                                    Logger.recordOutput("Swerve/FF Char/kV", kV);
                                })
                        .named("Accelerate and gather data");

        return resetData
                .andThen(orientModules)
                .andThen(startTimer)
                .andThen(accelerateAndGatherData)
                .named("Swerve FF Characterization");
    }

    /** Measures the robot's wheel radius by spinning in a circle. */
    public static Command wheelRadiusCharacterization(Swerve drive) {
        SlewRateLimiter limiter =
                new SlewRateLimiter(
                        SwerveConstants.Characterization.WHEEL_RADIUS_RAMP_RATE.in(
                                RadiansPerSecondPerSecond));
        WheelRadiusCharacterizationState state = new WheelRadiusCharacterizationState();

        return Command.parallel(
                        // Drive control sequence
                        Command.sequence(
                                        // Reset acceleration limiter
                                        Command.noRequirements(
                                                        coroutine -> {
                                                            limiter.reset(0.0);
                                                        })
                                                .named("Wheel Radius Char Reset Limiter"),

                                        // Turn in place, accelerating up to full speed
                                        drive.runRepeatedly(
                                                        () -> {
                                                            double speed =
                                                                    limiter.calculate(
                                                                            SwerveConstants
                                                                                    .Characterization
                                                                                    .WHEEL_RADIUS_MAX_VELOCITY
                                                                                    .in(
                                                                                            RadiansPerSecond));
                                                            drive.runVelocity(
                                                                    new ChassisVelocities(
                                                                            0.0, 0.0, speed));
                                                        })
                                                .named("Wheel Radius Char Accelerate"))
                                .named("Wheel Radius Char Drive Control Sequence"),

                        // Measurement sequence
                        Command.sequence(
                                        // Wait for modules to fully orient before starting
                                        // measurement
                                        Command.waitFor(Seconds.of(1.0))
                                                .named("Wheel Radius Char Wait For Measurement"),

                                        // Record starting measurement
                                        Command.noRequirements(
                                                        coroutine -> {
                                                            state.positions =
                                                                    drive
                                                                            .getWheelRadiusCharacterizationPositions();
                                                            state.lastAngle = drive.getRotation();
                                                            state.gyroDelta = 0.0;
                                                        })
                                                .named(
                                                        "Wheel Radius Char Record Starting Measurement"),

                                        // Update gyro delta
                                        Command.noRequirements(
                                                        coroutine -> {
                                                            var rotation = drive.getRotation();
                                                            state.gyroDelta +=
                                                                    Math.abs(
                                                                            rotation.minus(
                                                                                            state.lastAngle)
                                                                                    .getRadians());
                                                            state.lastAngle = rotation;
                                                        })

                                                // When cancelled, calculate and print results
                                                .whenCanceled(
                                                        () -> {
                                                            double[] positions =
                                                                    drive
                                                                            .getWheelRadiusCharacterizationPositions();
                                                            double wheelDelta = 0.0;
                                                            for (int i = 0; i < 4; i++) {
                                                                wheelDelta +=
                                                                        Math.abs(
                                                                                        positions[i]
                                                                                                - state.positions[
                                                                                                        i])
                                                                                / 4.0;
                                                            }
                                                            double wheelRadius =
                                                                    (state.gyroDelta
                                                                                    * Swerve
                                                                                            .DRIVE_BASE_RADIUS)
                                                                            / wheelDelta;

                                                            Logger.recordOutput(
                                                                    "Swerve/Wheel Radius Char/Delta",
                                                                    Radians.of(wheelDelta));
                                                            Logger.recordOutput(
                                                                    "Swerve/Wheel Radius Char/Gyro Delta",
                                                                    Radians.of(state.gyroDelta));
                                                            Logger.recordOutput(
                                                                    "Swerve/Wheel Radius Char/Wheel Radius",
                                                                    Meters.of(wheelRadius));
                                                        })
                                                .named("Wheel Radius Char Measure"))
                                .named("Wheel Radius Char Measurement Sequence"))
                .named("Wheel Radius Char");
    }

    class WheelRadiusCharacterizationState {
        double[] positions = new double[4];
        Rotation2d lastAngle = new Rotation2d();
        double gyroDelta = 0.0;
    }
}
