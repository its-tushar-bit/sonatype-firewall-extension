<!--
  Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
  Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
  "Sonatype" is a trademark of Sonatype, Inc.
-->

# Playwright Test Module — Developer Guide

Guidance for writing, categorising, and maintaining Playwright tests in this module.
For build commands, Maven profiles, and run instructions see the [README](README.md).

---

## Selector strategy

Prefer selectors in this order:

1. **Role + accessible name** — `getByRole(AriaRole.BUTTON, setName("Search"))`. Resilient to CSS and ID changes; mirrors how assistive technology finds elements.
2. **Accessible label** — `getByLabel("Username")`. Good for form inputs that have an associated `<label>`.
3. **Visible text** — `getByText("Add a Policy")` or `filter(hasText(...))`. Readable and stable.
4. **`data-testid`** — `byTestId("policies-tile")`. Use when no semantic selector is practical and the frontend team has deliberately added a test hook.
5. **ID or CSS class** — `locator("#some-id")` / `locator(".some-class")`. Last resort. Acceptable when:
   - The element is an **unlabelled structural container** (no ARIA role/name) used only to scope subsequent role-based queries, **or**
   - The button/link text is **dynamically composed** at runtime from owner names, feature flags, or counts, making a fixed-name selector fragile.

When you do use an ID or class selector, add a Javadoc comment explaining why the semantic alternative is not available.

### Examples

```java
// ✅ Role + name — preferred
container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Search"))

// ✅ Role + accessible label
container().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1))

// ✅ Structural scope anchor — ID is unavoidable; interaction inside uses role
//    .iq-adv-search__field is an unlabelled layout div with no ARIA role.
Locator fieldSection = queryRows().nth(rowIndex).locator(".iq-adv-search__field");
fieldSection.getByRole(AriaRole.BUTTON).click();   // interaction is role-based

// ✅ Dynamic text — owner name included at runtime, ID is the stable anchor
ownerActionsMenu().locator("#delete-owner-link").click();   // text = "Delete {ownerName}"

// ❌ Avoidable — the button has static accessible text "Search"
locator("#advanced-search-button")
```

---

## Test category: Sanity vs Regression

Every test method (or class) must carry exactly one of:

```java
@Category(SanityTest.class)     // PR pre-merge gate, fast, focused
@Category(RegressionTest.class) // Broader coverage
```

REST API regression tests live in a separate module
(`insight-brain-api-regression-test`) and use their own `ApiRegressionTest` category —
they are not in this module.

### Decision guide

Ask these questions in order:

1. **Would a failure block you from merging a trivial backend fix?**
   If yes → Sanity. A test that only catches a specific edge case does not need to gate every PR.

2. **Is this the first thing you would check after a deployment?**
   Login, top-level navigation, and the happy path of each feature area belong in Sanity.
   Detailed validation paths, error messages, and secondary interactions belong in Regression.

3. **Is this an error path, "what if" scenario, or a secondary interaction?**
   Error/validation/edge-case paths → Regression. Happy paths → Sanity.

4. **Does the test require slow data setup, multi-org hierarchies, or a page reload to verify persistence?**
   Slow / multi-step tests → Regression, to avoid bloating the PR-blocking run.

**Still unsure?** Default to `RegressionTest`. The nightly run catches bugs too.

### Sanity examples — tests that gate PR merge

| Test | Why Sanity |
| --- | --- |
| `OrganizationPlaywrightTest.testOrgPolicies` | Verifies the Policies tile loads on a child org — the most basic check that policy management is functional. |
| `OrganizationPlaywrightTest.testOrgAccess` | Verifies the Access tile renders — simple navigation / page-load check. |
| `OrganizationPlaywrightTest.testCreateOrganizationWithTemporaryEntity` | Core CRUD happy path: create an org and verify it appears. |
| `RootOrganizationPolicyEditorPlaywrightTest.testRootOrgHasNoInheritedPolicies_andCreatePolicyAppearsInList` | Create a policy at the root org and verify it appears in the tile — happy path of the policy editor. |
| `RootOrganizationPolicyEditorPlaywrightTest.testPolicyAtRootIsInheritedByChildOrg_andIsReadOnlyThere` | Policy created at root appears read-only in a child org — the fundamental inheritance contract. |

### Regression examples — tests that run nightly

| Test | Why Regression |
| --- | --- |
| `OrganizationRegressionPlaywrightTest.testCreateOrganization_emptyNameShowsValidation` | Error / validation path (empty name shows a required-field error). Happy-path org creation is already covered in Sanity. |
| `OrganizationRegressionPlaywrightTest.testChildOrgActionsDropdownOptions` | Verifies the exact set of items in the Actions dropdown — brittle to minor UI copy changes; not a PR blocker if it fails. |
| `OrganizationRegressionPlaywrightTest.testEditComponentLabel_updatedValuesPersistInLabelsTile` | Full edit-then-reload CRUD cycle. Slow (seed + edit + navigate back); tile-load level is already in Sanity. |
| `OrganizationRegressionPlaywrightTest.testLegacyViolationsLicenseGate_errorAlertShownWhenNotSupported` | Feature-flag combination test — strips a license feature via route interception. Edge case, not a happy path. |
| `RootOrganizationPolicyEditorPlaywrightTest.testInheritedPolicyWithActionsOverride_updatePersistsOverride` | Edge case: child org saves an actions override and the selection persists after a page reload. Multi-step; requires reload to verify persistence. |
| `AccessEditorPlaywrightTest.testDeleteRoleAssignment_confirmationModalAndNavigatesAway` | Full modal confirmation flow (cancel + confirm paths). Secondary interaction on an already-tested feature area. |
| `AccessEditorPlaywrightTest.testEditMode_noChangesMade_submitShowsValidationError` | Validation error path (no-changes-to-save message). Error path, not a happy path. |
| `AdvancedSearchQueryBuilderPlaywrightTest.testQueryBuilder_ClearAllTermsResetsQueryAndDisablesSearch` | Edge-case builder state (all rows removed → empty state + disabled Search button). |

---

## Key rules

- **Clean up with `TemporaryEntity`** — all tests that create database entities must use `@Rule public TemporaryEntity tempEntity`. UI-created entities are also cleaned up because `TemporaryEntity.after()` deletes all non-root orgs and apps.
- **No `Thread.sleep()`** — use explicit Playwright `waitFor` / `waitForURL` / `waitForSelector`.
- **Feature flags** — if a test enables a system-configuration flag in `@Before`, disable it in `@After` to avoid leaking state to sibling test classes running in the same JVM session.
- **Mocking the API** — when the embedded IQ server lacks a required index (e.g. Advanced Search / OpenSearch), mock the endpoint with `page.route(...)` and document why in the test class Javadoc.
