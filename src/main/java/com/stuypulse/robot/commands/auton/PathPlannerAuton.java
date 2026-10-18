/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.commands.auton;

import com.stuypulse.robot.constants.Field;
import com.pathplanner.lib.path.PathPlannerPath;
import java.util.ArrayList;
import java.util.List;

import org.wpilib.command3.Command;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;

/**
 * Implementation of {@link Auton} using PathPlanner paths
 */
public class PathPlannerAuton implements Auton {
    private final List<PathPlannerPath> subPaths = new ArrayList<PathPlannerPath>();

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
            subPaths.add(path);
        }

        this.command = Command.sequence(commands).named(name);
    }

    @Override
    public void displayPaths() {
        for (int i = 0; i < subPaths.size(); i++) {
            if (MatchState.getAlliance().get() == Alliance.BLUE) {
                Field.FIELD_2D
                        .getObject("path: " + subPaths.get(i).name)
                        .setPoses(subPaths.get(i).getPathPoses());
            } else if (MatchState.getAlliance().isEmpty()) {
                Field.FIELD_2D
                        .getObject("path: " + subPaths.get(i).name)
                        .setPoses(
                                Field.transformToOppositeAlliance(subPaths.get(i).getPathPoses()));
            } else {
                Field.FIELD_2D
                        .getObject("path: " + subPaths.get(i).name)
                        .setPoses(
                                Field.transformToOppositeAlliance(subPaths.get(i).getPathPoses()));
            }
        }
    }

    @Override
    public void clearPaths() {
        for (int i = 0; i < subPaths.size(); i++) {
            Field.FIELD_2D.getObject("path: " + subPaths.get(i).name).setPoses();
        }
    }

    @Override
    public Command getCommand() {
        return command;
    }
}
