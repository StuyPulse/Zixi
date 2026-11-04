package com.stuypulse.robot.subsystems.vision;

import static org.wpilib.units.Units.Meters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.photonvision.PhotonCamera;
import org.photonvision.PhotonUtils;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.math.util.Units;

public class VisionIOObjDetectionPhoton implements VisionIO {
    private final PhotonCamera camera;
    private final Transform3d robotToCamera;
    private final Supplier<Pose2d> robotPoseSupplier;

    public VisionIOObjDetectionPhoton(String cameraName, Transform3d robotToCamera, Supplier<Pose2d> robotPoseSupplier) {
        camera = new PhotonCamera(cameraName);

        this.robotToCamera = robotToCamera;

        this.robotPoseSupplier = robotPoseSupplier;
    }

    private Optional<Pose3d> getObjectFieldPose(PhotonTrackedTarget target) {

        double yawRad = Units.degreesToRadians(-target.getYaw()); // negative
        double pitchRad = Units.degreesToRadians(target.getPitch());

        double cameraPitchRad = -robotToCamera.getRotation().getY(); // negative

        double distance = PhotonUtils.calculateDistanceToTargetMeters(
            robotToCamera.getZ(),
            VisionConstants.VisionSettings.OBJ_DET_TARGET_HEIGHT.in(Meters),
            cameraPitchRad,
            pitchRad
        );

        if (distance <= 0 || Double.isNaN(distance)) {
            return Optional.empty();
        }

        Translation3d camToPiece = new Translation3d(PhotonUtils.estimateCameraToTargetTranslation(distance, new Rotation2d(yawRad)));

        Pose3d fieldToCamera = new Pose3d(robotPoseSupplier.get()).transformBy(robotToCamera);
        Pose3d fieldToPiece = fieldToCamera.transformBy(
            new Transform3d(camToPiece, new Rotation3d())
        );

        return Optional.of(fieldToPiece);
    }

    @Override 
    public void updateInputs(VisionIOInputs inputs) {
        inputs.connected = camera.isConnected();

        List<Pose3d> objectPoses = new ArrayList<>(); 

        for (var result: camera.getAllUnreadResults()) {
            if (!result.hasTargets()) {
                continue;
            }

            for (PhotonTrackedTarget target: result.getTargets()) {
                var objectPose = getObjectFieldPose(target);

                if (objectPose.isPresent()) {
                    objectPoses.add(objectPose.get());
                } 
            }
        }

        inputs.objectPoses = objectPoses.toArray(Pose3d[]::new);
    }

    @Override 
    public void applyOutputs(VisionIOOutputs outputs) {
        camera.setPipelineIndex(outputs.pipeline);
    }
}
