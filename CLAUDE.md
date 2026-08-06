# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Nexus IQ Server** (insight-brain) implements a suite of products designed to secure the software supply chain, primarily focusing on open-source software (OSS) components. Their key products include:

* **Sonatype SBOM Manager:** This platform is designed to help organizations manage their Software Bill of Materials (SBOMs) effectively. It allows for the generation, ingestion, management, and storage of SBOMs in standard formats like CycloneDX and SPDX. By leveraging Sonatype Lifecycle's vulnerability data, SBOM Manager provides insights into components, vulnerabilities, and malware, enabling organizations to detect and mitigate security risks, demonstrate compliance with regulations (like NIS2 and US Executive Order on Cybersecurity), and streamline auditing processes. It also supports continuous monitoring and integrates with existing workflows.
* **Sonatype Lifecycle:** Lifecycle is Sonatype's software composition analysis (SCA) tool that helps manage open-source risks across the entire Software Development Lifecycle (SDLC). It allows organizations to define and automatically enforce policies for open-source components based on security, license, and quality risks. Lifecycle integrates with various development tools (IDEs, SCMs, CI/CD) to provide continuous monitoring, actionable insights, and automated remediation guidance, helping developers make secure open-source choices and accelerate their development processes.
* **Sonatype Firewall:** This product acts as a first line of defense against malicious open-source code entering your development pipelines. Sonatype Firewall automatically identifies and blocks known and suspected malicious components, including malware, AI models, and containers, before they can be downloaded into your repositories or workflows. It leverages AI behavioral analysis and policy enforcement to prevent risky components from reaching developers, thereby reducing remediation work and minimizing security incidents. Firewall can also continuously scan existing repositories to identify and quarantine previously introduced threats.

## Deployment Variants

**Nexus IQ Server** is available in two deployment variants, both implementing all three products listed above:

* **On-Premises IQ Server:** Traditional single-tenant deployment for individual organizations, with full administrative control and customization capabilities.
* **Multi-Tenant IQ (MTIQ):** A specialized variant designed to support multiple isolated tenants within a single deployment. This allows service providers and large enterprises to serve multiple organizations or business units while maintaining strict data isolation and security boundaries between tenants.

## Build Commands

### Maven Build Commands
- **Full build with tests**: `mvn clean install` (WARNING: takes a long time)
- **Quick build (skip tests)**: `mvn clean install -Pquick`
- **Fast tests only**: `mvn verify -DexcludedGroups=SlowTest`
- **Skip functional tests**: Add `-D skip-functional-test` to any mvn command
- **Local Chrome for functional tests**: `-Dwebdriver.chrome.driver=/your/path/to/chromedriver`
- **Single test class**: `mvn verify -Dtest=TestClassName -Dit.test=TestClassName`
- **Single test method**: `mvn verify -Dtest=TestClassName#testMethodName -Dit.test=TestClassName#testMethodName`

### Frontend Build Commands (insight-brain-frontend/)
- **Start dev server**: `yarn start`
- **Build for production**: `yarn build`
- **Run tests**: `yarn test` (runs Jest tests and lint)
- **Lint**: `yarn test-lint`
- **Jest tests**: `yarn jest`
- **Jest watch mode**: `yarn jest-watch`
- **Individual test file**: `yarn jest -- <test-name>`

### Fast Frontend Development Loop with Functional Tests
To iterate on frontend changes without a full rebuild, use the esbuild dev server mode:

1. Start the dev server from `insight-brain-frontend/`: `yarn start` (serves on port 8070, proxies API calls to port 8072)
2. Run any functional test with `-Dfunctional-test-webpack-dev-server=true` (from `insight-brain-playwright-test/`)

```bash
cd insight-brain-playwright-test
mvn verify -Dit.test=SomeTest#someMethod -Dfunctional-test-webpack-dev-server=true
```

In this mode the test server starts on fixed port 8072 (matching the dev server proxy target) and the browser points at the dev server on port 8070. Frontend changes are picked up instantly without any rebuild of `insight-brain-service`.

