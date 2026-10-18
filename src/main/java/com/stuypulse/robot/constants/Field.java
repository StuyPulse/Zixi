/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.constants;

import org.wpilib.math.geometry.Pose2d;
import org.wpilib.smartdashboard.Field2d;

import com.pathplanner.lib.util.FlippingUtil;
import java.util.List;

public interface Field {
    Field2d FIELD_2D = new Field2d();

    public static Pose2d transformToOppositeAlliance(Pose2d pose) {
        return FlippingUtil.flipFieldPose(pose);
    }

    public static List<Pose2d> transformToOppositeAlliance(List<Pose2d> poses) {
        return poses.stream().map(Field::transformToOppositeAlliance).toList();
    }
}
