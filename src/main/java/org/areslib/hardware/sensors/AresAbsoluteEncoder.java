package org.areslib.hardware.sensors;

public interface AresAbsoluteEncoder extends AresEncoder {
    /**
     * Gets the absolute position of the encoder.
     * @return the absolute position in radians (0 to 2PI)
     */
    double getAbsolutePositionRad();

    /**
     * Sets the zero offset for formatting the absolute position.
     * @param offsetRad Offset in radians.
     */
    void setOffset(double offsetRad);
}
