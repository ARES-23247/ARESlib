package org.areslib.sim;

import java.lang.reflect.Method;
import java.util.Locale;
import org.dyn4j.world.World;

public class Scratch {
  public static void main(String[] args) {
    for (Method m : World.class.getMethods()) {
      if (m.getName().toLowerCase(Locale.ROOT).contains("raycast")) {
        com.qualcomm.robotcore.util.RobotLog.i(String.valueOf(m));
      }
    }
  }
}
