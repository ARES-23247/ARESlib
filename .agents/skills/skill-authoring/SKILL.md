---
name: skill-authoring
description: Helps create and maintain AI skills for the ARESLib2 framework. Use when a new subsystem, hardware abstraction, or framework feature is added and needs a matching skill for long-term AI context.
---

You are a documentation architect for Team ARES. When creating or updating AI skills for the ARESLib2 codebase, follow these rules strictly.

## 1. When to Create a Skill

Create a new skill whenever you:
- Add a new subsystem (e.g., `ClawSubsystem`, `ElevatorSubsystem`)
- Implement a new IO abstraction (e.g., `ColorSensorIO`)
- Build a new framework utility (e.g., `AutoBuilder`, `GhostRecorder`)
- Add a complex integration (e.g., a new vision pipeline)
- Establish a new pattern that should be followed consistently

**Rule: Every subsystem gets a skill.** If you build it, document it. Skills are the AI's long-term memory.

**Rule: Claude / AI Agent Compatibility.** Every new skill MUST be encapsulated in the `.agents/skills/<skill-name>` directory and contain a valid `SKILL.md` file with proper YAML frontmatter. This architecture acts as the native plugin/wrapper that allows Claude, Gemini, and other supported AI agents to automatically discover and index the skill. Without this directory structure and frontmatter, the AI cannot import the skill into its context window.

## 2. YAML Frontmatter

Keep it minimal. Only two fields are required:

```yaml
---
name: <skill-name>
description: <One sentence. Start with a verb. Include "Use when..." clause.>
---
```

**That's it.** No license, compatibility, metadata, version, or category fields. The content matters, not the metadata.

### Description Rules
- Start with a verb: "Helps...", "Documents...", "Defines..."
- Include a "Use when..." clause: "Use when configuring swerve modules, tuning odometry, or adding new drive types."
- Single line, no YAML multiline (`>-`).

## 3. Persona Opening

Every skill MUST begin with a persona line immediately after the frontmatter. This anchors the AI's behavior:

```markdown
You are an expert [domain role] for Team ARES. When [doing what this skill covers], adhere strictly to the following guidelines.
```

**Examples:**
- `You are a simulation engineer for Team ARES. When modifying the dyn4j physics environment...`
- `You are a controls engineer for Team ARES. When implementing PID loops or feedforward models...`
- `You are a vision engineer for Team ARES. When configuring AprilTag pipelines...`

## 4. Skill Structure Template

```markdown
---
name: <skill-name>
description: <verb + when-to-use>
---

You are an expert [role] for Team ARES. When [scope], adhere strictly to the following guidelines.

## 1. Architecture
<Class hierarchy. Always show the IO pattern: Interface → IOReal → IOSim → Subsystem.>

## 2. Key Rules
<Numbered rules with bold names. Use "Rule A:", "Rule B:" format for scannability.>

### Rule A: <Short Name>
<Explanation + code example>

### Rule B: <Short Name>
<Explanation + code example>

## 3. Configuration & Constants
<Document constants, config objects, tuning parameters with actual class paths.>

## 4. Usage Examples
<Real code showing instantiation in both real and sim modes.>

## 5. Telemetry & Log Keys
<Exact AdvantageScope log keys this subsystem publishes.>

## 6. Testing
<Headless JUnit 5 test example. Reference areslib-testing skill.>

## 7. Common Pitfalls
<Anti-patterns with BAD/GOOD code comparisons.>
```

Not every section is required for every skill. Omit irrelevant sections (e.g., tooling skills don't need "Telemetry").

## 5. Cross-Skill References

Always reference related skills instead of duplicating content:

```markdown
For coordinate system conversions, see the `areslib-architecture` skill.
For testing patterns, see the `areslib-testing` skill.
```

## 6. Naming Conventions

| Component Type | Skill Name Pattern | Example |
|:---|:---|:---|
| Subsystem | `areslib-<name>` | `areslib-elevator` |
| Hardware IO | `areslib-<sensor>` | `areslib-colorsensor` |
| Framework utility | `areslib-<feature>` | `areslib-autonomous` |
| External integration | `<vendor>-<feature>` | `pathplanner-ftc` |
| Tooling/build | `<tool>-<purpose>` | `gradle-ftc-desktop` |

## 7. Required Checklist

Before finalizing a skill:

- [ ] **YAML** — `name` + `description` only
- [ ] **Persona** — "You are an expert [role] for Team ARES..."
- [ ] **Description** — starts with verb, includes "Use when..."
- [ ] **IO pattern** — Interface → IOReal → IOSim documented (if applicable)
- [ ] **Code examples** — real Java code, not pseudocode
- [ ] **Telemetry keys** — exact log paths for AdvantageScope (if applicable)
- [ ] **Cross-references** — links to related skills
- [ ] **Anti-patterns** — BAD/GOOD comparisons

## 8. Generating a Skill from Existing Code

### Step 1: Scan the Source
```bash
grep -r "class [Name]" src/main/java/ --include="*.java" -l
grep -r "[Name]IO" src/main/java/ --include="*.java" -l
grep -r "[Name]" src/test/java/ --include="*.java" -l
```

### Step 2: Extract the Architecture
- Identify the IO interface and all implementations (Real, Sim)
- Identify the Subsystem class and its `periodic()` method
- Identify any Commands that use this subsystem
- Identify any StateMachine enums

### Step 3: Document Telemetry
- Find all `AresAutoLogger.recordOutput()` and `AresTelemetry.put*()` calls
- Map log keys to AdvantageScope visualization types

### Step 4: Write the SKILL.md
Use the template above. **Be specific** — include actual class names, actual method signatures, actual log keys. Generic skills are useless.

### Step 5: Write Tests (if missing)
If the subsystem doesn't have a test file yet, create one following the `areslib-testing` skill patterns.

### Step 6: Agent Compatibility Registration
- **Create the Manifest:** You MUST create a `plugin.json` file inside the new skill directory to ensure AI agents (like Claude) can detect it. Use an existing one (e.g., `areslib-vision/plugin.json`) as a template.
- **Update Marketplace:** You MUST register the new skill by adding its name and relative source path to the `.agents/skills/marketplace.json` array.

### Step 7: Commit
```bash
git add .agents/skills/<skill-name>/
git add .agents/skills/marketplace.json
git commit -m "feat: add <skill-name> AI skill for <purpose>"
git push
```

## 9. Updating Existing Skills

When modifying an existing subsystem:
1. Read the existing skill: `.agents/skills/<name>/SKILL.md`
2. Update any changed method signatures, config values, or log keys
3. Commit with message: `docs: update <skill-name> skill for <change>`

## 10. Anti-Patterns

### Don't: Create generic skills
```markdown
# BAD
## Usage
Use the subsystem by calling its methods.
```

### Don't: Skip the IO pattern
```markdown
# BAD
## Architecture
There is a subsystem class.
```

### Don't: Forget coordinate system context
```markdown
# BAD
## Poses
The subsystem uses poses.

# GOOD — specific to ARESLib2's coordinate convention
## Poses
All poses use WPILib convention (X-forward, Y-left, θ CCW+).
PathPlanner uses WPILib convention natively, so no conversion is needed.
For unit conversions, use `CoordinateUtil`.
```

### Don't: Duplicate content across skills
```markdown
# BAD — copying path planning API docs into the autonomous skill
follower.followPath(path);

# GOOD — reference the source
See the PathPlanner documentation for current API signatures.
```
