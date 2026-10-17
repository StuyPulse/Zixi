package com.stuypulse.robot.commands.auton;


import java.util.ArrayList;
import java.util.List;

import org.wpilib.command3.Command;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.constants.Field;

import bline.field.BLineField;
import bline.path.Path;

/**
 *
 *
 * <h2>BLineAuton </h2>
 *
 * <p>Made for the purpose of previewing the selected auton in elastic
 *
 * <p>The selected path disappears when a different auton is selected or when the robot is enabled
 *
 * <p>Define an auton in {@link AutonFactory} and add it to the auton chooser as shown in the
 * example auton in {@link RobotContainer}
 */
public class BLineAuton implements Auton{
    private final List<Path> displayedPaths = new ArrayList<>();

    private final Command command;

    /**
     * Creates an auton that previews the given paths and runs the given commands
     *
     * @param paths Paths to preview
     * @param commands Auton command seqeunce
     */
    public BLineAuton(Path[] paths, String name, Command... commands) {
        for (Path path : paths) {
            displayedPaths.add(path.copy());
        }

        this.command = Command.sequence(commands).named(name);
    }

    /**
     * Creates an auton without paths, for sysid and characterization routines
     *
     * @param command The command to run
     */
    public BLineAuton(Command command) {
        this.command = command;
    }

    @Override
    public void displayPaths() {
        for (Path path : displayedPaths) {
            if (Robot.isBlue()) {
                path.setFlipped(false);
                BLineField.drawPath(Field.FIELD_2D, path);
            } else {
                path.setFlipped(true);
                BLineField.drawPath(Field.FIELD_2D, path);
            }
        }
    }

    @Override
    public void clearPaths() {
        for (Path path : displayedPaths) {
            Field.FIELD_2D.getObject(BLineField.drawPath(Field.FIELD_2D, path)).setPoses();
        }
    }

    @Override
    /** @return The auton command to schedule */
    public Command getCommand() {
        return command;
    }
}