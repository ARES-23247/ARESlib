---
name: areslib-elite-mining
description: Helps ingest, analyze, and port advanced code from World Champion and Elite FRC teams (and top FTC teams) directly into the ARESLib architecture. Use when extracting custom trajectory math, state-machine layouts, vision fusion systems, and control frameworks. Validates and translates from specific high-performing repositories like 1690, 254, 6328, 2910, or elite FTC teams.
---

You are an expert repository mining engineer for Team ARES. When extracting custom trajectory math, state-machine layouts, vision fusion systems, or control frameworks from Elite FRC or FTC teams, adhere strictly to the following guidelines.

# ARESLib Elite Repository Mining Agent

This skill dictates how to safely clone, parse, translate, and securely integrate logic from Elite FRC and FTC team implementations into the ARESLib infrastructure. Because ARESLib utilizes WPILib math, CommandScheduler, and AdvantageKit's `@AutoLog` schema, FRC code maps almost 1:1 into our framework.
**CRITICAL RULE:** Do NOT under any circumstances use `search_web` tools to find repositories. You must rely EXCLUSIVELY on the static manifest of GitHub URLs provided below. Clone or query these URLs directly.

## 1. Top Tier Manifest (The Elite Hit List)

### FTC/FRC Season Reference Table
Use this table to map common game names to their respective competition years when parsing repository names.

| Year | FRC Game Name | FTC Game Name |
| :--- | :--- | :--- |
| **2026** | TBA | TBA |
| **2025** | Reefscape | Into The Deep |
| **2024** | Crescendo | Centerstage |
| **2023** | Charged Up | Powerplay |
| **2022** | Rapid React | Freight Frenzy |
| **2021** | Infinite Recharge (At Home) | Ultimate Goal |
| **2020** | Infinite Recharge | Skystone |
| **2019** | Destination: Deep Space | Rover Ruckus |
| **2018** | FIRST Power Up | Relic Recovery |

When tasked with "seeing how X team solved Y problem," use the following catalog to inform your specific GitHub URLs:

### FRC Elite Repositories (Architecture & Math)
*   **Team 6328 (Mechanical Advantage):** Core *AdvantageKit* creators. Use for evaluating logging abstraction structures.
    *   2026: `https://github.com/Mechanical-Advantage/RobotCode2026Public`
    *   2025: `https://github.com/Mechanical-Advantage/RobotCode2025Public`
    *   2024: `https://github.com/Mechanical-Advantage/RobotCode2024Public`
    *   2023: `https://github.com/Mechanical-Advantage/RobotCode2023`
    *   2022: `https://github.com/Mechanical-Advantage/RobotCode2022`
    *   2020: `https://github.com/Mechanical-Advantage/RobotCode2020`
*   **Team 254 (The Cheesy Poofs):** Pioneers of arbitrary *Path Following* and *State-Space*. Use for custom math controllers and pure-pursuit trajectory logic.
    *   2025: `https://github.com/Team254/FRC-2025-Public`
    *   2024: `https://github.com/Team254/FRC-2024-Public`
    *   2023: `https://github.com/Team254/FRC-2023-Public`
    *   2022: `https://github.com/Team254/FRC-2022-Public`
    *   2020: `https://github.com/Team254/FRC-2020-Public`
*   **Team 1690 (Orbit):** Elite *Targeting* and *Continuous Motion*. Use for shoot-on-the-move math and high-speed multi-subsystem orchestration.
    *   2024: `https://github.com/Orbit-Robotics/2024-Robot`
*   **Team 2910 (Jack in the Bot):** *Swerve Architecture* pioneers. Use to analyze SDS motor/encoder geometric models.
    *   2025: `https://github.com/FRCTeam2910/2025CompetitionRobot-Public`
    *   2024: `https://github.com/FRCTeam2910/2024CompetitionRobot-Public`
    *   2023: `https://github.com/FRCTeam2910/2023CompetitionRobot-Public`
    *   2022: `https://github.com/FRCTeam2910/2022CompetitionRobot`
    *   2021: `https://github.com/FRCTeam2910/2021CompetitionRobot`
    *   2020: `https://github.com/FRCTeam2910/2020CompetitionRobot`
