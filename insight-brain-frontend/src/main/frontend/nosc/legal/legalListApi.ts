/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ViolationRow,
  ViolationsFilterState,
  ViolationsListFacets,
} from 'MainRoot/nosc/violations/violationListTypes';
import {
  createDefaultViolationsFilterState,
  isDefaultThreatRange,
  toSortedArray,
  VIOLATIONS_PAGE_SIZE,
} from 'MainRoot/nosc/violations/violationsListApi';
import { applicationDetailHref } from 'MainRoot/nosc/applications/applicationDetailUtils';

/** Default page size — shared with Violations chrome. */
export const LEGAL_PAGE_SIZE = VIOLATIONS_PAGE_SIZE;

/** Default sort — highest license threat first. */
export const LEGAL_DEFAULT_ORDER_BY = '-licenseThreatLevel';

/** Wire body for POST /rest/dashboard/legal/list. */
export interface LegalListRequest {
  readonly page: number;
  readonly pageSize: number;
  readonly includeFacets: boolean;
  readonly orderBy: string;
  readonly search?: string;
  readonly organizationIds?: ReadonlyArray<string>;
  readonly applicationIds?: ReadonlyArray<string>;
  readonly stageIds?: ReadonlyArray<string>;
  /** License threat group display names (ALP language; useful for non-ALP too). */
  readonly licenseThreatGroupNames?: ReadonlyArray<string>;
  readonly licenseThreatLevelRange?: {
    readonly minPolicyThreatLevel: number;
    readonly maxPolicyThreatLevel: number;
  };
}

export interface LegalFindingRow {
  readonly legalFindingId: string;
  readonly threatLevel?: number;
  readonly severity?: string;
  readonly licenseId?: string;
  readonly licenseName?: string;
  readonly licenseThreatGroupName?: string;
  readonly organizationId?: string;
  readonly organizationName?: string;
  readonly applicationId?: string;
  readonly applicationPublicId?: string;
  readonly applicationName?: string;
  readonly componentName?: string;
  readonly componentVersion?: string;
  readonly componentHash?: string;
  readonly stage?: string;
  readonly reportId?: string;
}

export interface LegalListFacets {
  readonly totalFindings?: number;
  readonly stages?: Readonly<Record<string, number>>;
  readonly organizations?: Readonly<Record<string, number>>;
  readonly applications?: Readonly<Record<string, number>>;
  readonly licenseThreatGroups?: Readonly<Record<string, number>>;
  readonly organizationNames?: Readonly<Record<string, string>>;
  readonly applicationNames?: Readonly<Record<string, string>>;
}

export interface LegalListResponse {
  readonly findings: ReadonlyArray<LegalFindingRow>;
  readonly facets?: LegalListFacets;
  readonly total: number;
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage?: boolean;
  readonly source?: string;
}

/** Fresh Legal filters — LTG names reuse {@code threatCategories} in the shared filter state. */
export function createDefaultLegalFilterState(): ViolationsFilterState {
  return createDefaultViolationsFilterState();
}

/**
 * True when the user has narrowed beyond Legal defaults.
 * LTG selections (stored in threatCategories) count as active.
 */
export function hasActiveLegalFilters(filters: ViolationsFilterState): boolean {
  return (
    filters.threatCategories.size > 0 ||
    filters.stageIds.size > 0 ||
    filters.organizationIds.size > 0 ||
    filters.applicationIds.size > 0 ||
    !isDefaultThreatRange(filters.threatRange)
  );
}

export function buildLegalListRequest(params: {
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly includeFacets?: boolean;
  readonly filters?: ViolationsFilterState;
}): LegalListRequest {
  const search = params.search?.trim();
  const filters = params.filters;
  const stageIds = filters ? toSortedArray(filters.stageIds) : undefined;
  const organizationIds = filters ? toSortedArray(filters.organizationIds) : undefined;
  const applicationIds = filters ? toSortedArray(filters.applicationIds) : undefined;
  // Reuse threatCategories set for LTG names (filter rail "License Threat Group").
  const licenseThreatGroupNames = filters ? toSortedArray(filters.threatCategories) : undefined;
  const threatRange =
    filters && !isDefaultThreatRange(filters.threatRange) ? filters.threatRange : undefined;

  return {
    page: params.page,
    pageSize: params.pageSize ?? LEGAL_PAGE_SIZE,
    includeFacets: params.includeFacets ?? true,
    orderBy: LEGAL_DEFAULT_ORDER_BY,
    ...(search ? { search } : {}),
    ...(stageIds ? { stageIds } : {}),
    ...(organizationIds ? { organizationIds } : {}),
    ...(applicationIds ? { applicationIds } : {}),
    ...(licenseThreatGroupNames ? { licenseThreatGroupNames } : {}),
    ...(threatRange
      ? {
          licenseThreatLevelRange: {
            minPolicyThreatLevel: threatRange[0],
            maxPolicyThreatLevel: threatRange[1],
          },
        }
      : {}),
  };
}

