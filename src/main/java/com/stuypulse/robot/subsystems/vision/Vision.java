/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.vision;

import com.stuypulse.robot.constants.FieldConstants;
import com.stuypulse.robot.constants.GlobalSettings;
import com.stuypulse.robot.subsystems.vision.VisionConstants.Camera;
import com.stuypulse.robot.subsystems.vision.VisionConstants.CameraData;
import com.stuypulse.robot.subsystems.vision.VisionConstants.Pipeline;
import com.stuypulse.robot.subsystems.vision.VisionConstants.VisionSettings;
import com.stuypulse.robot.subsystems.vision.VisionIO.MegaTagMode;
import com.stuypulse.robot.subsystems.vision.VisionIO.PoseObservation;
import com.stuypulse.robot.subsystems.vision.VisionIO.PoseObservationType;
import com.stuypulse.robot.subsystems.vision.VisionIO.VisionIOOutputs;
import com.stuypulse.robot.util.PeriodicSubsystem;

import org.wpilib.command3.Command;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.system.Timer;
import org.wpilib.util.Alert;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.littletonrobotics.junction.Logger;

public class Vision extends PeriodicSubsystem {
    private final VisionConsumer consumer;
    private final EnumMap<Camera, VisionIO> io;
    private final EnumMap<Camera, VisionIOInputsAutoLogged> inputs;
    private final EnumMap<Camera, VisionIOOutputs> outputs;
    private final EnumMap<Camera, Alert> disconnectedAlerts;

    private final Timer hdrTimer;

    public Vision(VisionConsumer consumer, EnumMap<Camera, VisionIO> cameraIOMap) {
        this.consumer = consumer;

        this.io = new EnumMap<>(cameraIOMap);

        // Initialize inputs
        this.inputs = new EnumMap<>(Camera.class);
        this.outputs = new EnumMap<>(Camera.class);

        // Initialize disconnected alerts
        this.disconnectedAlerts = new EnumMap<>(Camera.class);

        this.hdrTimer = new Timer();
        for (Camera camera: io.keySet()) { 
            inputs.put(camera, new VisionIOInputsAutoLogged());
            outputs.put(camera, new VisionIOOutputs());
            disconnectedAlerts.put(
                    camera, 
                    new Alert(
                            "Vision " + camera.getName(),
                            "Vision camera " + camera.getName() + " is disconnected.",
                            Alert.Level.MEDIUM));
        }

        hdrTimer.start();
    }

    /**
     * Returns the X angle to the best target, which can be used for simple servoing with vision.
     *
     * @param camera The camera to use
     */
    public Rotation2d getTargetX(Camera camera) {
        return inputs.get(camera).latestTargetObservation.tx();
    }

    public Optional<Integer> getPipeline() {
        Iterator<VisionIOInputsAutoLogged> iterator = inputs.values().iterator();
        return iterator.hasNext() ? Optional.of(iterator.next().currentPipeline) : Optional.empty();
    }

