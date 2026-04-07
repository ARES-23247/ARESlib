---
name: areslib-ci
description: Defines the GitHub Actions CI/CD pipeline for ARESLib2. Use when configuring automated builds, test runners, or deployment workflows.
---

You are a DevOps engineer for Team ARES. When configuring CI/CD pipelines, build automation, or test infrastructure for ARESLib2, adhere strictly to the following guidelines.

## 1. Architecture

ARESLib2 uses GitHub Actions for continuous integration. The pipeline lives in `.github/workflows/`:

```
.github/workflows/
├── build.yml       # Compile + test on every push/PR
├── release.yml     # Tagged release builds (optional)
```

## 2. Key Rules

### Rule A: Every Push Builds and Tests
The `build.yml` workflow triggers on every push to `master` and every pull request:
```yaml
name: Build & Test
on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build
        run: ./gradlew build -x test
      - name: Test
        run: ./gradlew test
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: build/reports/tests/
```

### Rule B: Use Java 17
ARESLib2 targets Java 17 (the FTC SDK minimum). Always specify `java-version: '17'` in the setup step. Do not use Java 21 — it introduces bytecode incompatibilities with the FTC runtime.

### Rule C: Cache Gradle Dependencies
Add Gradle caching to speed up CI builds:
```yaml
      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: ${{ runner.os }}-gradle-
```

### Rule D: Separate Build and Test Steps
Always separate `build -x test` and `test` into two steps. This way:
- Build failures are clearly distinguished from test failures
- Test results are always uploaded even if some tests fail (`if: always()`)

## 3. Test Configuration

### Headless Testing
ARESLib2 tests run headless (no Android, no robot). The `build.gradle` configures this via the `.aar` extraction pipeline. See the `gradle-ftc-desktop` skill for details.

### Test Filtering
Run specific test suites in CI:
```yaml
# Run only physics tests
- run: ./gradlew test --tests "org.areslib.hardware.*"

# Run only command tests
- run: ./gradlew test --tests "org.areslib.command.*"
```

## 4. Branch Protection (Recommended)

Configure GitHub branch protection for `master`:
- Require status checks to pass before merging
- Require the `build` job to succeed
- Require PR reviews from at least 1 team member

## 5. Common Pitfalls

### Don't: Skip tests in CI
```yaml
# BAD — hides regressions
run: ./gradlew build -x test

# GOOD — always test
run: ./gradlew build
```

### Don't: Use latest Java
```yaml
# BAD — Java 21 breaks FTC bytecode compatibility
java-version: '21'

# GOOD — matches FTC SDK requirements
java-version: '17'
```

### Don't: Forget to upload test artifacts
Without `upload-artifact`, failed test reports disappear when the runner terminates. Always upload `build/reports/tests/` so teammates can debug failures from the GitHub UI.
