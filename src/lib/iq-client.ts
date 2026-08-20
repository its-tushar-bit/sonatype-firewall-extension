import {
  ApiRepositoryManager,
  ApiVrm,
  ApiVrmRepo,
  ApiWaiverReason,
  ComponentInfo,
  CVE,
  ExtensionSettings,
  FirewallVerdict,
  IntegrityRating,
  PolicyVerdict,
  WaiverRequestOptions,
  WaiverScope,
} from "../types";

function authHeader(s: ExtensionSettings): string {
  return `Basic ${btoa(`${s.userCode}:${s.passCode}`)}`;
}

function baseUrl(s: ExtensionSettings): string {
  return s.iqServerUrl.replace(/\/$/, "");
}

function jsonHeaders(s: ExtensionSettings): Record<string, string> {
  return {
    "Content-Type": "application/json",
    Accept: "application/json",
    Authorization: authHeader(s),
  };
}

// IQ Server enforces CSRF on POST requests: it expects the CLM-CSRF-TOKEN
// cookie value echoed as the X-CSRF-TOKEN header. The cookie is set by any
// prior GET response — we ensure it exists (issuing a cheap GET if not),
// then read it via chrome.cookies. All authenticated requests must also
// send `credentials: "include"` so the cookie actually rides along.
async function getCsrfCookie(baseUrlStr: string): Promise<string | null> {
  try {
    const c = await chrome.cookies.get({
      url: baseUrlStr,
      name: "CLM-CSRF-TOKEN",
    });
    return c?.value || null;
  } catch {
    return null;
  }
}

async function ensureCsrfToken(settings: ExtensionSettings): Promise<string> {
  const base = baseUrl(settings);
  let token = await getCsrfCookie(base);
  if (!token) {
    // Trigger a GET so IQ sets the cookie, then re-read.
    await fetch(`${base}/api/v2/firewall/repositoryManagers`, {
      credentials: "include",
      headers: { Authorization: authHeader(settings), Accept: "application/json" },
    });
    token = await getCsrfCookie(base);
  }
  if (!token) {
    throw new Error(
      "Could not obtain CSRF token from IQ Server. Ensure the extension has cookies permission and the IQ host is in host_permissions.",
    );
  }
  return token;
}

async function jsonHeadersWithCsrf(
  s: ExtensionSettings,
): Promise<Record<string, string>> {
  const token = await ensureCsrfToken(s);
  return { ...jsonHeaders(s), "X-CSRF-TOKEN": token };
}

/**
 * Reachability check. /api/v2/firewall/repositoryManagers is a cheap read that
 * works through the frontend dev proxy (which only forwards /rest, /api, /ui,
 * /policy-assets, /saml — /ping 404s in dev). 401 tells us the URL is right
 * but the token is wrong.
 */
export async function ping(settings: ExtensionSettings): Promise<void> {
  let res: Response;
  try {
    res = await fetch(`${baseUrl(settings)}/api/v2/firewall/repositoryManagers`, {
      credentials: "include",
      headers: { Authorization: authHeader(settings), Accept: "application/json" },
    });
  } catch (e: any) {
    throw new Error(`Network error reaching ${baseUrl(settings)} — ${e.message}`);
  }
  if (res.status === 401) throw new Error("Unauthorized — check user code / pass code");
  if (res.ok) return;
  throw new Error(`Unexpected HTTP ${res.status} from IQ Server`);
}

interface ApiVirtualRepositoryManagerListDTO {
  virtualRepositoryManagers?: ApiVrm[];
}
interface ApiRepositoryListDTO {
  repositories?: ApiVrmRepo[];
}
interface ApiRepositoryManagerListDTO {
  repositoryManagers?: ApiRepositoryManager[];
}

// List every repository manager configured on the IQ instance. Waiver-scope
// pickers filter this to exclude VIRTUAL managers per product spec (a VRM is
// not a valid waiver-request owner; it just aggregates other managers).
export async function listRepositoryManagers(
  settings: ExtensionSettings,
): Promise<ApiRepositoryManager[]> {
  const res = await fetch(`${baseUrl(settings)}/api/v2/firewall/repositoryManagers`, {
    credentials: "include",
    headers: { Authorization: authHeader(settings), Accept: "application/json" },
  });
  if (res.status === 401) throw new Error("Unauthorized — check user code / pass code");
  if (!res.ok) throw new Error(`Repository managers list failed: HTTP ${res.status}`);
  const dto = (await res.json()) as ApiRepositoryManagerListDTO;
  return dto.repositoryManagers ?? [];
}