### Development Profiles
- **Quick profile**: `-Pquick` - skips tests, linting, and checks

## Architecture

### Multi-Module Maven Project Structure
- **insight-brain-service**: Main server application (Dropwizard + JAX-RS)
- **insight-brain-frontend**: React frontend (npm/webpack)
- **insight-brain-db**: Database layer (jOOQ/PostgreSQL/H2)
- **insight-brain-data**: Data access layer
- **insight-brain-policy**: Policy engine (Drools)
- **insight-brain-client**: Client library
- **nexus-iq-server**: Main server bundle
- **nexus-mtiq-server**: Multi-tenant server variant

### Technology Stack
- **Backend**: Java 25, Dropwizard 5.x, JAX-RS, Guice DI
- **Database**: PostgreSQL (prod), H2 (dev/test/light prod), jOOQ
- **Frontend (Legacy IQ UI)**: React 19, Redux Toolkit, UI Router, esbuild, SCSS
- **Frontend (Guide SPA)**: React 19, React Router 7, `@guide/ui-core`, Radix UI Themes, TypeScript, CSS Modules
- **Testing**: JUnit 4, Mockito, Selenium, Jest, React Testing Library
- **Security**: Apache Shiro, Spring Security SAML2 (OpenSAML5)

### Key Configuration
- Main config files: `config.yml` (Dropwizard YAML)
- Development configs in `src/test/resources/config-*.yml`
- Database migrations in `insight-brain-db/src/main/resources/db/`

## Running the Application

### Quick Start
Two convenience scripts are provided for local development:

- **`./local-dev-build.sh`** - Builds insight-brain-service and all dependencies (including frontend). Uses `-Pquick` to skip tests.
- **`./local-dev-run.sh`** - Starts backend on port 8072 and frontend dev server on port 8070. Access the app at http://localhost:8070

### Server Deployment
Run from `insight-brain-service/` directory:
```bash
mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.spring.InsightBrainSpringApplication -Dexec.args='server src/test/resources/config-dev.yml'
```
Default credentials: admin/admin123

### Requirements
- Java 25
- Maven 3.9.x
- yarn (for frontend)
- Docker (for tests requiring external services)
- License file required on first launch

### Test Database Cleanup
Tests using IQ database must cleanup with `TemporaryEntity` rule:
```java
@Rule
public TemporaryEntity tempEntity = new TemporaryEntity();
```

## Development Notes
### Git Workflow
- Use descriptive branch names. The branch name should be prefixed with the Jira ticket ID. For example:
  `CLM-12345-some-meaningful-description`
- Keep commits focused and well-described. The commit title should be suffixed with the Jira ticket ID.
- When creating a PR in github, the first line in the PR description should be a link to the Jira ticket. For ex:
  Jira: https://sonatype.atlassian.net/browse/CLM-12345

### Feature Flags
New experimental features use `SystemConfigurationPropertyFeature` enum in database.

### External Dependencies
- **HDS (hosted-data-services)**: Provides vulnerability/license data
- **insight-scanner**: Component scanning library
- **react-shared-components**: Shared React component library

### Code Quality
- Spotless auto-formatter with Eclipse formatter config (`sonatype-config/sonatype-eclipse.xml`)
  - Locally: `spotless:apply` auto-formats changed files (via `sonatype` profile, active by default)
  - CI: `spotless:check` validates formatting (apply is skipped via `ci` profile)
  - Only formats files changed vs `origin/main` (ratchetFrom)
- License headers required (use `header.txt`)

### jakarta.inject Migration
The codebase has been migrated to `jakarta.inject` as part of the Jakarta EE 11 upgrade. Use `jakarta.inject` for all dependency injection.
Do not use `javax.inject` as it is no longer supported. Mixing javax and jakarta imports can cause runtime errors.

