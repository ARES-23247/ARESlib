---
name: advantagescope-layouts
description: Exposes standardized execution rules for properly building and structuring `layout.json` configurations using the `advantagescope-mcp` tools when deploying new telemetry or simulation features. Use when creating AdvantageScope layouts, adding visualization tabs, configuring point cloud renders, or debugging telemetry display issues.
---

# AdvantageScope Automated Layout Structuring


You are an expert telemetry engineer for Team ARES. When creating or updating AdvantageScope layout configurations, follow these rules strictly.
As the agent, you have direct programmatic access to the `advantagescope-mcp` architecture via attached specialized tools.
Because AdvantageScope is heavily integrated into the debugging telemetry for `ARESLib2`, and users often request advanced 3D visual abstractions (Lidars, Swerve wheels, AprilTags), you MUST correctly hook into the MCP.

## Invocation Conventions
When requested to create or update an AdvantageScope layout to visualize a new feature, follow this flow:

### 1. Generating State Files
- Ensure you start by calling the `mcp_advantagescope-mcp_create_layout` tool.
- By default, name these configurations something relative to the sub-function being tested (e.g. `swerve_test_layout.json`).
- *Important Bug Avoidance:* If you manipulate a file using the MCP, it is known to strip version tags. ALWAYS confirm the layout file contains `"version": "26.0.0"` manually appending it via `multi_replace_file_content` if missing, or else AdvantageScope will throw a corrupted format error!

### 2. Configuring Point Clouds
When visualizing `ArrayLidarIOSim` or dynamic dynamic fields:
- Use `mcp_advantagescope-mcp_add_source` to attach the field `[AutoLog Inputs Path]/LidarArray` array.
- **Log Type**: You must explicitly set `log_type: "double[]"` to force AdvantageScope to deserialize it linearly.
- Set the render option to **Points** in the Tab configuration, NOT a "Swerve State" visualization.

### 3. Rendering Gamepads
AdvantageScope now supports Gamepad Joystick rendering overlays in 3D. When mapping virtual joysticks from the HUD, you must push inputs into `/DriverStation/Joystick[0]` paths in our AdvantageKit logic before linking that schema into the AdvantageScope layout!

## Code Examples

### Creating a Layout with Line Graph + Field2D
```json
{
  "version": "26.0.0",
  "hubs": [{
    "x": 0, "y": 0, "width": 1280, "height": 720,
    "tabs": [
      {"type": 1, "title": "Drive Telemetry"},
      {"type": 2, "title": "Field View"}
    ]
  }]
}
```

### Adding a Source via MCP
```
mcp_advantagescope-mcp_add_source(
  file_path: "layout.json",
  tab_index: 0,
  type: "stepped",
  log_key: "/RealOutputs/Drive/Pose",
  log_type: "Pose2d"
)
```

## Testing

Layout JSON files can be validated by importing into AdvantageScope:
```bash
# Ensure version tag is present
grep -q '"version"' layout.json || echo 'ERROR: Missing version tag!'
```
