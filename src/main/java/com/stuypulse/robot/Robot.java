/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot;

import com.stuypulse.robot.commands.auton.Auton;
import com.stuypulse.robot.constants.GlobalSettings;

import org.wpilib.command3.Scheduler;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;
import org.wpilib.util.Alert;

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
    private static final Alert allianceMissing =
            new Alert(
                    "MissingAllianceWarning",
                    "Alliance optional has no value present",
                    Alert.Level.MEDIUM);

    /**
     * Checks the alliance the robot is on
     *
     * @return true if the robot is on the blue alliance, false if the robot is on the red alliance,
     *     and false if alliance cannot be determined.
     */
    public static boolean isBlue() {
        final Optional<Alliance> alliance = MatchState.getAlliance();
        if (alliance.isPresent()) {
            allianceMissing.set(false);
            return alliance.get() == Alliance.BLUE;
        } else {
            allianceMissing.set(true);
        }
        return false;
    }

    private final RobotContainer robot;
    private final Scheduler defaultScheduler;
    private Auton auto;

    public Robot() {
        robot = new RobotContainer();
        defaultScheduler = Scheduler.getDefault();

        logGitInfo();
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
                        new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_replay")));
            }
        }

        Logger.start();
    }

    private void logGitInfo() {
        Logger.recordMetadata("ProjectName", BuildConstants.MAVEN_NAME);
        Logger.recordMetadata("BuildDate", BuildConstants.BUILD_DATE);
        Logger.recordMetadata("GitCommitMessage", BuildConstants.GIT_MESSAGE);
        if (!BuildConstants.GIT_LINK.isBlank()) {
            Logger.recordMetadata("GitCommitLink", BuildConstants.GIT_LINK);
        }
        Logger.recordMetadata(
                "GitPushStatus",
                switch (BuildConstants.GIT_PUSH_STATUS) {
                    case "PUSHED" -> "Commit pushed to remote repository";
                    case "NOT_PUSHED" -> "Local commit not pushed to remote repository";
                    case "STALE" -> "Could not verify push status. Cached information may be stale";
                    default -> "Unable to determine commit push status";
                });
        Logger.recordMetadata("GitSHA", BuildConstants.GIT_SHA);
        Logger.recordMetadata("GitDate", BuildConstants.GIT_DATE);
        Logger.recordMetadata("GitBranch", BuildConstants.GIT_BRANCH);
        Logger.recordMetadata(
                "GitDirty",
                switch (BuildConstants.DIRTY) {
                    case 0 -> "All changes committed";
                    case 1 -> "Uncommitted changes";
                    default -> "Unknown";
                });
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
    public void disabledPeriodic() {
        if (auto != robot.getAutonomousCommand() && auto != null) {
            auto.clearPaths();

            auto = robot.getAutonomousCommand();

            auto.displayPaths();
        }

        if (auto == null) {
            auto = robot.getAutonomousCommand();
            if (auto != null) {
                auto.displayPaths();
            }
        }
    }

    /***********************/
    /*** AUTONOMOUS MODE ***/
    /***********************/

    @Override
    public void autonomousInit() {
        if (auto != null) {
            auto.clearPaths();
        }

        auto = robot.getAutonomousCommand();

        if (auto != null) {
            defaultScheduler.schedule(auto.getCommand());
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
            auto.clearPaths();
            defaultScheduler.cancel(auto.getCommand());
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
