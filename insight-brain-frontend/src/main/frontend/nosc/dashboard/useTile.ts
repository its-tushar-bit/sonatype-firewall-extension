/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
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
export type TileStatus = 'loading' | 'ready' | 'error';

export interface UseTileResult<T> {
  status: TileStatus;
  data: T | null;
  error: Error | null;
  retry: () => void;
}

export function useTile<T>(url: string, requestConfig?: AxiosRequestConfig): UseTileResult<T> {
  const [status, setStatus] = useState<TileStatus>('loading');
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [attempt, setAttempt] = useState(0);
  const mountedRef = useRef(true);

  useEffect(() => {
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setStatus('loading');
    setError(null);

    axios
      .get<T>(url, { ...requestConfig, signal: controller.signal })
      .then((response) => {
        if (!mountedRef.current) return;
        setData(response.data);
        setStatus('ready');
      })
      .catch((err) => {
        if (!mountedRef.current) return;
        if (axios.isCancel?.(err)) return;
        const e = err instanceof Error ? err : new Error(String(err));
        setError(e);
        setStatus('error');
      });

    return () => controller.abort();
    // requestConfig intentionally excluded — callers should memoize or pass
    // stable shape; including it would cause refetch on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [url, attempt]);

  const retry = useCallback(() => {
    setAttempt((a) => a + 1);
  }, []);

  return { status, data, error, retry };
}