    @Override
    public void periodic() {
        if (!GlobalSettings.EnabledSubsystems.VISION.get()) {
            return;
        }

        for (Entry<Camera, VisionIO> entry : io.entrySet()) {
            VisionIO currentIO = entry.getValue();
            VisionIOInputsAutoLogged currentInputs = inputs.get(entry.getKey());
            currentIO.updateInputs(currentInputs);
            Logger.processInputs("Vision/" + entry.getKey().getName(), currentInputs);
        }

        Optional<Integer> pipeline = getPipeline();

        if (hdrTimer.hasElapsed(VisionConstants.VisionSettings.HDR_SWITCHING_INTERVAL)
                && VisionConstants.VisionSettings.HDR_ENABLED.get()
                && pipeline.isPresent()) {
            Pipeline nextPipeline =
                    pipeline.get() == Pipeline.HIGH_SUN.getIndex()
                            ? Pipeline.LOW_SUN
                            : Pipeline.HIGH_SUN;

            for (VisionIOOutputs outputs : outputs.values()) {
                outputs.pipeline = nextPipeline.getIndex();
            }

            hdrTimer.reset();
        }

        // Initialize logging values
        List<Pose3d> allTagPoses = new LinkedList<>();
        List<Pose3d> allRobotPoses = new LinkedList<>();
        List<Pose3d> allRobotPosesAccepted = new LinkedList<>();
        List<Pose3d> allRobotPosesRejected = new LinkedList<>();

        // Loop over cameras
        for (Entry<Camera, VisionIO> entry : io.entrySet()) {
            CameraData currentCameraData = entry.getKey().getData();
            VisionIOInputsAutoLogged currentInputs = inputs.get(entry.getKey());

            // Update disconnected alert
            disconnectedAlerts.get(entry.getKey()).set(!currentInputs.connected);

            // Initialize logging values
            List<Pose3d> tagPoses = new LinkedList<>();
            List<Pose3d> robotPoses = new LinkedList<>();
            List<Pose3d> robotPosesAccepted = new LinkedList<>();
            List<Pose3d> robotPosesRejected = new LinkedList<>();

            // Add tag poses
            for (int tagId : currentInputs.tagIds) {
                Optional<Pose3d> tagPose = FieldConstants.FIELD_LAYOUT.getTagPose(tagId);
                if (tagPose.isPresent()) {
                    tagPoses.add(tagPose.get());
                }
            }

            // Loop over pose observations
            for (PoseObservation observation : currentInputs.poseObservations) {
                // Check whether to reject pose
                boolean rejectPose =
                        observation.tagCount() == 0 // Must have at least one tag
                                || (observation.tagCount() == 1
                                        && observation.ambiguity()
                                                > VisionSettings
                                                        .MAX_AMBIGUITY) // Cannot be high ambiguity
                                || Math.abs(observation.pose().getZ())
                                        > VisionSettings
                                                .MAX_Z_ERROR // Must have realistic Z coordinate

                                // Must be within the field boundaries
                                || observation.pose().getX() < 0.0
                                || observation.pose().getX()
                                        > FieldConstants.FIELD_LAYOUT.getFieldLength()
                                || observation.pose().getY() < 0.0
                                || observation.pose().getY()
                                        > FieldConstants.FIELD_LAYOUT.getFieldWidth();

                // Add pose to log
                robotPoses.add(observation.pose());
                if (rejectPose) {
                    robotPosesRejected.add(observation.pose());
                } else {
                    robotPosesAccepted.add(observation.pose());
                }

                // Skip if rejected
                if (rejectPose) {
                    continue;
                }

                // Calculate standard deviations
                double stdDevFactor =
                        Math.pow(observation.averageTagDistance(), 2.0) / observation.tagCount();
                double linearStdDev = VisionSettings.LINEAR_STD_DEV_BASELINE * stdDevFactor;
                double angularStdDev = VisionSettings.ANGULAR_STD_DEV_BASELINE * stdDevFactor;
                if (observation.type() == PoseObservationType.MEGATAG_2) {
                    linearStdDev *= VisionSettings.LINEAR_STD_DEV_MEGATAG_2_FACTOR;
                    angularStdDev *= VisionSettings.ANGULAR_STD_DEV_MEGATAG_2_FACTOR;
                }
                linearStdDev *= currentCameraData.stdDevFactor();
                angularStdDev *= currentCameraData.stdDevFactor();

                // Send vision observation
                consumer.accept(
                        observation.pose().toPose2d(),
                        observation.timestamp(),
                        VecBuilder.fill(linearStdDev, linearStdDev, angularStdDev));
            }

            // Log camera metadata
            Logger.recordOutput(
                    "Vision/Camera" + currentCameraData.name() + "/TagPoses",
                    tagPoses.toArray(new Pose3d[tagPoses.size()]));
            Logger.recordOutput(
                    "Vision/Camera" + currentCameraData.name() + "/RobotPoses",
                    robotPoses.toArray(new Pose3d[robotPoses.size()]));
            Logger.recordOutput(
                    "Vision/Camera" + currentCameraData.name() + "/RobotPosesAccepted",
                    robotPosesAccepted.toArray(new Pose3d[robotPosesAccepted.size()]));
            Logger.recordOutput(
                    "Vision/Camera" + currentCameraData.name() + "/RobotPosesRejected",
                    robotPosesRejected.toArray(new Pose3d[robotPosesRejected.size()]));
            allTagPoses.addAll(tagPoses);
            allRobotPoses.addAll(robotPoses);
            allRobotPosesAccepted.addAll(robotPosesAccepted);
            allRobotPosesRejected.addAll(robotPosesRejected);
        }

        // Log summary data
        Logger.recordOutput("Vision/Summary/TagPoses", allTagPoses.toArray(new Pose3d[0]));
        Logger.recordOutput("Vision/Summary/RobotPoses", allRobotPoses.toArray(new Pose3d[0]));
        Logger.recordOutput(
                "Vision/Summary/RobotPosesAccepted", allRobotPosesAccepted.toArray(new Pose3d[0]));
        Logger.recordOutput(
                "Vision/Summary/RobotPosesRejected", allRobotPosesRejected.toArray(new Pose3d[0]));
    }

    @FunctionalInterface
    public static interface VisionConsumer {
        public void accept(
                Pose2d visionRobotPoseMeters,
                double timestampSeconds,
                Matrix<N3, N1> visionMeasurementStdDevs);
    }

    public void enable() {
        GlobalSettings.EnabledSubsystems.VISION.set(true);
    }

    public void disable() {
        GlobalSettings.EnabledSubsystems.VISION.set(false);
    }

    public Command setPipeline(int pipeline) {
        return run(coroutine -> {
                    for (VisionIOOutputs output : outputs.values()) {
                        output.pipeline = pipeline;
                    }
                })
                .named("Vision Set Pipeline");
    }

    public Command setMegaTagMode(MegaTagMode mode) {
        return run(coroutine -> {
                    for (VisionIOOutputs output : outputs.values()) {
                        output.megaTagMode = mode;
                    }
                })
                .named("Vision Set MegaTag Mode");
    }

    public Command setVisionEnabled() {
        return run(coroutine -> {
                    this.enable();
                })
                .named("Vision Set Enabled");
    }

    public Command setVisionDisabled() {
        return run(coroutine -> {
                    this.disable();
                })
                .named("Vision Set Enabled");
    }

    public Command setAprilTagWhitelist(int[] whitelist) {
        return run(coroutine -> {
                    for (VisionIOOutputs output : outputs.values()) {
                        output.aprilTagWhitelist = whitelist.clone();
                    }
                })
                .named("Vision Set April Tag Whitelist");
    }

    private Command setIMUMode(int imuMode, String name) {
        return run(coroutine -> {
                    for (VisionIOOutputs output : outputs.values()) {
                        output.imuMode = imuMode;
                    }
                })
                .named(name);
    }

    public Command setIMUMode(int imuMode) {
        return setIMUMode(imuMode, "Vision Set IMU Mode");
    }

    public Command resetIMU() {
        return setIMUMode(VisionSettings.RESET_IMU_INDEX, "Vision Reset IMU");
    }
}