### Changes to classes structure or JSON serialization that may break policy violation comparison
Classes that are converted to JSON or to Drools code and already exist in the `main` branch should not be changed without close inspection and peer/Tech Lead review.
Changing such structure leads to existing policy violations and waivers in a database are no longer considered the same after upgrading IQ Server, which leads to data being seemingly loss (i.e. waivers no longer applied).
Those classes are in the `insight-brain-data` module. Some examples:
- Classes in the `com.sonatype.insight.brain.model.policy.facts` package.
- Classes in the `com.sonatype.insight.brain.model.policy.conditions` package with the method `generateDroolsConditionCode`.
- Classes with comments similar to "Any change to this class structure or to its JSON serialization may break policy violation comparison".

### Incremental database SQL scripts need to be immutable
Incremental database SQL scripts (aka migrations) that already exists in the target branch of a pull-request should not be changed.
Doing so would case inconsistencies in database schemas both between teammates and potentially customers as well.
Such SQL scripts are located under `insight-brain-db/src/main/resources/db` and are prefixed by `schema_incremental_`.
Instead of changing the existing script, a new incremental SQL script should be created to take the database schema to the desired form.
Scripts `schema.sql` on the other hand are expected to change so databases that are created for the first time using that file get the new schema from the start.

## Guide Frontend (Self-Hosted SPA)

The Guide SPA is a **separate single-page application** within `insight-brain-frontend/` that provides Sonatype Guide for self-hosted customers. It is fully independent from the legacy IQ Server UI — separate entry point (`guide/index.tsx`), routing, build bundle, and component library.

The SaaS version of Guide is a Next.js app in the `seaworthy` repo. The self-hosted SPA reuses the same shared UI components via `@guide/ui-core`. **React Router 7** was chosen (instead of the legacy UI Router used elsewhere in this codebase) because its APIs closely mirror Next.js routing APIs — this means the `NavigationAdapter` in `@guide/ui-core` translates cleanly to both environments with minimal overhead. Using UI Router would have required a much more complex adapter with significant semantic mismatches.

Guide and legacy IQ UI code must **never cross-import** — they share no runtime state.

For complete directory structure conventions, naming rules, component organization, and testing patterns, see [`insight-brain-frontend/CLAUDE.md`](insight-brain-frontend/CLAUDE.md) under the "Guide SPA" section.

## Code Review Guidelines

These guidelines are used by both automated AI reviewers and human reviewers. They are ordered by priority.
Based on analysis of 300 merged PRs, 15 reverts, 81 bug-fix PRs, and 100+ Jira bugs from Dec 2025 – Mar 2026.

