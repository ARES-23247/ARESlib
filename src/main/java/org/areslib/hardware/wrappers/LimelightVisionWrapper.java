package org.areslib.hardware.wrappers;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.areslib.hardware.interfaces.VisionIO;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class LimelightVisionWrapper implements VisionIO {

    private final Limelight3A limelight;

    public LimelightVisionWrapper(HardwareMap hardwareMap, String deviceName) {
        // Officially supported in FTC SDK >= 10.1
        this.limelight = hardwareMap.get(Limelight3A.class, deviceName);
        this.limelight.pipelineSwitch(0);
        this.limelight.start();
    }

    @Override
    public void updateInputs(VisionInputs inputs) {
        if (!limelight.isConnected()) {
            inputs.hasTarget = false;
            return;
        }

        LLResult result = limelight.getLatestResult();
        
        if (result == null || !result.isValid()) {
            inputs.hasTarget = false;
            inputs.latencyMs = 0;
            inputs.fiducialCount = 0;
            return;
        }

        inputs.hasTarget = true;
        inputs.tx = result.getTx();
        inputs.ty = result.getTy();
        inputs.ta = result.getTa();
        inputs.pipelineIndex = result.getPipelineIndex();
        inputs.fiducialCount = result.getClassifierResults() != null ? result.getClassifierResults().size() : 
                               (result.getBarcodeResults() != null ? result.getBarcodeResults().size() : 1);
        
        // FId details usually hidden inside arrays for complex multi-tag operations, 
        // fallback to standard Pose3d processing.
        
        // Ensure network latency is accounted for
        inputs.latencyMs = result.getCaptureLatency() + result.getTargetingLatency();

        // Process Botpose (Returns WPI field-centric pose if Limelight is configured correctly)
        // FTC SDK stores Pose3d object format
        Pose3D botpose = result.getBotpose();
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

        Pose3D botposeMT2 = result.getBotpose_MT2();
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
    }

    @Override
    public void setPipeline(int index) {
        if (limelight != null) {
            limelight.pipelineSwitch(index);
        }
    }

    /**
     * Provide raw access to the driver if teams need Limelight-specific deep functions.
     * Use sparingly to avoid breaking simulator portability.
     * @return the raw Limelight3A device driver
     */
    public Limelight3A getRawDriver() {
        return limelight;
    }
}
