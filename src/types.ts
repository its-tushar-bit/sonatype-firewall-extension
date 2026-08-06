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
}

export interface ExtensionSettings {
  iqServerUrl: string;
  userCode: string;
  passCode: string;
  nexusProxyUrl: string;
  rewriteInstallCommands: boolean;
  enabledSites: { npm: boolean; pypi: boolean; maven: boolean };
}

export const DEFAULT_SETTINGS: ExtensionSettings = {
  iqServerUrl: "http://localhost:8765",
  userCode: "demo-user",
  passCode: "demo-pass",
  nexusProxyUrl: "https://nexus.acme.com/repository",
  rewriteInstallCommands: true,
  enabledSites: { npm: true, pypi: true, maven: true },
};

export type RuntimeMessage =
  | { type: "GET_VERDICT"; purl: string }
  | { type: "GET_SETTINGS" }
  | { type: "SET_SETTINGS"; settings: ExtensionSettings }
  | { type: "REQUEST_WAIVER"; purl: string; reason: string }
  | { type: "GET_LAST_VIEWED" };

export type RuntimeResponse =
  | { ok: true; verdict: FirewallVerdict }
  | { ok: true; settings: ExtensionSettings }
  | { ok: true; waiverId: string }
  | { ok: true; lastViewed: FirewallVerdict | null }
  | { ok: false; error: string };
