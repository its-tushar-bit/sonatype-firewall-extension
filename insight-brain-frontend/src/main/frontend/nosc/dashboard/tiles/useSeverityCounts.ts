/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo } from 'react';
import { usePreviewNewestRisksData } from 'MainRoot/nosc/dashboard/usePreviewNewestRisksData';

/**
 * Phase-1.5 / CLM-39641 (S2-PR-D-5): hook backing `SeverityStripTile`.
 *
 * Aggregates the shared `previewDashboardNewestRisks` Redux slice (one
 * `/rest/dashboard/policy/newestRisks` POST for all Overview aggregators)
 * into Critical / Severe / Moderate / Low bucket counts.
 *
 * Constraint — first page only:
 *   The endpoint is page-based at 100 rows per page; the shared slice
 *   intentionally fetches a single page (page 0) without paging
 *   forward. The Phase-1.5 spec explicitly defers the per-tenant
 *   aggregate endpoint to a follow-on Initiative (F6 §9.3).
 */

export type SeverityBucket = 'critical' | 'severe' | 'moderate' | 'low';

export interface SeverityCounts {
  readonly critical: number;
  readonly severe: number;
  readonly moderate: number;
  readonly low: number;
}

export type UseSeverityCountsStatus = 'loading' | 'ready' | 'error';

export interface UseSeverityCountsResult {
  readonly status: UseSeverityCountsStatus;
  readonly counts: SeverityCounts;
  readonly error: Error | null;
  readonly retry: () => void;
}

const ZERO_COUNTS: SeverityCounts = {
  critical: 0,
  severe: 0,
  moderate: 0,
  low: 0,
};

/**
 * Map a numeric policy threat level (0..10) to its severity bucket.
 * Mirrors the `SEVERITY_TO_THREAT_RANGE` table in
 * `nosc/dashboard/tabs/PreviewViolationsTab.tsx` so a card-click and
 * a URL paste (`?severity=critical`) bucket the same set of
 * violations.
 */
export function bucketForThreatLevel(threatLevel: number): SeverityBucket {
  if (threatLevel >= 8) return 'critical';
  if (threatLevel >= 4) return 'severe';
  if (threatLevel >= 2) return 'moderate';
  return 'low';
}

export function aggregateSeverityCounts(
  violations: ReadonlyArray<{ readonly threatLevel?: number }> | undefined,
): SeverityCounts {
  if (!violations || violations.length === 0) return ZERO_COUNTS;
  let critical = 0;
  let severe = 0;
  let moderate = 0;
  let low = 0;
  for (const v of violations) {
    const lvl = typeof v.threatLevel === 'number' ? v.threatLevel : 0;
    const bucket = bucketForThreatLevel(lvl);
    if (bucket === 'critical') critical += 1;
    else if (bucket === 'severe') severe += 1;
    else if (bucket === 'moderate') moderate += 1;
    else low += 1;
  }
  return { critical, severe, moderate, low };
}

function toHookStatus(
  sliceStatus: 'idle' | 'loading' | 'ready' | 'error',
): UseSeverityCountsStatus {
  if (sliceStatus === 'idle' || sliceStatus === 'loading') return 'loading';
  return sliceStatus;
}

export function useSeverityCounts(): UseSeverityCountsResult {
  const { status: sliceStatus, violations, error, retry } = usePreviewNewestRisksData();
  const counts = useMemo(() => aggregateSeverityCounts(violations), [violations]);

  return {
    status: toHookStatus(sliceStatus),
    counts,
    error,
    retry,
  };
}