*   **Team 1678 (Citrus Circuits):** *State-Machine Design* & System Reliability. Use for robust sequencing maps.
    *   2025: `https://github.com/frc1678/C2025-Public`
    *   2024: `https://github.com/frc1678/C2024-Public`
    *   2023: `https://github.com/frc1678/C2023-Public`
    *   2022: `https://github.com/frc1678/C2022-Public`
    *   2021: `https://github.com/frc1678/cardinal-2021-public`
    *   2020: `https://github.com/frc1678/C2020`
*   **Team 3005 (RoboChargers):** *AdvantageKit Native* & highly clean abstractions.
    *   2025: `https://github.com/FRC3005/Reefscape-2025`
    *   2024: `https://github.com/FRC3005/Crescendo-2024-Public`
    *   2023: `https://github.com/FRC3005/Charged-Up-2023-Public`
    *   2022: `https://github.com/FRC3005/Rapid-React-2022-Public`
    *   2020: `https://github.com/FRC3005/Infinite-Recharge-2020`
*   **Team 364 (BaseFalconSwerve):**
    *   Template: `https://github.com/Team364/BaseFalconSwerve`
*   **Team 111 (WildStang):** Exceptional Component-Based Architecture & clean abstractions.
    *   2026: `https://github.com/wildstang/2026_111_robot_software`
    *   2025: `https://github.com/wildstang/2025_111_robot_software`
    *   2024: `https://github.com/wildstang/2024_111_robot_software`
    *   2023: `https://github.com/wildstang/2023_111_robot_software`
    *   2022: `https://github.com/wildstang/2022_111_robot_software`
    *   2020: `https://github.com/wildstang/2020_robot_software`
*   **Team 2767 (Stryke Force):** 'ThirdCoast' Custom Swerve Framework & advanced control math.
    *   ThirdCoast Framework: `https://github.com/strykeforce/thirdcoast`
    *   2024 (Crescendo): `https://github.com/strykeforce/crescendo`
    *   2023 (Charged Up): `https://github.com/strykeforce/chargedup`
    *   2022 (Rapid React): `https://github.com/strykeforce/rapidreact`
    *   2020 (Infinite Recharge): `https://github.com/strykeforce/infiniterecharge`
*   **Team 5940 (B.R.E.A.D.):** Bleeding-Edge Vision Fusion, AdvantageKit usage, & Odometry Hardening.
    *   2025: `https://github.com/BREAD5940/2025-Public`
    *   2024: `https://github.com/BREAD5940/2024-Onseason`
    *   2023: `https://github.com/BREAD5940/2023-Onseason`
    *   2022: `https://github.com/BREAD5940/2022-Onseason`
    *   2021: `https://github.com/BREAD5940/2021-Robot`
    *   2020: `https://github.com/BREAD5940/2020-Onseason`
*   **Team 604 (Quixilver):** Highly Object-Oriented Simulation & robust WPILib Architecture.
    *   2025: `https://github.com/frc604/2025-public`
    *   2024: `https://github.com/frc604/2024-public`
    *   2023: `https://github.com/frc604/2023-public`
    *   2022: `https://github.com/frc604/2022-public`
    *   2020: `https://github.com/frc604/FRC-2021-v2`
*   **Team 973 (Greybots):** Competition-Tested Superstructure State Machines & robust mechanical integration.
    *   2025: `https://github.com/Team973/2025-inseason`
    *   2023: `https://github.com/Team973/2023-inseason`
    *   2022: `https://github.com/Team973/2022-inseason`
*   **Team 4414 (HighTide):** `https://github.com/team4414` - Elite *System Integrations* and robust software sequencing for maximizing scoring cycle-times.
*   **Team 118 (Robonauts):** `https://github.com/Robonauts118` - High-level multi-joint subsystem kinematics and advanced autonomous orchestration.
*   **Team 3476 (Code Orange):** `https://github.com/CodeOrange3476` - Excellent source material for deep custom Vision tracking pipelines and Limelight parsing.
*   **Other Notables (AdvantageKit & Cycles):**
    *   Team 125: `https://github.com/nutrons`
    *   Team 4099: `https://github.com/Team4099`
    *   Team 1323: `https://github.com/Team1323`

