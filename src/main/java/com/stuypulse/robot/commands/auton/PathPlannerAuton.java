/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.commands.auton;

import com.stuypulse.robot.RobotContainer;
import com.stuypulse.robot.constants.Field;
import com.pathplanner.lib.path.PathPlannerPath;
import java.util.ArrayList;
import java.util.List;

import org.wpilib.command3.Command;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;

/**
 *
 *
 * <h2>PathPlannerAuton </h2>
 *
 * <p>Made for the purpose of previewing the selected auton in elastic
 *
 * <p>The selected path disappears when a different auton is selected or when the robot is enabled
 *
 * <p>Define an auton in {@link AutonFactory} and add it to the auton chooser as shown in the
 * example auton in {@link RobotContainer}
 */
public class PathPlannerAuton implements Auton{
    private final List<PathPlannerPath> subPaths = new ArrayList<PathPlannerPath>();

    private final Command command;

    /**
     * Creates an auton that previews the given paths and runs the given commands
     *
     * @param paths Paths to preview
     * @param commands Auton command seqeunce
     */
    public PathPlannerAuton(PathPlannerPath[] paths, String name, Command... commands) {
        for (PathPlannerPath path : paths) {
            subPaths.add(path);
        }

        this.command = Command.sequence(commands).named(name);
    }

    /**
     * Creates an auton without paths, for sysid and characterization routines
     *
     * @param command The command to run
     */
    public PathPlannerAuton(Command command) {
        this.command = command;
    }

    /** Sets path poses as field objects which display as trajectories on elastic */
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

    /** Sets field objects to empty poses so they disappear */
    public void clearPaths() {
        for (int i = 0; i < subPaths.size(); i++) {
            Field.FIELD_2D.getObject("path: " + subPaths.get(i).name).setPoses();
        }
    }

    /** @return The auton command to schedule */
    public Command getCommand() {
        return command;
    }
}