// Same endpoint as listReposForVrm — /repositories/configuration/{id} accepts
// any repository manager id, VRM or not. Kept as a distinct helper so callers
// document intent (scope-picker fetch vs verdict-time repo enumeration).
export async function listReposForRepositoryManager(
  settings: ExtensionSettings,
  repositoryManagerId: string,
): Promise<ApiVrmRepo[]> {
  return listReposForVrm(settings, repositoryManagerId);
}

// Preset waiver reasons the org configured (via IQ UI). Optional dropdown
// on the request form — some orgs require picking one so approvers can group
// requests. See ApiPolicyWaiverReasonResource.java.
export async function listWaiverReasons(
  settings: ExtensionSettings,
): Promise<ApiWaiverReason[]> {
  const url = `${baseUrl(settings)}/api/v2/policyWaiverReasons`;
  const res = await fetch(url, {
    credentials: "include",
    headers: { Authorization: authHeader(settings), Accept: "application/json" },
  });
  console.log("[hexawatch] listWaiverReasons", url, "→ HTTP", res.status);
  if (res.status === 401) throw new Error("Unauthorized — check user code / pass code");
  if (!res.ok) {
    throw new Error(`Waiver reasons list failed: HTTP ${res.status}${await readIqError(res)}`);
  }
  const text = await res.text();
  console.log("[hexawatch] listWaiverReasons body:", text);
  try {
    const parsed = JSON.parse(text);
    // Some IQ builds wrap the list; accept either an array or {waiverReasons/policyWaiverReasons: [...]}.
    if (Array.isArray(parsed)) return parsed as ApiWaiverReason[];
    if (Array.isArray(parsed?.waiverReasons)) return parsed.waiverReasons as ApiWaiverReason[];
    if (Array.isArray(parsed?.policyWaiverReasons)) {
      return parsed.policyWaiverReasons as ApiWaiverReason[];
    }
    console.warn("[hexawatch] waiver reasons response was not an array:", parsed);
    return [];
  } catch (e) {
    console.error("[hexawatch] failed to parse waiver reasons body:", e);
    return [];
  }
}

export async function listVrms(settings: ExtensionSettings): Promise<ApiVrm[]> {
  const res = await fetch(`${baseUrl(settings)}/api/v2/firewall/virtualManagers`, {
    headers: { Authorization: authHeader(settings), Accept: "application/json" },
  });
  if (res.status === 401) throw new Error("Unauthorized — check user code / pass code");
  if (res.status === 404) {
    throw new Error(
      "VRM endpoint returned 404 — the iq-firewall-enterprise-enabled and iq-firewall-enterprise-redirect-ui-enabled feature flags must both be on.",
    );
  }
  if (!res.ok) throw new Error(`Virtual managers list failed: HTTP ${res.status}`);
  const dto = (await res.json()) as ApiVirtualRepositoryManagerListDTO;
  return dto.virtualRepositoryManagers ?? [];
}

export async function listReposForVrm(
  settings: ExtensionSettings,
  vrmId: string,
): Promise<ApiVrmRepo[]> {
  const res = await fetch(
    `${baseUrl(settings)}/api/v2/firewall/repositories/configuration/${encodeURIComponent(vrmId)}`,
    {
      credentials: "include",
      headers: { Authorization: authHeader(settings), Accept: "application/json" },
    },
  );
  if (res.status === 401) throw new Error("Unauthorized — check user code / pass code");
  if (!res.ok) throw new Error(`Repository list failed: HTTP ${res.status}`);
  const dto = (await res.json()) as ApiRepositoryListDTO;
  const repos = dto.repositories ?? [];
  // The list endpoint doesn't always include upstreamUrl. /rest/repositories/{id}
  // does, so enrich each entry with a per-repo detail fetch. Best-effort: if a
  // detail fetch fails we still return the base entry without remoteUrl.
  const enriched = await Promise.all(
    repos.map(async (r: any) => {
      const rep = r.repository || r;
      const id = rep.id || rep.repositoryId;
      let remoteUrl: string | undefined = r.upstreamUrl || rep.remoteUrl || rep.url;
      if (!remoteUrl && id) {
        try {
          const d = await fetchRepositoryDetail(settings, id);
          remoteUrl = d?.upstreamUrl || undefined;
        } catch {
          // best effort
        }
      }
      return {
        repositoryId: id,
        publicId: rep.publicId,
        format: rep.format,
        remoteUrl,
      } as ApiVrmRepo;
    }),
  );
  return enriched;
}

