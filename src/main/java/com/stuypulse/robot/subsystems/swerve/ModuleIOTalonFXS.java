/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.subsystems.swerve;

import static org.wpilib.units.Units.Rotations;

import com.stuypulse.robot.generated.TunerConstants;

import org.wpilib.math.filter.Debouncer;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.util.Units;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Current;
import org.wpilib.units.measure.Voltage;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CANdiConfiguration;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.CANdi;
import com.ctre.phoenix6.hardware.ParentDevice;
import com.ctre.phoenix6.hardware.TalonFXS;
import com.ctre.phoenix6.signals.BrushedMotorWiringValue;
import com.ctre.phoenix6.signals.ExternalFeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.MotorArrangementValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.swerve.SwerveModuleConstants;

import java.util.Queue;
import java.util.function.Supplier;

/**
 * Module IO implementation for Talon FXS drive motor controller, Talon FXS turn motor controller,
 * and CANdi (PWM 1). Configured using a set of module constants from Phoenix.
 *
 * <p>Device configuration and other behaviors not exposed by TunerConstants can be customized here.
 */
public class ModuleIOTalonFXS implements ModuleIO {
    private static void tryUntilOk(int maxAttempts, Supplier<StatusCode> command) {
        for (int i = 0; i < maxAttempts; i++) {
            var error = command.get();
            if (error.isOK()) break;
        }
    }

    // Hardware objects
    private final TalonFXS driveTalon;
    private final TalonFXS turnTalon;
    private final CANdi candi;

    // Voltage control requests
    private final VoltageOut voltageRequest = new VoltageOut(0);
    private final PositionVoltage positionVoltageRequest = new PositionVoltage(0.0);
    private final VelocityVoltage velocityVoltageRequest = new VelocityVoltage(0.0);

    // Timestamp inputs from Phoenix thread
    private final Queue<Double> timestampQueue;

    // Inputs from drive motor
    private final StatusSignal<Angle> drivePosition;
    private final Queue<Double> drivePositionQueue;
    private final StatusSignal<AngularVelocity> driveVelocity;
    private final StatusSignal<Voltage> driveAppliedVolts;
    private final StatusSignal<Current> driveCurrent;

    // Inputs from turn motor
    private final StatusSignal<Angle> turnAbsolutePosition;
    private final StatusSignal<Angle> turnPosition;
    private final Queue<Double> turnPositionQueue;
    private final StatusSignal<AngularVelocity> turnVelocity;
    private final StatusSignal<Voltage> turnAppliedVolts;
    private final StatusSignal<Current> turnCurrent;

    // Connection debouncers
    private final Debouncer driveConnectedDebounce =
            new Debouncer(0.5, Debouncer.DebounceType.FALLING);
    private final Debouncer turnConnectedDebounce =
            new Debouncer(0.5, Debouncer.DebounceType.FALLING);
    private final Debouncer turnEncoderConnectedDebounce =
            new Debouncer(0.5, Debouncer.DebounceType.FALLING);

