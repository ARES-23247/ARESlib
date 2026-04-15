---
name: areslib-network
description: Helps manage NetworkTables communication, dashboard data pipelines, and telemetry streaming. Use when integrating with FTC Dashboard, reading telemetry data, or handling network resilience.
---

# ARESLib Network & Communication Skill

You are a network engineer for Team ARES 23247. When integrating with external systems or managing telemetry:

## 1. Architecture

ARESLib uses NetworkTables for all robot-dashboard communication:

| Component | Purpose |
|---|---|
| NetworkTables `Publisher` | Sends telemetry to FTC Dashboard (pose, mechanisms, diagnostics) |
| NetworkTables `Subscriber` | Receives configuration from FTC Dashboard (@Config variables) |
| `@Config` annotation | Auto-syncs variables with dashboard for live tuning |
| Hardware layer `updateInputs()` | Reads sensor data for logging and streaming |

### Data Flow
```
Robot Hardware → Hardware Layer → Subsystem → NetworkTables → FTC Dashboard
                                                            ↓
                                                    Driver Station Display
```

## 2. Key Rules

### Rule A: NetworkTables for Dashboard Only
Use NetworkTables primarily for FTC Dashboard communication. Unlike FRC, FTC typically doesn't use coprocessors, so NT usage is focused on:
- Streaming telemetry to driver station
- Live-tuning `@Config` variables from dashboard
- Receiving autonomous selection from dashboard

### Rule B: Publish Telemetry in Subsystem periodic()
Telemetry data MUST be published in the subsystem's `periodic()` method, not in the hardware layer. This ensures consistent timing and allows dashboard updates even when hardware layer has issues.

### Rule C: Batch NetworkTables Updates
Don't send individual NetworkTables updates at 50Hz — this floods the network. Batch updates and send at 10-20Hz maximum. Use a single telemetry update routine that collects all data and sends in one transaction.

### Rule D: Handle Network Graceful Degradation
Always assume network connection may be poor or lost:
- Dashboard disconnection must not crash robot code
- `@Config` variables must have safe default values
- Autonomous selection should fall back to switch-based selection if network unavailable

### Rule E: Minimize Bandwidth Over Driver Station WiFi
The Driver Station WiFi has limited bandwidth. Do NOT stream raw arrays or high-frequency data:
- Limit pose updates to 10Hz (drivers don't need faster)
- Prune debug data — only send what's useful during match
- Use efficient data types (doubles, not strings)

## 3. Adding New Telemetry Streams

1. Create `@Config` parameters if dashboard needs to tune values:
   ```java
   @Config
   public double kP = 1.0; // Automatically appears in dashboard
   ```

2. Add telemetry publishing in subsystem's `periodic()`:
   ```java
   @Override
   public void periodic() {
       // Read hardware inputs
       hardware.updateInputs(inputs);

       // Publish to dashboard
       telemetry.publish("Elevator/Position", inputs.position);
       telemetry.publish("Elevator/Velocity", inputs.velocity);
   }
   ```

3. For complex data structures, create a custom telemetry class:
   ```java
   public class ElevatorTelemetry {
       public double position;
       public double velocity;
       public double appliedVolts;
   }

   telemetry.publish("Elevator", elevatorTelemetry);
   ```

## 4. FTC-Specific Network Considerations

### Driver Station WiFi Limitations
- **Bandwidth**: ~10 Mbps shared between both alliances
- **Latency**: Can spike to 500ms+ during matches
- **Reliability**: May drop packets during high-noise conditions

### Recommended Update Rates
| Data Type | Update Rate | Notes |
|---|---|---|
| Robot Pose | 10Hz | Drivers don't need faster updates |
| Mechanism Positions | 10Hz | Batch all mechanisms together |
| Motor Telemetry | 5Hz | Only during debugging, never in match |
| Diagnostics | 1Hz | Fault alerts only |
| @Config sync | Event-based | Only when value changes |

### Dashboard Integration
- **FTC Dashboard**: Web-based, shows graphs, field overlay, configurable variables
- **AdvantageScope**: Advanced 3D visualization, works with FTC data
- **Custom dashboards**: Can consume NT data for specialized displays

## 5. Telemetry

### Standard Telemetry Points
- `Robot/Pose` — Robot pose on field (x, y, heading)
- `Drivetrain/Velocity` — Wheel velocities
- `{Mechanism}/Position` — Mechanism position
- `{Mechanism}/Velocity` — Mechanism velocity
- `{Mechanism}/AppliedVolts` — Voltage being applied

### Diagnostics Telemetry
- `Diagnostics/Faults` — Active fault list
- `Diagnostics/BatteryVoltage` — Current battery voltage
- `Diagnostics/NetworkLatency` — Round-trip time to dashboard

### Performance Telemetry
- `Performance/LoopTime` — Main loop execution time
- `Performance/Overruns` — Number of loop overruns
- `Performance/CanUtilization` — CAN bus utilization percentage

## 6. NetworkTables Best Practices

### Path Naming
- Use `/` separator: `Drivetrain/LeftVelocity`
- Group related telemetry: `Elevator/Position`, `Elevator/Velocity`
- Use consistent naming across subsystems

### Data Type Selection
- **Primitive doubles**: Most efficient, use when possible
- **String arrays**: Only for debug, not during matches
- **Custom structs**: Avoid — FTC Dashboard has limited support

### Performance Monitoring
```java
// Monitor NT publish rate
if (System.currentTimeMillis() - lastPublishTime > 200) {
    // Log warning if NT updates are too slow
}
```
