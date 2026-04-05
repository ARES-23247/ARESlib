package org.areslib.sim;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

public class VirtualGamepadWrapper {

    public final Gamepad gamepad = new Gamepad();
    private ControllerManager controllerManager;
    private final DesktopKeyboardListener keyboardListener;

    public enum InputMode {
        PHYSICAL,
        KEYBOARD
    }

    private InputMode currentMode = InputMode.KEYBOARD;

    public VirtualGamepadWrapper(DesktopKeyboardListener keyboardListener) {
        this.keyboardListener = keyboardListener;
        try {
            controllerManager = new ControllerManager();
            controllerManager.initSDLGamepad();
            System.out.println("[VirtualGamepad] SDL2 Jamepad Initialized Successfully.");
        } catch (Exception e) {
            System.err.println("[VirtualGamepad] Failed to initialize SDL2: " + e.getMessage());
            controllerManager = null;
        }
    }

    public void setInputMode(InputMode mode) {
        this.currentMode = mode;
    }

    public InputMode getInputMode() {
        return this.currentMode;
    }

    public ControllerManager getControllerManager() {
        return controllerManager;
    }

    public void update() {
        // Reset gamepad state to 0
        resetGamepad();

        if (currentMode == InputMode.PHYSICAL && controllerManager != null) {
            controllerManager.update();
            try {
                ControllerState state = controllerManager.getState(0);
                if (state.isConnected) {
                    gamepad.left_stick_x = (float) state.leftStickX;
                    gamepad.left_stick_y = (float) state.leftStickY;
                    gamepad.right_stick_x = (float) state.rightStickX;
                    gamepad.right_stick_y = (float) state.rightStickY;
                    gamepad.left_trigger = (float) state.leftTrigger;
                    gamepad.right_trigger = (float) state.rightTrigger;

                    gamepad.a = state.a;
                    gamepad.b = state.b;
                    gamepad.x = state.x;
                    gamepad.y = state.y;

                    gamepad.left_bumper = state.lb;
                    gamepad.right_bumper = state.rb;

                    gamepad.dpad_up = state.dpadUp;
                    gamepad.dpad_down = state.dpadDown;
                    gamepad.dpad_left = state.dpadLeft;
                    gamepad.dpad_right = state.dpadRight;

                    // Handle generic rumble requests (e.g. if the FTC code sets it, we capture it via polling if FTC Gamepad API allowed it).
                    // FTC SDK Gamepad class does not natively expose readable rumble queues easily to external wrappers.
                    // For now, if rumble happens, you'd hook the physical controller here.
                }
            } catch (Exception e) {
                // Controller 0 disconnected
            }
        } else if (currentMode == InputMode.KEYBOARD && keyboardListener != null) {
            // Apply keyboard bounds
            float lx = 0;
            float ly = 0;
            if (keyboardListener.isKeyDown('a')) lx -= 1.0f;
            if (keyboardListener.isKeyDown('d')) lx += 1.0f;
            if (keyboardListener.isKeyDown('w')) ly += 1.0f; // Remember to invert downstream if necessary
            if (keyboardListener.isKeyDown('s')) ly -= 1.0f;

            float rx = 0;
            float ry = 0;
            if (keyboardListener.isKeyDown(java.awt.event.KeyEvent.VK_LEFT)) rx -= 1.0f;
            if (keyboardListener.isKeyDown(java.awt.event.KeyEvent.VK_RIGHT)) rx += 1.0f;
            if (keyboardListener.isKeyDown(java.awt.event.KeyEvent.VK_UP)) ry += 1.0f;
            if (keyboardListener.isKeyDown(java.awt.event.KeyEvent.VK_DOWN)) ry -= 1.0f;

            gamepad.left_stick_x = lx;
            gamepad.left_stick_y = ly;
            gamepad.right_stick_x = rx;
            gamepad.right_stick_y = ry;

            // Face Buttons
            gamepad.a = keyboardListener.isKeyDown('z');
            gamepad.b = keyboardListener.isKeyDown('x');
            gamepad.x = keyboardListener.isKeyDown('c');
            gamepad.y = keyboardListener.isKeyDown('v');

            // Bumpers & Triggers
            gamepad.left_bumper = keyboardListener.isKeyDown('q');
            gamepad.right_bumper = keyboardListener.isKeyDown('e');
            gamepad.left_trigger = keyboardListener.isKeyDown(java.awt.event.KeyEvent.VK_SPACE) ? 1.0f : 0.0f;
            gamepad.right_trigger = keyboardListener.isKeyDown(java.awt.event.KeyEvent.VK_SHIFT) ? 1.0f : 0.0f;
        }
    }

    private void resetGamepad() {
        gamepad.left_stick_x = 0f;
        gamepad.left_stick_y = 0f;
        gamepad.right_stick_x = 0f;
        gamepad.right_stick_y = 0f;
        gamepad.left_trigger = 0f;
        gamepad.right_trigger = 0f;
        gamepad.a = false;
        gamepad.b = false;
        gamepad.x = false;
        gamepad.y = false;
        gamepad.left_bumper = false;
        gamepad.right_bumper = false;
        gamepad.dpad_up = false;
        gamepad.dpad_down = false;
        gamepad.dpad_left = false;
        gamepad.dpad_right = false;
    }
}
