/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import {
  ApiApplicationReport,
  FlatViolation,
  PolicyComponent,
  PolicyThreatsResponse,
  PolicyViolation,
  RawReportComponent,
  TabId,
  ThreatColor,
  ThreatLabel,
} from './applicationDetailTypes';

export const VIOLATION_PAGE_SIZE = 20;
export const THREAT_GROUPS: ReadonlyArray<{ readonly group: ThreatLabel; readonly range: string }> = [
  { group: 'Critical', range: '8-10' },
  { group: 'Severe', range: '4-7' },
  { group: 'Moderate', range: '2-3' },
  { group: 'Low', range: '1' },
  { group: 'None', range: '0' },
];

export function classifyThreat(level: number): { label: ThreatLabel; color: ThreatColor } {
  if (level >= 8) return { label: 'Critical', color: 'red' };
  if (level >= 4) return { label: 'Severe', color: 'orange' };
  if (level >= 2) return { label: 'Moderate', color: 'yellow' };
  if (level === 1) return { label: 'Low', color: 'indigo' };
  return { label: 'None', color: 'gray' };
}

export function deriveComponentDisplay(component: PolicyComponent): string {
  if (component.displayName) return component.displayName;
  if (component.packageUrl) return component.packageUrl;
  const coords = component.componentIdentifier?.coordinates;
  if (coords) {
    const parts = [coords.groupId, coords.artifactId, coords.version, coords.name, coords.packageId]
      .filter(Boolean);
    if (parts.length > 0) return parts.join(':');
  }
  if (component.pathnames && component.pathnames.length > 0) return component.pathnames[0];
  return component.hash ?? '(unknown component)';
}

export function getAllViolations(
  component: PolicyComponent,
): ReadonlyArray<PolicyViolation> {
  return (
    component.allViolations ??
    [...(component.activeViolations ?? []), ...(component.waivedViolations ?? [])]
  );
}

export function flattenViolations(report: PolicyThreatsResponse | null): FlatViolation[] {
  if (!report?.aaData) return [];
  const out: FlatViolation[] = [];
  for (const component of report.aaData) {
    if (!component.hash || component.hash === 'null') continue;
    const display = deriveComponentDisplay(component);
    const violations = getAllViolations(component);
    for (const v of violations) {
      const { label, color } = classifyThreat(v.policyThreatLevel);
      out.push({
        key: `${component.hash}:${v.policyViolationId ?? v.policyName + ':' + (v.constraints?.[0]?.constraintName ?? '')}`,
        policyName: v.policyName,
        policyThreatLevel: v.policyThreatLevel,
        policyThreatCategory: v.policyThreatCategory || 'Other',
        threatLabel: label,
        threatColor: color,
        componentDisplay: display,
        componentHash: component.hash,
        waived: !!v.waived,
        legacy: v.legacyViolation ?? !!v.grandfathered,
        constraintName: v.constraints?.[0]?.constraintName ?? '',
      });
    }
  }
  return out;
}

/** Extract the scanId from a report's `embeddableReportHtmlUrl`, which has the
 *  shape `ui/links/application/{publicId}/report/{scanId}/embeddable`. The IQ
 *  ApiApplicationReportDTOV2 doesn't expose the scanId as its own field, so we
 *  parse it out of the stable-link URL. */
export function extractScanId(report: ApiApplicationReport): string | null {
  const candidate =
    report.embeddableReportHtmlUrl ??
    report.reportHtmlUrl ??
    report.reportPdfUrl ??
    report.reportDataUrl ??
    '';
  const match = candidate.match(/\/report\/([^/]+)/);
  return match ? match[1] : null;
}

/**
 * Pick the most recent report across all stages. IQ returns one entry per
 * stage; we sort by evaluationDate descending and take the head.
 */
export function pickLatestReport(reports: ReadonlyArray<ApiApplicationReport>): ApiApplicationReport | null {
  if (!reports.length) return null;
  // Guard against missing/malformed evaluationDate: treat invalid dates as
  // oldest so a single bad entry can't sort to the top (and NaN comparisons
  // don't scramble the order).
  const ts = (r: ApiApplicationReport): number => {
    const t = new Date(r.evaluationDate).getTime();
    return Number.isNaN(t) ? -Infinity : t;
  };
  const sorted = [...reports].sort((a, b) => ts(b) - ts(a));
  return sorted[0];
}

const URL_TO_TAB: Readonly<Record<string, TabId>> = {
  overview: 'overview',
  violations: 'policy-failures',
  'policy-failures': 'policy-failures', // tolerate the internal name in URLs too
  components: 'components',
  sboms: 'sboms',
  waivers: 'waivers',
  'team-members': 'team-members',
};

export const TAB_TO_URL: Readonly<Record<TabId, string>> = {
  overview: 'overview',
  'policy-failures': 'violations',
  components: 'components',
  sboms: 'sboms',
  waivers: 'waivers',
  'team-members': 'team-members',
};

const DEFAULT_TAB: TabId = 'overview';

