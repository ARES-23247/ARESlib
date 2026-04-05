package org.areslib.sim;

import org.dyn4j.world.World;
import java.lang.reflect.Method;
public class Scratch {
    public static void main(String[] args) {
        for (Method m : World.class.getMethods()) {
            if (m.getName().toLowerCase().contains("raycast")) {
                System.out.println(m);
            }
        }
    }
}
