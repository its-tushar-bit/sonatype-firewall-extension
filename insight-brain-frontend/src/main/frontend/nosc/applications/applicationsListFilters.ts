/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ApplicationsListRequest } from 'MainRoot/nosc/applications/applicationsListApi';
import { normalizeRange } from 'MainRoot/nosc/util/normalizeRange';

/** Integer policy threat domain for the Applications filter slider (matches Violations). */
export const APPLICATIONS_THREAT_MIN = 0;
export const APPLICATIONS_THREAT_MAX = 10;

export type ApplicationsThreatRange = readonly [number, number];

export const DEFAULT_APPLICATIONS_THREAT_RANGE: ApplicationsThreatRange = [
  APPLICATIONS_THREAT_MIN,
  APPLICATIONS_THREAT_MAX,
];

/** Set-valued sidebar filter groups (everything except {@link ApplicationsListFilterState.threatRange}). */
export type ApplicationsListFilterSetField =
  | 'stageIds'
  | 'organizationIds'
  | 'applicationIds'
  | 'policyTypes'
  | 'violationStates';

export type ApplicationsListFilterState = {
  readonly stageIds: ReadonlySet<string>;
  readonly organizationIds: ReadonlySet<string>;
  readonly applicationIds: ReadonlySet<string>;
  readonly policyTypes: ReadonlySet<string>;
  readonly violationStates: ReadonlySet<string>;
  readonly threatRange: ApplicationsThreatRange;
};

export const EMPTY_APPLICATIONS_LIST_FILTERS: ApplicationsListFilterState = {
  stageIds: new Set(),
  organizationIds: new Set(),
  applicationIds: new Set(),
  policyTypes: new Set(),
  violationStates: new Set(),
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
  return normalizeRange(next, APPLICATIONS_THREAT_MIN, APPLICATIONS_THREAT_MAX);
}

export function hasActiveApplicationsListFilters(
  filters: ApplicationsListFilterState,
): boolean {
  return (
    filters.stageIds.size > 0
    || filters.organizationIds.size > 0
    || filters.applicationIds.size > 0
    || filters.policyTypes.size > 0
    || filters.violationStates.size > 0
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

/**
 * Maps Martha sidebar filter state into POST /rest/dashboard/applications/list body fields.
 *
 * Wire formats match the backend filter DTOs (same as the Violations list):
 * {@code policyThreatCategories} is a comma-delimited string consumed by
 * {@code PolicyThreatCategoryFilter}'s String constructor, while {@code policyViolationStates} is an
 * array of enum names consumed by {@code PolicyViolationStateFilter}'s {@code @JsonCreator} Set
 * constructor.
 */
export function applicationsListFiltersToRequest(
  filters: ApplicationsListFilterState,
): Pick<
  ApplicationsListRequest,
  | 'stageIds'
  | 'organizationIds'
  | 'applicationIds'
  | 'policyThreatLevelRanges'
  | 'policyThreatCategories'
  | 'policyViolationStates'
> {
  const stageIds = filters.stageIds.size > 0 ? Array.from(filters.stageIds) : undefined;
  const organizationIds =
    filters.organizationIds.size > 0 ? Array.from(filters.organizationIds) : undefined;
  const applicationIds =
    filters.applicationIds.size > 0 ? Array.from(filters.applicationIds) : undefined;
  const policyThreatCategories =
    filters.policyTypes.size > 0 ? Array.from(filters.policyTypes).sort().join(',') : undefined;
  const policyViolationStates =
    filters.violationStates.size > 0 ? Array.from(filters.violationStates).sort() : undefined;
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
    ...(policyThreatCategories ? { policyThreatCategories } : {}),
    ...(policyViolationStates ? { policyViolationStates } : {}),
    ...(policyThreatLevelRanges ? { policyThreatLevelRanges } : {}),
  };
}