### 1. Database migration integrity
- Incremental SQL scripts (`schema_incremental_*.sql`) already in the target branch MUST NOT be modified — create a new incremental script instead. Violating this causes schema drift between existing and fresh installations (caused reverts: PR #15241, PR #15249; reviewer caught in PR #15204)
- New columns MUST be added at the END of the table definition in `schema.sql` (PostgreSQL `ALTER TABLE ADD COLUMN` always appends; mismatches break `CanonicalSchemaValidationTest`)
- No `COMMENT` statements in schema files (causes byte-for-byte drift between schema.sql and migrations)
- PostgreSQL INSERT statements in tests must explicitly list all column names (never rely on column order)
- `schema.sql` files ARE expected to change (for fresh installs) — only incrementals are immutable
- **New nullable columns and existing data**: when adding columns that will be NULL for existing records, ALL code paths reading that column must handle NULL. Java immutable collections (`Set.of()`, `List.of()`) throw NPE on `.contains(null)` — this caused a Critical production bug (CLM-37961: NPE in SBOM vulnerability details post-upgrade from v191→v195)

### 2. Policy violation comparison classes (data loss risk)
- Classes in `com.sonatype.insight.brain.model.policy.facts` and `com.sonatype.insight.brain.model.policy.conditions` with `generateDroolsConditionCode` are serialized to JSON for violation/waiver matching. Changing their structure or JSON serialization breaks existing waivers in customer databases (waivers stop matching violations, appearing as data loss)
- Any structural change to these classes requires Tech Lead review and a migration plan
- Look for comments like "Any change to this class structure or to its JSON serialization may break policy violation comparison"

### 3. Bugs and logic errors
- **Null handling**: Missing null checks on data from external systems (HDS, OpenSearch, user input). Comparators without `nullsFirst`/`nullsLast` have caused same-day reverts (PR #15290 → #15303 → #15305). Optional misuse (`.get()` without `.isPresent()`). Java immutable collections (`Set.of()`, `List.of()`) throw NPE on `.contains(null)` unlike `HashSet` (CLM-37961)
- **Concurrency issues**: Race conditions in telemetry, caching, and batch operations. Double-checked locking bugs (PR #15048: httpClient assignment outside synchronized block). `BooleanSupplierShutdownRequest` polling forever when supplier returns true instead of false (PR #15048)
- **Off-by-one errors** in pagination, aggregation sizes, retry loop bounds (PR #14975: retry loop doing `maxRetries - 1` instead of `maxRetries`), or index calculations
- **Policy evaluation logic**: High regression risk — verify constraint evaluation doesn't introduce performance bottlenecks (CLM-38159: security vulnerability group constraints caused Sev-1 outages at $1.5M ARR customer)
- **Arithmetic overflow**: Exponential backoff calculations with bit shifts can overflow for large attempt values (PR #14975). Cap shift values before computing.
- **Return null vs empty collection**: Methods returning `List` should return `Collections.emptyList()` instead of `null` to keep API consistent and reduce null-handling at call sites (flagged in PR #15168)

### 4. Regressions
- If the PR modifies **policy evaluation** or Drools rules: flag for extra scrutiny (historically high regression area across v195/v196 releases). CLM-38699: DependencyType condition with other conditions never matches
- If the PR modifies **SBOM processing** (CycloneDX/SPDX): check for API backward compatibility (CLM-37982: DELETE SBOM API broke in v195). Verify non-standard license identifiers are handled gracefully — recurring failures on Artistic-dist (CLM-38792), SMAIL-GPL (CLM-38555), license URLs exceeding 200 chars (CLM-38729), and SPDX license expressions (CLM-38381)
- If the PR modifies **authentication/authorization** (Shiro, Spring Security SAML2, GitHub App): verify all auth modes still work and inheritance logic doesn't bypass security checks. GitHub App auth is the #1 source of bugs: 29 PRs, 2 reverts, 13+ Jira bugs since Dec 2025
- If the PR modifies **OpenSearch queries**: flag for performance review
- If the PR removes or changes `@Transactional` boundaries or OpenJPA entity relationships: flag potential data consistency issues. Entity manager lifecycle bugs cause "entity manager closed" exceptions when accessed across thread boundaries or in async callbacks (PR #15190)
- If the PR changes **feature flag** logic (`SystemConfigurationPropertyFeature`): verify `enabledWhenAbsent` behavior is correct (CLM-38204). Incorrect feature gating silently disables functionality — CLM-38213 (Critical): container scanning returned no violations because feature was gated on license feature
- If the PR modifies **scan report data or fields**: verify behavior in BOTH manual scan AND continuous monitoring contexts. CLM-38947 (Critical): reachability markers lost during CM, causing auto-waivers to disappear
- If the PR modifies **streaming endpoints or HTTP headers**: verify behavior behind load balancers/proxies (ALB, nginx) and across HTTP 1.0/1.1. CLM-38045 (Blocker): ALB idle timeout from delayed CSV output. CLM-37981: Advanced Search Export broken with HTTP 1.0. CLM-38675: invalid `Content-Encoding: utf-8` header broke Jenkins pipelines

### 5. GitHub App / SCM configuration changes
GitHub App authentication is the highest-churn, highest-bug-rate area in the codebase (29 PRs, 2 reverts, 13+ Jira bugs since Dec 2025). PRs in this area require extra scrutiny:
- **Inheritance hierarchy**: verify behavior at ALL levels (root org → child org → application) and ALL transitions (PAT ↔ GitHub App ↔ inherited). CLM-38874: 500 error switching auth methods. CLM-38951: incorrect display at org level. CLM-38709: GitHub App remains selected after switching to PAT
- **Registration/installation lifecycle**: verify cleanup on deletion (CLM-39016: GitHub App not deleted from DB when App/Org deleted), re-registration flows (CLM-38945), and personal account vs organization account paths (CLM-38950, CLM-38932)
- **State management in frontend**: source control config has complex state interactions between provider selection, auth type, inheritance, and GitHub App installation. Review comments on PR #15276 flagged missing feature flag checks, inconsistent naming, and need for shared utility methods
- **Caching with tenant safety**: GitHub App auth strategies must be cached per-ownerId and wrapped in `TenantReference` for MTIQ safety (PR #15120 review feedback)
- **Accidental scope creep**: PR #15156 reverted because unrelated Auto-PR/PR-commenting changes were accidentally merged into the GitHub App registration feature. PRs should contain only changes related to the stated Jira ticket(s)

### 6. Missing or inadequate tests
- New backend logic without tests — this project uses JUnit 4 + Mockito, but integration tests with real beans are preferred over mocked unit tests
- Tests using `Thread.sleep()` instead of explicit waits or synchronization (ongoing flakiness source — PRs #15219, #15284, #15348)
- Tests using `@Ignore` to suppress failures without a clear justification and linked ticket to re-enable (PR #14972 reverted an `@Ignore` that was premature)
- Tests that don't use `TemporaryEntity` rule for database cleanup
- Frontend tests using `fireEvent` instead of `userEvent` for user interactions (React Testing Library best practice)
- Calendar/time-dependent tests without mocked clocks (break on month boundaries — PR #15402)
- Tests that pass on the feature branch but fail after merge to main — this has caused multiple reverts (PRs #15329, #15352). Ensure tests don't depend on branch-specific state
- Don't write tests for trivial getters/setters (PR #15249 review feedback)

### 7. Multi-tenant (MTIQ) safety
- Changes to **license validation** logic: intermittent 402 errors have caused tenant-wide outages (CLM-38370). Verify license check paths handle nulls and edge cases
- **Database connection pool** changes: verify health checks test write capability, not just connectivity (CLM-37841, CLM-37843). MTIQ uses Aurora — connection pool must handle failover (CLM-37842: `maxConnectionLifetimeSeconds` needed)
- **Tenant isolation**: verify no data leakage between tenants. Changes to shared services must not expose tenant-specific data. Caches must be wrapped in `TenantReference` (PR #15120)
- Feature flags: ensure `enabledWhenAbsent` defaults are correct for both on-prem and MTIQ variants
- **SaaS-specific behavior**: features that should be disabled for SaaS tenants (CLM-38607: SaaS customers able to configure custom email servers). Verify SaaS-only restrictions are enforced
- **Tenant lifecycle**: operations on deleted/unlicensed tenants must be handled (CLM-38973: TenantLicenseUpdaterTask updates deleted tenants; CLM-38966: infinite license update loop)

### 8. Resource and performance issues
- **Unbounded collections** (ConcurrentHashMap, ArrayList without size limits) that grow over time — telemetry memory leaks have been a recurring issue (PR #15280, CLM-38822, CLM-36031). Prefer returning `TelemetryData` from methods instead of accumulating in instance variable lists (PR #15280 review feedback)
- **Missing database indexes** on columns used in WHERE/ORDER BY clauses
- **N+1 query patterns**: especially in policy evaluation (CLM-38159: 3400 component versions each triggering vulnerability group queries — Sev-1 at Vanguard), SBOM processing, org hierarchy traversal (CLM-38233: Support Zip import taking 15+ min), and license threat group operations (CLM-38299: thousands of queries on save)
- **Large-scale data operations**: operations that iterate over all apps/components/violations in an org must be reviewed for scale with 1000+ entities. CLM-38965: org deletion with thousands of apps taking days. Consider batch processing and progress tracking
- **UI performance at scale**: queries that work for small datasets but degrade at enterprise scale (40k+ apps — CLM-36272). CLM-38700: UI unusable with hundreds of concurrent scans. Verify pagination and filtering
- **Excessive logging**: recurring issue — CLM-38750 (applicableWaivers REST API excessive logging), CLM-34561 (ThirdPartyScanResultsProcessor). Use parameterized logging (`log.warn("msg {}", arg)`) instead of string concatenation (PR #15168 review feedback)
- **Static allocation**: avoid creating new collections per method invocation when a static final field suffices (PR #14899: `new HashSet<>(Set.of(...))` on every call → `private static final Set`)

### 9. Security
- **Authentication bypass**: changes to Shiro configuration, Spring Security SAML2, or GitHub App auth that might skip auth for protected endpoints
- **Authorization logic**: filter/permission bypasses (PR #15273: container waiver permission filtering fix). GitHub App auth inheritance must not bypass security checks at any level (PR #15297, CLM-38874)
- **Feature-gated security scanning**: incorrect feature flag checks can silently disable security functionality. CLM-38213 (Critical): container scanning returned no policy violations because the feature was gated on a license feature the customer didn't have — scans should fail explicitly rather than silently return empty results
- **Cookie security**: SameSite attributes, HttpOnly flags (CLM-31884)
- Missing input validation at API boundaries (user input, external API responses)
- Exposed secrets or credentials in config files or logs. Version information exposure on SaaS instances (CLM-38871)
- **Dependency upgrades**: verify upgrades don't introduce breaking changes (PR #15353: Jetty CVE, PR #15268: Axios DoS). Cryptographic library upgrades are especially sensitive — PR #15075: BouncyCastle upgrade broke FIPS loading, requiring revert. Always run FIPS tests after crypto library changes

### 10. Upgrade path compatibility
PRs that add new database columns, change data formats, or modify API contracts must be verified against upgrade scenarios:
- **New columns with null existing data**: code must handle NULL values for the new column in all existing records (CLM-37961: Critical NPE post-upgrade)
- **API backward compatibility**: existing API consumers must continue to work after changes (CLM-37982: DELETE SBOM API broke in v195; CLM-37981: Advanced Search Export broke with HTTP 1.0 from CLM-35734 changes)
- **File system operations in HA/shared storage**: file operations must handle NFS attribute caching and concurrent access (CLM-38445: FileAlreadyExistsException on NFS). Check file existence before deletion (CLM-37904: Delete SBOM binary only if file exists)
- **Reachability/scan data preservation**: features that enrich scan reports must preserve data through re-evaluation and continuous monitoring (CLM-38947)

### 11. Jakarta EE migration
- `javax.inject` imports MUST NOT be used — the codebase has migrated to `jakarta.inject`. Mixing javax and jakarta causes runtime injection failures
- Watch for `javax.inject` in new or modified files

### 12. Configuration and infrastructure
- Dropwizard `config.yml` changes: verify they apply to both on-prem and MTIQ variants (PR #15172: syntax fix in MTIQ config.yml)
- Environment-specific configs in `src/test/resources/config-*.yml`
- Jenkinsfile changes: verify pipeline stages, parallelism, and timeout values. See [`jenkins/DEPLOYMENT-PIPELINE.md`](jenkins/DEPLOYMENT-PIPELINE.md) for the full MTIQ deployment pipeline runbook (job flow, failure modes, rollback procedures).
- Build profile changes (`-Pquick`, `--Pci`, `sonatype` profile): verify Spotless behavior (apply vs check)

### 13. Documentation coherence
- If the PR changes behavior documented in CLAUDE.md, the CLAUDE.md must be updated too
- Javadoc that contradicts actual behavior

### 14. Process compliance
- PR description must include a Jira ticket link (`Jira: https://sonatype.atlassian.net/browse/CLM-####`) and a Jenkins build link (`Jenkins: https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/feature-snapshots/job/<branch>/`)
- Branch name must be prefixed with Jira ticket ID (e.g., `CLM-12345-description`)
- Spotless formatting must be applied (`mvn spotless:apply`)
- PRs should contain only changes related to the stated Jira ticket(s) — unrelated changes should be in separate PRs (PR #15156 revert: unrelated backend changes accidentally merged)
- Add a comment to the Jira ticket with a link to the PR as a proper markdown link (`PR: [#123](https://github.com/sonatype/insight-brain/pull/123)`) — no Jenkins URL in the Jira comment

### 15. Code reuse and query patterns
- **IN-clause queries**: Use `AbstractSqlDAO.getListWithSqlInClause()` or `getStreamWithSqlInClause()` (`insight-brain-data/.../dataaccess/AbstractSqlDAO.java:287,326`) instead of writing manual batch-splitting logic. These handle chunking large collections automatically.
- **No correlated subqueries**: Don't use subqueries in WHERE clauses that reference columns from the outer query — they execute once per row. Use JOINs instead: `LEFT JOIN child ON ... WHERE child.id IS NULL` instead of `WHERE id NOT IN (SELECT ... WHERE child.col = parent.col)`
- **Batch DB operations**: Never query inside a loop. Collect IDs, then batch-fetch with IN-clause utilities.
- **Reuse existing logic**: Before writing new pattern-matching, hostname-filtering, or permission-checking code, grep the codebase — it likely already exists and reimplementing creates drift risk.

### 16. Testing philosophy
- **Assert real state, not mock interactions**: Prefer `assertThat(pool.getActiveCount()).isEqualTo(1)` over `verify(mock).getConnection()`. Tests should verify observable behavior/state, not that the right methods were called.
- **Don't test implementation details**: Enums, internal command construction, and static content are implementation details. Test the user-facing behavior instead.
- **No redundant tests**: Before adding a test, check if the same scenario is already covered in the file or a sibling test class. Merge similar tests rather than creating new classes.
- **Classify correctly**: Unit tests don't use real plumbing. Integration tests use real infrastructure. Don't mislabel.

### 17. API and interface design
- **Names must match behavior**: If a method creates two data sources, name it `createDataSources`. Return types should match existing patterns in the class.
- **Minimize API surface**: Expose the decision, not the internals. One public method that takes raw input and returns a boolean beats two composable public methods (parse + check).
- **Keep internal helpers off public interfaces**: Methods that are utilities for subclasses shouldn't be on the public interface.

### 18. Frontend patterns
*(Applies to the legacy IQ UI in `insight-brain-frontend/`. Guide SPA uses `@guide/ui-core` instead — see `insight-brain-frontend/CLAUDE.md`.)*
- **Use React Shared Components (RSC)**: Use `NxTile`, `NxP`, `NxCode`, `NxH1`-`NxH3` instead of bare HTML elements. See usage examples in `insight-brain-frontend/src/main/frontend/violation/ViolationDetailsTile.jsx`.
- **No CSS anti-patterns**: No `!important` (escape hatch, not a tool), no `vh` units (assumes page layout), no hardcoded colors (use CSS variables like `var(--nx-border-default)`), use `overflow: auto` not `overflow: scroll`.

### 19. Code Commenting
- **Comments must describe current behavior:** Code comments and Javadoc describe *current* behavior only. Do NOT narrate what changed, what it used to do, or why it's different from before. Avoid phrases like "previously…", "used to…", "this PR adds…", or "no longer…".

### What NOT to flag in reviews
- Code formatting or style (Spotless handles this)
- Missing comments on self-explanatory code
- Naming preferences that are subjective
- Theoretical issues that require impossible conditions
- Copyright headers (automated checks handle this)
- Import ordering
- Suggestions to add error handling for scenarios that genuinely cannot happen
- Trivial getter/setter tests (PR #15249)
