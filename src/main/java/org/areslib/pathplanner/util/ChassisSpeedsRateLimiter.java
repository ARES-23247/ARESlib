package org.areslib.pathplanner.util;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.kinematics.ChassisSpeeds;
public class ChassisSpeedsRateLimiter {
    public ChassisSpeedsRateLimiter(double min, double max) {}
    public void reset(ChassisSpeeds s) {}
    public ChassisSpeeds calculate(ChassisSpeeds s) { return s; }
}