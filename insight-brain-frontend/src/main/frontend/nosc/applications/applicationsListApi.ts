/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ApplicationRiskCounts,
  ApplicationRiskScore,
  ApplicationStageRisk,
  ApplicationsFilterFacetCounts,
} from 'MainRoot/nosc/applications/applicationListTypes';
import { deriveFacetsFromPageRows } from 'MainRoot/nosc/applications/deriveFacetsFromPageRows';
import { staticThreatLevelFacets } from 'MainRoot/nosc/applications/applicationsListFilters';

/** Mirrors backend {@code RiskDTO}. */
export type ApiRiskCounts = {
  readonly totalRisk?: number;
  readonly criticalRisk?: number;
  readonly severeRisk?: number;
  readonly moderateRisk?: number;
  readonly lowRisk?: number;
};

/** Mirrors backend {@code StageRiskScoreDTO}. */
export type ApiStageRiskScore = {
  readonly stageTypeId?: string;
  readonly stageTypeName?: string;
  readonly scanId?: string;
  /** Epoch millis from the list API. */
  readonly evaluationTime?: number;
  readonly risk?: ApiRiskCounts;
};

/** Mirrors backend {@code ApplicationRiskScoreDTO}. */
export type ApiApplicationRiskScore = {
  readonly organizationName?: string;
  readonly organizationId?: string;
  readonly applicationName?: string;
  readonly applicationId?: string;
  readonly totalApplicationRisk?: ApiRiskCounts;
  readonly stageRisks?: ReadonlyArray<ApiStageRiskScore>;
  readonly lastEvaluationTime?: number;
};

/** Mirrors backend {@code ApplicationsListFacetsDTO}. */
export type ApiApplicationsListFacets = {
  readonly totalApplications?: number;
  readonly organizations?: Readonly<Record<string, number>> | null;
  readonly applications?: Readonly<Record<string, number>> | null;
  readonly stages?: Readonly<Record<string, number>> | null;
};

export type ApplicationsListApiResponse = {
  readonly applications?: ReadonlyArray<ApiApplicationRiskScore>;
  readonly facets?: ApiApplicationsListFacets | null;
  readonly total?: number;
  readonly page?: number;
  readonly pageSize?: number;
  readonly hasNextPage?: boolean;
  readonly source?: string;
};

export const APPLICATIONS_LIST_PAGE_SIZE = 50;

export type ApplicationsListRequest = {
  readonly page?: number;
  readonly pageSize?: number;
  readonly includeFacets?: boolean;
  readonly organizationIds?: ReadonlyArray<string>;
  readonly applicationIds?: ReadonlyArray<string>;
  readonly stageIds?: ReadonlyArray<string>;
  readonly policyThreatLevelRanges?: ReadonlyArray<{
    readonly minPolicyThreatLevel: number;
    readonly maxPolicyThreatLevel: number;
  }>;
};

function toRiskCounts(risk?: ApiRiskCounts): ApplicationRiskCounts {
  return {
    totalRisk: risk?.totalRisk ?? 0,
    criticalRisk: risk?.criticalRisk ?? 0,
    severeRisk: risk?.severeRisk ?? 0,
    moderateRisk: risk?.moderateRisk ?? 0,
    lowRisk: risk?.lowRisk ?? 0,
  };
}

export function evaluationTimeToIso(evaluationTime?: number): string | undefined {
  if (evaluationTime == null || Number.isNaN(evaluationTime) || evaluationTime <= 0) return undefined;
  const parsed = new Date(evaluationTime);
  if (Number.isNaN(parsed.getTime())) return undefined;
  return parsed.toISOString();
}

function mapStageRisk(stage: ApiStageRiskScore): ApplicationStageRisk | null {
  if (!stage.stageTypeId || !stage.scanId) return null;
  return {
    stageTypeId: stage.stageTypeId,
    stageTypeName: stage.stageTypeName || stage.stageTypeId,
    scanId: stage.scanId,
    evaluationDate: evaluationTimeToIso(stage.evaluationTime),
    risk: toRiskCounts(stage.risk),
  };
}

