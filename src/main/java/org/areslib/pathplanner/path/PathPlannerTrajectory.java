package org.areslib.pathplanner.path;

import java.util.*;
import org.areslib.command.Command;
import org.areslib.math.Pair;
import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.pathplanner.util.GeometryUtil;

/** Trajectory created from a pathplanner path */
public class PathPlannerTrajectory {
  private final List<State> states;
  private final List<Pair<Double, Command>> eventCommands;

  /**
   * Generate a PathPlannerTrajectory
   *
   * @param path {@link org.areslib.pathplanner.path.PathPlannerPath} to generate the trajectory for
   * @param startingSpeeds Starting speeds of the robot when starting the trajectory
   * @param startingRotation Starting rotation of the robot when starting the trajectory
   */
  public PathPlannerTrajectory(
      PathPlannerPath path, ChassisSpeeds startingSpeeds, Rotation2d startingRotation) {
    if (path.isChoreoPath()) {
      PathPlannerTrajectory traj = path.getTrajectory(startingSpeeds, startingRotation);
      this.states = traj.states;
      this.eventCommands = traj.eventCommands;
    } else {
      this.states = generateStates(path, startingSpeeds, startingRotation);
      this.eventCommands = new ArrayList<>();

      for (EventMarker m : path.getEventMarkers()) {
        int pointIndex = (int) Math.round(m.getWaypointRelativePos() / PathSegment.RESOLUTION);
        eventCommands.add(Pair.of(states.get(pointIndex).timeSeconds, m.getCommand()));
      }

      eventCommands.sort(Comparator.comparing(Pair::getFirst));
    }
  }

  /**
   * Create a PathPlannerTrajectory from pre-generated states and event command timings.
   *
   * @param states Pre-generated trajectory states
   * @param eventCommands Pairs of timestamps + commands to run at those timestamps
   */
  public PathPlannerTrajectory(List<State> states, List<Pair<Double, Command>> eventCommands) {
    this.states = states;
    this.eventCommands = eventCommands;
  }

  /**
   * Create a PathPlannerTrajectory from pre-generated states.
   *
   * @param states Pre-generated trajectory states
   */
  public PathPlannerTrajectory(List<State> states) {
    this(states, Collections.emptyList());
  }

  private static int getNextRotationTargetIdx(PathPlannerPath path, int startingIndex) {
    int idx = path.numPoints() - 1;

    for (int i = startingIndex; i < path.numPoints() - 1; i++) {
      if (path.getPoint(i).rotationTarget != null) {
        idx = i;
        break;
      }
    }

    return idx;
  }

