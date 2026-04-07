package org.areslib.pathplanner.dummy;

/**
 * A dummy Filesystem that redirects local path queries directly to src/main/deploy.
 */
import java.io.File;
public class Filesystem {
    public static File getDeployDirectory() { return new File("src/main/deploy"); }
}