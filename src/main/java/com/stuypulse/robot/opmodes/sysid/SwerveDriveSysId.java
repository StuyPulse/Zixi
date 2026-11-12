/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.opmodes.sysid;

import static org.wpilib.units.Units.Volts;

import com.stuypulse.robot.subsystems.swerve.Swerve;
import com.stuypulse.robot.subsystems.swerve.SwerveConstants.SwerveCharacterization.DriveSysId;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.sysid.SysIdRoutine;
import org.wpilib.command3.sysid.SysIdRoutine.Direction;
import org.wpilib.opmode.OpMode;

import java.util.function.BiFunction;
import org.littletonrobotics.junction.Logger;

abstract class SwerveDriveSysId implements OpMode {
    private final Command routine;

    public SwerveDriveSysId(
            Swerve swerve,
            Direction direction,
            BiFunction<SysIdRoutine, Direction, Command> sysIdFunction) {
        SysIdRoutine.Config config =
                new SysIdRoutine.Config(
                        DriveSysId.RAMP_RATE,
                        DriveSysId.STEP_VOLTAGE,
                        DriveSysId.QUASISTATIC_TIMEOUT,
                        state -> Logger.recordOutput("SysId/SwerveDrive/State", state.toString()));

        SysIdRoutine.SysIdMechanism mechanism =
                new SysIdRoutine.SysIdMechanism(
                        volts -> swerve.runCharacterization(volts.in(Volts)),
                        null,
                        swerve,
                        "SwerveDrive");
        SysIdRoutine sysId = new SysIdRoutine(config, mechanism);

        routine = sysIdFunction.apply(sysId, direction);
    }

    @Override
    public void start() {
        Scheduler.getDefault().schedule(routine);
    }

    @Override
    public void end() {
        Scheduler.getDefault().cancel(routine);
    }
}