  private static List<State> generateStates(
      PathPlannerPath path, ChassisSpeeds startingSpeeds, Rotation2d startingRotation) {
    List<State> states = new ArrayList<>();

    double startVel =
        Math.hypot(startingSpeeds.vxMetersPerSecond, startingSpeeds.vyMetersPerSecond);

    double prevRotationTargetDist = 0.0;
    Rotation2d prevRotationTargetRot = startingRotation;
    int nextRotationTargetIdx = getNextRotationTargetIdx(path, 0);
    double distanceBetweenTargets = path.getPoint(nextRotationTargetIdx).distanceAlongPath;

    // Initial pass. Creates all states and handles linear acceleration
    for (int i = 0; i < path.numPoints(); i++) {
      State state = new State();

      PathConstraints constraints = path.getPoint(i).constraints;
      state.constraints = constraints;

      if (i > nextRotationTargetIdx) {
        prevRotationTargetDist = path.getPoint(nextRotationTargetIdx).distanceAlongPath;
        prevRotationTargetRot = path.getPoint(nextRotationTargetIdx).rotationTarget.getTarget();
        nextRotationTargetIdx = getNextRotationTargetIdx(path, i);
        distanceBetweenTargets =
            path.getPoint(nextRotationTargetIdx).distanceAlongPath - prevRotationTargetDist;
      }

      RotationTarget nextTarget = path.getPoint(nextRotationTargetIdx).rotationTarget;

      if (nextTarget.shouldRotateFast()) {
        state.targetHolonomicRotation = nextTarget.getTarget();
      } else {
        double interpolationFactor =
            (path.getPoint(i).distanceAlongPath - prevRotationTargetDist) / distanceBetweenTargets;
        interpolationFactor = Math.min(Math.max(0.0, interpolationFactor), 1.0);
        if (!Double.isFinite(interpolationFactor)) {
          interpolationFactor = 0.0;
        }

        state.targetHolonomicRotation =
            prevRotationTargetRot.interpolate(nextTarget.getTarget(), interpolationFactor);
      }

      state.positionMeters = path.getPoint(i).position;
      double curveRadius = path.getPoint(i).curveRadius;
      state.curvatureRadPerMeter =
          (Double.isFinite(curveRadius) && curveRadius != 0) ? 1.0 / curveRadius : 0.0;

      if (i == path.numPoints() - 1) {
        state.heading = states.get(states.size() - 1).heading;
        state.deltaPos =
            path.getPoint(i).distanceAlongPath - path.getPoint(i - 1).distanceAlongPath;
        state.velocityMps = path.getGoalEndState().getVelocity();
      } else if (i == 0) {
        state.heading = path.getPoint(i + 1).position.minus(state.positionMeters).getAngle();
        state.deltaPos = 0;
        state.velocityMps = startVel;
      } else {
        state.heading = path.getPoint(i + 1).position.minus(state.positionMeters).getAngle();
        state.deltaPos =
            path.getPoint(i + 1).distanceAlongPath - path.getPoint(i).distanceAlongPath;

        double v0 = states.get(states.size() - 1).velocityMps;
        double vMax =
            Math.sqrt(
                Math.abs(
                    Math.pow(v0, 2)
                        + (2 * constraints.getMaxAccelerationMpsSq() * state.deltaPos)));
        state.velocityMps = Math.min(vMax, path.getPoint(i).maxV);
      }

      states.add(state);
    }

    // Second pass. Handles linear deceleration
    for (int i = states.size() - 2; i > 1; i--) {
      PathConstraints constraints = states.get(i).constraints;

      double v0 = states.get(i + 1).velocityMps;

      double vMax =
          Math.sqrt(
              Math.abs(
                  Math.pow(v0, 2)
                      + (2 * constraints.getMaxAccelerationMpsSq() * states.get(i + 1).deltaPos)));
      states.get(i).velocityMps = Math.min(vMax, states.get(i).velocityMps);
    }

    // Final pass. Calculates time, linear acceleration, and angular velocity
    double time = 0;
    states.get(0).timeSeconds = 0;
    states.get(0).accelerationMpsSq = 0;
    states.get(0).headingAngularVelocityRps = startingSpeeds.omegaRadiansPerSecond;

    for (int i = 1; i < states.size(); i++) {
      double v0 = states.get(i - 1).velocityMps;
      double velocityMetersPerSecond = states.get(i).velocityMps;
      double dt = (2 * states.get(i).deltaPos) / (velocityMetersPerSecond + v0);

      time += dt;
      states.get(i).timeSeconds = time;

      double dv = velocityMetersPerSecond - v0;
      states.get(i).accelerationMpsSq = dv / dt;

      Rotation2d headingDelta = states.get(i).heading.minus(states.get(i - 1).heading);
      states.get(i).headingAngularVelocityRps = headingDelta.getRadians() / dt;
    }

    return states;
  }

  /**
   * Get the target state at the given point in time along the trajectory.
   *
   * @param time The time to sample the trajectory at in seconds
   * @return A NEW target state instance. Use the mutable sample() for zero-GC paths.
   */
  public State sample(double time) {
    State result = new State();
    sample(time, result);
    return result;
  }