### FTC Elite Repositories (Game Specific & Mechanisms)
*   **Team 14481 (Don't Blink):** `https://github.com/FTC14481` - Elite hardware mechanisms and OpMode sequences. 
*   **Team 11115 (Gluten Free):** - Historical FTC top-tier implementations.
*   **Team 8393 (Brain Stem):** - Consistently high-performing FTC architectures.

## 2. Ingestion Rules (Safety First)

Do **NOT** clone external elite code directly into the workspace root.
*   **Multi-Team Sourcing:** You must ALWAYS attempt to ingest and analyze code from at least TWO DIFFERENT TEAMS (whenever applicable) for any given architectural or implementation question, rather than relying on a single source of truth.
*   **Exhaustive Search & Follow-Up:** If you cannot find a satisfactory answer or implementation within the initially cloned repositories, you MUST autonomously expand your search to additional teams on the static manifest. Do not stop at the first failure.
*   **Targeting a Specific Year:** FRC/FTC teams typically create a new repository for each season (e.g., `Robot-2024`, `IntoTheDeep`). If the user asks to investigate a given year's code, you must guess or use github tools on the organization URL to find the exact repository URL for that specific year before cloning.
*   **Isolated Cloning:** Always execute an automated `git clone --depth 1 [EXACT_YEAR_REPO_URL] <appDataDir>\brain\<conversation-id>/scratch/[TEAM_NAME]_[YEAR]` to create an isolated sandbox to read from.

## 3. Pattern Matching Heuristics (Astute Grepping)

Top-tier teams have notoriously large repositories. Avoid getting lost by anchoring your grep searches around key API landmarks:
*   Search for `SwerveModuleState`, `MecanumDriveKinematics`, or `ChassisSpeeds` when auditing drivetrain movement.
*   Search for `PoseEstimator`, `Vision`, `LimelightHelpers`, or `PhotonCamera` when identifying localization math.
*   Search for `StateSpace`, `Matrix`, `LQR`, or `EKF` when looking for pure control theory loop structures.
*   Search for `Dashboard`, `Telemetry`, `AdvantageScope` when investigating UI and telemetry.
*   Search for `Logger`, `AutoLog`, or `@AutoLog` when parsing data logging backends.
*   **Cross-Team Issue Analysis:** If asked to see how all teams solved a *specific issue* for a given year (e.g. "how did teams score in the Basket in 2024?"), you must clone the relevant yearly repositories for *multiple* teams from the manifest. Systematically grep across all of them for game-specific keywords (e.g., `Specimen`, `Sample`, `Basket`, `Chamber`) and synthesize a comparative analysis of their differing approaches.
*   *Mandate Cross-Referencing:* If you identify a game-changing architecture in one repository, check it against another's approach before considering it generalized best-practice.
*   *WPILib Validation:* If you discover a heavily modified WPILib wrapper, you must trace the source back upstream (`github.com/wpilibsuite/allwpilib`) to verify exactly what WPILib limitations caused the team to fork the math.

## 4. Porting Constraints (Absolute Architecture Enforcements)

Extracting elite code is useless if it creates technical debt. Any porting attempt into `ARESLib` must rigidly comply with the following translations:

1.  **Dependency Injection Only:** Discard all singletons (e.g. `Drive.getInstance()`) and FTC hardware map singletons (`hardwareMap.get()`). Translate elite calculations to operate entirely inside ARESLib's `@AutoLog` generic `HardwareIO` interfaces.
2.  **No Vendor Lock-in API bleed:** Eliminate direct Rev Hub, GoBilda, or standard FTC SDK (`DcMotorEx`) calls from your final ported snippet. Replace them simply with math variables (e.g., Target Volts or System States), passing them downward into ARESLib's IO layer.
3.  **Strict Variable Formatting:** Nuke legacy `m_` prefixes. Everything must cleanly translate into unannotated `camelCase` parameters standardizing to modern Java layout.
4.  **Logging Normalization:** Convert `telemetry.addData()` or `FtcDashboard` usages natively into AdvantageKit-style `AresAutoLogger` or `AresTelemetry` outputs.
5.  **FTC to FRC Math Conversion:** If porting from standard FTC code (like RoadRunner), adapt `Pose2d` to WPILib math standards. If porting from FRC (like 254), leave the WPILib math intact but map it to our simulated `dyn4j` or `AresSimulator` structure if testing is needed.

## 5. Exit Validation

Before surfacing the ported architectural snippet up to the user:
*   Validate `spotlessApply` compliance across the local `ARESLib` Gradle context.
*   Confirm there are no unresolved static imports or FTC SDK blocking errors via `./gradlew build`.
