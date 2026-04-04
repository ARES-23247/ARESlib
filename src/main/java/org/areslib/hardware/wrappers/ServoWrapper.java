package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.Servo;
import org.areslib.hardware.interfaces.AresServo;

public class ServoWrapper implements AresServo {
    private final Servo servo;
    private double lastSentPosition = Double.NaN;
    private static final double CACHE_THRESHOLD = 0.005;

    public ServoWrapper(Servo servo) {
        this.servo = servo;
    }

    @Override
    public void setPosition(double position) {
        if (Double.isNaN(lastSentPosition) || Math.abs(position - lastSentPosition) > CACHE_THRESHOLD) {
            servo.setPosition(position);
            lastSentPosition = position;
        }
    }
}
