/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { getApplicationReportHistoryUrl } from 'MainRoot/util/CLMLocation';
import {
  compareIqLifecycleStageIds,
  iqLifecycleStageLabel,
} from './iqLifecycleStages';

/**
 * Severity counts on a historical evaluation.
 *
 * Low is absent by design: ScanPolicyEvaluator does not count LOW into PolicyEvaluationResult, so
 * these three will not sum to the violation total shown on Overview. Present the three explicitly
 * rather than deriving a total from them.
 */
export interface EvaluationSeverityCounts {
  readonly criticalPolicyViolationCount?: number;
  readonly severePolicyViolationCount?: number;
  readonly moderatePolicyViolationCount?: number;
  readonly totalComponentCount?: number;
}

/**
 * One row of `GET /api/v2/reports/applications/{id}/history`.
 *
 * `stage` is part of the wire payload; the tab groups by the requested stageId rather than this
 * field, but the type keeps payload shape fidelity for callers that need it.
 */
export interface EvaluationRow {
  readonly stage: string;
  readonly scanId: string;
  readonly evaluationDate: string;
  readonly scanTriggerTypeDisplayName?: string;
  readonly isForMonitoring?: boolean;
  readonly isReevaluation?: boolean;
  readonly scannerVersion?: string;
  readonly policyEvaluationResult?: EvaluationSeverityCounts;
}

interface ApiReportHistory {
  readonly applicationId?: string;
  readonly reports?: ReadonlyArray<EvaluationRow>;
}

/**
 * Rows requested per stage.
 *
 * The history endpoint reads policythreats.json and summary.json per row, so this bounds file I/O
 * at (stages x 2 x this) reads for one tab view. The endpoint's own `limit` defaults to 100 and is
 * not capped server-side (CLM-44035), so the value must always be sent explicitly — never omitted.
 */
export const EVALUATIONS_PER_STAGE = 5;

/**
 * Hard ceiling on stages fetched for one tab view. Known lifecycle stages are seven; this leaves
 * headroom for a few custom ids without letting a pathological reports payload fan out unbounded
 * history file reads.
 */
const MAX_STAGES_FOR_EVALUATIONS_TAB = 12;

/**
 * Cap concurrent history requests. Each request reads report files; unbounded Promise.all across
 * many stages can pile file I/O onto one node under estate-scale load.
 */
const MAX_PARALLEL_HISTORY_REQUESTS = 4;

export { iqLifecycleStageLabel as evaluationStageLabel };

export type EvaluationsStatus = 'loading' | 'ready' | 'error';

export interface StageEvaluations {
  readonly stageId: string;
  readonly rows: ReadonlyArray<EvaluationRow>;
  /** True when this stage's own request failed; other stages still render. */
  readonly errored: boolean;
}

export interface UseApplicationEvaluationsResult {
  readonly status: EvaluationsStatus;
  readonly stages: ReadonlyArray<StageEvaluations>;
  readonly retry: () => void;
}

function isRequestAborted(err: unknown): boolean {
  return Boolean(
    axios.isCancel?.(err) ||
      (axios.isAxiosError?.(err) && (err.code === 'ERR_CANCELED' || err.name === 'CanceledError')),
  );
}

/** Run `fn` over items with a fixed concurrency ceiling; preserves input order in the result. */
async function mapPool<T, R>(
  items: ReadonlyArray<T>,
  concurrency: number,
  fn: (item: T) => Promise<R>,
): Promise<R[]> {
  if (!items.length) return [];
  const results: R[] = new Array(items.length);
  let next = 0;
  const workers = Array.from({ length: Math.min(concurrency, items.length) }, async () => {
    while (next < items.length) {
      const index = next;
      next += 1;
      results[index] = await fn(items[index]);
    }
  });
  await Promise.all(workers);
  return results;
}

/**
 * Dedupe, drop empties, lifecycle-sort, and cap. Cap prefers known lifecycle stages so quieter
 * custom stages are the ones dropped if a payload ever exceeds the ceiling.
 */
function normalizeStageIds(stageIds: ReadonlyArray<string>): string[] {
  const unique = Array.from(new Set(stageIds.filter(Boolean)));
  unique.sort(compareIqLifecycleStageIds);
  return unique.slice(0, MAX_STAGES_FOR_EVALUATIONS_TAB);
}

/**
 * Load recent evaluations for each stage that has one.
 *
 * One request per stage rather than a single unfiltered call: an unfiltered history returns the N
 * most recent rows across all stages, so a frequently built stage would crowd quieter ones out of
 * the list entirely. Per-stage requests guarantee every stage appears.
 *
 * `stageIds` should come from the latest-per-stage reports the page has already loaded, which is a
 * database-only query — that keeps this to stages known to have evaluations instead of probing all
 * of them.
 */
export function useApplicationEvaluations(
  applicationInternalId: string | undefined,
  stageIds: ReadonlyArray<string>,
): UseApplicationEvaluationsResult {
  const [status, setStatus] = useState<EvaluationsStatus>('loading');
  const [stages, setStages] = useState<ReadonlyArray<StageEvaluations>>([]);
  const [attempt, setAttempt] = useState(0);

  // Sorted + joined so a re-render with an equivalent array does not refetch.
  const stageKey = useMemo(() => normalizeStageIds(stageIds).join(','), [stageIds]);

  useEffect(() => {
    if (!applicationInternalId) return;

    const ids = stageKey ? stageKey.split(',') : [];
    if (!ids.length) {
      setStages([]);
      setStatus('ready');
      return;
    }

    let cancelled = false;
    const controller = new AbortController();
    setStatus('loading');

    mapPool(ids, MAX_PARALLEL_HISTORY_REQUESTS, (stageId) =>
      axios
        .get<ApiReportHistory>(
          getApplicationReportHistoryUrl(applicationInternalId, stageId, EVALUATIONS_PER_STAGE),
          { signal: controller.signal },
        )
        // One stage failing must not blank the whole tab, so failures resolve to an errored entry.
        .then((response) => ({
          stageId,
          // Defend against a server that ignores `limit` (CLM-44035): never render more rows than
          // we intended to pay for in file I/O.
          rows: (response.data?.reports ?? []).slice(0, EVALUATIONS_PER_STAGE),
          errored: false,
        }))
        .catch((err: unknown) => {
          if (isRequestAborted(err)) throw err;
          return { stageId, rows: [] as ReadonlyArray<EvaluationRow>, errored: true };
        }),
    )
      .then((results) => {
        if (cancelled) return;
        setStages(results.sort((a, b) => compareIqLifecycleStageIds(a.stageId, b.stageId)));
        setStatus(results.every((r) => r.errored) ? 'error' : 'ready');
      })
      .catch(() => {
        if (cancelled) return;
        setStatus('error');
      });

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [applicationInternalId, stageKey, attempt]);

  const retry = useCallback(() => setAttempt((a) => a + 1), []);

  return { status, stages, retry };
}
