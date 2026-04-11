---
name: areslib-core-standards
description: Enforces strict elite coding standards including 'Never Nester', explicit unit nomenclature, avoiding Hungarian notation, and strict mathematical documentation. Use when writing ANY new file or refactoring logic to ensure compliance with world-class API standards.
---

# ARESLib Core Standards

You are an expert FRC Software Engineer for Team ARES. To produce championship-grade, scalable, and provably correct code, you must enforce the following strict coding standards derived from elite analytical frameworks.

## 1. Unit Nomenclature Requirements
Physical units must be explicitly and statically declared throughout all variable and parameter interactions. Primitive ambiguity is forbidden.

- **Use `edu.wpi.first.units` where applicable:** Public interfaces should accept strictly validated WPILib Units (e.g. `Measure<Distance> distance`).
- **Strict Prefixing/Suffixing on Doubles:** If `double` is used internally for speed/computation, the variable MUST explicitly state its SI unit suffix:
  - `double velocityMetersPerSecond` (NOT `double velocity` or `double vel`)
  - `double accelerationNewtons`
  - `double delaySeconds`
  - `double differenceBetweenGroundAndDesiredMeters`

## 2. No Hungarian Notation
Never prefix variables with identifiers indicating their type or access level. Modern IDEs handle this context.
- **BAD:** `double m_velocity;`, `final int k_maxSpeed = 5;`, `boolean bIsActive;`
- **GOOD:** `double velocity;`, `final int MAX_SPEED = 5;`, `boolean isActive;`

## 3. Be a "Never Nester"
Deeply nested conditional logic is unreadable and error-prone. Use immediate guard clauses and early returns.
- **BAD:**
  ```java
  public void process(Item item) {
      if (item != null) {
          if (item.isValid()) {
              item.process();
          }
      }
  }
  ```
- **GOOD:**
  ```java
  public void process(Item item) {
      if (item == null) return;
      if (!item.isValid()) return;

      item.process();
  }
  ```

## 4. Mathematical & Physics References
Whenever physics equations, motion kinematics, or control theory matrices are implemented in Java, you MUST include a comment block referencing the underlying math (e.g. Wiki, whitepaper link, or textbook citation).

## 5. Explicit, Descriptive Naming
Disallow single-character or massively abbreviated variable names outside of standard mathematical iterating bounds (`i`, `j`).
- **BAD:** `double x;`, `double m;`, `double diff;`
- **GOOD:** `double trackLengthX;`, `double chassisMassKg;`, `double errorToleranceMeters;`

## 6. File Limits
- Restrict logic class lengths strictly to functional encapsulation. Refactor large loops into bounded helper libraries if a subsystem natively exceeds ~600 lines.