export function mapApiApplicationRiskScore(row: ApiApplicationRiskScore): ApplicationRiskScore | null {
  if (!row.applicationId || !row.applicationName) return null;
  const stageRisks = (row.stageRisks ?? [])
    .map(mapStageRisk)
    .filter((stage): stage is ApplicationStageRisk => stage != null);
  return {
    organizationName: row.organizationName ?? '—',
    organizationId: row.organizationId ?? '',
    applicationName: row.applicationName,
    applicationId: row.applicationId,
    totalApplicationRisk: toRiskCounts(row.totalApplicationRisk),
    stageRisks,
    lastEvaluationDate: evaluationTimeToIso(row.lastEvaluationTime),
  };
}

function facetEntriesFromMap(
  counts: Readonly<Record<string, number>> | null | undefined,
  labelById: ReadonlyMap<string, string>,
): ReadonlyArray<{ readonly id: string; readonly label: string; readonly count: number }> {
  if (!counts) return [];
  // Labels come from the current page rows only; cross-page facet IDs fall back to the raw
  // id until server facet enrichment returns display names.
  return Object.entries(counts)
    .filter(([id]) => id.trim().length > 0)
    .map(([id, count]) => ({
      id,
      label: labelById.get(id) ?? id,
      count,
    }))
    .sort((left, right) => left.label.localeCompare(right.label));
}

function buildLabelMaps(
  applications: ReadonlyArray<ApplicationRiskScore>,
): {
  readonly organizations: Map<string, string>;
  readonly applications: Map<string, string>;
  readonly stages: Map<string, string>;
} {
  const organizations = new Map<string, string>();
  const apps = new Map<string, string>();
  const stages = new Map<string, string>();
  applications.forEach((app) => {
    if (app.organizationId) {
      organizations.set(app.organizationId, app.organizationName);
    }
    apps.set(app.applicationId, app.applicationName);
    app.stageRisks.forEach((stage) => {
      stages.set(stage.stageTypeId, stage.stageTypeName);
    });
  });
  return { organizations, applications: apps, stages };
}

export function resolveApplicationsListTotal(response: ApplicationsListApiResponse): number {
  if (typeof response.total === 'number') return response.total;
  if (typeof response.facets?.totalApplications === 'number') return response.facets.totalApplications;
  return response.applications?.length ?? 0;
}

/** Maps API facets + current page rows into filter-rail counts (selection lands in a follow-up). */
export function mapApiFacets(
  response: ApplicationsListApiResponse,
  applications: ReadonlyArray<ApplicationRiskScore>,
  totalApplications: number,
): ApplicationsFilterFacetCounts {
  const labels = buildLabelMaps(applications);
  const apiFacets = response.facets;

  const organizations = facetEntriesFromMap(apiFacets?.organizations, labels.organizations);
  const appFacets = facetEntriesFromMap(apiFacets?.applications, labels.applications);
  const stageFacets = facetEntriesFromMap(apiFacets?.stages, labels.stages);
  const derived = deriveFacetsFromPageRows(applications);

  return {
    totalApplications,
    // Threat bucket rows are static until the list API returns server-side facet counts.
    threatLevels: staticThreatLevelFacets(),
    stages: stageFacets.length > 0 ? stageFacets : derived.stages,
    organizations: organizations.length > 0 ? organizations : derived.organizations,
    applications: appFacets.length > 0 ? appFacets : derived.applications,
  };
}

export function mapApplicationsListResponse(
  response: ApplicationsListApiResponse,
): {
  readonly applications: ReadonlyArray<ApplicationRiskScore>;
  readonly facets: ApplicationsFilterFacetCounts;
  readonly total: number;
  readonly page: number | undefined;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
} {
  const applications = (response.applications ?? [])
    .map(mapApiApplicationRiskScore)
    .filter((row): row is ApplicationRiskScore => row != null);
  const total = resolveApplicationsListTotal(response);
  return {
    applications,
    facets: mapApiFacets(response, applications, total),
    total,
    page: response.page,
    pageSize: response.pageSize ?? APPLICATIONS_LIST_PAGE_SIZE,
    hasNextPage: Boolean(response.hasNextPage),
  };
}
