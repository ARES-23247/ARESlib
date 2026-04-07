package org.areslib.math.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProfiledPIDController}.
 */
class ProfiledPIDControllerTest {

    private static final double EPSILON = 1e-6;

    @Test
    @DisplayName("Zero goal from zero start produces zero output")
    void zeroGoalZeroStart() {
        ProfiledPIDController controller = new ProfiledPIDController(1.0, 0, 0, 10.0, 5.0, 0.02);
        controller.setGoal(0.0);
        controller.reset(0.0, 0.0);
        double output = controller.calculate(0.0);
        assertEquals(0.0, output, EPSILON);
    }

    @Test
    @DisplayName("Setpoint advances towards goal over time")
    void setpointAdvances() {
        ProfiledPIDController controller = new ProfiledPIDController(1.0, 0, 0, 10.0, 5.0, 0.02);
        controller.setGoal(100.0);
        controller.reset(0.0, 0.0);
        
        // After one step, the setpoint should have advanced from 0
        controller.calculate(0.0);
        assertTrue(controller.getSetpointPosition() > 0.0, "Setpoint should advance towards goal");
    }

    @Test
    @DisplayName("Velocity respects maximum constraint")
    void velocityRespected() {
        double maxVel = 5.0;
        ProfiledPIDController controller = new ProfiledPIDController(0.0, 0, 0, maxVel, 100.0, 0.02);
        controller.setGoal(1000.0);
        controller.reset(0.0, 0.0);
        
        // Run many iterations to reach max velocity
        for (int i = 0; i < 100; i++) {
            controller.calculate(controller.getSetpointPosition());
        }
        
        assertTrue(controller.getSetpointVelocity() <= maxVel + EPSILON,
            "Velocity should not exceed maximum: got " + controller.getSetpointVelocity());
    }

    @Test
    @DisplayName("Profile decelerates near goal")
    void deceleratesNearGoal() {
        ProfiledPIDController controller = new ProfiledPIDController(1.0, 0, 0, 10.0, 5.0, 0.02);
        controller.setGoal(1.0);
        controller.reset(0.0, 0.0);
        
        double lastVelocity = 0;
        boolean accelerated = false;
        boolean decelerated = false;
        
        for (int i = 0; i < 200; i++) {
            controller.calculate(controller.getSetpointPosition());
            double vel = controller.getSetpointVelocity();
            if (vel > lastVelocity + EPSILON) accelerated = true;
            if (vel < lastVelocity - EPSILON && accelerated) decelerated = true;
            lastVelocity = vel;
        }
        
        assertTrue(accelerated, "Should accelerate initially");
        assertTrue(decelerated, "Should decelerate near goal");
    }

    @Test
    @DisplayName("Setpoint converges to goal")
    void converges() {
        ProfiledPIDController controller = new ProfiledPIDController(1.0, 0, 0, 10.0, 5.0, 0.02);
        controller.setGoal(5.0);
        controller.reset(0.0, 0.0);
        
        for (int i = 0; i < 1000; i++) {
            controller.calculate(controller.getSetpointPosition());
        }
        
        assertEquals(5.0, controller.getSetpointPosition(), 0.1);
        assertEquals(0.0, controller.getSetpointVelocity(), 0.1);
    }

    @Test
    @DisplayName("Zero period returns zero output")
    void zeroPeriod() {
        ProfiledPIDController controller = new ProfiledPIDController(1.0, 0, 0, 10.0, 5.0, 0.0);
        controller.setGoal(10.0);
        controller.reset(0.0, 0.0);
        double output = controller.calculate(0.0);
        assertEquals(0.0, output, EPSILON);
    }

    @Test
    @DisplayName("Negative goal direction works")
    void negativeGoal() {
        ProfiledPIDController controller = new ProfiledPIDController(1.0, 0, 0, 10.0, 5.0, 0.02);
        controller.setGoal(-5.0);
        controller.reset(0.0, 0.0);
        
        controller.calculate(0.0);
        assertTrue(controller.getSetpointPosition() < 0.0, "Setpoint should move in negative direction");
    }

    @Test
    @DisplayName("Reset sets position and velocity")
    void reset() {
        ProfiledPIDController controller = new ProfiledPIDController(1.0, 0, 0, 10.0, 5.0, 0.02);
        controller.setGoal(100.0);
        controller.reset(50.0, 3.0);
        assertEquals(50.0, controller.getSetpointPosition(), EPSILON);
        assertEquals(3.0, controller.getSetpointVelocity(), EPSILON);
    }
}
