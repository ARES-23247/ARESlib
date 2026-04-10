# 🔧 ARESLib Pit Debugging Flowchart

Use this guide when the robot is misbehaving in the pits. Start at the top and follow the arrows.

---

## Master Triage

```mermaid
flowchart TD
    START["🤖 Robot Not Working"] --> Q1{"Does the Control Hub boot?<br/>(Green/Blue LED on Hub)"}

    Q1 -- No --> POWER_TREE["⚡ Go to: POWER TREE"]
    Q1 -- Yes --> Q2{"Does Driver Station<br/>show Comms/Ping?"}

    Q2 -- No --> NETWORK_TREE["🌐 Go to: NETWORK TREE"]
    Q2 -- Yes --> Q3{"Does Driver Station<br/>show Code/OpModes?"}

    Q3 -- No --> CODE_TREE["💻 Go to: CODE TREE"]
    Q3 -- Yes --> Q4{"Can you Init and Start<br/>an OpMode?"}

    Q4 -- No --> Q4A{"Check DS for<br/>Exceptions / Hardware<br/>errors"}
    Q4A --> POWER_TREE
    Q4 -- Yes --> Q5{"Does the robot<br/>drive correctly?"}

    Q5 -- No --> MECHANISM_TREE["🦾 Go to: MECHANISM TREE"]
    Q5 -- Yes --> Q6{"Does Autonomous<br/>work?"}

    Q6 -- No --> AUTO_TREE["🏎️ Go to: AUTO TREE"]
    Q6 -- Yes --> DONE["✅ Robot is Nominal!<br/>Run DiagnosticsOpMode<br/>to verify sensors."]
```

---

## ⚡ Power Tree

```mermaid
flowchart TD
    P1["⚡ POWER TREE"] --> P2{"Is the robot main<br/>switch ON?"}
    P2 -- No --> P2A["Turn the switch ON"]
    P2 -- Yes --> P3{"Battery voltage<br/>> 12.0V?"}

    P3 -- No --> P3A["🔋 Swap to a<br/>freshly charged battery"]
    P3 -- Yes --> P4{"Are all XT30 cables<br/>and motor wiring seated?"}

    P4 -- No --> P4A["Re-seat the<br/>cable connections"]
    P4 -- Yes --> P5{"Check for burnt<br/>smell or melted<br/>connectors"}

    P5 -- Yes --> P5A["🚨 STOP. Get a mentor.<br/>Inspect wiring immediately."]
    P5 -- No --> P6{"Control Hub Status<br/>LED is blinking blue/red?"}

    P6 -- Yes --> P6A["Hub is in fault condition.<br/>Restart Hub or check<br/>battery connection."]
    P6 -- No --> P7["Verify wiring between<br/>Control Hub and Expansion Hub."]
```

---

## 🌐 Network Tree

```mermaid
flowchart TD
    N1["🌐 NETWORK TREE"] --> N2{"Is the Hub<br/>broadcasting WiFi?"}
    N2 -- No --> N2A["Check power or plug in<br/>via USB-C to configure"]
    N2 -- Yes --> N3{"Laptop/Driver Station<br/>connected to robot WiFi?"}

    N3 -- No --> N3A["Connect to the correct<br/>robot WiFi network"]
    N3 -- Yes --> N4{"Can you ping<br/>192.168.43.1?"}

    N4 -- No --> N4A["IP issue or interference.<br/>Restart Hub."]
    N4 -- Yes --> N5{"DS shows Comms<br/>but high latency/disconnects?"}

    N5 -- Yes --> N5A["Check 2.4Ghz/5Ghz band.<br/>Change WiFi channel if<br/>interference is high."]
    N5 -- No --> N6["Restart Driver Station App<br/>and Robot Code.<br/>If persistent, check ESD."]
```

---

## 💻 Code Tree

```mermaid
flowchart TD
    C1["💻 CODE TREE"] --> C2{"Did code deploy<br/>successfully?"}
    C2 -- No --> C2A["Check Android Studio /<br/>Gradle output for errors"]
    C2 -- Yes --> C3{"OpModes missing<br/>from Driver Station?"}

    C3 -- Yes --> C3A{"Check logcat via<br/>ADB or Robot Controller<br/>console tab"}
    C3A --> C3B{"Crash on boot?<br/>HardwareMap error?"}
    C3B -- Yes --> C3C["Verify config names<br/>match code exactly.<br/>Check for missing sensors."]
    C3B -- No --> C3D["Possible dependency issue.<br/>Run: ./gradlew clean assembleDebug."]

    C3 -- No --> C4{"Code runs but<br/>throws exceptions?"}
    C4 -- Yes --> C4A["Check RobotLog/<br/>AdvantageScope logs for<br/>stack traces."]
    C4 -- No --> C5["Code is healthy.<br/>Issue is elsewhere."]
```