    public ModuleIOTalonFXS(
            SwerveModuleConstants<TalonFXSConfiguration, TalonFXSConfiguration, CANdiConfiguration>
                    constants) {
        driveTalon = new TalonFXS(constants.DriveMotorId, TunerConstants.kCANBus);
        turnTalon = new TalonFXS(constants.SteerMotorId, TunerConstants.kCANBus);
        candi = new CANdi(constants.EncoderId, TunerConstants.kCANBus);

        // Configure drive motor
        var driveConfig = constants.DriveMotorInitialConfigs;
        driveConfig.Commutation.MotorArrangement =
                switch (constants.DriveMotorType) {
                    case TalonFXS_NEO_JST -> MotorArrangementValue.NEO_JST;
                    case TalonFXS_VORTEX_JST -> MotorArrangementValue.VORTEX_JST;
                    default -> MotorArrangementValue.Disabled;
                };
        driveConfig.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        driveConfig.Slot0 = constants.DriveMotorGains;
        driveConfig.ExternalFeedback.SensorToMechanismRatio = constants.DriveMotorGearRatio;
        driveConfig.CurrentLimits.StatorCurrentLimit = constants.SlipCurrent;
        driveConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        driveConfig.MotorOutput.Inverted =
                constants.DriveMotorInverted
                        ? InvertedValue.Clockwise_Positive
                        : InvertedValue.CounterClockwise_Positive;
        tryUntilOk(5, () -> driveTalon.getConfigurator().apply(driveConfig, 0.25));
        tryUntilOk(5, () -> driveTalon.setPosition(0.0, 0.25));

        // Configure turn motor
        var turnConfig = new TalonFXSConfiguration();
        turnConfig.Commutation.MotorArrangement =
                switch (constants.SteerMotorType) {
                    case TalonFXS_Minion_JST -> MotorArrangementValue.Minion_JST;
                    case TalonFXS_NEO_JST -> MotorArrangementValue.NEO_JST;
                    case TalonFXS_VORTEX_JST -> MotorArrangementValue.VORTEX_JST;
                    case TalonFXS_NEO550_JST -> MotorArrangementValue.NEO550_JST;
                    case TalonFXS_Brushed_AB, TalonFXS_Brushed_AC, TalonFXS_Brushed_BC ->
                            MotorArrangementValue.Brushed_DC;
                    default -> MotorArrangementValue.Disabled;
                };
        turnConfig.Commutation.BrushedMotorWiring =
                switch (constants.SteerMotorType) {
                    case TalonFXS_Brushed_AC -> BrushedMotorWiringValue.Leads_A_and_C;
                    case TalonFXS_Brushed_BC -> BrushedMotorWiringValue.Leads_B_and_C;
                    default -> BrushedMotorWiringValue.Leads_A_and_B;
                };
        turnConfig.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        turnConfig.Slot0 = constants.SteerMotorGains;
        turnConfig.ExternalFeedback.FeedbackRemoteSensorID = constants.EncoderId;
        turnConfig.ExternalFeedback.ExternalFeedbackSensorSource =
                switch (constants.FeedbackSource) {
                    case RemoteCANdiPWM1 -> ExternalFeedbackSensorSourceValue.RemoteCANdiPWM1;
                    case FusedCANdiPWM1 -> ExternalFeedbackSensorSourceValue.FusedCANdiPWM1;
                    case SyncCANdiPWM1 -> ExternalFeedbackSensorSourceValue.SyncCANdiPWM1;
                    default ->
                            throw new RuntimeException(
                                    "You have selected a turn feedback source that is not supported by the default implementation of ModuleIOTalonFXS (CANdi PWM 1). Please check the AdvantageKit documentation for more information on alternative configurations: https://docs.advantagekit.org/getting-started/template-projects/talonfx-swerve-template#custom-module-implementations");
                };
        turnConfig.ExternalFeedback.RotorToSensorRatio = constants.SteerMotorGearRatio;
        turnConfig.MotionMagic.MotionMagicCruiseVelocity = 100.0 / constants.SteerMotorGearRatio;
        turnConfig.MotionMagic.MotionMagicAcceleration =
                turnConfig.MotionMagic.MotionMagicCruiseVelocity / 0.100;
        turnConfig.MotionMagic.MotionMagicExpo_kV = 0.12 * constants.SteerMotorGearRatio;
        turnConfig.MotionMagic.MotionMagicExpo_kA = 0.1;
        turnConfig.ClosedLoopGeneral.ContinuousWrap = true;
        turnConfig.MotorOutput.Inverted =
                constants.SteerMotorInverted
                        ? InvertedValue.Clockwise_Positive
                        : InvertedValue.CounterClockwise_Positive;
        tryUntilOk(5, () -> turnTalon.getConfigurator().apply(turnConfig, 0.25));

        // Configure CANdi
        CANdiConfiguration candiConfig = constants.EncoderInitialConfigs;
        candiConfig.PWM1.AbsoluteSensorOffset = constants.EncoderOffset;
        candiConfig.PWM1.SensorDirection = constants.EncoderInverted;
        candi.getConfigurator().apply(candiConfig);

        // Create timestamp queue
        timestampQueue = PhoenixOdometryThread.getInstance().makeTimestampQueue();

        // Create drive status signals
        drivePosition = driveTalon.getPosition();
        drivePositionQueue =
                PhoenixOdometryThread.getInstance().registerSignal(drivePosition.clone());
        driveVelocity = driveTalon.getVelocity();
        driveAppliedVolts = driveTalon.getMotorVoltage();
        driveCurrent = driveTalon.getStatorCurrent();

        // Create turn status signals
        turnAbsolutePosition = candi.getPWM1Position();
        turnPosition = turnTalon.getPosition();
        turnPositionQueue =
                PhoenixOdometryThread.getInstance().registerSignal(turnPosition.clone());
        turnVelocity = turnTalon.getVelocity();
        turnAppliedVolts = turnTalon.getMotorVoltage();
        turnCurrent = turnTalon.getStatorCurrent();

        // Configure periodic frames
        BaseStatusSignal.setUpdateFrequencyForAll(
                Swerve.ODOMETRY_FREQUENCY, drivePosition, turnPosition);
        BaseStatusSignal.setUpdateFrequencyForAll(
                50.0,
                driveVelocity,
                driveAppliedVolts,
                driveCurrent,
                turnAbsolutePosition,
                turnVelocity,
                turnAppliedVolts,
                turnCurrent);
        ParentDevice.optimizeBusUtilizationForAll(driveTalon, turnTalon);
    }

