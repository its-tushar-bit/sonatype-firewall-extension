/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';

export type EstateComponentPagedPage<TRow> = {
  readonly rows: ReadonlyArray<TRow>;
  readonly total: number;
  readonly hasNextPage: boolean;
};

export type UseEstateComponentPagedTabOptions<TRow> = {
  readonly componentHash: string;
  /** Short endpoint label for console diagnostics (e.g. components/usage/applications). */
  readonly endpointLabel: string;
  readonly fetchPage: (
    pageIndex: number,
    signal: AbortSignal,
  ) => Promise<EstateComponentPagedPage<TRow>>;
  readonly loadErrorMessage: string;
};

export type UseEstateComponentPagedTabResult<TRow> = {
  /** True only for the initial empty load (keeps prior rows visible while paging). */
  readonly loading: boolean;
  readonly error: string | null;
  readonly rows: ReadonlyArray<TRow>;
  readonly total: number;
  readonly hasNextPage: boolean;
  /** 0-based API page. */
  readonly page: number;
  readonly setPage: (pageIndex: number) => void;
  readonly onRetry: () => void;
};

/**
 * Shared paged fetch for estate Component Detail list tabs (Violations / Applications).
 * Hash-keyed + page-sized only — never materializes the estate client-side.
 */
export function useEstateComponentPagedTab<TRow>(
  options: UseEstateComponentPagedTabOptions<TRow>,
): UseEstateComponentPagedTabResult<TRow> {
  const { componentHash, endpointLabel, fetchPage, loadErrorMessage } = options;
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [rows, setRows] = useState<ReadonlyArray<TRow>>([]);
  const [total, setTotal] = useState(0);
  const [hasNextPage, setHasNextPage] = useState(false);
  const [page, setPage] = useState(0);
  const [trackedHash, setTrackedHash] = useState(componentHash);
  const [retryToken, setRetryToken] = useState(0);

  // Reset pagination synchronously when the reviewed component changes so a prior
  // page index cannot fetch an empty page for the next hash.
  if (componentHash !== trackedHash) {
    setTrackedHash(componentHash);
    setPage(0);
  }

  const load = useCallback(
    async (pageIndex: number, signal: AbortSignal): Promise<void> => {
      if (!componentHash) {
        setStatus('error');
        return;
      }
      setStatus('loading');
      try {
        const data = await fetchPage(pageIndex, signal);
        if (signal.aborted) return;
        setRows(data.rows);
        setTotal(data.total);
        setHasNextPage(data.hasNextPage);
        setStatus('ready');
      } catch (err) {
        if (axios.isCancel(err) || signal.aborted) return;
        console.error(`Failed to load ${endpointLabel}`, { componentHash, err });
        setStatus('error');
      }
    },
    [componentHash, endpointLabel, fetchPage],
  );

  useEffect(() => {
    const controller = new AbortController();
    void load(page, controller.signal);
    return () => controller.abort();
  }, [load, page, retryToken]);

  return {
    loading: status === 'loading' && rows.length === 0,
    error: status === 'error' ? loadErrorMessage : null,
    rows,
    total,
    hasNextPage,
    page,
    setPage,
    onRetry: () => {
      // Retry from page 0 so a mid-list failure cannot leave the user stuck on an empty page
      // after the estate shrinks (e.g. waivers applied while paging).
      setPage(0);
      setRetryToken((token) => token + 1);
    },
  };
}
