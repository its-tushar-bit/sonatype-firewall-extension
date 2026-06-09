/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { getDashboardLegalObligationsUrl } from 'MainRoot/util/CLMLocation';
import {
  isEmpty,
  isPermissionDenied,
  type LegalObligationsResponse,
} from './legalObligationsTypes';

/**
 * Phase-1.5 / CLM-39604 (S2-PR-D-2): hook backing `LegalObligationsTile`.
 *
 * Whereas the slim S1-PR7 tile reused the generic `useTile` against
 * `/rest/licenseThreatGroup/.../counts`, the ALP-aware variant needs richer
 * state — `permission-denied` and `empty` are first-class outcomes per
 * UX-F11-004 / UX-F11-005, not "ready with no rows" or "error". Lifting
 * those into the hook keeps the tile renderer trivial and lets us assert
 * each branch in isolation.
 *
 * Implementation mirrors `useTile`:
 *  - Single GET against `/rest/dashboard/legalObligations`.
 *  - AbortController-backed cancellation on unmount / retry.
 *  - No cache, no refetch-on-focus — same scope as `useTile`.
 */

export type UseLegalObligationsStatus =
  | 'loading'
  | 'ready'
  | 'error'
  | 'permission-denied'
  | 'empty';

export interface UseLegalObligationsResult {
  status: UseLegalObligationsStatus;
  /**
   * Always set when status === 'ready' to a discriminated payload whose
   * `variant` is either 'ALP' or 'TOP_LEGAL_VIOLATIONS'. `null` for any
   * other status (loading / error / permission-denied / empty).
   */
  data: LegalObligationsResponse | null;
  error: Error | null;
  retry: () => void;
}

export function useLegalObligations(): UseLegalObligationsResult {
  const [status, setStatus] = useState<UseLegalObligationsStatus>('loading');
  const [data, setData] = useState<LegalObligationsResponse | null>(null);
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
      .get<LegalObligationsResponse>(getDashboardLegalObligationsUrl(), {
        signal: controller.signal,
      })
      .then((response) => {
        if (!mountedRef.current) return;
        const payload = response.data;
        if (isPermissionDenied(payload)) {
          setData(null);
          setStatus('permission-denied');
          return;
        }
        if (isEmpty(payload)) {
          setData(null);
          setStatus('empty');
          return;
        }
        setData(payload);
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
  }, [attempt]);

  const retry = useCallback(() => {
    setAttempt((a) => a + 1);
  }, []);

  return { status, data, error, retry };
}