interface ApiRepositoryDetail {
  upstreamUrl?: string;
  repository?: { id?: string; publicId?: string; format?: string };
}

async function fetchRepositoryDetail(
  settings: ExtensionSettings,
  repoId: string,
): Promise<ApiRepositoryDetail | null> {
  const res = await fetch(
    `${baseUrl(settings)}/rest/repositories/${encodeURIComponent(repoId)}`,
    {
      credentials: "include",
      headers: { Authorization: authHeader(settings), Accept: "application/json" },
    },
  );
  if (!res.ok) return null;
  return (await res.json()) as ApiRepositoryDetail;
}

// -----------------------------------------------------------------------------
// Verdict — real IQ composition
// -----------------------------------------------------------------------------

interface RawDetailsResponse {
  componentDetails?: RawComponentDetail[];
}
interface RawComponentDetail {
  component: RawComponent;
  matchState?: string;
  catalogDate?: string;
  integrityRating?: string;
  hygieneRating?: string;
  licenseData?: {
    declaredLicenses?: Array<{ licenseId?: string; licenseName?: string }>;
    observedLicenses?: Array<{ licenseId?: string; licenseName?: string }>;
    effectiveLicenseThreats?: Array<{ licenseThreatCategory?: string }>;
  };
  securityData?: {
    securityIssues?: RawSecurityIssue[];
  };
}
interface RawComponent {
  packageUrl?: string;
  hash?: string;
  displayName?: string;
  componentIdentifier?: {
    format?: string;
    coordinates?: {
      groupId?: string;
      artifactId?: string;
      version?: string;
    };
  };
}
interface RawSecurityIssue {
  reference?: string;
  severity?: number;
  source?: string;
  url?: string;
  threatCategory?: string;
  description?: string;
}

interface RawEvaluateResponse {
  repositoryPublicId?: string;
  repositoryType?: string;
  results?: RawEvaluateResult[];
}
interface RawEvaluateResult {
  quarantined?: boolean;
  component?: RawComponent;
  policyViolations?: RawPolicyViolation[];
}
interface RawPolicyViolation {
  policyId?: string;
  policyName?: string;
  policyViolationId?: string;
  threatLevel?: number;
  constraintViolations?: Array<{ constraintName?: string; reasons?: Array<{ reason?: string }> }>;
}

interface RawRemediationResponse {
  remediation?: {
    versionChanges?: RawVersionChange[];
  };
}
interface RawVersionChange {
  type?: string;
  data?: { component?: RawComponent };
}

async function readIqError(res: Response): Promise<string> {
  const text = await res.text().catch(() => "");
  const preview = text.length > 300 ? `${text.slice(0, 300)}…` : text;
  return preview ? ` — ${preview}` : "";
}

async function fetchDetails(
  purl: string,
  settings: ExtensionSettings,
): Promise<RawComponentDetail> {
  const res = await fetch(`${baseUrl(settings)}/api/v2/components/details`, {
    method: "POST",
    credentials: "include",
    headers: await jsonHeadersWithCsrf(settings),
    body: JSON.stringify({ components: [{ packageUrl: purl }] }),
  });
  if (!res.ok) {
    throw new Error(`Component details failed: HTTP ${res.status}${await readIqError(res)}`);
  }
  const dto = (await res.json()) as RawDetailsResponse;
  const first = dto.componentDetails?.[0];
  if (!first) throw new Error(`No component details returned for ${purl}`);
  return first;
}

/**
 * Build the repository-relative pathname IQ Firewall's evaluate endpoint
 * expects. For maven: `<group-as-path>/<artifact>/<version>/<artifact>-<version>.<type>`.
 */
