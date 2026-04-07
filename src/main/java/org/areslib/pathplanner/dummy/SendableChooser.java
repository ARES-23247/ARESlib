package org.areslib.pathplanner.dummy;

/**
 * A dummy SendableChooser that operates as a standard HashMap for simulation auto dropdowns.
 */
import org.areslib.command.Command;
import java.util.HashMap;
import java.util.Map;
public class SendableChooser<V> implements Sendable {
    private Map<String, V> map = new HashMap<>();
    private V selected;
    public void setDefaultOption(String name, V object) { map.put(name, object); if (selected == null) selected = object; }
    public void addOption(String name, V object) { map.put(name, object); }
    public V getSelected() { return selected; }
}