export const NEXUS_ONE_APPLICATION_DETAIL_PARENT_STATE = 'nexusOneApplicationsDetail';

/** Map a URL `{tab}` slug to the internal TabId, falling back to the default
 *  so a malformed bookmark still mounts the page cleanly. */
export function tabFromSlug(slug: string | undefined): TabId {
  if (!slug) return DEFAULT_TAB;
  return URL_TO_TAB[slug] ?? DEFAULT_TAB;
}

/** Derive the active tab from a UI-Router child state name (`nexusOneApplicationsDetail.{suffix}`). */
export function tabFromApplicationDetailStateName(stateName: string | undefined): TabId {
  if (!stateName?.startsWith(`${NEXUS_ONE_APPLICATION_DETAIL_PARENT_STATE}.`)) {
    return DEFAULT_TAB;
  }
  const suffix = stateName.slice(NEXUS_ONE_APPLICATION_DETAIL_PARENT_STATE.length + 1);
  return tabFromSlug(suffix);
}

/** Target child state for a tab click (`nexusOneApplicationsDetail.violations`, etc.). */
export function applicationDetailStateNameForTab(tab: TabId): string {
  return `${NEXUS_ONE_APPLICATION_DETAIL_PARENT_STATE}.${TAB_TO_URL[tab]}`;
}

/** Classic (legacy bundle) href, context-path / MTIQ-prefix aware. */
export function classicHref(path: string): string {
  return bundleIndexUrl('classic', path);
}

export function classicAppDetailHref(publicId: string): string {
  return classicHref(`/management/view/application/${encodeURIComponent(publicId)}`);
}

export function classicReportHref(publicId: string, scanId: string): string {
  return classicHref(
    `/applicationReport/${encodeURIComponent(publicId)}/${encodeURIComponent(scanId)}/policy`,
  );
}

export const COMPONENTS_PAGE_SIZE = 20;

export function deriveComponentName(c: RawReportComponent): string {
  if (c.displayName) return c.displayName;
  const coords = c.componentIdentifier?.coordinates ?? {};
  const a = coords.artifactId || coords.packageId || coords.name;
  const v = coords.version;
  if (a && v) return `${a}:${v}`;
  if (a) return a;
  return c.packageUrl ?? '(unknown)';
}

export function deriveLicense(c: RawReportComponent): string {
  const ls = c.licenseData?.effectiveLicenses ?? [];
  if (ls.length === 0) return '—';
  return ls.map((l) => l.licenseName || l.licenseId || '?').join(', ');
}

/** Highest severity across security issues (0-10) and license threat
 *  (also 0-10). Used to render the per-row Threat badge. */
export function deriveComponentThreat(c: RawReportComponent): number {
  let max = 0;
  for (const s of c.securityData?.securityIssues ?? []) {
    if (typeof s.severity === 'number' && s.severity > max) max = s.severity;
  }
  for (const t of c.licenseData?.effectiveLicenseThreats ?? []) {
    if (typeof t.licenseThreatGroupLevel === 'number' && t.licenseThreatGroupLevel > max) {
      max = t.licenseThreatGroupLevel;
    }
  }
  return max;
}

export function matchStateLabel(matchState: string | undefined): string {
  if (!matchState) return 'Unknown';
  const lower = matchState.toLowerCase();
  if (lower.includes('exact')) return 'Exact';
  if (lower.includes('similar')) return 'Similar';
  return 'Unknown';
}

export function matchStateColor(matchState: string | undefined): 'green' | 'yellow' | 'gray' {
  const label = matchStateLabel(matchState);
  if (label === 'Exact') return 'green';
  if (label === 'Similar') return 'yellow';
  return 'gray';
}

export function threatColorFor(level: number): 'red' | 'orange' | 'yellow' | 'indigo' | 'gray' {
  if (level >= 8) return 'red';
  if (level >= 4) return 'orange';
  if (level >= 2) return 'yellow';
  if (level === 1) return 'indigo';
  return 'gray';
}

/**
 * Classic IQ has no `applicationReport.components` state -- the prior
 * URL shape produced an "Unknown Address" unrecoverable error. The
 * actual canonical states (per applicationReport/route.js) are:
 *
 *   applicationReport.policy            -> /policy            (overall report)
 *   applicationReport.componentDetails  -> /componentDetails/{hash}  (per-component)
 *   applicationReport.componentDetails.overview  -> .../overview     (default tab)
 *
 * - Per-row link (hash supplied) goes to the component detail page on
 *   the Overview tab, the same destination the Classic Application
 *   Composition Report sidebar opens when the user clicks a component.
 * - Page-level "View in Classic" (no hash) goes to the overall policy
 *   report at /policy, which is what Classic's Application Detail
 *   button uses too.
 */
export function classicReportHrefForComponent(publicId: string, scanId: string, hash?: string): string {
  const base = classicHref(
    `/applicationReport/${encodeURIComponent(publicId)}/${encodeURIComponent(scanId)}`,
  );
  if (!hash) return `${base}/policy`;
  return `${base}/componentDetails/${encodeURIComponent(hash)}/overview`;
}
