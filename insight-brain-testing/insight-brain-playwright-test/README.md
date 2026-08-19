<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Functional tests (Playwright + JUnit 4)

End-to-end UI functional tests for **Nexus IQ Server (insight-brain)** on-premise.

The active test stack is **Playwright for Java + JUnit 4**, running against an embedded
IQ Server started in-process via `AbstractIqUiTest`. The legacy Selenium / Selenide
infrastructure has been retired; only one Selenium test remains for parity reasons. New
UI tests **must** be written in Playwright following the [Authoring new tests](#authoring-new-tests)
section below.

### Base class hierarchy

Playwright lifecycle and server bootstrap are split so a future MTIQ UI base can reuse the
browser layer without duplicating it:

```
AbstractPlaywrightTest          # Browser/context/page, tracing, video, generic nav helpers
  └── AbstractIqUiTest          # Embedded IQ (TestCLMServer, DB, Shiro, license, IQ login/logout)
        └── *PlaywrightTest     # Concrete tests (extend AbstractIqUiTest — do not extend AbstractPlaywrightTest directly)
```

| Class | Responsibility |
| --- | --- |
| `AbstractPlaywrightTest` | Shared Playwright-only layer: Chromium launch, per-test `BrowserContext`/`Page`, trace/screenshot/video on failure, browser console capture, `playwrightNavigateTo` / `playwrightRefreshOrOpen` / `playwrightHardreset`, etc. No IQ or MTIQ imports. |
| `AbstractIqUiTest` | On-prem embedded IQ: `TestCLMServer`, `ReverseProxyServer`, `DatabaseContainer`, `TemporaryEntity`, Guice test modules, HDS mock, license helpers, and IQ-specific `playwrightLogin` / `playwrightLogout`. |
| `*PlaywrightTest` | Feature tests — orchestration only; assertions live in `*Assertions` companions. |

When MTIQ Playwright tests are added, they should extend `AbstractPlaywrightTest` via a sibling
class (e.g. `AbstractMtiqUiTest`) with MTIQ-flavoured server bootstrap, not by subclassing
`AbstractIqUiTest`.

---

## Module layout

```
insight-brain-playwright-test/
├── pom.xml                                # Failsafe + Playwright config
├── README.md                              # this file
├── run-playwright-diagnostics.sh          # smoke-checks TestCLMServer + Playwright wiring
└── src/test/
    ├── java/com/sonatype/clm/testing/playwright/   # Playwright UI test framework
    │   ├── AbstractPlaywrightTest.java    # Shared Playwright lifecycle (see hierarchy above)
    │   ├── AbstractIqUiTest.java          # Embedded IQ + IQ login helpers; extend this in new tests
    │   ├── architecture/                  # ArchUnit rules (e.g. PlaywrightStabilityRulesCheck)
    │   ├── categories/                    # JUnit @Category markers (SanityTest, RegressionTest)
    │   ├── pages/                         # Page Objects + *Assertions companions (~90 classes)
    │   ├── testdatamanager/               # TestDataManager – optional JSON fixtures
    │   ├── tests/                         # *PlaywrightTest classes (~53 classes / ~166 @Test methods)
    │   └── utils/                         # Wait / action / timing helpers
    └── resources/
        ├── test-data/                     # Optional JSON fixtures (most tests use Java constants)
        ├── componentDetails/              # HDS canned responses (cross-referenced from test-data/*.json)
        ├── vulnerabilityDetails/          # HDS canned responses
        ├── legal/                         # HDS canned responses
        ├── canned-hds-responses/          # Shared HDS payloads (also used by insight-brain-service tests)
        ├── canned-reports/                # Pre-evaluated scan reports (small/large/empty/v3/v4 …)
        ├── policyExport/                  # Policy import bundles
        ├── reference-policies-v3.json     # Reference policy bundle
        ├── ldapData/                      # LDAP test fixtures
        ├── EnterpriseReporting/           # Enterprise-reporting fixtures
        ├── AppEvalReport/, sbom/, com/, …# Other backend fixtures
        ├── config-test.yml                # Dropwizard test config
        └── logback-test.xml               # Test logging config
```

> Note on fixtures: `test-data/*.json` files contain UI strings, expected text, and
> *paths* to backend fixtures (e.g. `"resourcePath": "/componentDetails/foo.json"`).
> The sibling resource folders are mock backend payloads served by `HdsMockServer`
> and are also consumed by integration tests in the `insight-brain-service` module
> via the `insight-brain-functional-test-common` jar — **do not move them**.

---

## Requirements

| Tool                | Version                                                                         |
| ------------------- | ------------------------------------------------------------------------------- |
| **JDK**             | Java **25**                                                                     |
| **Maven**           | 3.9.x                                                                           |
| **Playwright deps** | Resolved automatically (browsers downloaded on first run; no manual install)    |
| **License file**    | Provided automatically by `TestProductLicenseManager`                           |

The Selenium-era requirement to install a `chromedriver` binary no longer applies.

---

## Running tests

All commands assume you are in the **repo root** unless noted otherwise.

### Single Playwright test (typical dev loop)

```bash
mvn verify -pl insight-brain-playwright-test \
  -Dit.test=LoginPlaywrightTest \
  -Dskip-functional-test=false
```

### Single test method

```bash
mvn verify -pl insight-brain-playwright-test \
  -Dit.test=ComponentDetailsPlaywrightTest#testComponentDetails_shouldRenderSecurityTab
```

### Whole module

```bash
mvn verify -pl insight-brain-playwright-test
```

### CI partitions: sanity vs. regression

The Playwright suite is split into two mutually exclusive JUnit 4 `@Category` partitions:

| Partition | Marker interface | CI job | Purpose |
| --- | --- | --- | --- |
| Sanity | `com.sonatype.clm.testing.playwright.categories.SanityTest` | PR pre-merge | Critical user paths whose failure should block merge — login, navigation, base-URL, top-level routing, plus the happy path of each feature area. |
| Regression | `com.sonatype.clm.testing.playwright.categories.RegressionTest` | Nightly | Edge cases, error paths, permutation matrices, feature-flag combinations, slow data setup. |

Every `@Test` method belongs to exactly one partition — never both, never neither.

> **Transitional state.** All Playwright tests are currently tagged
> `@Category(SanityTest.class)` so they run in the `-Psanity` CI job. Tests will be
> migrated to `@Category(RegressionTest.class)` as the nightly regression job is wired up.

**Local invocation (sanity partition):**

```bash
mvn verify -pl insight-brain-playwright-test -Psanity
```

**CI invocation (recommended):**

```bash
mvn -B -V --no-transfer-progress \
  -pl insight-brain-playwright-test -am \
  -Psanity \
  verify
```

`-B -V --no-transfer-progress` keeps logs tidy; `-am` builds upstream module deps on
clean CI workspaces. Failure artifacts (traces, screenshots, diagnostics) land under
`target/` — see [Test diagnostics & artifacts](#test-diagnostics--artifacts).

**Equivalent without the profile** (set the property directly):

```bash
mvn verify -pl insight-brain-playwright-test \
  -Dfailsafe.groups=com.sonatype.clm.testing.playwright.categories.SanityTest
```

**Run everything *except* the sanity partition:**

```bash
mvn verify -pl insight-brain-playwright-test \
  -Dfailsafe.excludedGroups=com.sonatype.clm.testing.playwright.categories.SanityTest
```

#### Choosing the partition for a new test

Apply the marker at **method level** so a single class can carry tests from both
partitions side by side:

> **Decision guide** — ask these questions in order:
>
> 1. **Would a failure block you from merging a trivial backend fix?**
>    If yes → Sanity. A test that only catches a specific edge case does not need to gate every PR.
>
> 2. **Is this the first thing you would check after a deployment?**
>    Login, top-level navigation, and the happy path of each feature area belong in Sanity.
>    Detailed validation paths, error messages, and secondary interactions belong in Regression.
>
> 3. **Is this an error path, "what if" scenario, or a secondary interaction?**
>    Error/validation/edge-case paths → Regression. Happy paths → Sanity.
>
> 4. **Does the test require slow data setup, multi-org hierarchies, or a page reload to verify persistence?**
>    Slow / multi-step tests → Regression, to avoid bloating the PR-blocking run.
>
> **Still unsure?** Default to `RegressionTest`. The nightly run catches bugs too.

##### Sanity examples — tests that gate PR merge

| Test | Why Sanity |
| --- | --- |
| `OrganizationPlaywrightTest.testOrgPolicies` | Verifies the Policies tile loads on a child org — the most basic check that policy management is functional. |
| `OrganizationPlaywrightTest.testOrgAccess` | Verifies the Access tile renders — simple navigation / page-load check. |
| `OrganizationPlaywrightTest.testCreateOrganizationWithTemporaryEntity` | Core CRUD happy path: create an org and verify it appears. |
| `RootOrganizationPolicyEditorPlaywrightTest.testRootOrgHasNoInheritedPolicies_andCreatePolicyAppearsInList` | Create a policy at the root org and verify it appears in the tile — happy path of the policy editor. |
| `RootOrganizationPolicyEditorPlaywrightTest.testPolicyAtRootIsInheritedByChildOrg_andIsReadOnlyThere` | Policy created at root appears read-only in a child org — the fundamental inheritance contract. |

##### Regression examples — tests that run nightly

| Test | Why Regression |
| --- | --- |
| `OrganizationRegressionPlaywrightTest.testCreateOrganization_emptyNameShowsValidation` | Error / validation path (empty name shows a required-field error). Happy-path org creation is already covered in Sanity. |
| `OrganizationRegressionPlaywrightTest.testChildOrgActionsDropdownOptions` | Verifies the exact set of items in the Actions dropdown — brittle to minor UI copy changes; not a PR blocker if it fails. |
| `OrganizationRegressionPlaywrightTest.testEditComponentLabel_updatedValuesPersistInLabelsTile` | Full edit-then-reload CRUD cycle. Slow (seed + edit + navigate back); the tile-load level is already covered in Sanity. |
| `OrganizationRegressionPlaywrightTest.testLegacyViolationsLicenseGate_errorAlertShownWhenNotSupported` | Feature-flag combination test — strips a license feature via route interception. Edge case, not a happy path. |
| `RootOrganizationPolicyEditorPlaywrightTest.testInheritedPolicyWithActionsOverride_updatePersistsOverride` | Edge case: child org saves an actions override and the selection persists after a page reload. Multi-step; requires reload to verify persistence. |
| `AccessEditorPlaywrightTest.testDeleteRoleAssignment_confirmationModalAndNavigatesAway` | Full modal confirmation flow (cancel + confirm paths). Secondary interaction on an already-tested feature area. |
| `AccessEditorPlaywrightTest.testEditMode_noChangesMade_submitShowsValidationError` | Validation error path (no-changes-to-save message). Error path, not a happy path. |
| `AdvancedSearchQueryBuilderPlaywrightTest.testQueryBuilder_ClearAllTermsResetsQueryAndDisablesSearch` | Edge-case builder state (all rows removed → empty state + disabled Search button). |

```java
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

@Test
@Category(SanityTest.class)
public void testCriticalLoginPath() { ... }

@Test
@Category(RegressionTest.class)        // nightly regression partition
public void testRememberMeEdgeCase() { ... }
```

The partition is explicit and grep-able:

```bash
grep -rl '@Category(SanityTest.class)' src/test/java/com/sonatype/clm/testing/playwright/tests
```

### Skip functional tests in a wider build

```bash
mvn install -Dskip-functional-test          # legacy flag, still honored
mvn install -Dskip.functional.tests=true    # equivalent
```

### Headed mode (watch the browser)

```bash
mvn verify -pl insight-brain-playwright-test \
  -Dit.test=LoginPlaywrightTest \
  -Dplaywright.headless=false \
  -Dplaywright.slowMo=200
```

### Pause for the Playwright Inspector

```bash
mvn verify -pl insight-brain-playwright-test \
  -Dit.test=LoginPlaywrightTest \
  -Dplaywright.headless=false \
  -Dplaywright.manualPause=true
```

In tests that call `playwrightManualPauseIfEnabled()` the run will block, allowing you to
step through with the Playwright Inspector.

### Fast frontend dev loop (webpack-dev-server)

To iterate on `insight-brain-frontend` changes without rebuilding `insight-brain-service`:

1. From `insight-brain-frontend/`: `yarn start` (serves on port 8070, proxies API to 8072)
2. From this module:

```bash
mvn verify -pl insight-brain-playwright-test \
  -Dit.test=SomeTest#someMethod \
  -Dfunctional-test-webpack-dev-server=true
```

The test server binds to fixed port 8072 (matching the WDS proxy target) and the browser
points at the WDS on 8070. Frontend changes are picked up instantly.

### Database mode

```bash
mvn verify -pl insight-brain-playwright-test \
  -Dit.test=LoginPlaywrightTest
```

The Playwright module uses embedded-postgres by default, so Docker is not required for
normal local execution.

---

## Test diagnostics & artifacts

`AbstractPlaywrightTest` captures failure artifacts while the `Page` is still open (via a
JUnit `TestRule` that wraps each test method). After a run, look under `target/`:

| Path | Purpose |
| --- | --- |
| `target/playwright-traces/<class>.<method>.zip` | Playwright trace (failed tests by default; `-Dplaywright.trace=always` for all). |
| `target/playwright-screenshots/<class>.<method>.png` | Full-page screenshot on failure. |
| `target/playwright-videos/<class>.<method>.webm` | Per-test video when `-Dplaywright.video=on` (off by default — large files). |
| `target/playwright-diagnostics/<class>.<method>.diag.txt` | Plain-text failure summary: URL, title, body snippet, browser console warnings/errors. |
| `target/failsafe-reports/` | Standard JUnit/Failsafe XML + text reports (primary CI signal). |
| `target/failsafe-reports/*-output.txt` | Test JVM stdout/stderr when redirect is enabled. |

### View a trace locally

```bash
mvn exec:java -pl insight-brain-playwright-test \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="show-trace target/playwright-traces/com.example.SomeTest.testMethod.zip"
```

Replace the path with the zip from your failed test. This uses the official Playwright trace
viewer (no custom HTML report in this module).

A self-contained HTML overview report with step timelines and inline trace viewing is
planned but not yet implemented in this module.

### Disable trace capture (CI cost trimming)

```bash
mvn verify -pl insight-brain-playwright-test -Dplaywright.trace=off
```

### Run the infrastructure smoke-check

```bash
./run-playwright-diagnostics.sh
```

Verifies that `TestCLMServer` starts, the embedded server is reachable, admin
credentials work, and Playwright can connect.

---

## Authoring new tests

Follow these rules before writing new tests:

1. Create a Page Object under `playwright/pages/` (extend `BasePage` where appropriate).
   Put all `should*` assertion methods in a separate `<PageName>Assertions` companion class
   in the same package, accepting the page object via constructor. Test classes must not
   contain assertion logic — only orchestration.
2. Put expected UI strings, seed values, and HDS fixture paths as `private static final`
   Java constants on the test class. Use a JSON fixture under
   `src/test/resources/test-data/<your-test>.json` **only** when the data meets one of the
   criteria in the `TestDataManager` Javadoc (data-driven lists, per-environment variants,
   shared across suites, or non-engineer-editable payloads).
3. If you do use a JSON fixture, load it via `TestDataManager.load(...)` into a typed Java `record`.
4. Extend **`AbstractIqUiTest`** (not `AbstractPlaywrightTest` directly) unless you are
   introducing a new server-flavoured base class. Use inherited helpers:
   `playwrightLogin()`, `playwrightNavigateTo(path)`, `page`, `context`, `tempEntity`, etc.
5. Tag every `@Test` method with exactly one partition marker:
   `@Category(SanityTest.class)` for PR pre-merge, or `@Category(RegressionTest.class)`
   for nightly regression (see [CI partitions: sanity vs. regression](#ci-partitions-sanity-vs-regression)).
6. Follow the [Testing Library query priority](https://testing-library.com/docs/queries/about#priority)
   (mirrored in `BasePage`): **`getByRole` → `getByLabel` → `getByPlaceholder` → `getByText`
   → `getByTestId` → CSS only as a last resort** (tables, submit masks, layout hooks).
   Cross-check names against the matching `*.jestspec.jsx` in `insight-brain-frontend`.
7. Use explicit visibility waits — never `Thread.sleep` or `page.waitForTimeout`.
8. Use `TemporaryEntity` (from `AbstractIqUiTest`) for database cleanup; never leak state between tests.

The ArchUnit rules under `architecture/` enforce items 7–8 at compile time — you cannot
introduce `Thread.sleep` or `page.waitForTimeout` without an explicit allowlist entry in
`PlaywrightWaitUtils`.

---

## Troubleshooting

### 1) "UnsupportedClassVersionError ... class file version 69.0"

Caused by running Maven with an older JDK than the project's compiled dependencies.

**Fix.** Confirm both IDE and shell Maven use JDK 25:

```bash
java -version
mvn -version
```

### 2) "Unenhanced classes were detected" (OpenJPA)

```
openjpa.Enhance - Unenhanced classes were detected even though the enhancer has ran.
```

OpenJPA byte-code enhancement on the data module didn't run.

**Fix.** From `insight-brain-data/`:

```bash
mvn process-classes
```

### 3) Playwright test times out waiting for a button / locator

Almost always a race between the test and an asynchronous frontend render. **Do not add
`Thread.sleep`** — it masks product bugs and is a known revert source.

**Fix.** Use an explicit visibility wait before interacting:

```java
assertThat(page.locator("#some-button")).isVisible();
page.locator("#some-button").click();
```

See this README's Troubleshooting guidance and the
`PlaywrightStabilityRulesCheck` ArchUnit rule for the no-sleep flake policy.

### 4) "Strict mode violation: locator resolved to N elements"

Playwright's strict mode rejects ambiguous locators.

**Fix.** Tighten the locator with `getByRole`, `nth(...)`, or scope it to a parent
container. Avoid raw CSS that can match multiple components.

### 5) Tests pass on the feature branch but fail after merge to `main`

A known cause of reverts. Verify your test does not depend on branch-specific state
(e.g. a fixture, seeded entity, or feature-flag default present only on your branch).

### 6) Many test classes report "Tests run: N, Errors: N, Time elapsed: 0.001 s" with `Could not start embedded postgres`

Symptom in `target/failsafe-reports/*.txt`:

```
Caused by: java.lang.IllegalStateException: Could not start embedded postgres
Caused by: java.io.IOException: Gave up waiting for server to start after 10000ms
```

Failsafe may run multiple parallel forks (`failsafe.forkCount` defaults to `1C` in
`pom.xml` — one fork per CPU core). Each fork spins up its own embedded postgres,
Dropwizard server, and Chromium; under-resourced runners (or laptops doing other
work) can saturate I/O so postgres misses its 10-second startup window. When a
fork's static class initialiser fails, every test class assigned to that fork is
marked as a tombstoned `Errors: N, Time: 0.001 s` even though it never executed.

**Fix.** Force a single fork:

```bash
mvn verify -pl insight-brain-playwright-test -Psanity -Dfailsafe.forkCount=1
```

For CI, pin a stable count in your job (e.g. `-Dfailsafe.forkCount=2` on a quiet
8-vCPU runner) rather than relying on `1C`, which varies by runner shape.

---

## CI / build

- Failsafe is configured with `forkCount=${failsafe.forkCount}` and `reuseForks=true`.
- Playwright runs **headless** by default in CI (`playwright.headless=true`).
- Traces default to `playwright.trace=on-failure` (zip saved only when a test fails).
  Override with `-Dplaywright.trace=always` to capture every test, or
  `-Dplaywright.trace=off` to disable tracing entirely.
- Spotless formatting is enforced via the parent build (`spotless:check` on CI,
  `spotless:apply` locally).

---

## Cross-module notes

- This module depends on `insight-brain-service`, `insight-brain-data`,
  `insight-brain-db`, `insight-brain-common`, and `insight-brain-functional-test-common`.
- Resource folders under `src/test/resources/` (`canned-hds-responses/`,
  `EnterpriseReporting/`, `policyExport/`, `componentDetails/`, `vulnerabilityDetails/`,
  `legal/`, `sbom/`, `com/…`, `reference-policies-v3.json`) are also consumed by
  integration tests in `insight-brain-service`. **Renaming or moving them will break a
  sibling module's tests** — always grep for the path first.
- Production code in `insight-brain-service` (e.g. `PolicyImportExport.java`) reads
  some of the same classpath resources at runtime.
