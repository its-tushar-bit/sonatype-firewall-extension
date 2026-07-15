/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { THREAT_GROUPS } from 'MainRoot/nosc/applications/applicationDetailUtils';
import { ApplicationsListRequest } from 'MainRoot/nosc/applications/applicationsListApi';

/** Policy threat level bucket ids — align with {@link THREAT_GROUPS} labels. */
export type ApplicationsThreatLevelId = (typeof THREAT_GROUPS)[number]['group'];

function parseThreatGroupRange(range: string): readonly [number, number] {
  if (range === '0') {
    return [0, 0];
  }
  if (range.includes('-')) {
    const [min, max] = range.split('-').map((part) => Number(part));
    return [min, max];
  }
  const value = Number(range);
  return [value, value];
}

const THREAT_GROUP_RANGES = Object.fromEntries(
  THREAT_GROUPS.map(({ group, range }) => [group, parseThreatGroupRange(range)]),
) as Record<ApplicationsThreatLevelId, readonly [number, number]>;

export type ApplicationsListFilterState = {
  readonly stageIds: ReadonlySet<string>;
  readonly organizationIds: ReadonlySet<string>;
  readonly applicationIds: ReadonlySet<string>;
  readonly threatLevelIds: ReadonlySet<ApplicationsThreatLevelId>;
};

export const EMPTY_APPLICATIONS_LIST_FILTERS: ApplicationsListFilterState = {
  stageIds: new Set(),
  organizationIds: new Set(),
  applicationIds: new Set(),
  threatLevelIds: new Set(),
};

export function hasActiveApplicationsListFilters(
  filters: ApplicationsListFilterState,
): boolean {
  return (
    filters.stageIds.size > 0
    || filters.organizationIds.size > 0
    || filters.applicationIds.size > 0
    || filters.threatLevelIds.size > 0
  );
}

export function toggleApplicationsListFilterId(
  filters: ApplicationsListFilterState,
  field: keyof ApplicationsListFilterState,
  id: string,
): ApplicationsListFilterState {
  // Omit None: rail facets hide it, and level-0 ≠ clean-app semantics until that lands.
  if (field === 'threatLevelIds' && (!(id in THREAT_GROUP_RANGES) || id === 'None')) {
    return filters;
  }
  const current = filters[field];
  const next = new Set(current);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  return { ...filters, [field]: next };
}

function buildThreatLevelRanges(
  threatLevelIds: ReadonlySet<ApplicationsThreatLevelId>,
): NonNullable<ApplicationsListRequest['policyThreatLevelRanges']> | undefined {
  if (threatLevelIds.size === 0) return undefined;
  const ranges: Array<{ minPolicyThreatLevel: number; maxPolicyThreatLevel: number }> = [];
  threatLevelIds.forEach((id) => {
    const range = THREAT_GROUP_RANGES[id];
    if (range) {
      ranges.push({ minPolicyThreatLevel: range[0], maxPolicyThreatLevel: range[1] });
    }
  });
  return ranges.length > 0 ? ranges : undefined;
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
  const policyThreatLevelRanges = buildThreatLevelRanges(filters.threatLevelIds);

  return {
    ...(stageIds ? { stageIds } : {}),
    ...(organizationIds ? { organizationIds } : {}),
    ...(applicationIds ? { applicationIds } : {}),
    ...(policyThreatLevelRanges ? { policyThreatLevelRanges } : {}),
  };
}

/** Static threat facet rows for the filter rail until server facet counts land. */
const STATIC_THREAT_LEVEL_FACETS = THREAT_GROUPS.filter(({ group }) => group !== 'None').map(({ group, range }) => ({
  id: group,
  label: `${range} ${group}`,
  count: 0,
}));

export function staticThreatLevelFacets(): ReadonlyArray<{
  readonly id: string;
  readonly label: string;
  readonly count: number;
}> {
  return STATIC_THREAT_LEVEL_FACETS;
}
