/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.commands.auton;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.constants.FieldConstants;

import org.wpilib.command3.Command;

import com.pathplanner.lib.path.PathPlannerPath;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link Auton} using PathPlanner paths
 *
 * @author Aiden Zhang (https://github.com/aidenzhang21345)
 */
public class PathPlannerAuton implements Auton {
    private final List<PathPlannerPath> paths = new ArrayList<PathPlannerPath>();

    private final Command command;

    /**
     * Creates an auton that previews the given paths and runs the given commands
     *
     * @param paths Paths to preview
     * @param name Name of auton command given to scheduler
     * @param commands Auton command seqeunce
     */
    public PathPlannerAuton(PathPlannerPath[] paths, String name, Command... commands) {
        for (PathPlannerPath path : paths) {
            this.paths.add(path);
        }

        this.command = Command.sequence(commands).named(name);
    }

    @Override
    public void displayPaths() {
        for (PathPlannerPath path : paths) {
            FieldConstants.FIELD_2D
                    .getObject("path: " + path.name)
                    .setPoses(
                            Robot.isBlue()
                                    ? path.getPathPoses()
                                    : FieldConstants.transformToOppositeAlliance(
                                            path.getPathPoses()));
        }
    }

    @Override
    public void clearPaths() {
        for (PathPlannerPath path : paths) {
            FieldConstants.FIELD_2D.getObject("path: " + path.name).setPoses();
        }
    }

    @Override
    public Command getCommand() {
        return command;
    }
}
