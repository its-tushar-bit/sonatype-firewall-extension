/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type {
  ApplicationsListFilterSetField,
  ApplicationsListFilterState,
} from 'MainRoot/nosc/applications/applicationsListFilters';
import {
  DEFAULT_APPLICATIONS_THREAT_RANGE,
  isDefaultApplicationsThreatRange,
  normalizeApplicationsThreatRange,
  type ApplicationsThreatRange,
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

function serializeCsvParam(values: ReadonlySet<string>): string | undefined {
  if (values.size === 0) return undefined;
  return Array.from(values).sort().join(',');
}

function parseIntegerToken(token: string): number | undefined {
  return /^\d+$/.test(token) ? Number(token) : undefined;
}

/**
 * Parse {@code threat=min-max} (Violations-compatible). Malformed or legacy bucket tokens fall
 * back to the full-domain default.
 */
export function parseApplicationsThreatRange(value: string | null | undefined): ApplicationsThreatRange {
  if (!value?.trim()) return DEFAULT_APPLICATIONS_THREAT_RANGE;
  const parts = value.split('-');
  if (parts.length !== 2) return DEFAULT_APPLICATIONS_THREAT_RANGE;
  const min = parseIntegerToken(parts[0].trim());
  const max = parseIntegerToken(parts[1].trim());
  if (min === undefined || max === undefined) return DEFAULT_APPLICATIONS_THREAT_RANGE;
  return normalizeApplicationsThreatRange([min, max]);
}

function serializeApplicationsThreatRange(range: ApplicationsThreatRange): string | undefined {
  return isDefaultApplicationsThreatRange(range) ? undefined : `${range[0]}-${range[1]}`;
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
      threatRange: parseApplicationsThreatRange(
        typeof params.threat === 'string' ? params.threat : null,
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
  const threat = serializeApplicationsThreatRange(state.filters.threatRange);

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
  const setFields: ApplicationsListFilterSetField[] = [
    'stageIds',
    'organizationIds',
    'applicationIds',
  ];
  const setsEqual = setFields.every((field) => {
    const leftIds = left[field];
    const rightIds = right[field];
    if (leftIds.size !== rightIds.size) return false;
    return Array.from(leftIds).every((id) => rightIds.has(id));
  });
  return (
    setsEqual
    && left.threatRange[0] === right.threatRange[0]
    && left.threatRange[1] === right.threatRange[1]
  );
}