function pathnameFromMavenPurl(purl: string): string | null {
  // pkg:maven/<group>/<artifact>@<version>?type=<type>
  const m = purl.match(/^pkg:maven\/([^/]+)\/([^@]+)@([^?]+)(?:\?(.+))?$/);
  if (!m) return null;
  const [, group, artifact, version, qs] = m;
  const qsMap = new URLSearchParams(qs || "");
  const type = qsMap.get("type") || "jar";
  return `${group.replace(/\./g, "/")}/${artifact}/${version}/${artifact}-${version}.${type}`;
}

async function evaluateComponent(
  purl: string,
  hash: string,
  vrmId: string,
  repoId: string,
  settings: ExtensionSettings,
): Promise<RawEvaluateResult> {
  const url =
    `${baseUrl(settings)}/api/v2/firewall/components/` +
    `${encodeURIComponent(vrmId)}/${encodeURIComponent(repoId)}/evaluate`;
  const pathname = pathnameFromMavenPurl(purl);
  const componentBody: Record<string, string> = { packageUrl: purl, hash };
  if (pathname) componentBody.pathname = pathname;
  const res = await fetch(url, {
    method: "POST",
    credentials: "include",
    headers: await jsonHeadersWithCsrf(settings),
    body: JSON.stringify({ format: "maven2", components: [componentBody] }),
  });
  if (!res.ok) {
    throw new Error(`Evaluate failed: HTTP ${res.status}${await readIqError(res)}`);
  }
  const dto = (await res.json()) as RawEvaluateResponse;
  const first = dto.results?.[0];
  if (!first) throw new Error(`Evaluate returned no results for ${purl}`);
  return first;
}

async function fetchRemediation(
  purl: string,
  hash: string,
  repoId: string,
  settings: ExtensionSettings,
): Promise<RawRemediationResponse> {
  const url =
    `${baseUrl(settings)}/api/v2/components/remediation/repository/` +
    encodeURIComponent(repoId);
  const res = await fetch(url, {
    method: "POST",
    credentials: "include",
    headers: await jsonHeadersWithCsrf(settings),
    body: JSON.stringify({ packageUrl: purl, hash }),
  });
  if (!res.ok) {
    // Remediation is best-effort; if it fails we can still show a verdict.
    return {};
  }
  return (await res.json()) as RawRemediationResponse;
}

// -----------------------------------------------------------------------------
// Mapping — IQ DTOs → our FirewallVerdict shape
// -----------------------------------------------------------------------------

function mapIntegrityRating(raw?: string): IntegrityRating {
  switch ((raw || "").toLowerCase()) {
    case "malicious":
      return "Malicious";
    case "suspicious":
      return "Suspicious";
    case "pending":
      return "Pending";
    case "normal":
    case "known":
      return "Normal";
    default:
      return "Unknown";
  }
}

function severityBucket(cvss: number): CVE["severity"] {
  if (cvss >= 9) return "critical";
  if (cvss >= 7) return "high";
  if (cvss >= 4) return "medium";
  return "low";
}

function toCves(detail: RawComponentDetail): CVE[] {
  return (detail.securityData?.securityIssues || []).map((i) => ({
    id: i.reference || "unknown",
    sonatypeId: i.source === "sonatype" ? i.reference : undefined,
    cvss: typeof i.severity === "number" ? i.severity : 0,
    severity: severityBucket(typeof i.severity === "number" ? i.severity : 0),
    title: i.description || i.reference || "Security issue",
  }));
}

function pickWorstViolation(vs: RawPolicyViolation[] | undefined): RawPolicyViolation | undefined {
  if (!vs || vs.length === 0) return undefined;
  return [...vs].sort((a, b) => (b.threatLevel || 0) - (a.threatLevel || 0))[0];
}

function mapVerdict(
  quarantined: boolean | undefined,
  worst: RawPolicyViolation | undefined,
): PolicyVerdict {
  // Trust IQ's actual proxy decision. If IQ quarantined the artifact, that's
  // a hard block. If there are policy violations that only trigger Warn (not
  // Fail), IQ leaves quarantined=false — we surface that as "warn", not "block",
  // regardless of the underlying threat level.
  if (quarantined) return "block";
  if (worst) return "warn";
  return "allow";
}

