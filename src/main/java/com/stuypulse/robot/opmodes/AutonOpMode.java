/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.opmodes;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.commands.auton.Auton;

import org.wpilib.opmode.OpMode;

import java.util.function.Consumer;
import org.littletonrobotics.junction.networktables.LoggedNetworkChooser;

public class AutonOpMode implements OpMode {
    protected static final LoggedNetworkChooser<Auton> chooser =
            new LoggedNetworkChooser<>("Auton");

    protected final Robot robot;
    protected Auton auto;

    public static void addDefaultAuton(String name, Auton auton) {
        chooser.addDefault(name, auton);
    }

    public static void addAuton(String name, Auton auton) {
        chooser.add(name, auton);
    }

    public AutonOpMode(Robot robot) {
        this.robot = robot;

        setAuto(chooser.getSelected());
        chooser.onChange(this::setAuto);
    }

    private void setAuto(Auton auton) {
        ifNotNull(Auton::clearPaths);
        auto = auton;
        ifNotNull(Auton::displayPaths);
    }

    private void ifNotNull(Consumer<Auton> consumer) {
        if (auto != null) {
            consumer.accept(auto);
        }
    }

    @Override
    public final void start() {
        ifNotNull(Auton::clearPaths);

        auto = chooser.getSelected();

        ifNotNull(a -> robot.defaultScheduler.schedule(a.getCommand()));
        this.startCallback();
    }

    /**
     * Start callback method for being overridden by extending autons
     *
     * <p>Will run after the command is scheduled and auto paths cleared from the display
     */
    protected void startCallback() {}

    @Override
    public final void end() {
        ifNotNull(a -> robot.defaultScheduler.cancel(a.getCommand()));
        this.endCallback();
    }

    @Override
    public final void close() {
        ifNotNull(a -> robot.defaultScheduler.cancel(a.getCommand()));
        ifNotNull(Auton::clearPaths);
        chooser.onChange(null);
        this.closeCallback();
    }

    /**
     * End callback method for being overridden by extending autons
     *
     * <p>Will run after the command is canceled and auto paths cleared from the display
     */
    public void endCallback() {}

    /**
     * Close callback method for being overridden by extending autons
     *
     * <p>Will run after the command is canceled, auto paths cleared from the display, and the
     * chooser listener is removed
     */
    public void closeCallback() {}
}