    @Override
    public void updateInputs(ModuleIOInputs inputs) {
        // Refresh all signals
        var driveStatus =
                BaseStatusSignal.refreshAll(
                        drivePosition, driveVelocity, driveAppliedVolts, driveCurrent);
        var turnStatus =
                BaseStatusSignal.refreshAll(
                        turnPosition, turnVelocity, turnAppliedVolts, turnCurrent);
        var turnEncoderStatus = BaseStatusSignal.refreshAll(turnAbsolutePosition);

        // Update drive inputs
        inputs.driveConnected = driveConnectedDebounce.calculate(driveStatus.isOK());
        inputs.drivePosition = drivePosition.getValue();
        inputs.driveVelocity = driveVelocity.getValue();
        inputs.driveAppliedVolts = driveAppliedVolts.getValue();
        inputs.driveCurrentAmps = driveCurrent.getValue();

        // Update turn inputs
        inputs.turnConnected = turnConnectedDebounce.calculate(turnStatus.isOK());
        inputs.turnEncoderConnected =
                turnEncoderConnectedDebounce.calculate(turnEncoderStatus.isOK());
        inputs.turnAbsolutePosition =
                Rotation2d.fromRotations(turnAbsolutePosition.getValueAsDouble());
        inputs.turnPosition = Rotation2d.fromRotations(turnPosition.getValueAsDouble());
        inputs.turnVelocityRadPerSec = Units.rotationsToRadians(turnVelocity.getValueAsDouble());
        inputs.turnAppliedVolts = turnAppliedVolts.getValueAsDouble();
        inputs.turnCurrentAmps = turnCurrent.getValueAsDouble();

        // Update odometry inputs
        inputs.odometryTimestamps =
                timestampQueue.stream().mapToDouble((Double value) -> value).toArray();
        inputs.odometryDrivePositions =
                drivePositionQueue.stream()
                        .map((Double value) -> Rotations.of(value))
                        .toArray(Angle[]::new);
        inputs.odometryTurnPositions =
                turnPositionQueue.stream()
                        .map((Double value) -> Rotation2d.fromRotations(value))
                        .toArray(Rotation2d[]::new);
        timestampQueue.clear();
        drivePositionQueue.clear();
        turnPositionQueue.clear();
    }

    @Override
    public void applyOutputs(ModuleIOOutputs outputs) {
        switch (outputs.driveOutputMode) {
            case VELOCITY ->
                    driveTalon.setControl(
                            velocityVoltageRequest.withVelocity(outputs.driveTargetVelocity));
            case OPEN_LOOP ->
                    driveTalon.setControl(voltageRequest.withOutput(outputs.driveTargetOpenLoop));
        }
        switch (outputs.turnOutputMode) {
            case POSITION ->
                    turnTalon.setControl(
                            positionVoltageRequest.withPosition(
                                    outputs.turnTargetPosition.in(Rotations)));
            case OPEN_LOOP ->
                    turnTalon.setControl(voltageRequest.withOutput(outputs.turnTargetOpenLoop));
        }
    }
}
