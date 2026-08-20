export type IntegrityRating = "Normal" | "Suspicious" | "Malicious" | "Pending" | "Unknown";

export type PolicyVerdict = "allow" | "warn" | "quarantine" | "block";

export type ThreatType =
  | "trojan"
  | "backdoor"
  | "crypto-stealer"
  | "secrets-exfiltration"
  | "dropper"
  | "typosquat"
  | "brandjack"
  | "hijack"
  | "data-corruption";

export interface CVE {
  id: string;
  sonatypeId?: string;
  cvss: number;
  severity: "critical" | "high" | "medium" | "low";
  title: string;
  reachable?: boolean;
}

export interface ComponentInfo {
  purl: string;
  ecosystem: "npm" | "pypi" | "maven" | "nuget" | "go" | "rubygems" | "cargo";
  name: string;
  version: string;
  integrityRating: IntegrityRating;
  threatTypes: ThreatType[];
  cves: CVE[];
  license: { declared: string; observed: string[]; advancedLegalPack?: string };
  goldenVersion?: { version: string; fixesCves: string[]; breakingChanges: boolean };
  abfMatch?: { matched: boolean; matchedAgainst?: string };
}

export interface PolicyEvalResult {
  verdict: PolicyVerdict;
  policyName?: string;
  stage: "Proxy" | "Develop" | "Build" | "Stage" | "Release" | "Operate";
  reasons: string[];
  waiverEligible: boolean;
}

export interface FirewallVerdict {
  component: ComponentInfo;
  policy: PolicyEvalResult;
  reachability?: { reachable: boolean; appsScanned: number };
  fetchedAt: number;
  source: "mock" | "iq-server";
  policyViolationId?: string;
  repositoryId?: string;
}

export type IqMode = "mock" | "real";

export interface ApiVrm {
  id: string;
  name: string;
  childRepositoryCount?: number;
}

export interface ApiVrmRepo {
  repositoryId: string;
  publicId: string;
  format: string;
  remoteUrl?: string;
}

export interface ApiRepositoryManager {
  id: string;
  name: string;
  managerType: "HOSTED" | "PROXY" | "GROUP" | "VIRTUAL" | string;
  instanceId?: string;
  productName?: string;
  productVersion?: string;
}

// Waiver scope maps 1:1 to IQ's OwnerType + ownerId. See
// insight-brain/.../v2/ApiPolicyWaiverRequestResource.java.
export type WaiverScope =
  | { ownerType: "organization"; ownerId: string; label: string }
  | { ownerType: "repository_container"; ownerId: string; label: string }
  | { ownerType: "repository_manager"; ownerId: string; label: string }
  | { ownerType: "repository"; ownerId: string; label: string };

export type WaiverMatcherStrategy = "DEFAULT" | "EXACT_COMPONENT" | "ALL_COMPONENTS" | "ALL_VERSIONS";

export interface WaiverRequestOptions {
  scope: WaiverScope;
  policyViolationId: string;
  matcherStrategy: WaiverMatcherStrategy;
  comment?: string;
  noteToReviewer?: string;
  expiryTime?: string;
  expireWhenRemediationAvailable: boolean;
  waiverReasonId?: string;
}

export interface ApiWaiverReason {
  id: string;
  type: string;
  reasonText: string;
}

export interface ExtensionSettings {
  mode: IqMode;
  iqServerUrl: string;
  userCode: string;
  passCode: string;
  vrmId: string;
  vrmName: string;
  selectedRepoIds: string[];
  // Full repo objects for the selected repositories — the background reads
  // remoteUrl from these to dynamically register content scripts, so we no
  // longer hardcode Maven Central / npm / PyPI in the manifest.
  selectedRepos: ApiVrmRepo[];
  hexawatchUrl: string;
}

export const DEFAULT_SETTINGS: ExtensionSettings = {
  mode: "real",
  iqServerUrl: "http://localhost:8765",
  userCode: "demo-user",
  passCode: "demo-pass",
  vrmId: "",
  vrmName: "",
  selectedRepoIds: [],
  selectedRepos: [],
  hexawatchUrl: "http://localhost:9090",
};

export type RuntimeMessage =
  | { type: "GET_VERDICT"; purl: string }
  | { type: "REFRESH_VERDICT"; purl: string }
  | { type: "GET_SETTINGS" }
  | { type: "SET_SETTINGS"; settings: ExtensionSettings }
  | {
      type: "REQUEST_WAIVER";
      purl: string;
      options: WaiverRequestOptions;
    }
  | { type: "GET_LAST_VIEWED" }
  | { type: "TEST_CONNECTION" }
  | { type: "LIST_REPOS_FOR_VRM"; vrmId: string }
  | { type: "LIST_REPO_MANAGERS" }
  | { type: "LIST_REPOS_FOR_MANAGER"; repositoryManagerId: string }
  | { type: "LIST_WAIVER_REASONS" }
  | { type: "PULL_HEXAWATCH_CONFIG" }
  | { type: "PUSH_HEXAWATCH_CONFIG" }
  | { type: "SAVE_SETTINGS_SYNCED"; settings: ExtensionSettings }
  | { type: "RESCAN" };

export type TestConnectionResult =
  | { ok: true; vrms: ApiVrm[] }
  | { ok: false; error: string };

export type ListReposResult =
  | { ok: true; repos: ApiVrmRepo[] }
  | { ok: false; error: string };

export type ListRepoManagersResult =
  | { ok: true; managers: ApiRepositoryManager[] }
  | { ok: false; error: string };

export type ListWaiverReasonsResult =
  | { ok: true; reasons: ApiWaiverReason[] }
  | { ok: false; error: string };

export type HexawatchSyncResult =
  | { ok: true; source: "hexawatch" | "local" }
  | { ok: false; error: string };

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

export type RuntimeResponse =
  | { ok: true; verdict: FirewallVerdict }
  | { ok: true; settings: ExtensionSettings }
  | { ok: true; waiverResult: WaiverSubmitResult }
  | { ok: true; lastViewed: FirewallVerdict | null }
  | { ok: true; testResult: TestConnectionResult }
  | { ok: true; reposResult: ListReposResult }
  | { ok: true; managersResult: ListRepoManagersResult }
  | { ok: true; waiverReasonsResult: ListWaiverReasonsResult }
  | { ok: true; syncResult: HexawatchSyncResult }
  | { ok: false; error: string };
