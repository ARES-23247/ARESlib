package org.areslib.hardware;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.areslib.core.simulation.AresPhysicsWorld;
import org.areslib.hardware.interfaces.ArrayLidarIO;
import org.areslib.hardware.interfaces.OdometryIO.OdometryInputs;
import org.areslib.hardware.wrappers.ArrayLidarIOSim;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArrayLidarIOSimTest {

    private World<Body> world;
    private OdometryInputs currentOdo;
    private ArrayLidarIOSim lidarSim;

    @BeforeEach
    public void setup() {
        AresPhysicsWorld physicsWorld = AresPhysicsWorld.getInstance();
        physicsWorld.reset(); // clear the world
        this.world = physicsWorld.getWorld();
        
        currentOdo = new OdometryInputs();
        currentOdo.xMeters = 0.0;
        currentOdo.yMeters = 0.0;
        currentOdo.headingRadians = 0.0; // Pointing along positive X

        lidarSim = new ArrayLidarIOSim(() -> currentOdo, world);
    }

    @Test
    public void testRaycastHitsObject() {
        // Create an "Obelisk" object 1.0 meters directly in front of the robot (positive X axis)
        Body obelisk = new Body();
        BodyFixture fixture = obelisk.addFixture(Geometry.createRectangle(0.5, 0.5));
        obelisk.translate(1.0, 0.0); // 1 meter dead ahead
        world.addBody(obelisk);

        // Update inputs
        ArrayLidarIO.ArrayLidarInputs inputs = new ArrayLidarIO.ArrayLidarInputs();
        lidarSim.updateInputs(inputs);

        // Standard MAX_RANGE is 4 meters (4000 mm). The center rays should hit the obelisk at ~0.75m to 1.0m (750mm - 1000mm depending on geometry edge)
        // With an 8x8 (64px) array, center pixels should detect it. Let's check if ANY pixel reads < 4000
        boolean hit = false;
        for (double dist : inputs.distanceZonesMm) {
            if (dist < 3999.0) {
                hit = true;
                break;
            }
        }

        assertTrue(hit, "Lidar simulation should have detected the Obelisk 1m directly ahead using physics raycasts.");
    }
}
