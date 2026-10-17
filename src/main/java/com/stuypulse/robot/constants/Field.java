package com.stuypulse.robot.constants;

import java.util.List;

import org.wpilib.math.geometry.Pose2d;
import org.wpilib.smartdashboard.Field2d;

import com.pathplanner.lib.util.FlippingUtil;

public interface Field {
    Field2d FIELD_2D = new Field2d();

    public static Pose2d transformToOppositeAlliance(Pose2d pose) {
        return FlippingUtil.flipFieldPose(pose);
    }

    public static List<Pose2d> transformToOppositeAlliance(List<Pose2d> poses) {
        return poses.stream().map(Field::transformToOppositeAlliance).toList();
    }
}
