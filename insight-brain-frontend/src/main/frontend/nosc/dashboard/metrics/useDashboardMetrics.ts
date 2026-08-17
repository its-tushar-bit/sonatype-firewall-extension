/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import axios from 'axios';
import { getDashboardMetricsUrl } from 'MainRoot/util/CLMLocation';
import type { TileStatus } from 'MainRoot/nosc/dashboard/useTile';
import type { DashboardMetricsResponse, DashboardMetricsScope } from './dashboardMetricsTypes';

/**
 * Two-phase dashboard metrics load (estate scale).
 *
 * Fast ({@code includeHeavyMetrics: false}) and heavy POSTs start together. The grid
 * becomes {@code ready} when the fast tier returns so Applications / Orgs / Policies /
 * Waivers paint without waiting on the detailed aggregations; heavy tiles overlay when
 * that response arrives.
 */

export type DashboardMetricsStatus = TileStatus;

export interface UseDashboardMetricsResult {
  readonly status: DashboardMetricsStatus;
  readonly data: DashboardMetricsResponse | null;
  readonly error: Error | null;
  /** True while the heavy (countDistinct) request is still in flight. */
  readonly heavyLoading: boolean;
  readonly heavyError: Error | null;
  readonly retry: () => void;
  readonly retryHeavy: () => void;
}

interface SummaryRequestIdentity {
  readonly requestKey: string;
  readonly generation: number;
}

function statusCodeOf(err: unknown): number | undefined {
  if (axios.isAxiosError(err)) return err.response?.status;
  return undefined;
}

function mergeMetrics(summary: DashboardMetricsResponse, heavy: DashboardMetricsResponse): DashboardMetricsResponse {
  return {
    ...summary,
    violations: heavy.violations,
    components: heavy.components,
    vulnerabilities: heavy.vulnerabilities,
    legal: heavy.legal,
    lastUpdatedAt: summary.lastUpdatedAt,
  };
}

function canonicalScope(scope: DashboardMetricsScope): DashboardMetricsScope {
  const normalizedIds = (ids: readonly string[] | undefined): readonly string[] | undefined => {
    if (!ids || ids.length === 0) return undefined;
    return [...new Set(ids)].sort();
  };
  const organizationIds = normalizedIds(scope.organizationIds);
  const applicationIds = normalizedIds(scope.applicationIds);
  const stageIds = normalizedIds(scope.stageIds);
  const tagIds = normalizedIds(scope.tagIds);
  return {
    ...(organizationIds && { organizationIds }),
    ...(applicationIds && { applicationIds }),
    ...(stageIds && { stageIds }),
    ...(tagIds && { tagIds }),
  };
}