  /**
   * Get the target state at the given point in time along the trajectory in-place. Eliminates
   * allocation overhead in the path-following loop.
   *
   * @param time The time to sample the trajectory at in seconds
   * @param out The state object to populate with the sampled data.
   */
  public void sample(double time, State out) {
    if (time <= getInitialState().timeSeconds) {
      out.set(getInitialState());
      return;
    }
    if (time >= getTotalTimeSeconds()) {
      out.set(getEndState());
      return;
    }

    int low = 1;
    int high = getStates().size() - 1;

    while (low != high) {
      int mid = (low + high) / 2;
      if (getState(mid).timeSeconds < time) {
        low = mid + 1;
      } else {
        high = mid;
      }
    }

    State sample = getState(low);
    State prevSample = getState(low - 1);

    if (Math.abs(sample.timeSeconds - prevSample.timeSeconds) < 1E-3) {
      out.set(sample);
      return;
    }

    prevSample.interpolate(
        sample,
        (time - prevSample.timeSeconds) / (sample.timeSeconds - prevSample.timeSeconds),
        out);
  }

  /**
   * Get all of the pre-generated states in the trajectory
   *
   * @return List of all states
   */
  public List<State> getStates() {
    return states;
  }

  /**
   * Get all of the pairs of timestamps + commands to run at those timestamps
   *
   * @return Pairs of timestamps and event commands
   */
  public List<Pair<Double, Command>> getEventCommands() {
    return eventCommands;
  }

  /**
   * Get the total run time of the trajectory
   *
   * @return Total run time in seconds
   */
  public double getTotalTimeSeconds() {
    return getEndState().timeSeconds;
  }

  /**
   * Get the goal state at the given index
   *
   * @param index Index of the state to get
   * @return The state at the given index
   */
  public State getState(int index) {
    return getStates().get(index);
  }

  /**
   * Get the initial state of the trajectory
   *
   * @return The initial state
   */
  public State getInitialState() {
    return getState(0);
  }

  /**
   * Get the initial target pose for a holonomic drivetrain NOTE: This is a "target" pose, meaning
   * the rotation will be the value of the next rotation target along the path, not what the
   * rotation should be at the start of the path
   *
   * @return The initial target pose
   */
  public Pose2d getInitialTargetHolonomicPose() {
    return getInitialState().getTargetHolonomicPose();
  }

  /**
   * Get this initial pose for a differential drivetrain
   *
   * @return The initial pose
   */
  public Pose2d getInitialDifferentialPose() {
    return getInitialState().getDifferentialPose();
  }

  /**
   * Get the end state of the trajectory
   *
   * @return The end state
   */
  public State getEndState() {
    return getState(getStates().size() - 1);
  }

  /** A state along the trajectory */
  public static class State {
    /** The time at this state in seconds */
    public double timeSeconds = 0;

    /** The velocity at this state in m/s */
    public double velocityMps = 0;

    /** The acceleration at this state in m/s^2 */
    public double accelerationMpsSq = 0;

    /** The time at this state in seconds */
    public double headingAngularVelocityRps = 0;

    /** The position at this state in meters */
    public Translation2d positionMeters = new Translation2d();

    /** The heading (direction of travel) at this state */
    public Rotation2d heading = new Rotation2d();

    /** The target holonomic rotation at this state */
    public Rotation2d targetHolonomicRotation = new Rotation2d();

    /** Optional holonomic angular velocity. Will only be provided for choreo paths */
    public Optional<Double> holonomicAngularVelocityRps = Optional.empty();

    /** The curvature at this state in rad/m */
    public double curvatureRadPerMeter = 0;

    /** The constraints to apply at this state */
    public PathConstraints constraints;

    /** Sets this state to match another state in-place. */
    public void set(State other) {
      this.timeSeconds = other.timeSeconds;
      this.velocityMps = other.velocityMps;
      this.accelerationMpsSq = other.accelerationMpsSq;
      this.headingAngularVelocityRps = other.headingAngularVelocityRps;
      this.positionMeters.set(other.positionMeters);
      this.heading.set(other.heading);
      this.targetHolonomicRotation.set(other.targetHolonomicRotation);
      this.holonomicAngularVelocityRps = other.holonomicAngularVelocityRps;
      this.curvatureRadPerMeter = other.curvatureRadPerMeter;
      this.constraints = other.constraints;
      this.deltaPos = other.deltaPos;
    }

