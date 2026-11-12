/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.opmodes;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.commands.auton.Auton;

import org.wpilib.command3.Command;
import org.wpilib.opmode.OpMode;

public abstract class AutonOpMode implements OpMode {
    protected final Robot robot;
    protected final Auton auto;
    protected final Command command;

    protected AutonOpMode(Robot robot, Auton auto) {
        this.robot = robot;
        this.auto = auto;
        this.command = auto.getCommand();
    }

    @Override
    public final void disabledPeriodic() {
        auto.displayPaths();
        this.disabledCallback();
    }

    /**
     * Disabled periodic callback method for being overriden by extending autons
     *
     * <p>Will run periodically after paths are shown on the display
     */
    protected void disabledCallback() {}
    ;

    @Override
    public final void start() {
        auto.clearPaths();
        robot.defaultScheduler.schedule(command);

        this.startCallback();
    }

    /**
     * Start callback method for being overriden by extending autons
     *
     * <p>Will run after the command is scheduled and auto paths cleared from the display
     */
    protected void startCallback() {}

    @Override
    public final void end() {
        robot.defaultScheduler.cancel(command);
        auto.clearPaths();
        this.endCallback();
    }

    /**
     * End callback method for being overriden by extending autons
     *
     * <p>Will run after the command is canceled and auto paths cleared from the display
     */
    public void endCallback() {}
}
