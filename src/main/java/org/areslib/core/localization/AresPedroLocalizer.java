package org.areslib.core.localization;

import org.areslib.hardware.interfaces.OdometryIO;
import com.pedropathing.localization.Localizer;
import com.pedropathing.geometry.Pose;
import com.pedropathing.math.Vector;

/**
 * ARESlib integration layer for Pedro Pathing's {@link Localizer}.
 * <p>
 * This class translates ARESLib's SI-unit based {@link OdometryIO} hardware abstractions
 * (meters, radians, m/s) into the specific imperial units (inches) expected by Pedro Pathing's
 * internal tracking algorithms. It handles coordinate offsets to support mid-match pathing updates.
 */
public class AresPedroLocalizer implements Localizer {
    private final OdometryIO.OdometryInputs inputs;
    
    // Internal state to handle Pedro's tracking of custom starting offsets
    private double offsetXInches = 0.0;
    private double offsetYInches = 0.0;
    private double offsetHeadingRadians = 0.0;

    /**
     * Constructs a new AresPedroLocalizer bound to a specific hardware odometry source.
     *
     * @param inputs The populated {@link OdometryIO.OdometryInputs} data object from the hardware layer.
     */
    public AresPedroLocalizer(OdometryIO.OdometryInputs inputs) {
        this.inputs = inputs;
    }

    /**
     * Gets the current field-centric pose of the robot.
     * Converts ARESlib meters internally into Pedro Pathing inches.
     *
     * @return The Cartesian coordinates and heading wrapped in a {@link Pose}.
     */
    @Override
    public Pose getPose() {
        double rawXInches = inputs.xMeters / 0.0254;
        double rawYInches = inputs.yMeters / 0.0254;
        
        double rotatedX = rawXInches * Math.cos(offsetHeadingRadians) - rawYInches * Math.sin(offsetHeadingRadians);
        double rotatedY = rawXInches * Math.sin(offsetHeadingRadians) + rawYInches * Math.cos(offsetHeadingRadians);
        
        return new Pose(
            rotatedX + offsetXInches,
            rotatedY + offsetYInches,
            inputs.headingRadians + offsetHeadingRadians
        );
    }

    /**
     * Gets the current velocity vector of the robot.
     * Conversions applied mapping m/s to in/s.
     *
     * @return A {@link Pose} where X = xVelocity, Y = yVelocity, and Heading = angularVelocity.
     */
    @Override
    public Pose getVelocity() {
        double rawVx = inputs.xVelocityMetersPerSecond / 0.0254;
        double rawVy = inputs.yVelocityMetersPerSecond / 0.0254;
        
        double rotatedVx = rawVx * Math.cos(offsetHeadingRadians) - rawVy * Math.sin(offsetHeadingRadians);
        double rotatedVy = rawVx * Math.sin(offsetHeadingRadians) + rawVy * Math.cos(offsetHeadingRadians);

        return new Pose(
            rotatedVx,
            rotatedVy,
            inputs.angularVelocityRadiansPerSecond
        );
    }

    /**
     * Retrieves the strictly positional velocity of the robot mapped as a Vector.
     *
     * @return A {@link Vector} containing planar X/Y inch/sec velocity.
     */
    @Override
    public Vector getVelocityVector() {
        Pose vel = getVelocity();
        Vector vec = new Vector();
        vec.setOrthogonalComponents(vel.getX(), vel.getY());
        return vec;
    }

    /**
     * Unused interface method. Start pose is dictated by {@link #setPose(Pose)}.
     */
    @Override
    public void setStartPose(Pose setStart) {
        // Unused in ARES
    }

    /**
     * Injects a specific absolute pose onto the robot.
     * Handled by calculating an integer offset over the raw hardware data returned by sensors.
     *
     * @param setPose The desired global coordinate pose.
     */
    @Override
    public void setPose(Pose setPose) {
        double currentRawX = inputs.xMeters / 0.0254;
        double currentRawY = inputs.yMeters / 0.0254;
        
        offsetHeadingRadians = setPose.getHeading() - inputs.headingRadians;
        
        // We need to rotate the current raw coordinates into the new global frame
        double rotatedRawX = currentRawX * Math.cos(offsetHeadingRadians) - currentRawY * Math.sin(offsetHeadingRadians);
        double rotatedRawY = currentRawX * Math.sin(offsetHeadingRadians) + currentRawY * Math.cos(offsetHeadingRadians);
        
        // The offset is the difference between the requested pose and the rotated raw position
        offsetXInches = setPose.getX() - rotatedRawX;
        offsetYInches = setPose.getY() - rotatedRawY;
    }

    /**
     * Core update routine invoked by Pedro Follower.
     * Left intentionally empty because ARESlib updates logic concurrently within {@code CommandScheduler}.
     */
    @Override
    public void update() {
        // Nothing to compute specifically here because ares hardware wrappers update internal IO state
        // asynchronously or strictly sequentially before Pedro updates.
    }

    /**
     * Gets the absolute heading of the robot including initialization offsets.
     */
    @Override
    public double getTotalHeading() {
        return inputs.headingRadians + offsetHeadingRadians;
    }

    /** Tracking wheel functional variable. Unused. */
    @Override
    public double getForwardMultiplier() {
        return 1.0;
    }

    /** Tracking wheel functional variable. Unused. */
    @Override
    public double getLateralMultiplier() {
        return 1.0;
    }

    /** Tracking wheel functional variable. Unused. */
    @Override
    public double getTurningMultiplier() {
        return 1.0;
    }

    /** Overriding the built-in IMU reset function. Unused as ARESlib sensors handle orientation externally. */
    @Override
    public void resetIMU() throws InterruptedException {
        // Handled outside of localizer
    }

    /** Retrieve absolute heading directly from sensors. */
    @Override
    public double getIMUHeading() {
        return inputs.headingRadians;
    }

    /** Detects if internal hardware mapping returned NaN invalid outputs. */
    @Override
    public boolean isNAN() {
        return Double.isNaN(inputs.xMeters) || Double.isNaN(inputs.yMeters);
    }

    /** Programmatically inject a raw X position without altering the rest of the pose. */
    @Override
    public void setX(double x) {
        Pose current = getPose();
        setPose(new Pose(x, current.getY(), current.getHeading()));
    }

    /** Programmatically inject a raw Y position without altering the rest of the pose. */
    @Override
    public void setY(double y) {
        Pose current = getPose();
        setPose(new Pose(current.getX(), y, current.getHeading()));
    }

    /** Programmatically inject a raw angular rotation without altering the rest of the pose. */
    @Override
    public void setHeading(double heading) {
        Pose current = getPose();
        setPose(new Pose(current.getX(), current.getY(), heading));
    }
}
