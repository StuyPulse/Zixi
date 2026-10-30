/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot;

import com.stuypulse.robot.commands.auton.Auton;
import com.stuypulse.robot.commands.auton.AutonFactory;
import com.stuypulse.robot.constants.FieldConstants;
import com.stuypulse.robot.constants.GlobalPorts;
import com.stuypulse.robot.constants.GlobalSettings;
import com.stuypulse.robot.generated.TunerConstants;
import com.stuypulse.robot.subsystems.swerve.GyroIO;
import com.stuypulse.robot.subsystems.swerve.GyroIOPigeon2;
import com.stuypulse.robot.subsystems.swerve.ModuleIO;
import com.stuypulse.robot.subsystems.swerve.ModuleIOSim;
import com.stuypulse.robot.subsystems.swerve.ModuleIOTalonFX;
import com.stuypulse.robot.subsystems.swerve.Swerve;
import com.stuypulse.robot.subsystems.vision.Vision;
import com.stuypulse.robot.subsystems.vision.VisionConstants;
import com.stuypulse.robot.subsystems.vision.VisionConstants.Camera;
import com.stuypulse.robot.subsystems.vision.VisionConstants.VisionMode;
import com.stuypulse.robot.subsystems.vision.VisionIO;
import com.stuypulse.robot.subsystems.vision.VisionIOLimelight;
import com.stuypulse.robot.subsystems.vision.VisionIOPhotonVision;
import com.stuypulse.robot.subsystems.vision.VisionIOPhotonVisionSim;

import org.wpilib.command3.button.CommandGamepad;
import org.wpilib.tunable.Tunables;

import java.util.EnumMap;
import org.littletonrobotics.junction.networktables.LoggedNetworkChooser;

public class RobotContainer {

    // Gamepads
    public final CommandGamepad driver =
            new CommandGamepad(GlobalPorts.Gamepad.DRIVER); // model-agnostic controller
    // public final CommandGamepad operator = new CommandGamepad(GlobalPorts.Gamepad.OPERATOR);

    // Subsystems
    private final Swerve swerve;
    private final Vision vision;
    // Autons
    private LoggedNetworkChooser<Auton> autonChooser = new LoggedNetworkChooser<>("Auton");

    // Robot container

    public RobotContainer() {
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
                                            camera.name(),
                                            camera.getRobotToCamera(),
                                            swerve::getRotation)
                                    : new VisionIOPhotonVision(
                                            camera.name(), camera.getRobotToCamera()));
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
                            new VisionIOPhotonVisionSim(
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

        configureLogging();
        configureDefaultCommands();
        configureButtonBindings();
        configureAutons();

        Tunables.publish("Field", FieldConstants.FIELD_2D);
    }

    /***************/
    /*** LOGGING ***/
    /***************/

    private void configureLogging() {}

    /****************/
    /*** DEFAULTS ***/
    /****************/

    private void configureDefaultCommands() {}

    /***************/
    /*** BUTTONS ***/
    /***************/

    private void configureButtonBindings() {}

    /**************/
    /*** AUTONS ***/
    /**************/

    public void configureAutons() {
        AutonFactory autonFactory = new AutonFactory();

        autonChooser.addDefault("Do Nothing", autonFactory.doNothingAuton());
    }

    /**
     * Use this to pass the autonomous command to the main {@link Robot} class.
     *
     * @return The command to run in autonomous
     */
    public Auton getAutonomousCommand() {
        return autonChooser.getSelected();
    }
}