/** Map Legal API facets into Violations filter-rail shape (LTG → threatCategories). */
export function adaptLegalFacetsForRail(facets: LegalListFacets | undefined): ViolationsListFacets | undefined {
  if (!facets) {
    return undefined;
  }
  return {
    totalViolations: facets.totalFindings ?? 0,
    stages: facets.stages,
    organizations: facets.organizations,
    applications: facets.applications,
    threatCategories: facets.licenseThreatGroups,
    organizationNames: facets.organizationNames,
    applicationNames: facets.applicationNames,
  };
}

/**
 * Map a LEGAL_VIOLATION finding into ViolationRow chrome fields.
 * {@code policyName} shows LTG (fallback license name). Drill href is overridden separately.
 */
export function adaptLegalFindingToViolationRow(finding: LegalFindingRow): ViolationRow {
  return {
    policyViolationId: finding.legalFindingId,
    threatLevel: finding.threatLevel,
    severity: finding.severity,
    threatCategory: 'license',
    policyName: finding.licenseThreatGroupName || finding.licenseName || 'License',
    organizationId: finding.organizationId,
    organizationName: finding.organizationName,
    applicationId: finding.applicationId,
    applicationPublicId: finding.applicationPublicId,
    applicationName: finding.applicationName,
    componentName: finding.componentName,
    componentVersion: finding.componentVersion,
    // Stash hash + reportId for V1 drill (not POLICY_VIOLATION fields).
    componentHash: finding.componentHash,
    reportId: finding.reportId,
    stage: finding.stage,
    state: 'OPEN',
    waivedWithAutoWaiver: false,
  };
}

/**
 * Nexus One hash → Classic Application Report Component Details Legal tab (embedded).
 * Available without ALP. Distinct from ALP {@code #/legal/component/{hash}}.
 * Mounted via {@link ClassicComponentMount} on {@code ApplicationReportRoot} in the N1 bundle.
 * <p>
 * {@code scanId} must be the policy-evaluation scan id (index field {@code reportId} on
 * LEGAL_VIOLATION docs is populated from {@code PolicyEvaluation.getScanId()}).
 */
function classicComponentLegalHref(publicId: string, scanId: string, hash: string): string {
  return (
    `#/applicationReport/${encodeURIComponent(publicId)}/${encodeURIComponent(scanId)}` +
    `/componentDetails/${encodeURIComponent(hash)}/legal`
  );
}

/**
 * Nexus One hash → ALP Classic Legal component overview (embedded in NOUX chrome).
 */
function classicLegalComponentHref(hash: string): string {
  return `#/legal/component/${encodeURIComponent(hash)}`;
}

/**
 * V1 drill-out for a legal finding — stays in Nexus One chrome with Classic RSC embedded.
 * <p>
 * With Advanced Legal Pack: {@code #/legal/component/{hash}} (obligations / attribution).
 * Without ALP: {@code #/applicationReport/.../componentDetails/.../legal} (license detections).
 * Native N1 Legal tab = V2 (Kitchen Sink).
 * <p>
 * {@code reportId} on list findings is the scan id (see {@link classicComponentLegalHref}).
 */
export function legalFindingHref(
  finding: Pick<LegalFindingRow, 'componentHash' | 'applicationPublicId' | 'reportId'>,
  options: { readonly advancedLegalPack?: boolean } = {},
): string {
  const hash = finding.componentHash?.trim();
  const publicId = finding.applicationPublicId?.trim();
  // Index field name is reportId; value is PolicyEvaluation.scanId (not a report-row PK).
  const scanId = finding.reportId?.trim();
  if (options.advancedLegalPack && hash) {
    return classicLegalComponentHref(hash);
  }
  if (publicId && scanId && hash) {
    return classicComponentLegalHref(publicId, scanId, hash);
  }
  if (publicId) {
    return applicationDetailHref(publicId);
  }
  // Last-resort self-link when hash/scan/publicId are missing from the index hit.
  // Prefer a stable Legal risk-list URL over a broken / empty href so the card remains clickable.
  return '#/legal-risk';
}

export function legalFindingHrefFromViolationRow(
  row: Pick<ViolationRow, 'componentHash' | 'applicationPublicId'> & { readonly reportId?: string },
  options: { readonly advancedLegalPack?: boolean } = {},
): string {
  return legalFindingHref(
    {
      componentHash: row.componentHash,
      applicationPublicId: row.applicationPublicId,
      reportId: row.reportId,
    },
    options,
  );
}
