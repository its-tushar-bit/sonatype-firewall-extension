/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import axios, { AxiosRequestConfig } from 'axios';

/**
 * Generic Dashboard-tile state machine (CLM-39641 / P1-F6).
 *
 * Each tile on the Preview Dashboard fetches its own data independently.
 * useTile owns the loading / ready / error / retry transitions so each tile
 * component stays focused on rendering. Per the F6 Epic, no tile blocks any
 * other: each call to useTile is its own AbortController-backed fetch.
 *
 * Phase-1 scope (per docs/superpowers/CLM-39545/epics/CLM-39641-P1-F6-dashboard.md
 * §9.1) deliberately does NOT include cache, refetch-on-focus, or stale-
 * while-revalidate. Those would be added in P1.5 if the tile count grows.
 */
export type TileStatus = 'loading' | 'ready' | 'error' | 'not-ready';

export interface UseTileResult<T> {
  status: TileStatus;
  data: T | null;
  error: Error | null;
  retry: () => void;
}

export interface UseTileOptions {
  readonly method?: 'get' | 'post';
  /** Request body for POST; also serialized into the refetch dependency key. */
  readonly body?: unknown;
  /** When false, no request is issued (status stays `loading` until enabled). */
  readonly enabled?: boolean;
  /** Maps HTTP status codes from failed responses to a terminal tile status. */
  readonly mapErrorStatus?: (statusCode: number | undefined) => Extract<TileStatus, 'error' | 'not-ready'>;
}

function statusCodeOf(err: unknown): number | undefined {
  if (axios.isAxiosError(err)) return err.response?.status;
  return undefined;
}

export function useTile<T>(
  url: string,
  requestConfig?: AxiosRequestConfig,
  options?: UseTileOptions,
): UseTileResult<T> {
  const method = options?.method ?? 'get';
  const enabled = options?.enabled ?? true;
  const bodyKey = useMemo(() => JSON.stringify(options?.body ?? null), [options?.body]);
  const [status, setStatus] = useState<TileStatus>('loading');
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!enabled) return;

    let cancelled = false;
    const controller = new AbortController();
    setStatus('loading');
    setError(null);

    const request =
      method === 'post'
        ? axios.post<T>(url, options?.body ?? {}, { ...requestConfig, signal: controller.signal })
        : axios.get<T>(url, { ...requestConfig, signal: controller.signal });

    request
      .then((response) => {
        if (cancelled) return;
        setData(response.data);
        setStatus('ready');
      })
      .catch((err) => {
        if (cancelled || axios.isCancel?.(err)) return;
        const e = err instanceof Error ? err : new Error(String(err));
        setError(e);
        const mapped = options?.mapErrorStatus?.(statusCodeOf(err)) ?? 'error';
        setStatus(mapped);
      });

    return () => {
      cancelled = true;
      controller.abort();
    };
    // requestConfig intentionally excluded — callers should memoize or pass
    // stable shape; including it would cause refetch on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [url, attempt, method, bodyKey, enabled]);

  const retry = useCallback(() => {
    setAttempt((a) => a + 1);
  }, []);

  return { status, data, error, retry };
}
