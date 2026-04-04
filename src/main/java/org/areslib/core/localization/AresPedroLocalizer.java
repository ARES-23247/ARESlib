package org.areslib.core.localization;

import org.areslib.hardware.interfaces.OdometryIO;
import com.pedropathing.localization.Localizer;
import com.pedropathing.geometry.Pose;
import com.pedropathing.math.Vector;

public class AresPedroLocalizer implements Localizer {
    private final OdometryIO.OdometryInputs inputs;
    
    // Internal state to handle Pedro's tracking of custom starting offsets 
    private Pose startPose = new Pose(0, 0, 0);
    private Pose userPoseOffset = new Pose(0, 0, 0);

    public AresPedroLocalizer(OdometryIO.OdometryInputs inputs) {
        this.inputs = inputs;
    }

    @Override
    public Pose getPose() {
        // Apply the offset to the literal hardware inputs (converted to Pedro's units, which is inches and radians)
        // ARESlib reads inputs natively in meters.
        double xInches = inputs.xMeters / 0.0254;
        double yInches = inputs.yMeters / 0.0254;
        
        return new Pose(
            xInches + userPoseOffset.getX(),
            yInches + userPoseOffset.getY(),
            inputs.headingRadians + userPoseOffset.getHeading()
        );
    }

    @Override
    public Pose getVelocity() {
        // Converted from m/s to in/s
        return new Pose(
            inputs.xVelocityMetersPerSecond / 0.0254,
            inputs.yVelocityMetersPerSecond / 0.0254,
            inputs.angularVelocityRadiansPerSecond
        );
    }

    @Override
    public Vector getVelocityVector() {
        Pose vel = getVelocity();
        Vector vec = new Vector();
        vec.setOrthogonalComponents(vel.getX(), vel.getY());
        return vec;
    }

    @Override
    public void setStartPose(Pose setStart) {
        this.startPose = setStart;
    }

    @Override
    public void setPose(Pose setPose) {
        // Calculate what the offset needs to be to make getPose() equal to setPose
        double currentRawX = inputs.xMeters / 0.0254;
        double currentRawY = inputs.yMeters / 0.0254;
        
        userPoseOffset = new Pose(
            setPose.getX() - currentRawX,
            setPose.getY() - currentRawY,
            setPose.getHeading() - inputs.headingRadians
        );
    }

    @Override
    public void update() {
        // Nothing to compute specifically here because ares hardware wrappers update internal IO state
        // asynchronously or strictly sequentially before Pedro updates.
    }

    @Override
    public double getTotalHeading() {
        return inputs.headingRadians + userPoseOffset.getHeading();
    }

    @Override
    public double getForwardMultiplier() {
        return 1.0;
    }

    @Override
    public double getLateralMultiplier() {
        return 1.0;
    }

    @Override
    public double getTurningMultiplier() {
        return 1.0;
    }

    @Override
    public void resetIMU() throws InterruptedException {
        // Handled outside of localizer
    }

    @Override
    public double getIMUHeading() {
        return inputs.headingRadians;
    }

    @Override
    public boolean isNAN() {
        return Double.isNaN(inputs.xMeters) || Double.isNaN(inputs.yMeters);
    }

    @Override
    public void setX(double x) {
        Pose current = getPose();
        setPose(new Pose(x, current.getY(), current.getHeading()));
    }

    @Override
    public void setY(double y) {
        Pose current = getPose();
        setPose(new Pose(current.getX(), y, current.getHeading()));
    }

    @Override
    public void setHeading(double heading) {
        Pose current = getPose();
        setPose(new Pose(current.getX(), current.getY(), heading));
    }
}
