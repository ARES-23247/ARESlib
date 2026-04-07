package org.areslib.pathplanner.util;
import org.areslib.math.geometry.Pose2d;
import org.areslib.pathplanner.path.PathPlannerPath;
import org.areslib.pathplanner.commands.PathPlannerAuto;
public class PPLibTelemetry {
    public static void setVelocities(double vx, double vy, double vr, double tv) {}
    public static void setPathInaccuracy(double i) {}
    public static void setCurrentPose(Pose2d p) {}
    public static void setTargetPose(Pose2d p) {}
    public static void setCurrentPath(PathPlannerPath p) {}
    public static void registerHotReloadPath(String n, PathPlannerPath p) {}
    public static void registerHotReloadAuto(String n, PathPlannerAuto a) {}
}