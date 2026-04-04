package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.areslib.command.button.Trigger;

public class AresGamepad {
    
    private final Gamepad gamepad;

    public AresGamepad(Gamepad gamepad) {
        this.gamepad = gamepad;
    }

    public double getLeftX() { return gamepad.left_stick_x; }
    public double getLeftY() { return -gamepad.left_stick_y; } // Invert so Up is positive natively
    public double getRightX() { return gamepad.right_stick_x; }
    public double getRightY() { return -gamepad.right_stick_y; }

    public double getLeftTriggerAxis() { return gamepad.left_trigger; }
    public double getRightTriggerAxis() { return gamepad.right_trigger; }

    // WPILib-style Trigger Bindings
    public Trigger a() { return new Trigger(() -> gamepad.a); }
    public Trigger b() { return new Trigger(() -> gamepad.b); }
    public Trigger x() { return new Trigger(() -> gamepad.x); }
    public Trigger y() { return new Trigger(() -> gamepad.y); }

    public Trigger leftBumper() { return new Trigger(() -> gamepad.left_bumper); }
    public Trigger rightBumper() { return new Trigger(() -> gamepad.right_bumper); }

    public Trigger dpadUp() { return new Trigger(() -> gamepad.dpad_up); }
    public Trigger dpadDown() { return new Trigger(() -> gamepad.dpad_down); }
    public Trigger dpadLeft() { return new Trigger(() -> gamepad.dpad_left); }
    public Trigger dpadRight() { return new Trigger(() -> gamepad.dpad_right); }
}
