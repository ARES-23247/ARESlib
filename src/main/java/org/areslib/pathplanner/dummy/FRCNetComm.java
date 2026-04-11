package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android
 * dependencies.
 */
public class FRCNetComm {
  public static class Instances {
    public static final int LANGUAGE_JAVA = 1;
  }

  public static class ResourceType {
    public static final int RESOURCE_TYPE_PATH_PLANNER = 1;
  }

  public static void frcNetworkCommunicationObserveUserProgramStarting() {}

  public static void frcNetworkCommunicationReserve() {}

  public static void report(int type, int inst) {}
}
