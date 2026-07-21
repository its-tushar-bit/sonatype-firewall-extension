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
 * 1. Fast tier — cheap index counts + SQL waivers ({@code includeHeavyMetrics: false}).
 * 2. Heavy tier — violations / components / vulnerabilities / legal overlays.
 *
 * The grid becomes {@code ready} as soon as the fast tier returns so Applications /
 * Orgs / Policies / Waivers paint without waiting on the detailed aggregations.
 */

export type DashboardMetricsStatus = TileStatus;

export interface UseDashboardMetricsResult {
  readonly status: DashboardMetricsStatus;
  readonly data: DashboardMetricsResponse | null;
  readonly error: Error | null;
  /** True while the heavy (countDistinct) request is still in flight after fast KPIs are ready. */
  readonly heavyLoading: boolean;
  readonly heavyError: Error | null;
  readonly retry: () => void;
  readonly retryHeavy: () => void;
}

interface SummaryRequestIdentity {
  readonly requestKey: string;
  readonly generation: number;
}

interface SummarySnapshot {
  readonly identity: SummaryRequestIdentity;
  readonly data: DashboardMetricsResponse;
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
  const [status, setStatus] = useState<DashboardMetricsStatus>('loading');
  const [summarySnapshot, setSummarySnapshot] = useState<SummarySnapshot | null>(null);
  const [data, setData] = useState<DashboardMetricsResponse | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [heavyLoading, setHeavyLoading] = useState(false);
  const [heavyError, setHeavyError] = useState<Error | null>(null);

  useEffect(() => {
    if (!enabled) {
      activeSummaryIdentity.current = null;
      setHeavyLoading(false);
      setHeavyError(null);
      return;
    }

    let cancelled = false;
    const controller = new AbortController();
    const identity = {
      requestKey: summaryRequestKey,
      generation: summaryGeneration.current + 1,
    };
    summaryGeneration.current = identity.generation;
    activeSummaryIdentity.current = identity;
    setStatus('loading');
    setError(null);
    setSummarySnapshot(null);
    setHeavyLoading(false);
    setHeavyError(null);

    const url = getDashboardMetricsUrl();

    (async () => {
      try {
        const fastResponse = await axios.post<DashboardMetricsResponse>(
          url,
          { ...scopeBody, includeHeavyMetrics: false },
          { signal: controller.signal }
        );
        if (cancelled || activeSummaryIdentity.current !== identity) return;
        // Mark heavy in-flight in the same commit as the summary so the grid never renders a
        // frame where summary cards are ready but heavyLoading is still false (missing slots).
        setSummarySnapshot({ identity, data: fastResponse.data });
        setHeavyLoading(true);
        setData(fastResponse.data);
        setStatus('ready');
      } catch (err) {
        if (cancelled || axios.isCancel?.(err)) return;
        const e = err instanceof Error ? err : new Error(String(err));
        setError(e);
        setStatus(statusCodeOf(err) === 409 ? 'not-ready' : 'error');
        setHeavyLoading(false);
      }
    })();

    return () => {
      cancelled = true;
      if (activeSummaryIdentity.current === identity) activeSummaryIdentity.current = null;
      controller.abort();
    };
  }, [enabled, scopeBody, summaryRequestKey]);

  useEffect(() => {
    if (!enabled || summarySnapshot?.identity !== activeSummaryIdentity.current) return;

    let cancelled = false;
    const controller = new AbortController();
    const identity = summarySnapshot.identity;
    const summaryData = summarySnapshot.data;
    setHeavyLoading(true);
    setHeavyError(null);

    axios
      .post<DashboardMetricsResponse>(
        getDashboardMetricsUrl(),
        { ...scopeBody, includeHeavyMetrics: true },
        { signal: controller.signal }
      )
      .then(({ data: heavyData }) => {
        if (cancelled || activeSummaryIdentity.current !== identity) return;
        setData(mergeMetrics(summaryData, heavyData));
      })
      .catch((err) => {
        if (cancelled || axios.isCancel?.(err)) return;
        setHeavyError(err instanceof Error ? err : new Error(String(err)));
      })
      .finally(() => {
        if (!cancelled) setHeavyLoading(false);
      });

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [enabled, heavyAttempt, scopeBody, summaryRequestKey, summarySnapshot]);

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
