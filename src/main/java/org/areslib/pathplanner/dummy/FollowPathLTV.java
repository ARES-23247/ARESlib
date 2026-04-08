package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android
 * dependencies.
 */
import org.areslib.command.Command;

public class FollowPathLTV extends Command {
  public FollowPathLTV(Object... args) {}
}
