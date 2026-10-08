/************************ PROJECT PHIL ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot;

import com.stuypulse.robot.constants.GlobalSettings;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;

import java.util.Optional;
import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;

/**
 * Robot Class
 *
 * <p>This is the main class for robot code, instantiated in {@link com.stuypulse.robot.Main} It
 * extends TimedRobot, meaning that the methods in this class are called automatically during
 * specific states of the robot.
 */
public class Robot extends LoggedRobot {
    /**
     * Checks the alliance the robot is on
     *
     * @return true if the robot is on the blue alliance, false if the robot is on the red alliance,
     *     and false if alliance cannot be determined.
     */
    public static boolean isBlue() {
        final Optional<Alliance> alliance = MatchState.getAlliance();
        if (alliance.isPresent()) {
            return alliance.get() == Alliance.BLUE;
        }
        return false;
    }

    private final RobotContainer robot;
    private final Scheduler defaultScheduler;
    private Command auto;

    public Robot() {
        robot = new RobotContainer();
        defaultScheduler = Scheduler.getDefault();

        switch (GlobalSettings.CURRENT_MODE) {
            case REAL -> {
                // Running on a real robot, log to a USB stick ("/U/logs")
                Logger.addDataReceiver(new WPILOGWriter());
                Logger.addDataReceiver(new NT4Publisher());
            }

            case SIM -> {
                // Running a physics simulator, log to NT
                Logger.addDataReceiver(new NT4Publisher());
            }

            case REPLAY -> {
                // Replaying a log, set up replay source
                setUseTiming(false); // Run as fast as possible
                String logPath = LogFileUtil.findReplayLog();
                Logger.setReplaySource(new WPILOGReader(logPath));
                Logger.addDataReceiver(
                        new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_sim")));
            }
        }
    }

    /*************************/
    /*** ROBOT SCHEDULEING ***/
    /*************************/

    @Override
    public void robotPeriodic() {
        defaultScheduler.run();
    }

    /*********************/
    /*** DISABLED MODE ***/
    /*********************/

    @Override
    public void disabledInit() {}

    @Override
    public void disabledPeriodic() {}

    /***********************/
    /*** AUTONOMOUS MODE ***/
    /***********************/

    @Override
    public void autonomousInit() {
        auto = robot.getAutonomousCommand();

        if (auto != null) {
            defaultScheduler.schedule(auto);
        }
    }

    @Override
    public void autonomousPeriodic() {}

    @Override
    public void autonomousExit() {}

    /*******************/
    /*** TELEOP MODE ***/
    /*******************/

    @Override
    public void teleopInit() {
        if (auto != null) {
            defaultScheduler.cancel(auto);
        }
    }

    @Override
    public void teleopPeriodic() {}

    @Override
    public void teleopExit() {}

    /*************************/
    /*** UTILITY/TEST MODE ***/
    /*************************/

    @Override
    public void utilityInit() {
        defaultScheduler.cancelAll();
    }

    @Override
    public void utilityPeriodic() {}

    @Override
    public void utilityExit() {}
}
