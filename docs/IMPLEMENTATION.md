# Sonatype Firewall extension — real insight-brain integration

## Context

The extension today talks to a local `mock-iq/server.js` that exposes a single custom `POST /api/v2/firewall/verdict` combining component metadata + policy verdict + reachability. Real IQ Server (in `../insight-brain`) has **no such combined endpoint**. To get off the mock we need to:

1. Compose the verdict client-side from real endpoints.
2. Give the extension a stable "firewall repository" scope to evaluate against, per ecosystem. The user has pointed us at the new **Virtual Repository Manager (VRM)** concept (`insight-brain` PRs #16867 / #16760, FIRE-660 / FIRE-665) as the intended container: a `repository_manager` with `manager_type='VIRTUAL'` that owns per-ecosystem proxy repositories with an upstream URL. That gives the extension a well-defined `(repoManagerId, repositoryId)` per ecosystem — same pair the real firewall evaluate endpoint takes.
3. Add a **Scan section in the popup** with three tabs — Verdict / History / Scan detail — so the user can (a) review recent scans this session and (b) see exactly which API calls the extension made and how they went. The current single-page popup only shows the "current" verdict, and even that gets clobbered between tabs (already fixed earlier in this session).

Mock mode stays in the codebase as an opt-in demo/offline path — same `FirewallVerdict` shape means popup code doesn't fork.

Outcome: extension works against a running insight-brain (local `./local-dev-run.sh`, port 8070) using real component data, real firewall policy verdicts, and real waiver submission — while adding the two UI surfaces the user asked for.

### Removed from scope

- **Install-command rewrite** (npm/pip snippets on package pages routed through `nexusProxyUrl`). Dropped by user decision — the download path is handled by the developer's own Nexus setup and doesn't belong in the extension. Phase 1 deletes:
  - `nexusProxyUrl` and `rewriteInstallCommands` fields from `ExtensionSettings` (`src/types.ts`).
  - `rewriteInstallSnippets(...)` function from `src/content/shared.ts` and its callers in `src/content/npm.ts` and `src/content/pypi.ts`.
  - The "Nexus proxy" section from the Options page.

---

## API mapping (mock → real)

All real endpoints live in `insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/`. Auth is HTTP Basic with user token (`userCode:passCode` → `Authorization: Basic …`). Base URL default `http://localhost:8070`.

| Mock (current) | Real IQ endpoint | Notes |
|---|---|---|
| `GET /health` | `GET /ping` → `"pong"` | Used by Settings → Test connection. `PingServletConfiguration.java:18-20` |
| `GET /api/v2/components/info?purl=…` | `POST /api/v2/components/details`, body `{components:[{packageUrl}]}` | `ApiComponentDetailsResourceV2.java:48-69`. Response has `integrityRating`, `securityData.securityIssues`, `licenseData`. |
| `POST /api/v2/firewall/verdict` (single call) | `POST /api/v2/firewall/components/{vrmId}/{repoId}/evaluate`, body list of `{packageUrl,hash?}` | `ApiFirewallResource.java:578-647`. Returns policy verdict + quarantine state + **policy violations with IDs** (needed for waivers). |
| `POST /api/v2/remediation` | `POST /api/v2/components/remediation/repository/{repoId}`, body `ApiComponentDTOV2` | `ApiComponentRemediationResource.java:37-95`. Map `next-no-violations` → `goldenVersion`. |
| `POST /api/v2/waivers` (by PURL) | `POST /api/v2/policyWaiverRequests/{policyViolationId}/repository/{repoId}` | `ApiPolicyWaiverRequestResource.java:66-70`. **Requires `policyViolationId` from the evaluate response** — that's the "create a policy violation record via VRM eval" flow the user wants. |
| ABF match, threat types | Read from `ApiComponentDetailsResultDTOV2` (integrity data). | No dedicated endpoint. |

**Client-side composition per package view (one background call, three requests in parallel):**

```
fetchVerdict(purl):
  ecosystem   = ecosystemFromPurl(purl)                 // already implemented
  repoId      = vrmRepoIds[ecosystem]                   // resolved once at startup, cached
  [details, evaluate, remediation] = Promise.all([
    POST /api/v2/components/details                 { components: [{packageUrl: purl}] }
    POST /api/v2/firewall/components/{vrmId}/{repoId}/evaluate  [ {packageUrl: purl} ]
    POST /api/v2/components/remediation/repository/{repoId}     { packageUrl: purl }
  ])
  return composeFirewallVerdict(details, evaluate, remediation)
```

`evaluate` returns policy violations attached to the component — we stash the first blocking violation's `policyViolationId` on the `FirewallVerdict` so the popup's waiver button knows what to waive.

---

## VRM configuration

The user configures **one VRM** and the extension auto-discovers its child repos.

**Settings additions** (`src/options/Options.tsx` + `ExtensionSettings` in `src/types.ts`):
- `mode: 'real' | 'mock'` — dropdown (default mock so the extension is usable out of the box)
- `iqServerUrl` — existing, default `http://localhost:8070`
- `userCode` / `passCode` — existing (already used as HTTP Basic)
- `virtualRepositoryManagerName` — new text field ("Firewall VRM name")
- **Test connection** button — hits `GET /ping`, then lists repos under the VRM and shows discovered child repos per ecosystem.

**VRM resolution flow (once per session, cached in `chrome.storage.local`):**
1. `GET /rest/repositories/repository_managers` (or existing list endpoint) → find the manager with `name = virtualRepositoryManagerName` and `managerType = 'VIRTUAL'` → extract `id`.
2. List repositories under that manager → group by `format` (npm / pypi / maven) → build `{ecosystem → repoId}`.
3. Store as `vrmContext = { managerId, repoIds: { npm, pypi, maven } }`.

Cache is invalidated when the user re-saves Settings or a request 404s on a stale repo id.

---

## CORS

IQ Server has CORS off by default (`FilterConfiguration.java:155-166`, only enabled if `webSettings.hasCorsSettings()`). Two options; we go with option 1:

1. **Chrome host_permissions bypass (recommended, MV3-standard).** Extensions listed in `host_permissions` bypass CORS entirely — no preflight, browser sends the request as if same-origin. Since the IQ URL is user-configurable, we can't hard-code it in `manifest.json`. Instead: keep the current `host_permissions` for mock, and use `chrome.permissions.request({origins: [<iqUrl>/*]})` after the user saves Settings ("Add permission for this IQ Server?" prompt). Standard MV3 pattern.
2. Change insight-brain's `config.yml` to allow chrome-extension origins — customer-side change, not something the extension can ship.

Add `"permissions": ["storage", "activeTab"]` → `["storage", "activeTab", "permissions"]` in the manifest.

---

## UI: popup restructured into three tabs

Popup width goes from ~360px to ~420px (comfortable for tabs). Header stays; tab bar sits under it; tab body swaps.

### Verdict tab (default; ~unchanged from today)
The current `Popup.tsx` content — component name/version, verdict pill, policy reasons, integrity rating, CVEs, golden version, waiver + settings buttons.

### History tab
Scrollable list of the last **20 scanned packages this session**, newest first, persisted to `chrome.storage.local` (evicts oldest past 20).

Each row: ecosystem chip · name@version · verdict pill · relative time. Click → switches to Verdict tab pre-loaded with that entry.

### Scan detail tab
For the currently-focused package (whatever's shown in Verdict): a call-by-call breakdown of what the extension did.

Each entry: method+path · status (200/4xx/5xx or `error`) · latency ms · expandable "response summary" (JSON preview). This is the diagnostic view we want during rollout — if a page shows "no verdict" the user can look here and see whether `evaluate` 401'd, `details` 404'd, etc.

### ASCII mockup

```
┌─────────────────────────────────────────────────────────┐
│ ● Sonatype Firewall                       REAL · IQ v2  │  ← header
├─────────────────────────────────────────────────────────┤
│ ┌──────────┐┌─────────────┐┌──────────────┐             │  ← tabs
│ │ Verdict  ││ History (12)││ Scan detail  │             │
│ └──────────┘└─────────────┘└──────────────┘             │
├─────────────────────────────────────────────────────────┤
│  NPM                                                    │
│  lodash                                                 │
│  version 4.17.10                                        │
│                                                         │
│  ┌─────────┐                                            │
│  │ BLOCKED │   Policy: Security-Critical · stage Build  │
│  └─────────┘   • Critical CVE present (CVE-2019-10744)  │
│                                                         │
│  INTEGRITY RATING                                       │
│  [Normal]                                               │
│                                                         │
│  CVES                                                   │
│  [9.1] CVE-2019-10744   Prototype pollution             │
│         Reachable in scanned apps                       │
│                                                         │
│  GOLDEN VERSION                                         │
│  ┌───────────────────────────────────┐                  │
│  │  4.17.21                          │                  │
│  │  Fixes 1 CVE(s) · non-breaking    │                  │
│  └───────────────────────────────────┘                  │
│                                                         │
│  [ Request waiver ]                       Settings      │
└─────────────────────────────────────────────────────────┘

              History tab
┌─────────────────────────────────────────────────────────┐
│ npm   lodash@4.17.10                [BLOCKED]   2m ago  │
│ npm   event-stream@3.3.6            [BLOCKED]   4m ago  │
│ pypi  ctx@0.1.2                     [BLOCKED]   5m ago  │
│ maven log4j-core@2.14.1             [BLOCKED]   7m ago  │
│ npm   lodash@4.17.21                [ALLOWED]  10m ago  │
│ npm   express@4.17.1                [ WARN  ]  12m ago  │
│  … (scroll)                                             │
│                                              Clear all  │
└─────────────────────────────────────────────────────────┘

              Scan detail tab
┌─────────────────────────────────────────────────────────┐
│ Package: lodash@4.17.10   pkg:npm/lodash@4.17.10        │
│ VRM: acme-vrm  →  repo: npm-proxy (id=r-42)             │
│                                                         │
│  ● POST /api/v2/components/details        200 · 142ms ▸ │
│  ● POST /api/v2/firewall/components/…/evaluate          │
│                                             200 · 218ms ▸│
│  ● POST /api/v2/components/remediation/…  200 ·  87ms ▸ │
│                                                         │
│ (expanded) evaluate →                                   │
│ {                                                       │
│   "results": [{                                         │
│     "packageUrl": "pkg:npm/lodash@4.17.10",             │
│     "policyEvaluationResult": { "policyViolations":     │
│       [{ "policyViolationId":"pv-9f21", … }] }          │
│   }]                                                    │
│ }                                                       │
└─────────────────────────────────────────────────────────┘
```

---

## Files to modify

- `src/manifest.json` — add `"permissions"` entry, drop hardcoded `localhost:8765` from `host_permissions` (moves to runtime request).
- `src/types.ts` — extend `ExtensionSettings` (`mode`, `virtualRepositoryManagerName`); add `VrmContext`, `ScanRecord`, `ScanCallLog`, `ApiCallEntry`; new `RuntimeMessage`s (`GET_HISTORY`, `CLEAR_HISTORY`, `GET_SCAN_DETAIL`, `RESOLVE_VRM`, `TEST_CONNECTION`); stash `policyViolationId` on `FirewallVerdict`.
- `src/lib/iq-client.ts` — **rewrite**: real endpoint composition, VRM resolution, per-request timing, records call log into a caller-supplied sink so background can persist it. Keep `MockClient` and `RealClient` behind one interface; select by `settings.mode`.
- `src/lib/settings.ts` — add new fields, migrate old settings that lack them.
- `src/lib/cache.ts` — add `history` and `scanDetail` stores keyed by tab id, backed by `chrome.storage.local` with size cap.
- `src/background/index.ts` — on `GET_VERDICT` write to history + scan detail; new handlers for `GET_HISTORY`, `GET_SCAN_DETAIL`, `RESOLVE_VRM`, `TEST_CONNECTION`; call `chrome.permissions.request` when settings change to a new origin.
- `src/popup/Popup.tsx` — split into a tab shell + three tab components: `tabs/VerdictTab.tsx` (moved from current), `tabs/HistoryTab.tsx` (new), `tabs/ScanDetailTab.tsx` (new). Small local state for active tab; last-viewed entry is source of truth for Verdict + Scan detail.
- `src/options/Options.tsx` — add VRM name field, mode dropdown, "Test connection" button that surfaces resolved repo ids per ecosystem.
- `mock-iq/data.js` — untouched; mock remains for offline demo.

---

## Waiver flow (per user's request)

1. On `fetchVerdict`, real client stashes `policyViolationId` (from the first blocking violation in the `evaluate` response) onto the `FirewallVerdict`.
2. Popup "Request waiver" now sends `{type: 'REQUEST_WAIVER_REAL', policyViolationId, repoId, reason}`.
3. Background calls `POST /api/v2/policyWaiverRequests/{policyViolationId}/repository/{repoId}` with the reason. In mock mode, keeps existing PURL-based path.
4. Because we're on a VRM-scoped repo the policy violation record already exists in the DB from the evaluate call — no extra bootstrap needed.

If no violation returned (e.g. verdict is `allow`), waiver button is hidden.

---

## Implementation plan (phased, step-by-step)

Each phase is independently testable — you can pause after any of them. Total ~7 phases.

### Phase 0 — Bootstrap doc + branch
1. Create branch `feat/insight-brain-integration` off `main`.
2. Copy this plan to `docs/IMPLEMENTATION.md` in the extension repo so the doc travels with the code.
3. Add a `.env.example` (not consumed at runtime — reference for reviewers) listing `IQ_URL=http://localhost:8070`, `IQ_USER_CODE=…`, `IQ_PASS_CODE=…`, `IQ_VRM_NAME=browser-ext-vrm`.

### Phase 1 — Types + settings scaffolding (no behavior change yet)
1. `src/types.ts`
   - Extend `ExtensionSettings` with `mode: 'mock' | 'real'` (default `'mock'`) and `virtualRepositoryManagerName: string`.
   - **Delete** `nexusProxyUrl` and `rewriteInstallCommands` from `ExtensionSettings` and `DEFAULT_SETTINGS` (dropped from scope — see Context).
   - Add types: `VrmContext { managerId: string; repoIds: Partial<Record<Ecosystem, string>>; resolvedAt: number }`.
   - Add types: `ScanRecord { purl, ecosystem, name, version, verdict, fetchedAt, tabId }` and `ScanCallLog { purl, entries: ApiCallEntry[] }` and `ApiCallEntry { method, path, status, latencyMs, error?, responsePreview? }`.
   - Extend `RuntimeMessage` union: `GET_HISTORY`, `CLEAR_HISTORY`, `GET_SCAN_DETAIL`, `RESOLVE_VRM`, `TEST_CONNECTION`.
   - Add optional `policyViolationId?: string` on `FirewallVerdict`.
2. `src/lib/settings.ts` — extend `DEFAULT_SETTINGS`, add a lightweight migration so existing users don't crash on missing keys (also drops the two removed fields when reading legacy stored state).
3. `src/content/shared.ts` — **delete** `rewriteInstallSnippets(...)`. Remove its calls from `src/content/npm.ts` and `src/content/pypi.ts`.
4. `src/options/Options.tsx` — add UI controls for the new settings and a placeholder "Test connection" button (wired in Phase 3). **Remove** the Nexus proxy section entirely.

**Verify:** `npm run build` succeeds; loading dist/ shows new fields in the Options page; existing mock verdicts still render.

### Phase 2 — Real IQ client (behind `mode: 'real'`)
1. Refactor `src/lib/iq-client.ts` into an interface + two implementations:
   - `interface IqClient { ping(): Promise<void>; resolveVrm(name): Promise<VrmContext>; fetchVerdict(purl, ctx): Promise<FirewallVerdict>; requestWaiver(...): Promise<string> }`
   - `MockClient` = current logic, unchanged.
   - `RealClient` = new; HTTP Basic auth from `settings.userCode:settings.passCode`.
2. Implement `RealClient.ping()` → `GET /ping`.
3. Implement `RealClient.resolveVrm(name)`:
   - `GET /api/v2/config/repositoryManagers` (or equivalent list endpoint — confirm exact path against `ApiFirewallResource` at build time).
   - Filter by `name` and `managerType==='VIRTUAL'` → get `managerId`.
   - `GET /api/v2/config/repositories?repositoryManagerId=…` → group by `format` → build `repoIds`.
4. Implement `RealClient.fetchVerdict(purl, ctx)`:
   - `Promise.all` on `POST /api/v2/components/details`, `POST /api/v2/firewall/components/{managerId}/{repoId}/evaluate`, `POST /api/v2/components/remediation/repository/{repoId}`.
   - Compose into `FirewallVerdict`. Map `evaluate.results[0].policyEvaluationResult` → `PolicyEvalResult`. Map `remediation.next-no-violations` → `goldenVersion`. Extract `policyViolationId` from first blocking violation.
5. Add a request-instrumentation helper `withCallLog(fetchFn, sink)` that records `{method,path,status,latencyMs}` per request into a per-PURL sink. Sink is passed in from background so background owns the store.
6. Select client at `fetchVerdict` boundary based on `settings.mode`.

**Verify:** unit-style manual test — flip mode to `real`, hit lodash page. Even if VRM resolution fails, the popup should degrade gracefully (show "Not configured" state, not crash).

### Phase 3 — Wire settings + Test connection + host_permissions
1. `src/manifest.json` — add `"permissions"` to `permissions` array (needed for `chrome.permissions.request`). Remove hardcoded `http://localhost:8765/*` from `host_permissions` and let it be requested at runtime.
2. `src/background/index.ts` — handler `TEST_CONNECTION`:
   - Call `chrome.permissions.request({origins: [`${settings.iqServerUrl}/*`]})` if not already granted.
   - Call `RealClient.ping()` → `RealClient.resolveVrm(name)` → return `{ok, vrmContext}` or `{ok:false, error}`.
   - On success, persist `vrmContext` to `chrome.storage.local`.
3. `src/options/Options.tsx` — Test button now sends `TEST_CONNECTION`, renders resolved repo ids per ecosystem, or the error.

**Verify:** with insight-brain running locally and a VRM created, Test connection returns OK and lists `npm=…, pypi=…, maven=…`.

### Phase 4 — Background: history + scan-detail stores
1. `src/lib/cache.ts` — add:
   - `historyStore`: bounded LRU (cap 20), persisted to `chrome.storage.local` under `firewall_history`.
   - `scanDetailStore`: `Map<purl, ScanCallLog>` in memory; last 20 also persisted.
2. `src/background/index.ts` — on every successful `GET_VERDICT`, push a `ScanRecord` into history and record the call log into scan-detail.
3. Handlers: `GET_HISTORY` → returns array; `CLEAR_HISTORY` → clears; `GET_SCAN_DETAIL(purl)` → returns ScanCallLog.

**Verify:** open background service worker inspector, run `chrome.runtime.sendMessage({type:'GET_HISTORY'})` — see recent scans.

### Phase 5 — Popup: three-tab layout
1. Restructure `src/popup/Popup.tsx` into a shell:
   - Reuses existing header.
   - Adds `<TabBar activeTab, onChange />` under it.
   - Renders `<VerdictTab />`, `<HistoryTab />`, or `<ScanDetailTab />` based on state.
   - Popup width 380px → 420px in `popup.css`.
2. `src/popup/tabs/VerdictTab.tsx` — move current Popup content here verbatim; accept the current `FirewallVerdict` as a prop.
3. `src/popup/tabs/HistoryTab.tsx` — new. Fetch `GET_HISTORY` on mount. Render rows (ecosystem chip, `name@version`, verdict pill, "Xm ago"). Click a row → callback into parent that sets the current verdict + switches to Verdict tab.
4. `src/popup/tabs/ScanDetailTab.tsx` — new. Fetch `GET_SCAN_DETAIL(currentPurl)` on mount. Render one row per API call with status pill + latency; row expands to show a JSON preview (limit to 4 KB).
5. Header shows current mode as a chip: `MOCK MODE` (yellow) / `REAL · IQ v2` (blue) / `NOT CONFIGURED` (grey).

**Verify:** manually navigate all three tabs with real IQ; verify per-tab isolation still holds (the earlier fix).

### Phase 6 — Waivers on real API
1. `RealClient.requestWaiver({policyViolationId, repoId, reason})` → `POST /api/v2/policyWaiverRequests/{policyViolationId}/repository/{repoId}` with `{comment: reason}`.
2. `VerdictTab` — hide "Request waiver" if `verdict.policy.verdict === 'allow'` OR `!verdict.policyViolationId`.
3. On click, background dispatches to `RealClient` (real mode) or `MockClient` (mock mode).

**Verify:** click Request waiver on BLOCKED package. IQ UI → Waiver Requests page shows the new request under the VRM child repo, with the reason string.

### Phase 7 — Cleanup, docs, screenshots
1. Update `README.md` — replace "Mock IQ Server" section with "Real IQ Server integration" instructions (VRM setup, tokens, mode toggle). Keep mock section as "Offline demo".
2. Add screenshots of all three tabs to `docs/`.
3. Sanity: `npm run build` clean; TypeScript strict passes; unused mock plumbing (`http://localhost:8765` references) removed from `manifest.json` but `mock-iq/` server left as-is.
4. Open PR with the plan file as the description.

---

---

# Part 2 — Insight-brain-side changes

The extension is one half of the story. IQ Server needs to (a) tell extension-originated activity apart from repo-manager / CI activity, (b) expose a "Browser Extension" list page so admins can review what developers scanned, and (c) surface a **Source** column on the existing Waiver Requests page plus a dedicated tab for extension-originated waiver requests. All changes go in `../insight-brain`.

## The `source` column — one enum, three tables

Add a nullable `source varchar(32)` column to two tables, backed by a new Java enum:

- `policy_waiver_request` — so extension-originated waivers can be filtered and surfaced on their own tab.
- `proxy_repository_component` — so extension-originated evaluations can be listed on the new Browser Extension page.

Enum values (`ScanSource` — new class alongside `ManagerType` in `insight-brain-data/src/main/java/com/sonatype/insight/brain/model/`):
`FIREWALL_PROXY` (default — anything from the repository manager path), `BROWSER_EXTENSION`, `IDE`, `CI`. Legacy rows stay NULL and are treated as `FIREWALL_PROXY` at read time.

Detection at the API boundary is by HTTP header — the extension sends `X-Scan-Source: BROWSER_EXTENSION` on every call. If the header is missing or unrecognized, the value defaults to `FIREWALL_PROXY`.

## Persistence path (evaluations)

Where the evaluate call currently persists:
`ApiFirewallResource.evaluateComponents` → `ApiFirewallService.evaluateComponents` (`insight-brain-service/…/ApiFirewallService.java:636-666`) → `RepositoryService.evaluateComponents` → `AbstractRepositoryService.evaluateComponents` (`AbstractRepositoryService.java:685-727`) → `RepositoryPolicyEvaluator.persistRepositoryComponent` (`RepositoryPolicyEvaluator.java:711-785`, insert on 761).

We thread the `ScanSource` from the resource down to `persistRepositoryComponent`:
1. Resource reads `X-Scan-Source` header, parses to `ScanSource` (fallback `FIREWALL_PROXY`).
2. Pass as a param through `evaluateComponents` (existing signature already carries `clientUserAgent` — same pattern).
3. `persistRepositoryComponent` sets `component.setSource(scanSource)` before the DAO insert.

## Persistence path (waivers)

`ApiPolicyWaiverRequestResource.addPolicyWaiverRequest` → `ApiPolicyWaiverRequestService.createPolicyWaiverRequest` (`ApiPolicyWaiverRequestService.java:465-497`, entity insert on 495).

Same header-based detection: resource extracts `ScanSource`, service sets `policyWaiverRequest.setSource(scanSource)` before `policyWaiverRequestDAO.insert(...)`.

Requester is already captured via `currentUser.getUserPrincipal()` (`ApiPolicyWaiverRequestService.java:485-487`). For extension-originated evaluations we add `@Inject CurrentUser currentUser` to `ApiFirewallService` (currently absent) and pass the username through to `persistRepositoryComponent` so the Browser Extension page can show who scanned.

## New read endpoint (list extension scans)

`GET /api/v2/firewall/extension-scans` under `ApiFirewallResource`:
- Query params: `repositoryManagerId` (VRM id, required), `page` (default 0), `pageSize` (default 50, max 200), `since` (ISO instant, optional).
- Response: paged list of `{ purl, componentName, version, format, verdict, policyViolations[], requester, createdAt, repositoryId }`.
- Backing DAO method: `ProxyRepositoryComponentDAO.listBySourceAndManager(managerId, source, page, pageSize, since)` — `WHERE repository_manager_id = ? AND source = 'BROWSER_EXTENSION'`.
- Authz: same permission gate as the existing firewall list endpoints (`FirewallPermissionGate.java:36-71`).

Waiver-request read is already scoped: existing `GET /api/v2/policyWaiverRequests/{ownerType}/{ownerId}` returns all of them; we just add `source` to the DTO so the frontend can filter/tab client-side. If admin volumes get large, add an optional `?source=BROWSER_EXTENSION` filter server-side later.

## Frontend

Two work-streams inside `insight-brain-frontend`:

### A. Waiver Requests page — Source column + Extension tab
Files (all under `src/main/frontend/firewall/waiverRequests/`):
- `FirewallWaiversPage.jsx` — the page has existing top-tabs (approved / requested at `lines 26-77`). Add a third tab: "Extension requests". Client-side filter on the same fetch (`firewallWaiverRequestsSlice.loadWaiverRequests`) — no new backend call for MVP.
- `FirewallRequestedWaiversTable.jsx` + `FirewallRequestedWaiversTableRow.jsx` — add a `Source` column rendering the source enum as a badge (`FIREWALL_PROXY` grey, `BROWSER_EXTENSION` blue, `IDE` purple, `CI` teal).
- `firewallWaiverRequestsSlice.js` / `firewallWaiverRequestsSelectors.js` — expose `waiverRequestsBySource` memoized selector for the Extension tab.

### B. New "Browser Extension" page
New directory: `src/main/frontend/firewall/browserExtension/`.
- `BrowserExtensionPage.jsx` — top-level page. Route param: `:vrmId`. Sections: VRM header (name + id), scan-list table.
- `BrowserExtensionScansTable.jsx` — columns: When · Requester · Ecosystem · Component · Version · Verdict · Violations count. Empty state and pagination (mirror `RepositoryResultsSummaryPage` at `src/main/frontend/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/`).
- `browserExtensionScansSlice.js` — Redux Toolkit slice with a `load({vrmId, page})` thunk hitting the new `GET /api/v2/firewall/extension-scans`. URL builder appended to `util/CLMLocation.js`.
- **Routing**: register `firewall.browserExtension` state in `firewall/route.js` (mirror `firewall.waivers` at lines 210-218).
- **Sidebar**: add a nav link in `firewall/FirewallSidebar.jsx` — new state constant at lines 25-31 and a `NxGlobalSidebar2NavigationLink` entry alongside 57-121. Label "Browser Extension", icon TBD (reuse an existing one).
- **Feature-flag gating**: same flags the VRM UI is behind — `iq-firewall-enterprise-enabled × iq-firewall-enterprise-redirect-ui-enabled` — hide nav entry when either is off.

---

## Insight-brain implementation plan (phased)

### Phase IB-1 — DB migration + model
1. Create `insight-brain-db/src/main/resources/db/insight_brain_ods/schema_incremental_0479.sql` (dialect-agnostic) with:
   ```sql
   ALTER TABLE policy_waiver_request        ADD COLUMN IF NOT EXISTS source varchar(32);
   ALTER TABLE proxy_repository_component   ADD COLUMN IF NOT EXISTS source varchar(32);
   ```
2. Update baseline `schema.sql` — add `source varchar(32)` to `policy_waiver_request` (lines 146-179) and `proxy_repository_component` (lines 632-661).
3. New `insight-brain-data/…/model/ScanSource.java` enum (`FIREWALL_PROXY`, `BROWSER_EXTENSION`, `IDE`, `CI`).
4. Entities `PolicyWaiverRequest.java` + `ProxyRepositoryComponent.java` gain `private ScanSource source;` with `@Enumerated(EnumType.STRING)` and getters/setters.
5. Regenerate jOOQ (there'll be a Justfile / Maven target — grep for `jooq-codegen`).

**Verify:** `mvn -pl insight-brain-data test`; `MigrationScriptsTest`, `MigrationScriptConstraintsTest`, `DatabaseConstraintTest`, `DatabaseMigrationScriptImmutabilityTest`, `SaasCompatibleSchemaMigrationTest` all green.

### Phase IB-2 — Header parsing + persistence threading
1. Add `ScanSource fromHeader(String value)` helper (bumps unknown → `FIREWALL_PROXY`).
2. `ApiPolicyWaiverRequestResource` — read `X-Scan-Source`, pass into service. Service sets the value on the entity before insert.
3. `ApiFirewallResource.evaluateComponents` — read `X-Scan-Source`. Add `@Inject CurrentUser currentUser`. Thread the source + username through `ApiFirewallService.evaluateComponents` → `AbstractRepositoryService.evaluateComponents` (add param) → `RepositoryPolicyEvaluator.persistRepositoryComponent` (set on entity).
4. Unit tests for both persistence paths — new source is persisted; missing header defaults to `FIREWALL_PROXY`; legacy tests remain unchanged.

**Verify:** existing `ApiFirewallServiceTest`, `ApiPolicyWaiverRequestServiceTest` still pass; new source-column assertions pass.

### Phase IB-3 — New list endpoint
1. `ProxyRepositoryComponentDAO.listBySourceAndManager(...)` — jOOQ query with paging (`limit`/`offset`) + optional `since`.
2. `ApiFirewallResource` — new `@GET @Path("/extension-scans")` method → `ApiFirewallService.listExtensionScans(...)` → DAO. Returns paged DTO.
3. DTO `ApiExtensionScanDTO` in `insight-brain-service/…/api/v2/dto/` mapping the columns the frontend needs (see Frontend section).
4. Same permission gate as the existing firewall list endpoints.

**Verify:** new resource test hitting the endpoint against an H2-backed test service; response shape asserted.

### Phase IB-4 — Waiver Requests page (Source column + Extension tab)
1. Extend DTO `ApiPolicyWaiverRequestDTO` with `public String source;`.
2. Adapter method that fills `source` from the entity.
3. Frontend `FirewallRequestedWaiversTable` — add a "Source" `<td>` between existing columns (pick a stable position; probably right after "Reason"). Add sort + a filter dropdown wired to `waiverRequestsBySource` selector.
4. Frontend `FirewallWaiversPage` — third tab "Extension requests" (`source === 'BROWSER_EXTENSION'`).

**Verify:** Jest snapshot updates for `FirewallRequestedWaiversTable.jestspec.jsx`; page renders both existing tabs + new tab.

### Phase IB-6 — Cross-scope waiver (policy-driven scope options + cascade endpoint)

**Problem.** Developers browse packages on npmjs.com / pypi.org / central.sonatype.com via the extension (VRM-scoped view). But they actually **download** via a *traditional* repository manager proxy (Nexus RM `npm-proxy`, `pypi-proxy`, etc.) that their `npm install --registry=…` command hits. A waiver approved only on the VRM child repo does nothing for the real install. We need the extension's "Request waiver" flow to be able to apply the waiver at the correct **policy scope** — org / RM / repo — with the specific target(s) chosen by the requester.

**Policy-driven scope options.** A waiver can only be created at a scope ≤ the policy's own owner scope. So the extension asks IQ what's possible for the current violation, and renders those choices.

New endpoint: `GET /api/v2/policyWaiverRequests/scope-options/{policyViolationId}` — returns
```json
{
  "policy": { "policyId": "…", "policyName": "Security-Critical", "ownerType": "organization", "ownerId": "acme" },
  "availableScopes": [
    { "kind": "ORGANIZATION",       "ownerType": "organization",       "ownerId": "acme",       "ownerName": "Acme Corp" },
    { "kind": "REPOSITORY_MANAGER", "ownerType": "repository_manager", "ownerId": "vrm-42",     "ownerName": "browser-ext-vrm"      },
    { "kind": "REPOSITORY_MANAGER", "ownerType": "repository_manager", "ownerId": "trad-nex-1", "ownerName": "nexus-prod"           },
    { "kind": "REPOSITORY",         "ownerType": "repository",         "ownerId": "r-401",      "ownerName": "vrm-npm-proxy",  "parentManagerId": "vrm-42" },
    { "kind": "REPOSITORY",         "ownerType": "repository",         "ownerId": "r-812",      "ownerName": "npm-central",    "parentManagerId": "trad-nex-1" }
  ]
}
```

Rules:
- If `policy.ownerType == 'repository'` → `availableScopes` returns only that one repo. Waiver must be at repo scope.
- If `policy.ownerType == 'repository_manager'` → returns that RM plus every repo under it. Waiver can be at either.
- If `policy.ownerType == 'organization'` → returns the org, every RM under it (both VRM and traditional), and every repo under those. User picks the level they want.

New endpoint: `POST /api/v2/policyWaiverRequests/cascade` — body:
```json
{
  "policyViolationId": "pv-9f21",
  "scopes": [
    { "ownerType": "repository", "ownerId": "r-401" },
    { "ownerType": "repository", "ownerId": "r-812" }
  ],
  "reason": "requested via browser extension …",
  "expiresAt": null
}
```
Server behavior: inserts one waiver-request row **per scope** inside a single transaction; every row shares the same generated `waiver_group_id varchar(36)`. Response returns the group id and per-row status.

**Schema addition** (part of `schema_incremental_0479.sql` from Phase IB-1):
```sql
ALTER TABLE policy_waiver_request ADD COLUMN IF NOT EXISTS waiver_group_id varchar(36);
CREATE INDEX IF NOT EXISTS policy_waiver_request_group_idx ON policy_waiver_request (waiver_group_id);
```

**Authorization.** Reuse the existing `ApiPolicyWaiverRequestService` authz check per-scope — a cascade call to a scope the requester lacks permission on returns a per-row `403` in the response without failing the whole cascade. Requester sees exactly what got created.

**Files touched (net add on top of IB-1..IB-5):**
- `insight-brain-service/…/api/v2/ApiPolicyWaiverRequestResource.java` — two new methods.
- `insight-brain-service/…/api/v2/service/ApiPolicyWaiverRequestService.java` — `getScopeOptions(policyViolationId)` and `createCascade(...)` methods.
- `insight-brain-service/…/api/v2/dto/ApiPolicyWaiverScopeOptionsDTO.java`, `…/ApiPolicyWaiverCascadeRequestDTO.java`, `…/ApiPolicyWaiverCascadeResultDTO.java` — new DTOs.
- `insight-brain-data/…/dataaccess/policy/PolicyWaiverRequestDAO.java` — accept `waiverGroupId` on insert; add `listByGroup(id)` helper for the frontend row-grouping view.
- `insight-brain-db/…/schema.sql` and `schema_incremental_0479.sql` — new nullable column + index (bundled with the source column from IB-1 into the same migration file).

**Extension side** (folded into Phase 6 — Waivers on real API):
1. Verdict tab: after real-mode fetch, if `verdict.policyViolationId` present, background pre-fetches `GET /scope-options/{id}` in parallel and stashes result on the verdict record.
2. Click "Request waiver" opens an inline scope picker:
   - Default option: highest-level scope in `availableScopes` up to the policy's own owner (usually `REPOSITORY_MANAGER` if policy is org-level).
   - Second row: multi-select of specific repos under that RM (checked by default; unchecking narrows).
   - "Advanced": drop up to organization scope (only visible if `availableScopes` contains an ORGANIZATION entry).
3. Submit → `POST /policyWaiverRequests/cascade` with the chosen scope(s). Popup shows a success line per row: `✓ Waiver submitted on vrm-npm-proxy` / `✓ Waiver submitted on npm-central`.

**Verify:** Create a policy at org scope in IQ. Trigger a violation on lodash. Click Request Waiver in the extension → dropdown offers ORG, both RMs, and both repos. Submit with both repos ticked → IQ shows two rows on Waiver Requests page, both grouped by the same `waiver_group_id` (rendered as one grouped card — see IB-7).

### Phase IB-7 — Grouped waiver rendering + linked-scope tab

Frontend view for cascade waivers.

1. `FirewallRequestedWaiversTable.jsx` — when rows share a `waiver_group_id`, render them as one grouped row: the parent row shows the component + policy + `Applies to N scopes` badge; expanding shows the child rows with their individual scope + status.
2. `firewallWaiverRequestsSelectors.js` — memoized selector `groupedWaiverRequests` that partitions the flat list into groups.
3. On the Extension tab, group indicator shows the two scope pills side-by-side: `VRM · vrm-npm-proxy  +  Nexus · npm-central`.
4. New sort: "Group size" so admins can find broadly-applied waivers first.

**Verify:** File a cascade waiver on two scopes → Waiver Requests page shows one grouped row; expanding shows both scopes. Approving the group approves all rows atomically (single request loop client-side; server-side approval is per-row unchanged).

### Phase IB-5 — New Browser Extension page
1. Directory + files listed under **Frontend / B** above.
2. Register `firewall.browserExtension` state in `firewall/route.js`.
3. Sidebar nav entry in `firewall/FirewallSidebar.jsx`, gated by the two feature flags.
4. Jest specs mirroring `firewallWaiverRequestsSlice.jestspec.js` and one component test rendering a page with 3 fake scans.

**Verify:** `yarn jest --testPathPattern=browserExtension` green. In local dev with an extension scan under a VRM, page shows the row.

---

# Part 3 — Cross-repo integration (end-to-end)

### Phase 12 — Extension sends `X-Scan-Source`
1. In `src/lib/iq-client.ts` (RealClient), add a shared `headers` composer that includes `X-Scan-Source: BROWSER_EXTENSION` on every request (evaluate, details, remediation, waiver, ping-optional).
2. No fallback needed — IQ ignores unknown headers, so this is safe to ship even before the IB-side changes land.

### End-to-end verification (all repos)
1. `cd ../insight-brain && ./local-dev-build.sh && ./local-dev-run.sh`.
2. Enable both feature flags. Create VRM `browser-ext-vrm` + npm/pypi/maven child repos.
3. Build extension `npm run build`, load `dist/`, configure Settings pointing at `http://localhost:8070`.
4. Visit npm/pypi/maven package pages. On IQ side:
   - Firewall → **Browser Extension** page shows the scan rows with correct requester and verdict.
   - Firewall → **Waivers** page shows the "Source" column populated. The Extension tab shows only extension-submitted waivers.
5. Same evaluation flow through the repo manager (non-extension path) should still show up with source `FIREWALL_PROXY` (regression check).

---

## Files changed — insight-brain-side summary

DB:
- `insight-brain-db/src/main/resources/db/insight_brain_ods/schema_incremental_0479.sql` (new)
- `insight-brain-db/src/main/resources/db/insight_brain_ods/schema.sql` (edit)

Backend:
- `insight-brain-data/…/model/ScanSource.java` (new)
- `insight-brain-data/…/model/policy/PolicyWaiverRequest.java`
- `insight-brain-data/…/model/repository/ProxyRepositoryComponent.java`
- `insight-brain-data/…/dataaccess/policy/PolicyWaiverRequestDAO.java`
- `insight-brain-data/…/dataaccess/repository/ProxyRepositoryComponentDAO.java`
- `insight-brain-service/…/api/v2/ApiFirewallResource.java`
- `insight-brain-service/…/api/v2/ApiFirewallService.java`
- `insight-brain-service/…/api/v2/ApiPolicyWaiverRequestResource.java`
- `insight-brain-service/…/api/v2/service/ApiPolicyWaiverRequestService.java`
- `insight-brain-service/…/integration/repository/AbstractRepositoryService.java`
- `insight-brain-service/…/repository/RepositoryPolicyEvaluator.java`
- `insight-brain-service/…/api/v2/dto/ApiPolicyWaiverRequestDTO.java`
- `insight-brain-service/…/api/v2/dto/ApiExtensionScanDTO.java` (new)

Frontend:
- `insight-brain-frontend/src/main/frontend/firewall/waiverRequests/FirewallWaiversPage.jsx`
- `insight-brain-frontend/src/main/frontend/firewall/waiverRequests/FirewallRequestedWaiversTable.jsx`
- `insight-brain-frontend/src/main/frontend/firewall/waiverRequests/FirewallRequestedWaiversTableRow.jsx`
- `insight-brain-frontend/src/main/frontend/firewall/waiverRequests/firewallWaiverRequestsSlice.js`
- `insight-brain-frontend/src/main/frontend/firewall/waiverRequests/firewallWaiverRequestsSelectors.js`
- `insight-brain-frontend/src/main/frontend/firewall/browserExtension/BrowserExtensionPage.jsx` (new)
- `insight-brain-frontend/src/main/frontend/firewall/browserExtension/BrowserExtensionScansTable.jsx` (new)
- `insight-brain-frontend/src/main/frontend/firewall/browserExtension/browserExtensionScansSlice.js` (new)
- `insight-brain-frontend/src/main/frontend/firewall/route.js`
- `insight-brain-frontend/src/main/frontend/firewall/FirewallSidebar.jsx`
- `insight-brain-frontend/src/main/frontend/util/CLMLocation.js`

---

## Risks + open questions

- **Exact list-repositories endpoint** for the VRM's children — confirm at build time; may be `/api/v2/config/repositories` (public) or an internal endpoint. If public-API doesn't expose it, we may need `/rest/repositories` (Dropwizard, session auth) as a fallback, in which case we need to also handle session cookies.
- **Feature flags** — VRM UI is gated behind `iq-firewall-enterprise-enabled × iq-firewall-enterprise-redirect-ui-enabled`. Users testing this MUST enable both. Document in README.
- **`chrome.permissions.request` requires user gesture** — the Options page "Test connection" button click qualifies, but not a background-triggered call. So we require the user to explicitly hit Test after entering the URL.
- **Rate limits / IQ auth lockouts** — repeated 401s from bad credentials shouldn't spam the server. Add a simple client-side throttle (skip real calls for 60s after a 401 until settings change).

## Verification

1. **Insight-brain up locally.** In `../insight-brain`: `./local-dev-build.sh && ./local-dev-run.sh`. Front on `:8070`, service on `:8072`. Log in with a user with tenant-admin.
2. **Create a VRM + child repos.** In IQ UI: enable flags `iq-firewall-enterprise-enabled` + `iq-firewall-enterprise-redirect-ui-enabled`. Create a VRM named e.g. `browser-ext-vrm`, add proxy repos for npm / pypi / maven with real upstreams.
3. **Configure the extension.** Load `dist/`. In Settings: mode=real, IQ URL=`http://localhost:8070`, VRM name=`browser-ext-vrm`, userCode/passCode from the IQ user token page. Approve the `host_permissions` prompt.
4. **Test connection.** Should return "OK · resolved repos: npm=…, pypi=…, maven=…".
5. **Golden path.** Visit `https://www.npmjs.com/package/lodash/v/4.17.10` — inline red banner, popup Verdict tab shows BLOCKED with real policy name / violation IDs.
6. **History tab.** Visit 3–4 more packages (npm + pypi + maven) — History shows them newest-first with correct verdicts. Clicking a row switches to Verdict tab for that entry.
7. **Scan detail tab.** For lodash: three rows (details, evaluate, remediation), all 200; expanded evaluate JSON shows `policyViolationId`.
8. **Waiver.** Click Request waiver on a BLOCKED package. Response is 200/204. Verify in IQ UI (Waiver Requests page) that a request was recorded against the VRM child repo with the reason string.
9. **Auth failure.** Change passCode to garbage → Test connection reports 401; Scan detail on next package shows a 401 row with error message. No console spam.
10. **Mock parity.** Flip mode=mock → popup still renders the same shape (regression check that composition + UI are decoupled from the source).
11. **Regression from earlier fix.** Open two malicious-package tabs — each popup shows its own package (per-tab verdict, per-tab badge — the fix from earlier in this session should still hold).