---

## 🦾 Mechanism Tree

```mermaid
flowchart TD
    M1["🦾 MECHANISM TREE"] --> M2{"Which mechanism<br/>is failing?"}

    M2 --> M_DRIVE["Drivetrain/Odometry"]
    M2 --> M_ELEV["Elevator/Slides"]
    M2 --> M_ARM["Arm/Pivot"]
    M2 --> M_INTAKE["Intake/Shooter"]

    M_DRIVE --> S1{"Motors spinning<br/>but not driving?"}
    S1 -- Yes --> S1A["Check wheel set screws<br/>or bevel gears."]
    S1 -- No --> S2{"One motor dead?"}
    S2 -- Yes --> S2A["Check motor cable and<br/>encoder wire seating."]
    S2 -- No --> S3{"Robot drives but<br/>odometry drifts?"]
    S3 -- Yes --> S3A["Check dead wheel springs.<br/>Verify IMU orientation.<br/>Check encoder wiring."]

    M_ELEV --> E1{"Slides not moving?"}
    E1 -- Yes --> E1A["Run diagnostics.<br/>Check spool string tension<br/>and motor connection."]
    E1 -- No --> E2{"Slides oscillating<br/>or overshooting?"}
    E2 -- Yes --> E2A["Reduce kP in<br/>constants. Verify kG<br/>(gravity FF) is tuned."]

    M_ARM --> A1{"Arm dropping<br/>under gravity?"}
    A1 -- Yes --> A1A["kG feedforward is wrong.<br/>Increase kG until arm<br/>holds position near 0 PID."]
    A1 -- No --> A2{"Arm hitting limits?"}
    A2 -- Yes --> A2A["Verify absolute encoder<br/>offset and soft limits."]

    M_INTAKE --> I1{"Motor spinning<br/>but no intake?"}
    I1 -- Yes --> I1A["Physical issue:<br/>Check rollers, belts,<br/>and compression."]
    I1 -- No --> I2{"Motor stalling?"}
    I2 -- Yes --> I2A["Check for physical jams.<br/>Check current limits."]
```

---

## 🏎️ Auto Tree

```mermaid
flowchart TD
    A1["🏎️ AUTO TREE"] --> A2{"Robot doesn't move<br/>in Auto?"}
    A2 -- Yes --> A3{"Is the starting pose<br/>set correctly?"}
    A3 -- No --> A3A["Verify pose reset is called<br/>at the start of auto."]
    A3 -- Yes --> A4{"PathPlanner/RoadRunner<br/>path loaded?"}
    A4 -- No --> A4A["Check that trajectory files<br/>are deployed to the hub."]
    A4 -- Yes --> A4B["Check telemetry to see<br/>if path is active."]

    A2 -- No --> A5{"Robot drives but<br/>is off-target?"}
    A5 -- Yes --> A6{"Consistently off<br/>in one direction?"}
    A6 -- Yes --> A6A["Odometry is drifting.<br/>Check dead wheel slipping.<br/>Check vision fusion offsets."]
    A6 -- No --> A6B["PID gains need tuning.<br/>Adjust Translation/Rotation kP."]

    A5 -- No --> A7{"Auto works but<br/>manipulators miss?"]
    A7 -- Yes --> A7A["Check timings and<br/>state machine delays.<br/>Verify sensor triggers."]
```

---

## Quick Reference Checklist

| Step | Action | Time |
|------|--------|------|
| 1 | Swap to a fresh battery (>12.5V) | 30s |
| 2 | Connect via USB/WiFi, verify Ping | 15s |
| 3 | Deploy code / Restart App | 45s |
| 4 | Init OpMode, check for exceptions | 10s |
| 5 | Verify physical connections (encoders, motors) | 30s |
| 6 | Check dead wheels and sensors | 15s |
| 7 | Run isolated system test | 30s |

> [!TIP]
> **Total pit turnaround target: under 3 minutes.** If you can't diagnose in 3 minutes, swap the battery and call the lead programmer or mentor.

> [!CAUTION]
> **NEVER enable the robot on blocks in the pit with mechanisms that could swing or extend.** Always have a spotter and ensure the e-stop (DS disable) is within arm's reach.
