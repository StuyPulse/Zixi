/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.opmodes;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.commands.auton.Auton;

import org.littletonrobotics.junction.networktables.LoggedNetworkChooser;
import org.wpilib.opmode.OpMode;

public class AutonOpMode implements OpMode {
    protected final Robot robot;
    protected Auton auto;
    protected static final LoggedNetworkChooser<Auton> chooser = new LoggedNetworkChooser<>("Auton");

    public AutonOpMode(Robot robot) {
        this.robot = robot;

        setAuto(chooser.getSelected());
        chooser.onChange(this::setAuto);
    }

    public static void addDefaultAuton(String name, Auton auton) {
        chooser.addDefault(name, auton);
    }

    public static void addAuton(String name, Auton auton) {
        chooser.add(name, auton);
    }

    private void setAuto(Auton auton) {
        clearPaths();
        auto = auton;
        displayPaths();
    }

    private void clearPaths() {
        if (auto != null) {
            auto.clearPaths();
        }
    }

    private void displayPaths() {
        if (auto != null) {
            auto.displayPaths();
        }
    }

    private void scheduleAuto() {
        if (auto != null) {
            robot.defaultScheduler.schedule(auto.getCommand());
        }
    }

    private void cancelAuto() {
        if (auto != null) {
            robot.defaultScheduler.cancel(auto.getCommand());
        }
    }

    @Override
    public final void start() {
        clearPaths();

        auto = chooser.getSelected();

        scheduleAuto();
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
        cancelAuto();
        this.endCallback();
    }

    @Override
    public final void close() {
        cancelAuto();
        clearPaths();
        chooser.onChange(null);
    }

    /**
     * End callback method for being overriden by extending autons
     *
     * <p>Will run after the command is canceled and auto paths cleared from the display
     */
    public void endCallback() {}
}
