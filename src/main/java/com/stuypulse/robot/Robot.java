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
import org.wpilib.system.Filesystem;
import org.wpilib.util.Alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
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

        logCommitHash();
        Logger.start();
    }

    private void logCommitHash() {
        Path commitFile = Filesystem.getDeployDirectory().toPath().resolve("commit.json");

        try {
            if (Files.exists(commitFile)) {
                ObjectMapper json = new ObjectMapper();
                JsonNode object = json.readTree(commitFile.toFile());

                String hash = object.path("hash").asText("unknown");
                String message = object.path("message").asText("unknown");
                String description = object.path("description").asText("");

                Logger.recordMetadata("Commit Hash", hash);
                Logger.recordMetadata("Commit Message", message);
                if (!description.isBlank()) {
                    Logger.recordMetadata("Commit Description", description);
                }
            } else {
                Logger.recordMetadata("Commit", "commit.json file missing");
            }
        } catch (Exception e) {
            Logger.recordMetadata("Commit", "JSON parse failed: " + e);
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
