# Code Review Guidelines

Derived from analysis of 5,258 JIRA bugs and 111,119 inline PR review comments from the insight-brain repository.

## Always check — Backend (Java)

- Input validation at API boundaries — every endpoint and form field rejects invalid formats, empty strings, excessively long values on both client and server before processing. (122 bugs, 23% Blocker+Critical)
- REST error responses and messages — every endpoint handles error paths gracefully; appropriate HTTP status codes; error bodies are user-facing and actionable; no leakage of stack traces, SQL fragments, or file paths. (250 bugs combined)
- Exception specificity — catch blocks handle specific types, not broad `catch(Exception)`; exceptions causing the correct types of HTTP response codes are used, with miscellaneous runtime exceptions being 500s. (192 bugs)
- Null safety — nullable parameters guarded before dereferencing; Optional for optional values; collection lookups (Map.get, List.get) handle missing entries. (117 NPE bugs, ~990 review comments)
- Error propagation — exceptions not swallowed; callers can distinguish success from failure; error info preserved through the chain; use specific exception types for wrapping (e.g., UncheckedIOException not RuntimeException). (~910 review comments)
- Concurrency safety — shared mutable state uses concurrent-safe collections; avoid parallel streams (shared ForkJoinPool); volatile/AtomicReference for cross-thread data; singletons are thread-safe. (118 bugs, 14 Blocker+Critical)
- SQL safety — queries parameterized (never string-concatenated); new queries covered by indexes; parameter counts bounded (PostgreSQL 65,535 limit); avoid LOWER/UPPER in WHERE clauses (prevents index usage). (75 bugs, 11 Blocker+Critical)
- Resource management — streams, connections, file handles closed via try-with-resources; cleanup in finally blocks; no leaks in error paths. (34 OOM/leak bugs, 18% Blocker+Critical)
- Unbounded data processing — results paginated or streamed, not loaded entirely into memory; caches have eviction policies and size limits; avoid DAO calls in loops (batch instead). (34 memory bugs)
- Input parsing robustness — parsing code handles malformed data, unexpected sizes, and atypical structure; test with directories named like files, empty manifests, circular dependencies, and oversized inputs. (164 scan-processing bugs)
- Credential and security handling — credentials never in URLs, properly encrypted at rest; security-sensitive paths thoroughly tested with distinct test credentials. (~1,100 security review comments)
- Logging hygiene — no credentials, tokens, or PII in logs; appropriate log levels (no ERROR for expected conditions); no logging inside tight loops without rate limiting; log messages include context (not just the exception). (31 bugs, ~530 review comments)
- API backward compatibility — public API changes maintain backward compatibility; schema changes include deprecation notices with removal dates; response DTOs match OpenAPI spec. (~810 review comments)
- DB migrations — idempotent; handle partial failure; backward-compatible with previous app version running concurrently. (75 DB bugs)
- Cache invalidation — invalidation triggers defined for all mutation paths; HTTP cache headers (ETag, Cache-Control) set correctly for API responses. (24 stale-data bugs)
- Date/time handling — stored and transmitted in UTC; display converts to user timezone; DST handled for scheduled jobs; explicit formatting patterns. (19 timezone bugs)

## Always check — Frontend (React/JS)

- Conditional rendering — every falsy-path shows the right thing (nothing, empty state, or loading indicator); all data-loading states covered (loading, error, empty, populated). (154 bugs)
- Navigation and links — routes constructed from constants or config, not hardcoded; base URL, context path, and URL encoding consistently applied. (127 bugs)
- Modal/dialog lifecycle — dismiss/close handlers reset state; no z-index overlap; focus trapping works; tooltip content bound to current context. (117 bugs)
- Layout and overflow — overflow behavior explicitly set for variable-length containers; minimum viewport sizes tested; no reflow from dynamic content. (98 bugs)
- Search/filter logic — input trimmed and normalized; case sensitivity matches expectations; AND/OR semantics correct; clearing one filter doesn't corrupt others. (70 bugs)
- Button/action handlers — loading/disabled state toggled during async ops; error callbacks re-enable the button; handler actually wired to click event. (67 bugs)
- Dropdown/select state — default selection explicitly set; clearing resets dependent fields; value sent to API matches expected case/format. (31 bugs)
- Sorting correctness — numeric fields use numeric comparison (not string); null values have defined sort position; sort stable across page loads. (31 bugs)
- Display accuracy — frontend aggregation matches backend query; filter state included in count queries; label/badge logic handles all enum values. (15 bugs)
- State management — mutations are immutable; unmount handlers clean up subscriptions; entity map keys use unique internal IDs. (9 bugs)
- Destructive UI operations — confirmation prompt exists for delete/cancel-with-unsaved-changes; "dirty" flags set only on actual user changes. (13 bugs)
- Content-hashed assets — frontend build outputs use content-hashed filenames to bust browser cache on deploy. (24 stale-data bugs)

