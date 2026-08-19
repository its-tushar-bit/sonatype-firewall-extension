import {
  ApiVrm,
  ApiVrmRepo,
  ComponentInfo,
  CVE,
  ExtensionSettings,
  FirewallVerdict,
  IntegrityRating,
  PolicyVerdict,
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
): Promise<FirewallVerdict> {
  if (settings.mode === "real") return fetchVerdictReal(purl, settings);
  return fetchVerdictMock(purl, settings);
}

async function fetchVerdictReal(
  purl: string,
  settings: ExtensionSettings,
): Promise<FirewallVerdict> {
  if (!settings.vrmId) {
    throw new Error("Real mode requires a Virtual Repository Manager. Open Settings.");
  }
  const repoId = settings.selectedRepoIds[0];
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

export async function requestWaiver(
  purl: string,
  reason: string,
  settings: ExtensionSettings,
  policyViolationId?: string,
  repositoryId?: string,
): Promise<string> {
  if (settings.mode === "real") {
    if (!policyViolationId || !repositoryId) {
      throw new Error("Waiver requires a policy violation id and repository id from the verdict.");
    }
    const url =
      `${baseUrl(settings)}/api/v2/policyWaiverRequests/repository/` +
      `${encodeURIComponent(repositoryId)}/policyViolation/${encodeURIComponent(policyViolationId)}`;
    const res = await fetch(url, {
      method: "POST",
      credentials: "include",
      headers: await jsonHeadersWithCsrf(settings),
      body: JSON.stringify({
        matcherStrategy: "DEFAULT",
        comment: reason,
        expireWhenRemediationAvailable: false,
      }),
    });
    if (res.status === 401) throw new Error("Unauthorized — check user code / pass code");
    if (!res.ok) {
      throw new Error(`Waiver failed: HTTP ${res.status}${await readIqError(res)}`);
    }
    const data = await res.json();
    return data.id || data.policyWaiverRequestId || "requested";
  }
  // Mock mode: keep the old flat POST /api/v2/waivers path.
  void purl; // purl is unused in the mock's opaque endpoint
  const res = await fetch(`${baseUrl(settings)}/api/v2/waivers`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: authHeader(settings),
    },
    body: JSON.stringify({ purl, reason }),
  });
  if (!res.ok) throw new Error(`Waiver failed: HTTP ${res.status}`);
  const data = await res.json();
  return data.waiverId;
}