function extractGolden(
  rem: RawRemediationResponse,
  currentVersion: string,
): ComponentInfo["goldenVersion"] {
  const changes = rem.remediation?.versionChanges || [];
  // Prefer no-violations; fall back to non-failing.
  const preferred =
    changes.find((c) => c.type === "next-no-violations") ||
    changes.find((c) => c.type === "next-non-failing");
  const nextComp = preferred?.data?.component;
  const nextVersion = nextComp?.componentIdentifier?.coordinates?.version;
  // If IQ can't offer a different version (or offers the same version we're
  // already on), there's no upgrade to suggest — hide the golden chip.
  if (!nextVersion || nextVersion === currentVersion) return undefined;
  return {
    version: nextVersion,
    fixesCves: [],
    breakingChanges: false,
  };
}

function composeRealVerdict(
  purl: string,
  detail: RawComponentDetail,
  evalRes: RawEvaluateResult,
  rem: RawRemediationResponse,
  repoId: string,
): FirewallVerdict {
  const c = detail.component;
  const coords = c.componentIdentifier?.coordinates;
  const worst = pickWorstViolation(evalRes.policyViolations);
  const violations = evalRes.policyViolations || [];

  const component: ComponentInfo = {
    purl,
    ecosystem: "maven",
    name:
      (coords?.groupId && coords?.artifactId
        ? `${coords.groupId}:${coords.artifactId}`
        : c.displayName) || purl,
    version: coords?.version || "",
    integrityRating: mapIntegrityRating(detail.integrityRating),
    threatTypes: [],
    cves: toCves(detail),
    license: {
      declared:
        detail.licenseData?.declaredLicenses?.[0]?.licenseName ||
        detail.licenseData?.declaredLicenses?.[0]?.licenseId ||
        "",
      observed: (detail.licenseData?.observedLicenses || [])
        .map((l) => l.licenseName || l.licenseId || "")
        .filter(Boolean),
    },
    goldenVersion: extractGolden(rem, coords?.version || ""),
  };

  return {
    component,
    policy: {
      verdict: mapVerdict(evalRes.quarantined, worst),
      policyName: worst?.policyName,
      stage: "Proxy",
      reasons: violations.map((v) => v.policyName || "policy").filter(Boolean) as string[],
      waiverEligible: violations.length > 0,
    },
    source: "iq-server",
    fetchedAt: Date.now(),
    policyViolationId: worst?.policyViolationId,
    repositoryId: repoId,
  };
}

// -----------------------------------------------------------------------------
// Public verdict entrypoint — dispatches on mode
// -----------------------------------------------------------------------------

export async function fetchVerdict(
  purl: string,
  settings: ExtensionSettings,
  pageUrl?: string,
): Promise<FirewallVerdict> {
  if (settings.mode === "real") return fetchVerdictReal(purl, settings, pageUrl);
  return fetchVerdictMock(purl, settings);
}

function safeOrigin(u?: string): string | null {
  if (!u) return null;
  try {
    return new URL(u).origin;
  } catch {
    return null;
  }
}

// Build a Map<pageOrigin, repositoryId> from the user's selected repos so
// verdict fetches use the repo that actually proxies the page in front of
// them. When multiple selected repos share the same upstream origin, the
// first one wins — we can't disambiguate further from the URL alone.
function buildOriginToRepoIdMap(settings: ExtensionSettings): Map<string, string> {
  const m = new Map<string, string>();
  for (const r of settings.selectedRepos) {
    const origin = safeOrigin(r.remoteUrl);
    if (!origin) continue;
    if (!m.has(origin)) m.set(origin, r.repositoryId);
  }
  return m;
}

// Pick the selected repo whose remoteUrl origin matches the page the user is
// on. Falls back to selectedRepoIds[0] when no page URL is available or no
// repo matches the page origin.
function pickRepoIdForPage(settings: ExtensionSettings, pageUrl?: string): string | undefined {
  const pageOrigin = safeOrigin(pageUrl);
  if (pageOrigin) {
    const byOrigin = buildOriginToRepoIdMap(settings);
    const hit = byOrigin.get(pageOrigin);
    if (hit) return hit;
  }
  return settings.selectedRepoIds[0];
}

