/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo, useState } from 'react';
import { usePreviewNewestRisksData } from 'MainRoot/nosc/dashboard/usePreviewNewestRisksData';

/**
 * Phase-1.5 / CLM-39641 (S2-PR-D-5): hook backing `RiskOverTimeTile`.
 *
 * Source: shared `previewDashboardNewestRisks` Redux slice. Each
 * violation's `firstOccurrenceTime` is bucketed into one of the last 14
 * daily buckets (UTC midnight boundaries); each bucket is a day-start
 * timestamp plus the count of violations whose first occurrence falls on
 * that calendar day.
 *
 * Constraint: same first-page-only constraint as the other F6 tiles.
 */

export type UseRiskOverTimeStatus = 'loading' | 'ready' | 'error';

export interface RiskBucket {
  /** Day-start epoch millis (UTC midnight) for this bucket. */
  readonly tsMillis: number;
  /** Violation count whose firstOccurrenceTime falls on this UTC day. */
  readonly count: number;
}

export interface UseRiskOverTimeResult {
  readonly status: UseRiskOverTimeStatus;
  readonly buckets: ReadonlyArray<RiskBucket>;
  readonly total: number;
  readonly error: Error | null;
  readonly retry: () => void;
}

const MS_PER_DAY = 24 * 60 * 60 * 1000;

export const DEFAULT_WINDOW_DAYS = 14;

/** Snap a timestamp to its UTC-midnight day-start. */
function dayStartUtc(ts: number): number {
  return Math.floor(ts / MS_PER_DAY) * MS_PER_DAY;
}

/**
 * Pure aggregator — exported for unit tests.
 *
 * Returns one bucket per day in the window ending at `nowMs` (UTC).
 * Each bucket's `tsMillis` is that day's UTC midnight; `count` is how
 * many violations have `firstOccurrenceTime` on that day.
 */
export function aggregateRiskOverTime(
  violations: ReadonlyArray<{ readonly firstOccurrenceTime?: number | string }> | undefined,
  nowMs: number,
  windowDays: number = DEFAULT_WINDOW_DAYS,
): ReadonlyArray<RiskBucket> {
  const lastBucketStart = dayStartUtc(nowMs);
  const buckets: RiskBucket[] = [];
  for (let i = windowDays - 1; i >= 0; i -= 1) {
    buckets.push({ tsMillis: lastBucketStart - i * MS_PER_DAY, count: 0 });
  }
  if (!violations || violations.length === 0) return buckets;

  const firstBucketStart = buckets[0].tsMillis;
  const tsToIndex = new Map<number, number>();
  buckets.forEach((b, idx) => tsToIndex.set(b.tsMillis, idx));

  for (const v of violations) {
    const raw = v.firstOccurrenceTime;
    if (raw === undefined || raw === null) continue;
    const ts = typeof raw === 'number' ? raw : Date.parse(String(raw));
    if (!Number.isFinite(ts)) continue;
    if (ts < firstBucketStart || ts > lastBucketStart + MS_PER_DAY) continue;
    const day = dayStartUtc(ts);
    const idx = tsToIndex.get(day);
    if (idx === undefined) continue;
    buckets[idx] = { ...buckets[idx], count: buckets[idx].count + 1 };
  }

  return buckets;
}

function toHookStatus(
  sliceStatus: 'idle' | 'loading' | 'ready' | 'error',
): UseRiskOverTimeStatus {
  if (sliceStatus === 'idle' || sliceStatus === 'loading') return 'loading';
  return sliceStatus;
}

export function useRiskOverTime(
  windowDays: number = DEFAULT_WINDOW_DAYS,
): UseRiskOverTimeResult {
  const { status: sliceStatus, violations, error, retry } = usePreviewNewestRisksData();
  const [nowMs] = useState<number>(() => Date.now());

  const buckets = useMemo(
    () => aggregateRiskOverTime(violations, nowMs, windowDays),
    [violations, nowMs, windowDays],
  );

  const total = useMemo(
    () => buckets.reduce((sum, b) => sum + b.count, 0),
    [buckets],
  );

  return {
    status: toHookStatus(sliceStatus),
    buckets,
    total,
    error,
    retry,
  };
}