export function useDashboardMetrics(scope: DashboardMetricsScope = {}, enabled = true): UseDashboardMetricsResult {
  const scopeKey = useMemo(() => JSON.stringify(canonicalScope(scope ?? {})), [scope]);
  const scopeBody = useMemo(() => JSON.parse(scopeKey) as DashboardMetricsScope, [scopeKey]);
  const [summaryAttempt, setSummaryAttempt] = useState(0);
  const [heavyAttempt, setHeavyAttempt] = useState(0);
  const summaryRequestKey = useMemo(() => JSON.stringify([scopeKey, summaryAttempt]), [scopeKey, summaryAttempt]);
  const summaryGeneration = useRef(0);
  const activeSummaryIdentity = useRef<SummaryRequestIdentity | null>(null);
  const summaryDataRef = useRef<DashboardMetricsResponse | null>(null);
  const pendingHeavyRef = useRef<DashboardMetricsResponse | null>(null);
  const [status, setStatus] = useState<DashboardMetricsStatus>('loading');
  const [data, setData] = useState<DashboardMetricsResponse | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [heavyLoading, setHeavyLoading] = useState(false);
  const [heavyError, setHeavyError] = useState<Error | null>(null);

  const fetchHeavy = useCallback(
    (
      identity: SummaryRequestIdentity,
      summaryData: DashboardMetricsResponse | null,
      signal: AbortSignal,
      isCancelled: () => boolean
    ) =>
      axios
        .post<DashboardMetricsResponse>(
          getDashboardMetricsUrl(),
          { ...scopeBody, includeHeavyMetrics: true },
          { signal }
        )
        .then(({ data: heavyData }) => {
          if (isCancelled() || activeSummaryIdentity.current !== identity) return;
          const summary = summaryData ?? summaryDataRef.current;
          if (summary) {
            setData(mergeMetrics(summary, heavyData));
            setHeavyLoading(false);
          } else {
            pendingHeavyRef.current = heavyData;
          }
        })
        .catch((err) => {
          if (isCancelled() || axios.isCancel?.(err)) return;
          setHeavyError(err instanceof Error ? err : new Error(String(err)));
          setHeavyLoading(false);
        }),
    [scopeBody]
  );

  useEffect(() => {
    if (!enabled) {
      activeSummaryIdentity.current = null;
      summaryDataRef.current = null;
      pendingHeavyRef.current = null;
      setHeavyLoading(false);
      setHeavyError(null);
      return;
    }

    let cancelled = false;
    const summaryController = new AbortController();
    const heavyController = new AbortController();
    const identity = {
      requestKey: summaryRequestKey,
      generation: summaryGeneration.current + 1,
    };
    summaryGeneration.current = identity.generation;
    activeSummaryIdentity.current = identity;
    summaryDataRef.current = null;
    pendingHeavyRef.current = null;
    setStatus('loading');
    setError(null);
    setHeavyLoading(true);
    setHeavyError(null);

    axios
      .post<DashboardMetricsResponse>(
        getDashboardMetricsUrl(),
        { ...scopeBody, includeHeavyMetrics: false },
        { signal: summaryController.signal }
      )
      .then(({ data: fastData }) => {
        if (cancelled || activeSummaryIdentity.current !== identity) return;
        summaryDataRef.current = fastData;
        const pendingHeavy = pendingHeavyRef.current;
        if (pendingHeavy) {
          pendingHeavyRef.current = null;
          setData(mergeMetrics(fastData, pendingHeavy));
          setHeavyLoading(false);
        } else {
          setData(fastData);
        }
        setStatus('ready');
      })
      .catch((err) => {
        if (cancelled || axios.isCancel?.(err)) return;
        const e = err instanceof Error ? err : new Error(String(err));
        setError(e);
        setStatus(statusCodeOf(err) === 409 ? 'not-ready' : 'error');
        setHeavyLoading(false);
        heavyController.abort();
      });

    fetchHeavy(identity, null, heavyController.signal, () => cancelled);

    return () => {
      cancelled = true;
      if (activeSummaryIdentity.current === identity) activeSummaryIdentity.current = null;
      summaryController.abort();
      heavyController.abort();
    };
  }, [enabled, scopeBody, summaryRequestKey, fetchHeavy]);

  useEffect(() => {
    if (!enabled || heavyAttempt === 0) return;
    const identity = activeSummaryIdentity.current;
    const summaryData = summaryDataRef.current;
    if (identity == null || summaryData == null) return;

    let cancelled = false;
    const controller = new AbortController();
    setHeavyLoading(true);
    setHeavyError(null);

    fetchHeavy(identity, summaryData, controller.signal, () => cancelled);

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [enabled, heavyAttempt, scopeBody, fetchHeavy]);

  const retry = useCallback(() => {
    setHeavyAttempt(0);
    setSummaryAttempt((a) => a + 1);
  }, []);

  const retryHeavy = useCallback(() => {
    // Always bump; the heavy effect no-ops when the summary identity was superseded, so a stale
    // Retry click cannot attach heavy work to the wrong summary generation.
    setHeavyAttempt((a) => a + 1);
  }, []);

  return { status, data, error, heavyLoading, heavyError, retry, retryHeavy };
}