async function fetchVerdictReal(
  purl: string,
  settings: ExtensionSettings,
  pageUrl?: string,
): Promise<FirewallVerdict> {
  if (!settings.vrmId) {
    throw new Error("Real mode requires a Virtual Repository Manager. Open Settings.");
  }
  const repoId = pickRepoIdForPage(settings, pageUrl);
  if (!repoId) {
    throw new Error("Real mode requires at least one repository selected. Open Settings.");
  }
  const detail = await fetchDetails(purl, settings);
  const hash = detail.component.hash;
  if (!hash) throw new Error(`IQ did not return a hash for ${purl}`);
  const [evalRes, rem] = await Promise.all([
    evaluateComponent(purl, hash, settings.vrmId, repoId, settings),
    fetchRemediation(purl, hash, repoId, settings),
  ]);
  return composeRealVerdict(purl, detail, evalRes, rem, repoId);
}

async function fetchVerdictMock(
  purl: string,
  settings: ExtensionSettings,
): Promise<FirewallVerdict> {
  const res = await fetch(`${baseUrl(settings)}/api/v2/firewall/verdict`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: authHeader(settings),
    },
    body: JSON.stringify({ purl }),
  });
  if (!res.ok) throw new Error(`Verdict failed: HTTP ${res.status}`);
  const data = (await res.json()) as FirewallVerdict;
  return { ...data, source: "mock" };
}

// -----------------------------------------------------------------------------
// Waiver — dispatches on mode
// -----------------------------------------------------------------------------

async function submitOneWaiver(
  settings: ExtensionSettings,
  scope: WaiverScope,
  policyViolationId: string,
  options: WaiverRequestOptions,
): Promise<string> {
  const url =
    `${baseUrl(settings)}/api/v2/policyWaiverRequests/` +
    `${encodeURIComponent(scope.ownerType)}/${encodeURIComponent(scope.ownerId)}` +
    `/policyViolation/${encodeURIComponent(policyViolationId)}`;

  // Only include optional fields when set — IQ's DTO uses @JsonInclude(NON_EMPTY)
  // for note/reason, and treats absent expiryTime as "never expires".
  const body: Record<string, unknown> = {
    matcherStrategy: options.matcherStrategy,
    expireWhenRemediationAvailable: options.expireWhenRemediationAvailable,
  };
  if (options.comment) body.comment = options.comment;
  if (options.noteToReviewer) body.noteToReviewer = options.noteToReviewer;
  if (options.expiryTime) body.expiryTime = options.expiryTime;
  if (options.waiverReasonId) body.waiverReasonId = options.waiverReasonId;

  // Identify the request as extension-originated so IQ tags the waiver source
  // as BROWSER_EXTENSION instead of the default FIREWALL_PROXY — the Waivers
  // page renders that as a "Hexawatch" chip. See ScanSource.java and
  // ApiPolicyWaiverRequestResource.java in insight-brain.
  const headers = {
    ...(await jsonHeadersWithCsrf(settings)),
    "X-Scan-Source": "BROWSER_EXTENSION",
  };
  const res = await fetch(url, {
    method: "POST",
    credentials: "include",
    headers,
    body: JSON.stringify(body),
  });
  if (res.status === 401) throw new Error("Unauthorized — check user code / pass code");
  if (!res.ok) {
    throw new Error(`Waiver request failed: HTTP ${res.status}${await readIqError(res)}`);
  }
  const data = await res.json();
  return data.id || data.policyWaiverRequestId || "requested";
}

// Find any VRM in the IQ instance that lists the given repo. The configured
// settings.vrmId is tried first (common case), then every other VRM. Returns
// undefined when no VRM proxies this repo — meaning Firewall never sees it
// and evaluate cannot produce a policy_violation for it.
async function findVrmContainingRepo(
  settings: ExtensionSettings,
  repoId: string,
): Promise<string | undefined> {
  if (settings.vrmId) {
    try {
      const repos = await listReposForVrm(settings, settings.vrmId);
      if (repos.some((r) => r.repositoryId === repoId)) return settings.vrmId;
    } catch {
      // fall through and try every VRM
    }
  }
  try {
    const vrms = await listVrms(settings);
    for (const v of vrms) {
      if (v.id === settings.vrmId) continue;
      try {
        const repos = await listReposForVrm(settings, v.id);
        if (repos.some((r) => r.repositoryId === repoId)) return v.id;
      } catch {
        // try the next VRM
      }
    }
  } catch (e: any) {
    console.warn("[hexawatch] VRM enumeration for repo lookup failed:", e?.message);
  }
  return undefined;
}

