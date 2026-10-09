/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util;

import org.wpilib.annotation.NoDiscard;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * A standard subsystem that includes an extra periodic callback which runs after the command
 * scheduler. Allows outputs to be published after all other periodic code has finished.
 */
public abstract class PeriodicSubsystem implements Mechanism {
    private static final List<PeriodicSubsystem> subsystemInstances = new ArrayList<>();

    private final String name;

    protected PeriodicSubsystem() {
        name = getClass().getSimpleName();
        subsystemInstances.add(this);
        this.getRegisteredScheduler().addPeriodic(this::periodic);
    }

    protected PeriodicSubsystem(String name) {
        this.name = name;
        subsystemInstances.add(this);
        this.getRegisteredScheduler().addPeriodic(this::periodic);
    }

    /**
     * Gets the name of this mechanism.
     *
     * @return The name of the mechanism.
     */
    @NoDiscard
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * This method is called periodically after {@link Scheduler#run}, and should be overriden for
     * applying outputs.
     */
    protected void periodicAfterScheduler() {}
    ;

    /**
     * This method is called periodically before {@link Scheduler#run}, and should be overriden for
     * processing inputs.
     */
    protected abstract void periodic();

    /** Run the {@link #periodicAfterScheduler} methods for all subsystems. */
    public static void runAllPeriodicAfterScheduler() {
        for (PeriodicSubsystem instance : subsystemInstances) {
            instance.periodicAfterScheduler();
        }
    }
}
