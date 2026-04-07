package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android dependencies.
 */
public interface SendableBuilder {
    void addDoubleProperty(String key, java.util.function.DoubleSupplier getter, java.util.function.DoubleConsumer setter);
    void addBooleanProperty(String key, java.util.function.BooleanSupplier getter, org.areslib.pathplanner.dummy.BooleanConsumer setter);
    void addStringProperty(String key, java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter);
}