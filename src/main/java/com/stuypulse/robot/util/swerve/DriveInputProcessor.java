/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util.swerve;

import static org.wpilib.units.Units.MetersPerSecond;
import static org.wpilib.units.Units.MetersPerSecondPerSecond;
import static org.wpilib.units.Units.Seconds;

import com.stuypulse.robot.constants.GlobalSettings;

import org.wpilib.command3.button.CommandGamepad;
import org.wpilib.math.filter.LinearFilter;
import org.wpilib.math.filter.SlewRateLimiter;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.util.MathUtil;
import org.wpilib.units.measure.LinearAcceleration;
import org.wpilib.units.measure.LinearVelocity;

/**
 *
 *
 * <h2>DriveInputProcessor</h2>
 *
 * <p>Class for processing driver input for the drivetrain. It's intended to replace StuyLib's
 * VStream and Filters and use pure WPILib and Java.
 *
 * <p>This will be used to be as close as the original StuyLib implementation as possible It will be
 * used for
 *
 * <ul>
 *   <li>Applying deadbands
 *   <li>Applying power curves
 *   <li>Limiting magnitude to 1
 *   <li>Scaling to max velocity
 *   <li>Applying rate limits
 *   <li>Applying a low pass filter
 * </ul>
 *
 * <p>To use within a command, create an instance of this class and call the {@link #update()}
 * method in the start of the Command.
 *
 * <p>To get the processed speed, call the {@link #get()} method.
 *
 * @author Faizaan (https://github.com/Faizaan-J)
 */
public class DriveInputProcessor {
    private final CommandGamepad controller;
    private final double deadband;
    private final double power;

    /** The max velocity in meters per second/ */
    private final LinearVelocity maxVelocity;

    private final SlewRateLimiter xRateLimiter;
    private final SlewRateLimiter yRateLimiter;

    private final LinearFilter xLowPassFilter;
    private final LinearFilter yLowPassFilter;

    /** The speed vector after the full processing and filtering. */
    private Translation2d processedSpeed;

    /**
     *
     *
     * <h4>Constructor for the DriveInputProcessor</h4>
     *
     * <p>Creates a new DriveInputProcessor with the specified parameters and processes the input
     * periodically.
     *
     * @param controller The CommandXboxController to get driver input from
     * @param deadband The deadband to apply to the input (0-1)
     * @param power The power to apply to the input
     * @param maxVelocity The maximum velocity to scale the input to
     * @param maxAcceleration The maximum acceleration to apply to the input
     * @param rc The time constant for the low pass filter (seconds)
     */
    public DriveInputProcessor(
            CommandGamepad controller,
            double deadband,
            double power,
            LinearVelocity maxVelocity,
            LinearAcceleration maxAcceleration,
            double rc) {
        this.controller = controller;
        this.deadband = deadband;
        this.power = power;
        this.maxVelocity = maxVelocity;

        this.xRateLimiter = new SlewRateLimiter(maxAcceleration.in(MetersPerSecondPerSecond));
        this.yRateLimiter = new SlewRateLimiter(maxAcceleration.in(MetersPerSecondPerSecond));
        this.xLowPassFilter = LinearFilter.singlePoleIIR(rc, GlobalSettings.DT.in(Seconds));
        this.yLowPassFilter = LinearFilter.singlePoleIIR(rc, GlobalSettings.DT.in(Seconds));

        this.processedSpeed = Translation2d.ZERO;
    }

    /**
     * Read the raw joystick axes and store them as a velocity vector in {@link #processedSpeed}
     *
     * @return This instance of the class
     */
    private DriveInputProcessor getDriverInputAsVelocity() {
        this.processedSpeed = new Translation2d(-controller.getLeftY(), -controller.getLeftX());
        return this;
    }

    /**
     * Apply a deadband on the X and Y axes to the current {@link #processedSpeed} vector using
     * {@link MathUtil#applyDeadband}
     *
     * @return This instance of the class
     */
    private DriveInputProcessor applyDeadband() {
        double deadbandX = MathUtil.applyDeadband(this.processedSpeed.getX(), deadband);
        double deadbandY = MathUtil.applyDeadband(this.processedSpeed.getY(), deadband);
        this.processedSpeed = new Translation2d(deadbandX, deadbandY);
        return this;
    }

    /**
     * Limits the magnitude of the current {@link #processedSpeed} vector to 1
     *
     * @return This instance of the class
     */
    private DriveInputProcessor applyLimitMagnitudeToOne() {
        double magnitude = this.processedSpeed.getNorm();

        if (magnitude > 1.0) {
            this.processedSpeed = this.processedSpeed.div(magnitude);
        }

        return this;
    }

    /**
     * Apply a power curve to the current {@link #processedSpeed} vector. The vector's magnitude is
     * raised according to the configured power while preserving direction.
     *
     * @return This instance of the class
     */
    private DriveInputProcessor applyPowerCurve() {
        this.processedSpeed =
                this.processedSpeed.times(Math.pow(this.processedSpeed.getNorm(), power - 1));
        return this;
    }

    /**
     * Scale the {@link #processedSpeed} vector to the robot's maximum velocity. Multiplies the
     * current vector by maxVelocity (m/s).
     *
     * @return This instance of the class
     */
    private DriveInputProcessor applyScalingToMaxVelocity() {
        this.processedSpeed = this.processedSpeed.times(maxVelocity.in(MetersPerSecond));
        return this;
    }

    /**
     * Applies a rate limit to {@link #processedSpeed} using a {@link SlewRateLimiter} for each
     * axis. This limits the rate of change of the velocity vector to the max acceleration (m/s^2).
     *
     * @return This instance of the class
     */
    private DriveInputProcessor applyRateLimit() {
        double limitedX = xRateLimiter.calculate(this.processedSpeed.getX());
        double limitedY = yRateLimiter.calculate(this.processedSpeed.getY());
        this.processedSpeed = new Translation2d(limitedX, limitedY);
        return this;
    }

    /**
     * Applies a low pass filter to {@link #processedSpeed} using a {@link LinearFilter} for each
     * axis. This smooths out the velocity vector and reduces noise.
     *
     * @return This instance of the class
     */
    private DriveInputProcessor applyLowPassFilter() {
        double filteredX = xLowPassFilter.calculate(this.processedSpeed.getX());
        double filteredY = yLowPassFilter.calculate(this.processedSpeed.getY());
        this.processedSpeed = new Translation2d(filteredX, filteredY);
        return this;
    }

    /**
     * Update method that processes all filters and updates the filtered speed. This should be
     * called once per cycle in the Command using this processor, before retrieving the processed
     * velocity vector via {@link #get()}.
     */
    public void update() {
        getDriverInputAsVelocity()
                .applyDeadband()
                .applyPowerCurve()
                .applyLimitMagnitudeToOne()
                .applyScalingToMaxVelocity()
                .applyRateLimit()
                .applyLowPassFilter();
    }

    /**
     * Get the processed speed.
     *
     * @return A Translation2d representing the processed speed
     */
    public Translation2d get() {
        return processedSpeed;
    }
}