// Try to make IQ create a fresh policy_violation record scoped to the user's
// chosen owner so we can waive against it. Only meaningful when the selected
// owner is a specific repository under some VRM — that's the shape
// /firewall/components/{vrmId}/{repoId}/evaluate accepts. Falls back through
// every VRM to find one that lists the repo. Returns a detailed error string
// in `error` when we couldn't get a fresh id, so the caller can surface it.
async function evaluatePolicyViolationForScope(
  purl: string,
  settings: ExtensionSettings,
  scope: WaiverScope,
): Promise<{ policyViolationId?: string; error?: string }> {
  if (scope.ownerType !== "repository") {
    return { error: "selected scope is not a repository — skipped evaluate" };
  }
  const vrmId = await findVrmContainingRepo(settings, scope.ownerId);
  if (!vrmId) {
    return {
      error: `no VRM proxies repository ${scope.ownerId} — IQ Firewall can't create a policy_violation for it`,
    };
  }
  try {
    const detail = await fetchDetails(purl, settings);
    const hash = detail.component.hash;
    if (!hash) return { error: "IQ did not return a component hash" };
    const evalRes = await evaluateComponent(purl, hash, vrmId, scope.ownerId, settings);
    const id = pickWorstViolation(evalRes.policyViolations)?.policyViolationId;
    if (!id) {
      return { error: "evaluate returned no policy violations for the selected repo" };
    }
    return { policyViolationId: id };
  } catch (e: any) {
    return { error: e?.message || String(e) };
  }
}

export interface WaiverSubmitCallResult {
  label: string;
  ok: boolean;
  id?: string;
  error?: string;
  policyViolationId?: string;
}

export interface WaiverSubmitResult {
  results: WaiverSubmitCallResult[];
}

export async function requestWaiver(
  purl: string,
  settings: ExtensionSettings,
  options: WaiverRequestOptions,
): Promise<WaiverSubmitResult> {
  if (settings.mode !== "real") {
    // Mock mode: opaque path kept for regression parity with earlier build.
    const res = await fetch(`${baseUrl(settings)}/api/v2/waivers`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: authHeader(settings) },
      body: JSON.stringify({ purl, reason: options.comment }),
    });
    if (!res.ok) throw new Error(`Waiver failed: HTTP ${res.status}`);
    const data = await res.json();
    return { results: [{ label: "mock", ok: true, id: data.waiverId }] };
  }

  if (!options.policyViolationId) {
    throw new Error("Waiver requires a policy violation id from the verdict.");
  }
  const { scope } = options;

  // Single waiver at the scope the user picked. IQ's
  // ApiPolicyWaiverRequestService.isViolationOwnerId walks the violation's
  // owner chain upward — so a waiver on an ancestor (VRM, root org) covers
  // the descendants automatically. For a specific-repo scope we first
  // evaluate to mint a policy_violation owned by that repo when possible.
  const evalOutcome = await evaluatePolicyViolationForScope(purl, settings, scope);
  const violationId = evalOutcome.policyViolationId || options.policyViolationId;
  const results: WaiverSubmitCallResult[] = [];
  try {
    const id = await submitOneWaiver(settings, scope, violationId, options);
    console.log("[hexawatch] waiver submitted:", id);
    results.push({ label: scope.label, ok: true, id, policyViolationId: violationId });
  } catch (e: any) {
    console.warn("[hexawatch] waiver failed:", e?.message);
    const evalNote = evalOutcome.error ? ` (evaluate: ${evalOutcome.error})` : "";
    results.push({
      label: scope.label,
      ok: false,
      error: `${e?.message || String(e)}${evalNote}`,
    });
  }
  if (results.every((r) => !r.ok)) {
    throw new Error(results.map((r) => `${r.label}: ${r.error}`).join(" | "));
  }
  return { results };
}
