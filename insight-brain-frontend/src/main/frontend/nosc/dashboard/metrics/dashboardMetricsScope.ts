/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import type { DashboardMetricsScope } from './dashboardMetricsTypes';

/**
 * Derive the `POST /rest/dashboard/metrics` request scope from the shared
 * `dashboardFilter` slice (CLM-40905).
 *
 * The metrics request reuses the SAME applied filter the Classic dashboard
 * tables use — it does NOT trigger the legacy eager `GET /rest/application`
 * full-array load. We read `appliedFilter` (the committed selection), not
 * `selected` (the in-flight, possibly-dirty draft), so the cards reflect what
 * the user has actually applied.
 *
 * Only non-empty selections are included so the default (nothing selected)
 * sends an empty `{}` body → the backend's default RBAC-scoped view.
 *
 * `repositories` is present on `appliedFilter` but is not forwarded yet:
 * `DashboardMetricsRequestDTO` has no `repositoryIds` field (CLM-40927 follow-up), so a
 * repository filter would be silently dropped if mapped today.
 */

interface FilterSets {
  readonly organizations?: ReadonlySet<string> | null;
  readonly applications?: ReadonlySet<string> | null;
  readonly stages?: ReadonlySet<string> | null;
  readonly categories?: ReadonlySet<string> | null;
  readonly repositories?: ReadonlySet<string> | null;
}

interface DashboardFilterSlice {
  readonly appliedFilter?: FilterSets | null;
}

interface StateWithDashboardFilter {
  readonly dashboardFilter?: DashboardFilterSlice;
}

function toIds(set: ReadonlySet<string> | null | undefined): string[] | undefined {
  if (!set || set.size === 0) return undefined;
  return Array.from(set);
}

/** Pure builder — exported for direct unit testing without a store. */
export function buildMetricsScope(appliedFilter: FilterSets | null | undefined): DashboardMetricsScope {
  if (!appliedFilter) return {};
  const scope: {
    organizationIds?: string[];
    applicationIds?: string[];
    stageIds?: string[];
    tagIds?: string[];
  } = {};
  const organizationIds = toIds(appliedFilter.organizations);
  const applicationIds = toIds(appliedFilter.applications);
  const stageIds = toIds(appliedFilter.stages);
  const tagIds = toIds(appliedFilter.categories);
  if (organizationIds) scope.organizationIds = organizationIds;
  if (applicationIds) scope.applicationIds = applicationIds;
  if (stageIds) scope.stageIds = stageIds;
  if (tagIds) scope.tagIds = tagIds;
  return scope;
}

const selectAppliedFilter = (state: StateWithDashboardFilter): FilterSets | null | undefined =>
  state.dashboardFilter?.appliedFilter;

/**
 * Memoized scope selector. Recomputes only when `appliedFilter` changes
 * (the reducer replaces the object on every apply), so the metrics hook
 * sees a stable scope across unrelated re-renders.
 */
export const selectDashboardMetricsScope = createSelector(
  selectAppliedFilter,
  (appliedFilter): DashboardMetricsScope => buildMetricsScope(appliedFilter),
);
