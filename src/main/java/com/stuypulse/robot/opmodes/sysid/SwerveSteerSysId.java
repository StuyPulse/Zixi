/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.opmodes.sysid;

import static org.wpilib.units.Units.Volts;

import com.stuypulse.robot.Robot;
import com.stuypulse.robot.subsystems.swerve.Swerve;
import com.stuypulse.robot.subsystems.swerve.SwerveConstants.SwerveCharacterization;
import com.stuypulse.robot.subsystems.swerve.SwerveConstants.SwerveCharacterization.SteerSysId;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.sysid.SysIdRoutine;
import org.wpilib.command3.sysid.SysIdRoutine.Config;
import org.wpilib.command3.sysid.SysIdRoutine.Direction;
import org.wpilib.command3.sysid.SysIdRoutine.SysIdMechanism;
import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Utility;
import org.wpilib.units.measure.Time;

import org.littletonrobotics.junction.Logger;

@Utility(
        name = "Swerve Steer",
        group = "SysId",
        description = "Runs only the steer motors. Put the robot on blocks.")
public class SwerveSteerSysId implements OpMode {
    private final Swerve swerve;
    private final Command routine;

    public SwerveSteerSysId(Robot robot) {
        swerve = robot.swerve;

        Config config =
                new SysIdRoutine.Config(
                        SteerSysId.RAMP_RATE,
                        SteerSysId.STEP_VOLTAGE,
                        SteerSysId.QUASISTATIC_TIMEOUT,
                        state -> Logger.recordOutput("SysId/SwerveSteer/State", state.toString()));

        SysIdMechanism mechanism =
                new SysIdRoutine.SysIdMechanism(
                        volts -> swerve.runSteerCharacterization(volts.in(Volts)),
                        log ->
                                Logger.recordOutput(
                                        "SysId/SwerveSteer/PositionRad",
                                        swerve.getSteerCharacterizationPositionRad()),
                        swerve,
                        "SwerveSteer");

        SysIdRoutine sysId = new SysIdRoutine(config, mechanism);
        Time dynamicTimeout = SteerSysId.DYNAMIC_TIMEOUT;

        routine =
                Command.sequence(
                                rest(),
                                sysId.quasistatic(Direction.FORWARD),
                                rest(),
                                sysId.quasistatic(Direction.REVERSE),
                                rest(),
                                sysId.dynamic(Direction.FORWARD).withTimeout(dynamicTimeout),
                                rest(),
                                sysId.dynamic(Direction.REVERSE).withTimeout(dynamicTimeout))
                        .named("Swerve Steer SysId");
    }

    private Command rest() {
        return swerve.run(
                        coroutine -> {
                            swerve.runSteerCharacterization(0.0);
                            coroutine.wait(SwerveCharacterization.SYSID_REST);
                        })
                .named("Swerve Steer SysId Rest");
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
