/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util.robotsimulation;

import org.wpilib.math.geometry.Pose3d;

/**
 * A simulated mechanism that provides an externally usable pose for external simulation files, such
 * as a robot visualizer or 3D simulator.
 */
@FunctionalInterface
public interface SimulatedMechanism {
    Pose3d getSimulatedPose();
}
