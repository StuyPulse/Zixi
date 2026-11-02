/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.vision;

import static org.wpilib.units.Units.Seconds;

import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.units.measure.Time;

import org.littletonrobotics.junction.networktables.LoggedNetworkBoolean;

public interface VisionConstants {
    public interface VisionSettings {
        LoggedNetworkBoolean HDR_ENABLED =
                new LoggedNetworkBoolean("/Tuning/Vision/HDR Enabled", false);

        Time HDR_SWITCHING_INTERVAL = Seconds.of(0.5);

        int RESET_IMU_INDEX = 1;

        // Basic filtering thresholds
        double MAX_AMBIGUITY = 0.3;
        double MAX_Z_ERROR = 0.75;

        // Standard deviation baselines, for 1 meter distance and 1 tag
        // (Adjusted automatically based on distance and # of tags)
        double LINEAR_STD_DEV_BASELINE = 0.02; // Meters
        double ANGULAR_STD_DEV_BASELINE = 0.06; // Radians

        // Multipliers to apply for MegaTag 2 observations
        double LINEAR_STD_DEV_MEGATAG_2_FACTOR = 0.5; // More stable than full 3D solve
        double ANGULAR_STD_DEV_MEGATAG_2_FACTOR =
                Double.POSITIVE_INFINITY; // No rotation data available

        double BUZZ_DEBOUNCE = 0.25;
    }

    record CameraData(String name, Transform3d robotToCamera, double stdDevFactor) {}

    /**
     * Represents a camera from its physical position and identified by name.
     *
     * <p>An constant of this enum looks like this <code>
     * NAME("detailed name", {@link Transform3d#Transform3d(double, double, double, Rotation3d) new Transform3d(x, y, z, rotation)}, 1.0);
     * </code>
     */
    public enum Camera {
        ;
        private final CameraData data;

        /**
         * Internal constructor to setup a camera with a {@link CameraData} instance
         *
         * @param name name of the camera, can be more detailed than its simple enum constant's name
         * @param robotToCamera transformation from the robot origin to the camera's position
         * @param stdDevFactor scalar factor to multiply the camrea's standard deviations by
         */
        private Camera(String name, Transform3d robotToCamera, double stdDevFactor) {
            this.data = new CameraData(name, robotToCamera, stdDevFactor);
        }

        public String getName() {
            return data.name();
        }

        public Transform3d getRobotToCamera() {
            return data.robotToCamera();
        }

        public double getStdDevFactor() {
            return data.stdDevFactor();
        }

        /**
         * Gets the backing data object
         *
         * @return The corresponding {@link CameraData} instance
         */
        public CameraData getData() {
            return data;
        }
    }

    public enum Pipeline {
        HIGH_SUN(0),
        LOW_SUN(1);

        private final int index;

        private Pipeline(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    public enum VisionMode {
        LIMELIGHT,
        PHOTONVISION
    }

    VisionMode VISION_MODE = VisionMode.LIMELIGHT;
}
