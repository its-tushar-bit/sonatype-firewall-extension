/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo } from 'react';
import { usePreviewNewestRisksData } from 'MainRoot/nosc/dashboard/usePreviewNewestRisksData';

/**
 * Phase-1.5 / CLM-39641 (S2-PR-D-5): hook backing
 * `TopPolicyViolationsTile`.
 *
 * Aggregates the shared `previewDashboardNewestRisks` Redux slice into
 * top-N `(policyId, policyName, count)` rows sorted descending by count.
 *
 * Constraint — first page only: same as `useSeverityCounts`.
 */

export type UseTopPolicyViolationsStatus = 'loading' | 'ready' | 'error';

export interface TopPolicyRow {
  readonly policyId: string;
  readonly policyName: string;
  readonly count: number;
}

export interface UseTopPolicyViolationsResult {
  readonly status: UseTopPolicyViolationsStatus;
  readonly rows: ReadonlyArray<TopPolicyRow>;
  readonly error: Error | null;
  readonly retry: () => void;
}

/** Default top-N cap for the tile. */
export const DEFAULT_TOP_N = 4;

/**
 * Pure aggregator: bucket violations by `policyId`, sum counts, and
 * return descending.
 */
export function aggregateTopPolicyViolations(
  violations: ReadonlyArray<{ readonly policyId?: string; readonly policyName?: string }> | undefined,
  topN: number = DEFAULT_TOP_N,
): ReadonlyArray<TopPolicyRow> {
  if (!violations || violations.length === 0) return [];

  const byId = new Map<string, { policyName: string; count: number }>();
  for (const v of violations) {
    if (!v.policyId) continue;
    const existing = byId.get(v.policyId);
    if (existing) {
      existing.count += 1;
    } else {
      byId.set(v.policyId, {
        policyName: v.policyName ?? '(unknown policy)',
        count: 1,
      });
    }
  }

  const rows: TopPolicyRow[] = [];
  for (const [policyId, agg] of byId.entries()) {
    rows.push({ policyId, policyName: agg.policyName, count: agg.count });
  }

  rows.sort((a, b) => {
    if (b.count !== a.count) return b.count - a.count;
    return a.policyName.localeCompare(b.policyName);
  });

  return rows.slice(0, Math.max(0, topN));
}

function toHookStatus(
  sliceStatus: 'idle' | 'loading' | 'ready' | 'error',
): UseTopPolicyViolationsStatus {
  if (sliceStatus === 'idle' || sliceStatus === 'loading') return 'loading';
  return sliceStatus;
}

export function useTopPolicyViolations(
  topN: number = DEFAULT_TOP_N,
): UseTopPolicyViolationsResult {
  const { status: sliceStatus, violations, error, retry } = usePreviewNewestRisksData();
  const rows = useMemo(
    () => aggregateTopPolicyViolations(violations, topN),
    [violations, topN],
  );

  return {
    status: toHookStatus(sliceStatus),
    rows,
    error,
    retry,
  };
}
