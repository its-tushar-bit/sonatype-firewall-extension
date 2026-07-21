/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ApplicationsListRequest } from 'MainRoot/nosc/applications/applicationsListApi';

/** Integer policy threat domain for the Applications filter slider (matches Violations). */
export const APPLICATIONS_THREAT_MIN = 0;
export const APPLICATIONS_THREAT_MAX = 10;

export type ApplicationsThreatRange = readonly [number, number];

export const DEFAULT_APPLICATIONS_THREAT_RANGE: ApplicationsThreatRange = [
  APPLICATIONS_THREAT_MIN,
  APPLICATIONS_THREAT_MAX,
];

/** Set-valued sidebar filter groups (everything except {@link ApplicationsListFilterState.threatRange}). */
export type ApplicationsListFilterSetField = 'stageIds' | 'organizationIds' | 'applicationIds';

export type ApplicationsListFilterState = {
  readonly stageIds: ReadonlySet<string>;
  readonly organizationIds: ReadonlySet<string>;
  readonly applicationIds: ReadonlySet<string>;
  readonly threatRange: ApplicationsThreatRange;
};

export const EMPTY_APPLICATIONS_LIST_FILTERS: ApplicationsListFilterState = {
  stageIds: new Set(),
  organizationIds: new Set(),
  applicationIds: new Set(),
  threatRange: DEFAULT_APPLICATIONS_THREAT_RANGE,
};

/**
 * Full-domain span means "no threat filter" (Violations slider parity). Narrow ranges such as
 * {@code [0, 0]} still filter level-0-only; only the complete {@code [0, 10]} span is treated as unset.
 */
export function isDefaultApplicationsThreatRange(range: ApplicationsThreatRange): boolean {
  return range[0] <= APPLICATIONS_THREAT_MIN && range[1] >= APPLICATIONS_THREAT_MAX;
}

export function normalizeApplicationsThreatRange(
  next: readonly number[],
): ApplicationsThreatRange {
  const clampThreat = (n: number): number => {
    const safe = Number.isFinite(n) ? n : APPLICATIONS_THREAT_MIN;
    return Math.min(
      APPLICATIONS_THREAT_MAX,
      Math.max(APPLICATIONS_THREAT_MIN, safe),
    );
  };
  const low = clampThreat(next[0]);
  const high = clampThreat(next[1] ?? next[0]);
  return [Math.min(low, high), Math.max(low, high)];
}

export function hasActiveApplicationsListFilters(
  filters: ApplicationsListFilterState,
): boolean {
  return (
    filters.stageIds.size > 0
    || filters.organizationIds.size > 0
    || filters.applicationIds.size > 0
    || !isDefaultApplicationsThreatRange(filters.threatRange)
  );
}

export function toggleApplicationsListFilterId(
  filters: ApplicationsListFilterState,
  field: ApplicationsListFilterSetField,
  id: string,
): ApplicationsListFilterState {
  const current = filters[field];
  const next = new Set(current);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  return { ...filters, [field]: next };
}

/** Maps Martha sidebar filter state into POST /rest/dashboard/applications/list body fields. */
export function applicationsListFiltersToRequest(
  filters: ApplicationsListFilterState,
): Pick<
  ApplicationsListRequest,
  'stageIds' | 'organizationIds' | 'applicationIds' | 'policyThreatLevelRanges'
> {
  const stageIds = filters.stageIds.size > 0 ? Array.from(filters.stageIds) : undefined;
  const organizationIds =
    filters.organizationIds.size > 0 ? Array.from(filters.organizationIds) : undefined;
  const applicationIds =
    filters.applicationIds.size > 0 ? Array.from(filters.applicationIds) : undefined;
  const policyThreatLevelRanges = isDefaultApplicationsThreatRange(filters.threatRange)
    ? undefined
    : [{
        minPolicyThreatLevel: filters.threatRange[0],
        maxPolicyThreatLevel: filters.threatRange[1],
      }];

  return {
    ...(stageIds ? { stageIds } : {}),
    ...(organizationIds ? { organizationIds } : {}),
    ...(applicationIds ? { applicationIds } : {}),
    ...(policyThreatLevelRanges ? { policyThreatLevelRanges } : {}),
  };
}
