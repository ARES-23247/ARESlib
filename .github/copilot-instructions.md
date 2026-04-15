# GitHub Copilot Instructions for ARESLib

You are operating within the **ARESLib Professional FTC Framework**.

ARESLib uses a highly specific architecture designed around zero-allocation performance, FTC hardware constraints, physics simulation, and modern telemetry integration.

## Agentic Skill Integration

ARESLib is equipped with specialized `.agent` Markdown skills located in the `.agents/skills/` directory.

Before writing code for any major robotic subsystem or utility, you **MUST** automatically read and apply the associated SKILL.md file if it matches the user's request.

### Key Skill Mappings to Auto-Fetch:
- **Writing Subsystems:** Always read `.agents/skills/areslib-hardware/SKILL.md`
- **Drivetrain:** Always read `.agents/skills/areslib-drivetrain/SKILL.md`
- **Simulation/Physics:** Always read `.agents/skills/areslib-simulation/SKILL.md`
- **Telemetry:** Always read `.agents/skills/areslib-telemetry/SKILL.md`
- **Writing Unit Tests:** Always read `.agents/skills/areslib-testing/SKILL.md`
- **Diagnosing Build Errors:** Always read `.agents/skills/areslib-ci/SKILL.md`
- **Control Theory / PID:** Always read `.agents/skills/areslib-control-theory/SKILL.md`
- **Code Audits:** Always read `.agents/skills/areslib-audit/SKILL.md`

### Core Principles
1. NEVER use the `new` keyword inside `periodic()`, `execute()`, or any loop structure. ARESLib prohibits dynamic allocations during runtime to prevent garbage collection pauses.
2. All hardware must be abstracted via the `IO` (e.g., `IOReal`, `IOSim`) interface to maintain deterministic replay capability.
3. Obey the rules from `.agents/skills/areslib-core-standards/SKILL.md` unconditionally.

## FTC-Specific Considerations

- **Hardware Platform**: FTC uses Control Hub, Expansion Hub, and Rev Robotics hardware (not RoboRIO/CTRE)
- **Loop Timing**: FTC OpMode loop runs at ~50Hz (20ms) vs FRC's 50Hz
- **Motor Controllers**: Use Rev Robotics motor controllers with different APIs than CTRE Phoenix
- **Gamepads**: Standard gamepads instead of FRC-specific controllers
- **Telemetry**: FTC Dashboard and AdvantageScope integration
- **Field**: FTC field dimensions and game elements vs FRC field

## Documentation Structure

ARESLib maintains comprehensive documentation parity with MARSLib (FRC Team 2614) while adapting for FTC:

- **README.md**: Project overview with quick start guide
- **CONTRIBUTING.md**: Detailed contribution guidelines and coding standards
- **CONTROLLER_MAPPINGS.md**: Standard controller mappings for FTC robots
- **docs/**: Extended documentation including tutorials and reference material
- **.agents/skills/**: AI agent skill files for framework-guided development

## Architecture Philosophy

ARESLib follows the same architectural principles as MARSLib but adapted for FTC:

1. **IO Abstraction**: Complete hardware isolation through interfaces
2. **Zero-Allocation**: No runtime memory allocation to prevent GC pauses
3. **Physics Simulation**: dyn4j integration for offline testing
4. **Deterministic Telemetry**: Complete observability for debugging
5. **FTC-Native**: Designed specifically for FTC hardware and constraints

## When in Doubt

If you encounter a situation where the instructions seem unclear or contradictory:

1. **Prioritize Safety**: Never generate code that could damage hardware or violate safety rules
2. **Ask for Clarification**: Request guidance on the specific FTC context
3. **Check Examples**: Reference template code in `src/main/java/org/areslib/templates/`
4. **Maintain Standards**: Follow the established patterns even if they seem more complex

ARESLib is built for championship-level FTC competition while maintaining architectural consistency with proven FRC frameworks.
