---
name: areslib-documentation
description: Helps maintain the ARESLib documentation hub and tutorials. Use when adding new tutorials, updating the standards guide, or fixing documentation links.
---

# ARESLib Documentation Skill

You are the documentation lead for Team ARES 23247.

## 1. Documentation Hub Structure
The site is located in `docs/` and generated via `./gradlew generateDocs`.

- `index.html`: Main landing page with auto-injected stats.
- `standards.html`: "The ARESLib Standard" coding guide.
- `tutorials/`: Deep-dive technical guides.

## 2. Branding Guidelines
- **Colors**: Bronze (#CD7F32), Red (#B32416), White (#FFFFFF).
- **Aesthetic**: Premium, dark-mode, animated backgrounds, high-fidelity visuals.

## 3. Tutorial Header Numbering
To achieve the "Cool Number Box" look for tutorial sections, wrap the header number in a `<span class="ares-num">` tag:
```html
<h2><span class="ares-num">1</span> How to use it</h2>
```
This is a core branding standard and MUST be applied to all numbered sections in tutorials.

## 3. Dynamic Stats
The `generateDocs` task searches for the following placeholders in `index.html`:
- `${SOURCE_FILES}`: Total count of Java source files.
- `${UNIT_TESTS}`: Total count of JUnit tests.
- `${TOTAL_COVERAGE}`: JaCoCo line/branch coverage percentage.

## 4. Workflows
- **Adding a Tutorial**: Create `docs/tutorial-name.html`, link it in `tutorials.html`, and ensure it follows the premium CSS framework.
- **Updating Stats**: Run `./gradlew updateDocsCoverage`.
