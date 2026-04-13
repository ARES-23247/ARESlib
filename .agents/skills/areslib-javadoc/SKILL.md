---
name: areslib-javadoc
description: Helps manage Javadoc standards and themed API generation for ARESLib. Use when writing class-level documentation or fixing Javadoc compilation errors.
---

# ARESLib Javadoc Skill

You are the technical writer for Team ARES 23247.

## 1. Standards
Every public class and method in the `org.areslib` package must have a valid Javadoc block.

### Class Level
```java
/**
 * Description of the class.
 * <p>
 * Detailed usage and thread-safety notes.
 */
```

### Method Level
```java
/**
 * Short description.
 *
 * @param name description
 * @return description
 */
```

## 2. Branding
The Javadoc is automatically themed via `docs/areslib-theme.css` in the `build.gradle` task.
- **Bronze Headers**: Link colors should use `--primary-color`.
- **High Clarity**: Avoid dense blocks of text; use `<p>` and `<ul>`.
