/************************ PROJECT PHIL ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.constants;

import static org.wpilib.units.Units.*;

import org.wpilib.framework.RobotBase;
import org.wpilib.units.measure.*;

import dev.doglog.DogLog;
import org.littletonrobotics.junction.networktables.LoggedNetworkBoolean;

/** File containing non-subsystem settings for the robot. */
public interface GlobalSettings {
    Time DT = Milliseconds.of(20);

    /**
     * Uses either a {@link DogLog#tunable(key, value)} or {@link LoggedNetworkBoolean} for each
     * subsystem to add subsystem toggling functionality from external dashboards.
     */
    interface EnabledSubsystems {}

    /** What mode the robot is in when running a simulation. */
    RobotMode SIM_MODE = RobotMode.SIM;

    RobotMode CURRENT_MODE = RobotBase.isReal() ? RobotMode.REAL : SIM_MODE;

    enum RobotMode {
        /** Running on a real robot. */
        REAL,

        /** Running in simulation. */
        SIM,

        /** Replaying from a log file. */
        REPLAY
    }
}
