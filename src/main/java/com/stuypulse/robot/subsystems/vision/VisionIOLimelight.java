/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.vision;

import com.stuypulse.robot.Robot;

import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Transform3d;

import com.limelightvision.Limelight;
import com.limelightvision.PoseEstimateType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class VisionIOLimelight implements VisionIO {
    private final Limelight camera;

    private final Supplier<Rotation2d> rotationSupplier;

    private MegaTagMode megaTagMode;

    public VisionIOLimelight(
            String cameraName, Transform3d robotToCamera, Supplier<Rotation2d> rotationSupplier) {
        camera =
                new Limelight(
                        cameraName,
                        new Pose3d(robotToCamera.getTranslation(), robotToCamera.getRotation()));

        this.rotationSupplier = rotationSupplier;

        megaTagMode = MegaTagMode.MEGATAG_1;
    }

    @Override
    public void updateInputs(VisionIOInputs inputs) {
        inputs.connected = camera.isConnected();

        inputs.currentPipeline = camera.getCurrentPipelineIndex();

        Limelight.setSharedRobotOrientation(rotationSupplier.get().getDegrees());

        List<PoseObservation> poses = new ArrayList<>();
        Set<Integer> tagIds = new HashSet<>();

        if (megaTagMode == MegaTagMode.MEGATAG_1) {
            for (var estimate :
                    camera.readAcceptedPoseEstimates(
                            Robot.isBlue()
                                    ? PoseEstimateType.MT1_WPIBLUE
                                    : PoseEstimateType.MT1_WPIRED)) {
                // Ambiguity is 0 because limelight automatically rejects results with too much
                // ambiguity
                poses.add(
                        new PoseObservation(
                                estimate.timestampSeconds,
                                new Pose3d(estimate.pose),
                                0,
                                estimate.reportedTagCount,
                                estimate.avgTagDistanceMeters,
                                PoseObservationType.MEGATAG_1));
                Collections.addAll(
                        tagIds,
                        Arrays.stream(estimate.rawFiducials)
                                .map(tag -> tag.fiducialId)
                                .toArray(Integer[]::new));
            }
        } else {
            for (var estimate :
                    camera.readAcceptedPoseEstimates(
                            Robot.isBlue()
                                    ? PoseEstimateType.MT2_WPIBLUE
                                    : PoseEstimateType.MT2_WPIRED)) {
                // Ambiguity is 0 because limelight automatically rejects results with too much
                // ambiguity
                poses.add(
                        new PoseObservation(
                                estimate.timestampSeconds,
                                new Pose3d(estimate.pose),
                                0,
                                estimate.reportedTagCount,
                                estimate.avgTagDistanceMeters,
                                PoseObservationType.MEGATAG_2));
                Collections.addAll(
                        tagIds,
                        Arrays.stream(estimate.rawFiducials)
                                .map(tag -> tag.fiducialId)
                                .toArray(Integer[]::new));
            }
        }

        inputs.poseObservations = poses.toArray(PoseObservation[]::new);
        inputs.tagIds = tagIds.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void applyOutputs(VisionIOOutputs outputs) {
        camera.setPipelineIndex(outputs.pipeline);
        camera.setFiducialIDFiltersOverride(outputs.aprilTagWhitelist);

        this.megaTagMode = outputs.megaTagMode;
    }
}
