/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot;

import com.stuypulse.robot.commands.auton.Auton;
import com.stuypulse.robot.commands.auton.AutonFactory;
import com.stuypulse.robot.commands.subsystems.DriveCommands;
import com.stuypulse.robot.constants.DriverConstants;
import com.stuypulse.robot.constants.FieldConstants;
import com.stuypulse.robot.constants.GlobalSettings;
import com.stuypulse.robot.generated.BuildConstants;
import com.stuypulse.robot.subsystems.swerve.GyroIO;
import com.stuypulse.robot.subsystems.swerve.GyroIOPigeon2;
import com.stuypulse.robot.subsystems.swerve.ModuleIO;
import com.stuypulse.robot.subsystems.swerve.ModuleIOSim;
import com.stuypulse.robot.subsystems.swerve.ModuleIOTalonFX;
import com.stuypulse.robot.subsystems.swerve.Swerve;
import com.stuypulse.robot.subsystems.swerve.TunerConstants;
import com.stuypulse.robot.subsystems.vision.Vision;
import com.stuypulse.robot.subsystems.vision.VisionConstants;
import com.stuypulse.robot.subsystems.vision.VisionConstants.Camera;
import com.stuypulse.robot.subsystems.vision.VisionConstants.VisionMode;
import com.stuypulse.robot.subsystems.vision.VisionIO;
import com.stuypulse.robot.subsystems.vision.VisionIOLimelight;
import com.stuypulse.robot.subsystems.vision.VisionIOPhoton;
import com.stuypulse.robot.subsystems.vision.VisionIOPhotonSim;
import com.stuypulse.robot.util.PeriodicSubsystem;

import org.wpilib.command3.Scheduler;
import org.wpilib.command3.button.CommandGamepad;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.tunable.Tunables;
import org.wpilib.util.Alert;

import java.util.EnumMap;
import java.util.Optional;
import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedOpModeRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.LoggedNetworkChooser;
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
public class Robot extends LoggedOpModeRobot {
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

    // Gamepads
    public final CommandGamepad driver =
            new CommandGamepad(DriverConstants.Driver.DRIVER_INDEX); // model-agnostic controller
    public final Swerve swerve;
    public final Vision vision;
    private final LoggedNetworkChooser<Auton> autonChooser = new LoggedNetworkChooser<>("Auton");
    public final Scheduler defaultScheduler;

    public Robot() {
        this.defaultScheduler = Scheduler.getDefault();
        configureLogging();

        EnumMap<Camera, VisionIO> visionIOMap = new EnumMap<>(Camera.class);
        switch (GlobalSettings.CURRENT_MODE) {
            case REAL -> {
                swerve =
                        new Swerve(
                                new GyroIOPigeon2(),
                                new ModuleIOTalonFX(TunerConstants.FrontLeft),
                                new ModuleIOTalonFX(TunerConstants.FrontRight),
                                new ModuleIOTalonFX(TunerConstants.BackLeft),
                                new ModuleIOTalonFX(TunerConstants.BackRight));
                for (Camera camera : VisionConstants.Camera.values()) {
                    visionIOMap.put(
                            camera,
                            VisionConstants.VISION_MODE == VisionMode.LIMELIGHT
                                    ? new VisionIOLimelight(
                                            camera.getName(),
                                            camera.getRobotToCamera(),
                                            swerve::getRotation)
                                    : new VisionIOPhoton(
                                            camera.getName(), camera.getRobotToCamera()));
                }
            }

            case SIM -> {
                swerve =
                        new Swerve(
                                new GyroIO() {},
                                new ModuleIOSim(TunerConstants.FrontLeft),
                                new ModuleIOSim(TunerConstants.FrontRight),
                                new ModuleIOSim(TunerConstants.BackLeft),
                                new ModuleIOSim(TunerConstants.BackRight));
                for (Camera camera : Camera.values()) {
                    visionIOMap.put(
                            camera,
                            new VisionIOPhotonSim(
                                    camera.getName(), camera.getRobotToCamera(), swerve::getPose));
                }
            }

            default -> {
                swerve =
                        new Swerve(
                                new GyroIO() {},
                                new ModuleIO() {},
                                new ModuleIO() {},
                                new ModuleIO() {},
                                new ModuleIO() {});
                for (Camera camera : Camera.values()) {
                    visionIOMap.put(camera, new VisionIO() {});
                }
            }
        }
        vision = new Vision(swerve.visionConsumer, visionIOMap);

        configureDefaultCommands();
        configureButtonBindings();
        configureAutons();
    }

    /*********************/
    /*** CONFIGURATION ***/
    /*********************/

    private void configureLogging() {
        // Git metadata
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

        // Log setup
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
                // setUseTiming(false); // Run as fast as possible
                String logPath = LogFileUtil.findReplayLog();
                Logger.setReplaySource(new WPILOGReader(logPath));
                Logger.addDataReceiver(
                        new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_replay")));
            }
        }

        Logger.start();
        Tunables.publish("Field", FieldConstants.FIELD_2D);
    }

    private void configureDefaultCommands() {
        swerve.setDefaultCommand(DriveCommands.joystickDrive(swerve, driver));
    }

    private void configureButtonBindings() {
        driver.dpadUp().onTrue(DriveCommands.resetHeading(swerve, Rotation2d.ZERO));
    }

    private void configureAutons() {
        AutonFactory autonFactory = new AutonFactory();

        autonChooser.addDefault("Do Nothing", autonFactory.doNothingAuton());
    }

    /************************/
    /*** ROBOT SCHEDULING ***/
    /************************/

    @Override
    public void robotPeriodic() {
        defaultScheduler.run();
        PeriodicSubsystem.runAllPeriodicAfterScheduler();
    }
}
