/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.vision;

import static org.wpilib.units.Units.Degrees;

import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.system.RobotController;

import com.limelightvision.FiducialTarget;
import com.limelightvision.Limelight;
import com.limelightvision.LimelightResults;
import com.limelightvision.PoseEstimateType;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** IO implementation for real Limelight hardware. */
public class VisionIOLimelight implements VisionIO {
    private final Limelight camera;

    private MegaTagMode megaTagMode;

    /**
     * Creates a new VisionIOLimelight.
     *
     * @param name The configured name of the Limelight.
     * @param rotationSupplier Supplier for the current estimated rotation, used for MegaTag 2.
     */
    public VisionIOLimelight(String name, Transform3d robotToCamera) {
        NetworkTable table = NetworkTableInstance.getDefault().getTable(name);
        this.camera = new Limelight(name);
        // orientationPublisher = table.getDoubleArrayTopic("robot_orientation_set").publish();
        // pipelinePublisher = table.getIntegerTopic("pipeline").publish();
        // imuModePublisher = table.getIntegerTopic("imumode_set").publish();
        // aprilTagWhitelistPublisher =
        // table.getDoubleArrayTopic("fiducial_id_filters_set").publish();
        // latencySubscriber = table.getDoubleTopic("tl").subscribe(0.0);
        // txSubscriber = table.getDoubleTopic("tx").subscribe(0.0);
        // tySubscriber = table.getDoubleTopic("ty").subscribe(0.0);
        // megatag1Subscriber =
        //         table.getDoubleArrayTopic("botpose_wpiblue").subscribe(new double[] {});
        // megatag2Subscriber =
        //         table.getDoubleArrayTopic("botpose_orb_wpiblue").subscribe(new double[] {});
        // pipelineSubscriber = table.getIntegerTopic("pipeline").subscribe(0);

        megaTagMode = MegaTagMode.MEGATAG_1;

        table.getDoubleArrayTopic("camerapose_robotspace_set")
                .publish()
                .accept(
                        new double[] {
                            robotToCamera.getX(),
                            robotToCamera.getY(),
                            robotToCamera.getZ(),
                            robotToCamera.getRotation().getMeasureX().in(Degrees),
                            robotToCamera.getRotation().getMeasureY().in(Degrees),
                            robotToCamera.getRotation().getMeasureZ().in(Degrees)
                        });
    }

    @Override
    public void updateInputs(VisionIOInputs inputs) {
        // Update connection status based on whether an update has been seen in the last
        // 250ms
        inputs.connected =
                ((RobotController.getMonotonicTime() * 1e-6) - camera.getTargetingLatencyMillis())
                        < 250;

        inputs.currentPipeline = camera.getCurrentPipelineIndex();

        // Update target observation
        inputs.latestTargetObservation =
                new TargetObservation(
                        Rotation2d.fromDegrees(camera.getTXDegrees()),
                        Rotation2d.fromDegrees(camera.getTYDegrees()));

        // Update orientation for MegaTag 2
        // done in vision.java now

        // NetworkTableInstance.getDefault()
        //         .flush(); // done in vision.java

        // Read new pose observations from NetworkTables
        Set<Integer> tagIds = new HashSet<>();
        List<PoseObservation> poseObservations = new LinkedList<>();

        if (megaTagMode == MegaTagMode.MEGATAG_1) {
            for (LimelightResults rawSample : camera.readResultsQueue()) {
                if (!rawSample.valid) { // ignore invalid results obv
                    continue;
                }
                for (FiducialTarget tag : rawSample.fiducialTargets) {
                    tagIds.add(tag.fiducialId);
                }
                double ambiguity = 0.0;
                // Ambiguity, using only the first tag because ambiguity isn't
                // applicable for
                // multitag
                if (megaTagMode == MegaTagMode.MEGATAG_1 && rawSample.fiducialTargets.length == 1) {
                    ambiguity = rawSample.fiducialTargets[0].ambiguity;
                }
                poseObservations.add(
                        new PoseObservation(
                                // Timestamp, based on server timestamp of publish and latency
                                rawSample.ntTimestampMicros * 1.0e-6
                                        - rawSample.timestampMillis * 1.0e-3,

                                // 3D pose estimate
                                rawSample.getRobotPose(PoseEstimateType.MT1_WPIBLUE),
                                ambiguity,

                                // Tag count
                                rawSample.fiducialTargets.length,

                                // Average tag distance
                                rawSample.avgTagDistanceMeters,

                                // Observation type
                                megaTagMode == MegaTagMode.MEGATAG_1
                                        ? PoseObservationType.MEGATAG_1
                                        : PoseObservationType.MEGATAG_2));
            }
        }

        // Save pose observations to inputs object
        inputs.poseObservations = new PoseObservation[poseObservations.size()];
        for (int i = 0; i < poseObservations.size(); i++) {
            inputs.poseObservations[i] = poseObservations.get(i);
        }

        // Save tag IDs to inputs objects
        inputs.tagIds = new int[tagIds.size()];
        int i = 0;
        for (int id : tagIds) {
            inputs.tagIds[i++] = id;
        }
    }

    @Override
    public void applyOutputs(VisionIOOutputs outputs) {
        camera.setPipelineIndex(outputs.pipeline);

        this.megaTagMode = outputs.megaTagMode;

        camera.setFiducialIDFiltersOverride(outputs.aprilTagWhitelist);
        camera.setIMUMode(outputs.imuMode);
    }
}
