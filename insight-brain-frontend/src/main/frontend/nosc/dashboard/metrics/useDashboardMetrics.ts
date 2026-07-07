/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo } from 'react';
import { getDashboardMetricsUrl } from 'MainRoot/util/CLMLocation';
import { useTile, type TileStatus, type UseTileResult } from 'MainRoot/nosc/dashboard/useTile';
import type { DashboardMetricsResponse, DashboardMetricsScope } from './dashboardMetricsTypes';

/**
 * Single-request data hook for the Nexus One preview dashboard metric grid (CLM-40905).
 *
 * Issues exactly ONE `POST /rest/dashboard/metrics` per mount (and again on an explicit
 * `retry()` or when the active filter scope changes), carrying the scope in the request
 * body. This deliberately replaces the legacy per-tab eager Redux dataset loads: the
 * landing reads aggregate counts in one round-trip instead of pulling the full
 * application / violation / waiver arrays.
 *
 * Status model (each state is rendered explicitly by the grid):
 *   - `loading`    — request in flight (grid shows per-card skeletons, or stale cards if
 *                    a prior response is held)
 *   - `ready`      — 200; `data` populated (zero totals render honestly)
 *   - `not-ready`  — 409; the search index is still building (friendly retry message)
 *   - `error`      — network / 5xx / other; inline error with retry (stale data kept)
 */
export type DashboardMetricsStatus = TileStatus;

export type UseDashboardMetricsResult = UseTileResult<DashboardMetricsResponse>;

export function useDashboardMetrics(
  scope: DashboardMetricsScope = {},
  enabled = true,
): UseDashboardMetricsResult {
  const scopeKey = useMemo(() => JSON.stringify(scope ?? {}), [scope]);
  const body = useMemo(() => JSON.parse(scopeKey) as DashboardMetricsScope, [scopeKey]);
  return useTile<DashboardMetricsResponse>(getDashboardMetricsUrl(), undefined, {
    method: 'post',
    body,
    enabled,
    mapErrorStatus: (statusCode) => (statusCode === 409 ? 'not-ready' : 'error'),
  });
}
