/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.opmodes;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.commands.auton.Auton;

import org.wpilib.opmode.OpMode;

public class AutonOpMode implements OpMode {
    protected final Robot robot;
    protected Auton auto;

    public AutonOpMode(Robot robot) {
        this.robot = robot;
    }

    @Override
    public final void disabledPeriodic() {
        if (auto != robot.getSelectedAuton() && auto != null) {
            auto.clearPaths();

            auto = robot.getSelectedAuton();

            auto.displayPaths();
        }

        if (auto == null) {
            auto = robot.getSelectedAuton();
            if (auto != null) {
                auto.displayPaths();
            }
        }

        this.disabledCallback();
    }

    /**
     * Disabled periodic callback method for being overriden by extending autons
     *
     * <p>Will run periodically after paths are shown on the display
     */
    protected void disabledCallback() {}

    @Override
    public final void start() {
        if (auto != null) {
            auto.clearPaths();
        }

        auto = robot.getSelectedAuton();

        if (auto != null) {
            robot.defaultScheduler.schedule(auto.getCommand());
        }

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
        if (auto != null) {
            robot.defaultScheduler.cancel(auto.getCommand());
        }
        this.endCallback();
    }

    /**
     * End callback method for being overriden by extending autons
     *
     * <p>Will run after the command is canceled and auto paths cleared from the display
     */
    public void endCallback() {}
}