## Always check — All code

- Boundary and edge cases — empty strings, whitespace-only, zero/negative, max collection sizes, very long strings, single-item vs. many-item scenarios all explicitly handled. (59 bugs, ~720 review comments)
- Test coverage — tests for each meaningful scenario (happy path, error path, edge cases); test names describe what they verify; tests assert the intended behavior. (~3,900 review comments)
- Permission consistency — frontend hide/disable and backend authorize checks reference the same permission constant for the same action; inherited roles resolved correctly; @Authorize annotations only on non-private methods (private methods bypass enforcement). (67 bugs, ~700 review comments)
- Conventions, naming, and cleanup — code follows existing codebase conventions for naming, structure, and test organization; names convey purpose without comments; no unused imports, dead code, leftover debug statements, or duplicated logic. (~12,600 review comments combined)
- Encoding — consistent UTF-8; URL parameters properly encoded/decoded; file I/O specifies charset; non-ASCII characters tested. (35 bugs)

## Always check — Nexus One (NOUX) Classic component embeds

- `mountClassicComponent` required-prop contract — `mountClassicComponent` does NOT inject ui-router resolve values. Classic components that read required props from route resolves (not Redux/selectors) receive `undefined` at mount time. For every mounted component, grep for `PropTypes.isRequired` declarations and trace `componentDidMount`/render logic. If a required prop (e.g., `isAuthorized: PropTypes.bool.isRequired`) isn't sourced from the Redux store, supply it via a thin wrapper component (`AuthorizedXxx`) that hard-codes the auth-guaranteed value (e.g., `isAuthorized={true}` after a passing `redirectTo` guard).
- Per-child-state `redirectTo` — `@uirouter/core`'s `onStart` hook fires `trans.to().redirectTo` for the **leaf state only**. A parent state's `redirectTo` does NOT fire when navigating directly to a child state. Every child state that requires an auth gate must declare its own `redirectTo` explicitly.
- `classicPreviewMap` route-type choice — routes with meaningful sub-paths (e.g., `/users/_new_`, `/users/{id}`, `/users/activity/{user}`) must use `SUBTREE_MAPPINGS`, not `NEXUS_ONE_TO_CLASSIC`. Identity entries in `NEXUS_ONE_TO_CLASSIC` return only the mapped base and discard the tail on the Classic ↔ Nexus One toggle. `SUBTREE_MAPPINGS` preserves everything after the base via `path.slice(nexusOne.length)`.
- `isNexusOnePath` completeness — adding a `SUBTREE_MAPPINGS` entry alone is insufficient. Without an explicit `isNexusOnePath` check for the new path prefix (`path === '/new-route' || path.startsWith('/new-route/')`), `isClassicPath` returns `true` first and `toClassicEquivalent` short-circuits to `CLASSIC_DEFAULT_PATH` before reaching the subtree logic.
- Dead `data` metadata — before registering `data.*` fields on a state (e.g., `data.activeTab`), verify the component actually reads them. Fields that look like they'd drive behavior (tab selection, panel visibility) may be ignored in favor of `router.currentState.name` or explicit `showXxxOnly` props — registering them creates misleading dead metadata.
- Page object deduplication — before creating a new Playwright page object, search for existing classes covering the same component (by root CSS selector, ARIA role, or component name). Duplicate page objects for the same component diverge over time and create conflicting locator strategies. Prefer extending the existing class with missing locators.
- Import ordering in Java — import blocks must be sorted alphabetically within each group. Out-of-order imports are caught by Spotless but are a review-time signal that an import was added hastily. Check alphabetical ordering whenever the diff adds a new Java import.
