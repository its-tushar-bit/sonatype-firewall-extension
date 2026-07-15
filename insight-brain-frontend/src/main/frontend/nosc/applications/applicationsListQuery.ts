/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type {
  ApplicationsListFilterState,
  ApplicationsThreatLevelId,
} from 'MainRoot/nosc/applications/applicationsListFilters';

/** Martha list API orderBy tokens (validator-enforced). */
export type ApplicationsListOrderBy = 'lastEvaluationTime' | '-lastEvaluationTime';

export const DEFAULT_APPLICATIONS_LIST_ORDER_BY: ApplicationsListOrderBy = '-lastEvaluationTime';

/** URL-friendly sort slugs persisted in the hash query. */
type ApplicationsListSortSlug = 'latest' | 'oldest';

const ORDER_BY_TO_SORT_SLUG: Record<ApplicationsListOrderBy, ApplicationsListSortSlug> = {
  '-lastEvaluationTime': 'latest',
  lastEvaluationTime: 'oldest',
};

const SELECTABLE_THREAT_LEVEL_IDS = new Set<ApplicationsThreatLevelId>([
  'Critical',
  'Severe',
  'Moderate',
  'Low',
]);

export function orderByToSortSlug(orderBy: ApplicationsListOrderBy): ApplicationsListSortSlug {
  return ORDER_BY_TO_SORT_SLUG[orderBy];
}

export function sortSlugToOrderBy(slug: string | null | undefined): ApplicationsListOrderBy {
  if (slug === 'oldest') return 'lastEvaluationTime';
  return DEFAULT_APPLICATIONS_LIST_ORDER_BY;
}

export function applicationsListOrderByLabel(orderBy: ApplicationsListOrderBy): string {
  return orderBy === '-lastEvaluationTime' ? 'Latest evaluation' : 'Oldest evaluation';
}

function parseCsvParam(value: string | null | undefined): ReadonlyArray<string> {
  if (!value?.trim()) return [];
  return value.split(',').map((part) => part.trim()).filter(Boolean);
}

function parseThreatLevelIds(values: ReadonlyArray<string>): ReadonlySet<ApplicationsThreatLevelId> {
  const ids = values.filter((value): value is ApplicationsThreatLevelId =>
    SELECTABLE_THREAT_LEVEL_IDS.has(value as ApplicationsThreatLevelId));
  return new Set(ids);
}

function serializeCsvParam(values: ReadonlySet<string>): string | undefined {
  if (values.size === 0) return undefined;
  return Array.from(values).sort().join(',');
}

/** Parse UI-Router params for the Martha Applications list page (CLM-42226). */
export function parseApplicationsListParams(
  params: Record<string, unknown>,
): {
  readonly search: string;
  readonly orderBy: ApplicationsListOrderBy;
  /** 0-based page index for the list API. */
  readonly page: number;
  readonly filters: ApplicationsListFilterState;
} {
  const search = typeof params.q === 'string' ? params.q.trim() : '';
  const orderBy = sortSlugToOrderBy(typeof params.sort === 'string' ? params.sort : null);
  const pageParam = typeof params.page === 'string' ? Number.parseInt(params.page, 10) : 1;
  const page = Number.isFinite(pageParam) && pageParam > 1 ? pageParam - 1 : 0;

  return {
    search,
    orderBy,
    page,
    filters: {
      stageIds: new Set(parseCsvParam(typeof params.stage === 'string' ? params.stage : null)),
      organizationIds: new Set(parseCsvParam(typeof params.org === 'string' ? params.org : null)),
      applicationIds: new Set(parseCsvParam(typeof params.app === 'string' ? params.app : null)),
      threatLevelIds: parseThreatLevelIds(
        parseCsvParam(typeof params.threat === 'string' ? params.threat : null),
      ),
    },
  };
}

export function buildApplicationsListRouteParams(state: {
  readonly search: string;
  readonly orderBy: ApplicationsListOrderBy;
  /** 0-based page index for the list API. */
  readonly page: number;
  readonly filters: ApplicationsListFilterState;
}): Record<string, string | undefined> {
  const sort = orderByToSortSlug(state.orderBy);
  const page = state.page > 0 ? String(state.page + 1) : undefined;
  const stage = serializeCsvParam(state.filters.stageIds);
  const org = serializeCsvParam(state.filters.organizationIds);
  const app = serializeCsvParam(state.filters.applicationIds);
  const threat = serializeCsvParam(state.filters.threatLevelIds);

  return {
    q: state.search.trim() || undefined,
    sort: sort === 'latest' ? undefined : sort,
    page,
    stage,
    org,
    app,
    threat,
  };
}

export function filtersEqual(
  left: ApplicationsListFilterState,
  right: ApplicationsListFilterState,
): boolean {
  const fields: (keyof ApplicationsListFilterState)[] = [
    'stageIds',
    'organizationIds',
    'applicationIds',
    'threatLevelIds',
  ];
  return fields.every((field) => {
    const leftIds = left[field];
    const rightIds = right[field];
    if (leftIds.size !== rightIds.size) return false;
    return Array.from(leftIds).every((id) => rightIds.has(id));
  });
}
