/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.commands.auton;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.constants.FieldConstants;

import org.wpilib.command3.Command;

import bline.field.BLineField;
import bline.path.Path;
import java.util.ArrayList;
import java.util.List;

/** Implementation of {@link Auton} using BLine paths */
public class BLineAuton implements Auton {
    private final List<Path> displayedPaths = new ArrayList<>();

    private final Command command;

    /**
     * Creates an auton that previews the given paths and runs the given commands
     *
     * @param paths Paths to preview
     * @param name Name of auton command given to scheduler
     * @param commands Auton command seqeunce
     */
    public BLineAuton(Path[] paths, String name, Command... commands) {
        for (Path path : paths) {
            displayedPaths.add(path.copy());
        }

        this.command = Command.sequence(commands).named(name);
    }

    @Override
    public void displayPaths() {
        for (Path path : displayedPaths) {
            if (Robot.isBlue()) {
                path.setFlipped(false);
                BLineField.drawPath(FieldConstants.FIELD_2D, path);
            } else {
                path.setFlipped(true);
                BLineField.drawPath(FieldConstants.FIELD_2D, path);
            }
        }
    }

    @Override
    public void clearPaths() {
        for (Path path : displayedPaths) {
            FieldConstants.FIELD_2D
                    .getObject(BLineField.drawPath(FieldConstants.FIELD_2D, path))
                    .setPoses();
        }
    }

    @Override
    public Command getCommand() {
        return command;
    }
}
