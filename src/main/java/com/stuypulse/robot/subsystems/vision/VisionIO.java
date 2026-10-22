/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.vision;

import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;

import org.littletonrobotics.junction.AutoLog;
import org.littletonrobotics.junction.AutoLogOutput;

public interface VisionIO {
    @AutoLog
    class VisionIOInputs {
        public boolean connected = false;
        public TargetObservation latestTargetObservation =
                new TargetObservation(Rotation2d.ZERO, Rotation2d.ZERO);
        public PoseObservation[] poseObservations = new PoseObservation[0];
        public int[] tagIds = new int[0];
        public int currentPipeline = 0;
    }

    class VisionIOOutputs {
        @AutoLogOutput(key = "Vision/MegaTag Mode")
        public MegaTagMode megaTagMode = MegaTagMode.MEGATAG_1;

        @AutoLogOutput(key = "Vision/Target Pipeline Index")
        public int pipeline = 0;

        @AutoLogOutput(key = "Vision/AprilTag Whitelist")
        public int[] aprilTagWhitelist = new int[0];

        @AutoLogOutput(key = "Vision/IMU Mode")
        public int imuMode = 1;
    }

    /** Represents the angle to a simple target, not used for pose estimation. */
    record TargetObservation(Rotation2d tx, Rotation2d ty) {}

    /** Represents a robot pose sample used for pose estimation. */
    record PoseObservation(
            double timestamp,
            Pose3d pose,
            double ambiguity,
            int tagCount,
            double averageTagDistance,
            PoseObservationType type) {}

    enum PoseObservationType {
        MEGATAG_1,
        MEGATAG_2,
        PHOTONVISION
    }

    enum MegaTagMode {
        MEGATAG_1,
        MEGATAG_2
    }

    public default void updateInputs(VisionIOInputs inputs) {}

    public default void applyOutputs(VisionIOOutputs outputs) {}
}