    // Values only used during generation
    private double deltaPos = 0;

    /**
     * Interpolate between this state and the given state
     *
     * @param endVal State to interpolate with
     * @param t Interpolation factor (0.0-1.0)
     * @return A NEW target state instance.
     */
    public State interpolate(State endVal, double t) {
      State lerpedState = new State();
      interpolate(endVal, t, lerpedState);
      return lerpedState;
    }

    /**
     * Interpolate between this state and the given state in-place.
     *
     * @param endVal State to interpolate with
     * @param t Interpolation factor (0.0-1.0)
     * @param out The state object to populate with the interpolated data.
     */
    public void interpolate(State endVal, double t, State out) {
      out.timeSeconds = GeometryUtil.doubleLerp(timeSeconds, endVal.timeSeconds, t);
      double deltaT = out.timeSeconds - timeSeconds;

      if (deltaT < 0) {
        endVal.interpolate(this, 1 - t, out);
        return;
      }

      out.velocityMps = GeometryUtil.doubleLerp(velocityMps, endVal.velocityMps, t);
      out.accelerationMpsSq =
          GeometryUtil.doubleLerp(accelerationMpsSq, endVal.accelerationMpsSq, t);
      out.positionMeters.set(positionMeters.interpolate(endVal.positionMeters, t));
      out.heading.set(heading.interpolate(endVal.heading, t));
      out.headingAngularVelocityRps =
          GeometryUtil.doubleLerp(headingAngularVelocityRps, endVal.headingAngularVelocityRps, t);
      out.curvatureRadPerMeter =
          GeometryUtil.doubleLerp(curvatureRadPerMeter, endVal.curvatureRadPerMeter, t);
      out.deltaPos = GeometryUtil.doubleLerp(deltaPos, endVal.deltaPos, t);

      if (holonomicAngularVelocityRps.isPresent()
          && endVal.holonomicAngularVelocityRps.isPresent()) {
        out.holonomicAngularVelocityRps =
            Optional.of(
                GeometryUtil.doubleLerp(
                    holonomicAngularVelocityRps.get(),
                    endVal.holonomicAngularVelocityRps.get(),
                    t));
      } else {
        out.holonomicAngularVelocityRps = Optional.empty();
      }

      out.targetHolonomicRotation.set(
          targetHolonomicRotation.interpolate(endVal.targetHolonomicRotation, t));

      if (t < 0.5) {
        out.constraints = constraints;
      } else {
        out.constraints = endVal.constraints;
      }
    }

    /**
     * Get the target pose for a holonomic drivetrain NOTE: This is a "target" pose, meaning the
     * rotation will be the value of the next rotation target along the path, not what the rotation
     * should be at the start of the path
     *
     * @return The target pose
     */
    public Pose2d getTargetHolonomicPose() {
      return new Pose2d(positionMeters, targetHolonomicRotation);
    }

    /**
     * Get this pose for a differential drivetrain
     *
     * @return The pose
     */
    public Pose2d getDifferentialPose() {
      return new Pose2d(positionMeters, heading);
    }

    /**
     * Get the state reversed, used for following a trajectory reversed with a differential
     * drivetrain
     *
     * @return The reversed state
     */
    public State reverse() {
      State reversed = new State();

      reversed.timeSeconds = timeSeconds;
      reversed.velocityMps = -velocityMps;
      reversed.accelerationMpsSq = -accelerationMpsSq;
      reversed.headingAngularVelocityRps = -headingAngularVelocityRps;
      reversed.positionMeters = positionMeters;
      reversed.heading =
          Rotation2d.fromDegrees(
              (((heading.getDegrees() + 180 + 180.0) % 360.0 + 360.0) % 360.0 - 180.0));
      reversed.targetHolonomicRotation = targetHolonomicRotation;
      reversed.holonomicAngularVelocityRps = holonomicAngularVelocityRps;
      reversed.curvatureRadPerMeter = -curvatureRadPerMeter;
      reversed.deltaPos = deltaPos;
      reversed.constraints = constraints;

      return reversed;
    }
  }
}
