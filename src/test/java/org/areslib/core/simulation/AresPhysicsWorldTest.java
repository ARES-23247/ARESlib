package org.areslib.core.simulation;

import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AresPhysicsWorldTest {

    @BeforeEach
    void setUp() {
        AresPhysicsWorld.getInstance().reset();
    }

    @AfterEach
    void tearDown() {
        AresPhysicsWorld.getInstance().reset();
    }

    @Test
    void testSingletonInstance() {
        AresPhysicsWorld instance1 = AresPhysicsWorld.getInstance();
        AresPhysicsWorld instance2 = AresPhysicsWorld.getInstance();
        assertSame(instance1, instance2, "AresPhysicsWorld should be a singleton");
    }

    @Test
    void testAddBodyAndStep() {
        AresPhysicsWorld physicsWorld = AresPhysicsWorld.getInstance();
        
        // Spawn a dynamic body
        Body robotBody = new Body();
        robotBody.addFixture(Geometry.createRectangle(0.5, 0.5));
        robotBody.setMass(MassType.NORMAL);
        
        // Apply initial velocity
        robotBody.setLinearVelocity(new Vector2(1.0, 0.0));
        
        physicsWorld.addBody(robotBody);
        
        assertTrue(physicsWorld.getWorld().containsBody(robotBody));
        
        // Step simulation 1 second (100 ticks of 0.01s)
        for(int i=0; i<100; i++) {
        	physicsWorld.step(0.01);
        }
        
        // Vector math verifies it has roughly moved 1 meter positively
        assertEquals(1.0, robotBody.getTransform().getTranslationX(), 0.05);
        assertEquals(0.0, robotBody.getTransform().getTranslationY(), 0.05);
    }
    
    @Test
    void testBoundaryCollisionConstraints() {
        AresPhysicsWorld physicsWorld = AresPhysicsWorld.getInstance();
        
        // Spawn Wall (static)
        Body wall = new Body();
        wall.addFixture(Geometry.createRectangle(0.1, 10.0));
        wall.setMass(MassType.INFINITE);
        wall.translate(1.0, 0.0);
        physicsWorld.addBody(wall);
        
        // Spawn Robot
        Body robotBody = new Body();
        robotBody.addFixture(Geometry.createRectangle(0.5, 0.5));
        robotBody.setMass(MassType.NORMAL);
        robotBody.translate(0.0, 0.0);
        robotBody.setLinearVelocity(new Vector2(10.0, 0.0)); // Huge velocity into wall
        
        physicsWorld.addBody(robotBody);
        
        // Step large amount
        for (int i=0; i<100; i++) {
            physicsWorld.step(0.01);
        }
        
        // Verify robot did not clip through the wall
        assertTrue(robotBody.getTransform().getTranslationX() < 1.0, "Robot should not cross the wall boundary");
    }
}
