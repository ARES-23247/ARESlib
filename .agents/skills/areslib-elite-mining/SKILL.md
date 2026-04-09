---
name: areslib-elite-mining
description: Helps ingest, analyze, and port advanced code from World Champion and Elite FRC teams (and top FTC teams) directly into the ARESLib2 architecture. Use when extracting custom trajectory math, state-machine layouts, vision fusion systems, and control frameworks. Validates and translates from specific high-performing repositories like 1690, 254, 6328, 2910, or elite FTC teams.
---

# ARESLib2 Elite Repository Mining Agent

This skill dictates how to safely clone, parse, translate, and securely integrate logic from Elite FRC and FTC team implementations into the ARESLib2 infrastructure. Because ARESLib2 utilizes WPILib math, CommandScheduler, and AdvantageKit's `@AutoLog` schema, FRC code maps almost 1:1 into our framework.

## 1. Top Tier Manifest (The Elite Hit List)

When tasked with "seeing how X team solved Y problem," use the following catalog to inform your specific GitHub URLs:

### FRC Elite Repositories (Architecture & Math)
*   **Team 6328 (Mechanical Advantage):** `https://github.com/Mechanical-Advantage` - Core *AdvantageKit* creators. Use for evaluating logging abstraction structures.
*   **Team 254 (The Cheesy Poofs):** `https://github.com/Team254` - Pioneers of arbitrary *Path Following* and *State-Space*. Use for custom math controllers and pure-pursuit trajectory logic.
*   **Team 1690 (Orbit):** `https://github.com/Orbit-Robotics` - Elite *Targeting* and *Continuous Motion*. Use for shoot-on-the-move math and high-speed multi-subsystem orchestration.
*   **Team 2910 (Jack in the Bot):** `https://github.com/FRCTeam2910` - *Swerve Architecture*. Use to analyze SDS motor/encoder geometric models.
*   **Team 1678 (Citrus Circuits):** `https://github.com/frc1678` - *State-Machine Design*. Use for robust sequencing maps.

### FTC Elite Repositories (Game Specific & Mechanisms)
*   **Team 14481 (Don't Blink):** `https://github.com/FTC14481` - Elite hardware mechanisms and OpMode sequences. 
*   **Team 11115 (Gluten Free):** - Historical FTC top-tier implementations.
*   **Team 8393 (Brain Stem):** - Consistently high-performing FTC architectures.
*   **RoadRunner Official:** `https://github.com/acmerobotics/road-runner` - For FTC-specific kinematics and state-of-the-art motion profiling.

## 2. Ingestion Rules (Safety First)

Do **NOT** clone external elite code directly into the workspace root.
*   **Multi-Team Sourcing:** You must ALWAYS attempt to ingest and analyze code from at least TWO DIFFERENT TEAMS (whenever applicable) for any given architectural or implementation question.
*   **Exhaustive Search & Follow-Up:** If you cannot find a satisfactory answer or implementation within the initially cloned repositories, you MUST autonomously expand your search to additional teams on the manifest, or prompt the user with a plan to use `search_web`.
*   **Targeting a Specific Year:** FRC/FTC teams typically create a new repository for each season (e.g., `Robot-2024`, `IntoTheDeep`). Use `search_web` to find the exact repository URL for that specific year before cloning.
*   **Isolated Cloning:** Always execute an automated `git clone --depth 1 [EXACT_YEAR_REPO_URL] <appDataDir>\brain\<conversation-id>/scratch/[TEAM_NAME]_[YEAR]` to create an isolated sandbox to read from.

## 3. Pattern Matching Heuristics (Astute Grepping)

Top-tier teams have notoriously large repositories. Avoid getting lost by anchoring your grep searches around key API landmarks:
*   Search for `SwerveModuleState`, `MecanumDriveKinematics`, or `ChassisSpeeds` when auditing drivetrain movement.
*   Search for `PoseEstimator`, `Vision`, `LimelightHelpers`, or `PhotonCamera` when identifying localization math.
*   Search for `StateSpace`, `Matrix`, `LQR`, or `EKF` when looking for pure control theory loop structures.
*   Search for `Dashboard`, `Telemetry`, `AdvantageScope` when investigating UI and telemetry.
*   Search for `Logger`, `AutoLog`, or `@AutoLog` when parsing data logging backends.
*   *Cross-Team Issue Analysis:* Systematically grep across multiple cloned repos for game-specific keywords (e.g., `Specimen`, `Sample`, `Basket`, `Chamber`) and synthesize a comparative analysis.

## 4. Porting Constraints (Absolute Architecture Enforcements)

Extracting elite code is useless if it creates technical debt. Any porting attempt into `ARESLib2` must rigidly comply with the following translations:

1.  **Dependency Injection Only:** Discard all singletons (e.g. `Drive.getInstance()`) and FTC hardware map singletons (`hardwareMap.get()`). Translate elite calculations to operate entirely inside ARESLib2's `@AutoLog` generic `HardwareIO` interfaces.
2.  **No Vendor Lock-in API bleed:** Eliminate direct Rev Hub, GoBilda, or standard FTC SDK (`DcMotorEx`) calls from your final ported snippet. Replace them simply with math variables (e.g., Target Volts or System States), passing them downward into ARESLib2's IO layer.
3.  **Strict Variable Formatting:** Nuke legacy `m_` prefixes. Everything must cleanly translate into unannotated `camelCase` parameters standardizing to modern Java layout.
4.  **Logging Normalization:** Convert `telemetry.addData()` or `FtcDashboard` usages natively into AdvantageKit-style `AresAutoLogger` or `AresTelemetry` outputs.
5.  **FTC to FRC Math Conversion:** If porting from standard FTC code (like RoadRunner), adapt `Pose2d` to WPILib math standards. If porting from FRC (like 254), leave the WPILib math intact but map it to our simulated `dyn4j` or `AresSimulator` structure if testing is needed.

## 5. Exit Validation

Before surfacing the ported architectural snippet up to the user:
*   Validate `spotlessApply` compliance across the local `ARESLib2` Gradle context.
*   Confirm there are no unresolved static imports or FTC SDK blocking errors via `./gradlew build`.
