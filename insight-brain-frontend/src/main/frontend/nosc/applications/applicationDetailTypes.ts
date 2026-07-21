/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export interface ApplicationDTO {
  id: string;
  publicId: string;
  name: string;
  organizationId?: string;
  organizationName?: string;
}

export interface ApiApplicationReport {
  stage: string;
  applicationId: string;
  evaluationDate: string;
  latestReportHtmlUrl?: string;
  reportHtmlUrl?: string;
  embeddableReportHtmlUrl?: string;
  reportPdfUrl?: string;
  reportDataUrl?: string;
}

export interface PolicyConstraint {
  constraintName?: string;
}

export interface PolicyViolation {
  policyId?: string;
  policyName: string;
  policyThreatLevel: number;
  policyThreatCategory?: string;
  policyViolationId?: string;
  waived?: boolean;
  grandfathered?: boolean;
  legacyViolation?: boolean;
  constraints?: ReadonlyArray<PolicyConstraint>;
}

export interface PolicyComponent {
  hash?: string | null;
  componentIdentifier?: {
    format?: string;
    coordinates?: Record<string, string>;
  };
  packageUrl?: string;
  displayName?: string;
  pathnames?: ReadonlyArray<string>;
  matchState?: string;
  proprietary?: boolean;
  allViolations?: ReadonlyArray<PolicyViolation>;
  activeViolations?: ReadonlyArray<PolicyViolation>;
  waivedViolations?: ReadonlyArray<PolicyViolation>;
}

export interface PolicyThreatsResponse {
  aaData?: ReadonlyArray<PolicyComponent>;
  reportTime?: string;
  scanId?: string;
}

/**
 * Subset of the full raw-report DTO returned by
 * /api/v2/applications/{publicId}/reports/{scanId}/raw. Used by the
 * Components tab to list every component scanned (not just those with
 * violations — that's what policythreats.json gives). License + match
 * + dependency metadata are present here and absent from policythreats.
 */
export interface RawReportComponent {
  hash?: string;
  packageUrl?: string;
  displayName?: string;
  proprietary?: boolean;
  matchState?: string;
  pathnames?: ReadonlyArray<string>;
  componentIdentifier?: {
    format?: string;
    coordinates?: Record<string, string>;
  };
  licenseData?: {
    effectiveLicenses?: ReadonlyArray<{ licenseId?: string; licenseName?: string }>;
    effectiveLicenseThreats?: ReadonlyArray<{
      licenseThreatGroupName?: string;
      licenseThreatGroupLevel?: number;
      licenseThreatGroupCategory?: string;
    }>;
  };
  securityData?: {
    securityIssues?: ReadonlyArray<{
      reference?: string;
      severity?: number;
      source?: string;
      threatCategory?: string;
    }>;
  };
  dependencyData?: {
    directDependency?: boolean;
    innerSource?: boolean;
    parentComponentPurls?: ReadonlyArray<string>;
  };
}

export interface RawReportResponse {
  components?: ReadonlyArray<RawReportComponent>;
  matchSummary?: {
    totalComponentCount?: number;
    knownComponentCount?: number;
  };
  globalInformation?: { dataVersionDate?: string };
}

export interface FlatViolation {
  key: string;
  policyName: string;
  policyThreatLevel: number;
  policyThreatCategory: string;
  threatLabel: ThreatLabel;
  threatColor: ThreatColor;
  componentDisplay: string;
  componentHash: string;
  /** Present when policythreats.json includes policyViolationId (preferred deep-link target). */
  policyViolationId?: string;
  waived: boolean;
  legacy: boolean;
  constraintName: string;
}

export type ThreatLabel = 'Critical' | 'Severe' | 'Moderate' | 'Low' | 'None';
export type ThreatColor = 'red' | 'orange' | 'yellow' | 'indigo' | 'gray';

/**
 * Radix Tabs.Trigger values for the application detail page.
 *
 * The violations tab uses internal id `policy-failures` while the
 * user-facing URL slug is `violations` — see `TAB_TO_URL` and
 * `tabFromSlug` in `applicationDetailUtils.ts`.
 *
 * Adding a new tab? Add its id here and wire both directions in
 * `applicationDetailUtils.ts` (`URL_TO_TAB` / `TAB_TO_URL`).
 */
export const TAB_IDS = ['overview', 'policy-failures', 'components', 'sboms', 'waivers', 'team-members'] as const;
export type TabId = (typeof TAB_IDS)[number];
