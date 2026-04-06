package org.areslib.hardware.wrappers;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.areslib.hardware.interfaces.VisionIO;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class LimelightVisionWrapper implements VisionIO {

    private final Limelight3A[] limelights;

    public LimelightVisionWrapper(HardwareMap hardwareMap, String... deviceNames) {
        // Officially supported in FTC SDK >= 10.1
        limelights = new Limelight3A[deviceNames.length];
        for (int i = 0; i < deviceNames.length; i++) {
            limelights[i] = hardwareMap.get(Limelight3A.class, deviceNames[i]);
            limelights[i].pipelineSwitch(0);
            limelights[i].start();
        }
    }

    @Override
    public void updateInputs(VisionInputs inputs) {
        LLResult bestResult = null;
        double maxTa = -1.0;

        // Iterate through all connected limelights to find the one with the highest target area
        for (Limelight3A limelight : limelights) {
            if (!limelight.isConnected()) continue;

            LLResult result = limelight.getLatestResult();
            if (result != null && result.isValid()) {
                if (result.getTa() > maxTa) {
                    maxTa = result.getTa();
                    bestResult = result;
                }
            }
        }

        if (bestResult == null) {
            inputs.hasTarget = false;
            inputs.latencyMs = 0;
            inputs.fiducialCount = 0;
            return;
        }

        inputs.hasTarget = true;
        inputs.tx = bestResult.getTx();
        inputs.ty = bestResult.getTy();
        inputs.ta = bestResult.getTa();
        inputs.pipelineIndex = bestResult.getPipelineIndex();
        inputs.fiducialCount = bestResult.getClassifierResults() != null ? bestResult.getClassifierResults().size() : 
                               (bestResult.getBarcodeResults() != null ? bestResult.getBarcodeResults().size() : 1);
        
        // Ensure network latency is accounted for
        inputs.latencyMs = bestResult.getCaptureLatency() + bestResult.getTargetingLatency();

        // Process Botpose (Returns WPI field-centric pose if Limelight is configured correctly)
        // FTC SDK stores Pose3d object format
        Pose3D botpose = bestResult.getBotpose();
        if (botpose != null) {
            inputs.botPose3d[0] = botpose.getPosition().toUnit(DistanceUnit.METER).x;
            inputs.botPose3d[1] = botpose.getPosition().toUnit(DistanceUnit.METER).y;
            inputs.botPose3d[2] = botpose.getPosition().toUnit(DistanceUnit.METER).z;
            
            double roll = botpose.getOrientation().getRoll(AngleUnit.RADIANS);
            double pitch = botpose.getOrientation().getPitch(AngleUnit.RADIANS);
            double yaw = botpose.getOrientation().getYaw(AngleUnit.RADIANS);
            
            // Euler to Quaternion [w, x, y, z] formulation
            double cr = Math.cos(roll * 0.5); double sr = Math.sin(roll * 0.5);
            double cp = Math.cos(pitch * 0.5); double sp = Math.sin(pitch * 0.5);
            double cy = Math.cos(yaw * 0.5); double sy = Math.sin(yaw * 0.5);
            
            inputs.botPose3d[3] = cr * cp * cy + sr * sp * sy; // W
            inputs.botPose3d[4] = sr * cp * cy - cr * sp * sy; // X
            inputs.botPose3d[5] = cr * sp * cy + sr * cp * sy; // Y
            inputs.botPose3d[6] = cr * cp * sy - sr * sp * cy; // Z
        }

        Pose3D botposeMT2 = bestResult.getBotpose_MT2();
        if (botposeMT2 != null) {
            inputs.botPoseMegaTag2[0] = botposeMT2.getPosition().toUnit(DistanceUnit.METER).x;
            inputs.botPoseMegaTag2[1] = botposeMT2.getPosition().toUnit(DistanceUnit.METER).y;
            inputs.botPoseMegaTag2[2] = botposeMT2.getPosition().toUnit(DistanceUnit.METER).z;
            
            double roll = botposeMT2.getOrientation().getRoll(AngleUnit.RADIANS);
            double pitch = botposeMT2.getOrientation().getPitch(AngleUnit.RADIANS);
            double yaw = botposeMT2.getOrientation().getYaw(AngleUnit.RADIANS);
            
            double cr = Math.cos(roll * 0.5); double sr = Math.sin(roll * 0.5);
            double cp = Math.cos(pitch * 0.5); double sp = Math.sin(pitch * 0.5);
            double cy = Math.cos(yaw * 0.5); double sy = Math.sin(yaw * 0.5);
            
            inputs.botPoseMegaTag2[3] = cr * cp * cy + sr * sp * sy; // W
            inputs.botPoseMegaTag2[4] = sr * cp * cy - cr * sp * sy; // X
            inputs.botPoseMegaTag2[5] = cr * sp * cy + sr * cp * sy; // Y
            inputs.botPoseMegaTag2[6] = cr * cp * sy - sr * sp * cy; // Z
        }

        // Loop again to package ALL valid individual camera poses into rawCameraPoses for visualization.
        int validCount = 0;
        for (Limelight3A limelight : limelights) {
            if (limelight.isConnected() && limelight.getLatestResult() != null && limelight.getLatestResult().isValid()) {
                if (limelight.getLatestResult().getBotpose() != null) {
                    validCount++;
                }
            }
        }
        
        inputs.rawCameraPoses = new double[validCount * 7];
        int idx = 0;
        
        for (Limelight3A limelight : limelights) {
            if (!limelight.isConnected() || limelight.getLatestResult() == null || !limelight.getLatestResult().isValid()) continue;
            
            Pose3D botposeCam = limelight.getLatestResult().getBotpose();
            if (botposeCam != null) {
                double poseX = botposeCam.getPosition().toUnit(DistanceUnit.METER).x;
                double poseY = botposeCam.getPosition().toUnit(DistanceUnit.METER).y;
                double poseZ = botposeCam.getPosition().toUnit(DistanceUnit.METER).z;
                
                double roll = botposeCam.getOrientation().getRoll(AngleUnit.RADIANS);
                double pitch = botposeCam.getOrientation().getPitch(AngleUnit.RADIANS);
                double yaw = botposeCam.getOrientation().getYaw(AngleUnit.RADIANS);
                
                double cr = Math.cos(roll * 0.5); double sr = Math.sin(roll * 0.5);
                double cp = Math.cos(pitch * 0.5); double sp = Math.sin(pitch * 0.5);
                double cy = Math.cos(yaw * 0.5); double sy = Math.sin(yaw * 0.5);
                
                inputs.rawCameraPoses[idx*7 + 0] = poseX;
                inputs.rawCameraPoses[idx*7 + 1] = poseY;
                inputs.rawCameraPoses[idx*7 + 2] = poseZ;
                inputs.rawCameraPoses[idx*7 + 3] = cr * cp * cy + sr * sp * sy; // W
                inputs.rawCameraPoses[idx*7 + 4] = sr * cp * cy - cr * sp * sy; // X
                inputs.rawCameraPoses[idx*7 + 5] = cr * sp * cy + sr * cp * sy; // Y
                inputs.rawCameraPoses[idx*7 + 6] = cr * cp * sy - sr * sp * cy; // Z
                
                idx++;
            }
        }
    }

    @Override
    public void setPipeline(int index) {
        for (Limelight3A limelight : limelights) {
            if (limelight != null && limelight.isConnected()) {
                limelight.pipelineSwitch(index);
            }
        }
    }

    /**
     * Provide raw access to the primary driver if teams need Limelight-specific deep functions.
     * Use sparingly to avoid breaking simulator portability.
     * @return the raw primary Limelight3A device driver
     */
    public Limelight3A getRawDriver() {
        return limelights.length > 0 ? limelights[0] : null;
    }
}
