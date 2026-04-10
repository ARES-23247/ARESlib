---
name: areslib-ci
description: Defines the GitHub Actions CI/CD pipeline for ARESLib2. Use when configuring automated builds, test runners, or deployment workflows.
---

You are an expert DevOps engineer for Team ARES. When configuring CI/CD pipelines, build automation, or test infrastructure for ARESLib2, adhere strictly to the following guidelines.

## 1. Architecture

ARESLib2 uses GitHub Actions for continuous integration. The pipeline lives in `.github/workflows/`:

```
.github/workflows/
├── ci.yml               # Matrix build, Test, PMD, Spotless, JaCoCo, Javadocs
├── dependabot-auto-merge.yml
├── release-drafter.yml
```

## 2. Key Rules

The `ci.yml` workflow triggers on push/PRs to `main` (ignoring markdown changes):
```yaml
name: ARESLib2 FTC CI
on:
  push:
    branches: [ "main" ]
    paths-ignore:
      - '**.md'
  pull_request:
    branches: [ "main" ]
    paths-ignore:
      - '**.md'

jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'corretto'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Compile (pre-check)
        run: ./gradlew compileJava compileTestJava
      - name: Build with Gradle and Coverage
        run: ./gradlew build jacocoTestReport
      - name: Upload Test Reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: ARESLib2-Test-Reports-${{ matrix.os }} # Avoids collision
          path: build/reports/tests/test/
```

### Rule B: Use Java 17
ARESLib2 targets Java 17 (the FTC SDK minimum). Always specify `java-version: '17'` in the setup step. Do not use Java 21 — it introduces bytecode incompatibilities with the FTC runtime.

### Rule C: Gradle Caching & PMD Annotations
ARESLib2 leverages `gradle/actions/setup-gradle@v4` for automatic build layer caching. We also enforce code quality natively in PRs using the Reviewdog PMD action and automatic Spotless commits:
```yaml
      - name: Apply Code Formatting
        if: matrix.os == 'ubuntu-latest'
        run: ./gradlew spotlessApply
        
      - name: Commit Auto-formatted Code
        if: matrix.os == 'ubuntu-latest'
        uses: stefanzweifel/git-auto-commit-action@v5
        with:
          commit_message: "style: auto-format code with spotless"
          
      - name: Run Reviewdog PMD Annotations
        uses: kemsakurai/action-pmd@v1
        if: github.event_name == 'pull_request' && matrix.os == 'ubuntu-latest'
        with:
          reporter: github-pr-review
```

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

Configure GitHub branch protection for `main`:
- Require status checks to pass before merging
- Require the `build` job to succeed across all matrix matrices
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
