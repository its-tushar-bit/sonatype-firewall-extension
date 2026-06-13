/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import {
  getWaiversAndAutoWaiversUrl,
  getWaiversUrl,
  getWaiverDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import type {
  PolicyWaiverDTO,
  PolicyWaiverDetailDTO,
  WaiversListResponse,
} from './waiverTypes';

/**
 * Phase 1 / CLM-39545 (P1-F7d): waiver-data hooks shared by Nexus One waiver
 * UIs.
 *
 * `useWaiversList` POSTs to /rest/dashboard/policy/policyWaivers (the same
 * endpoint Classic uses) with an optional filter. Filter is intentionally a
 * narrow subset of `createDashboardDataRequestPayload` — Phase 1 only cares
 * about scoping by application (for the per-app tab in ApplicationDetail)
 * and about page size. Sort and full filter UI are Phase-2.
 *
 * `useWaiverDetail` GETs /api/v2/policyWaivers/{ownerType}/{ownerId}/{waiverId}
 * for the detail page. We accept ownerType from the URL even though the
 * Classic page silently maps `root_organization → organization` — instead
 * of replicating that mapping here, the WaiverDetailPage normalizes the
 * URL segment before passing it in (single canonical place).
 */

export interface UseWaiversListOptions {
  applicationInternalId?: string;
  pageSize?: number;
  page?: number;
  includeAutoWaivers?: boolean;
}

export interface UseWaiversListResult {
  loading: boolean;
  error: string | null;
  waivers: ReadonlyArray<PolicyWaiverDTO>;
  hasNextPage: boolean;
  refetch: () => void;
}

const DEFAULT_PAGE_SIZE = 100;

export function useWaiversList(options: UseWaiversListOptions = {}): UseWaiversListResult {
  const {
    applicationInternalId,
    pageSize = DEFAULT_PAGE_SIZE,
    page = 0,
    includeAutoWaivers = true,
  } = options;

  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [waivers, setWaivers] = useState<ReadonlyArray<PolicyWaiverDTO>>([]);
  const [hasNextPage, setHasNextPage] = useState<boolean>(false);
  const [reloadToken, setReloadToken] = useState<number>(0);

  const refetch = useCallback(() => setReloadToken((t) => t + 1), []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    const url = includeAutoWaivers ? getWaiversAndAutoWaiversUrl() : getWaiversUrl();

    const body: Record<string, unknown> = { pageSize, page };
    if (applicationInternalId) {
      body.applicationIds = [applicationInternalId];
    }

    axios
      .post<WaiversListResponse>(url, body)
      .then((res) => {
        if (cancelled) return;
        setWaivers(res.data.dashboardResults || []);
        setHasNextPage(Boolean(res.data.hasNextPage));
      })
      .catch((err) => {
        if (cancelled) return;
        setError(extractAxiosMessage(err));
        setWaivers([]);
        setHasNextPage(false);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [applicationInternalId, pageSize, page, includeAutoWaivers, reloadToken]);

  return { loading, error, waivers, hasNextPage, refetch };
}

export interface UseWaiverDetailResult {
  loading: boolean;
  error: string | null;
  waiver: PolicyWaiverDetailDTO | null;
  refetch: () => void;
}

export function useWaiverDetail(
  ownerType: string | null,
  ownerId: string | null,
  waiverId: string | null
): UseWaiverDetailResult {
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [waiver, setWaiver] = useState<PolicyWaiverDetailDTO | null>(null);
  const [reloadToken, setReloadToken] = useState<number>(0);

  const refetch = useCallback(() => setReloadToken((t) => t + 1), []);

  useEffect(() => {
    if (!ownerType || !ownerId || !waiverId) {
      setLoading(false);
      setError('Missing waiver identifier');
      setWaiver(null);
      return undefined;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    axios
      .get<PolicyWaiverDetailDTO>(getWaiverDetailsUrl(ownerType, ownerId, waiverId))
      .then((res) => {
        if (cancelled) return;
        setWaiver(res.data);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(extractAxiosMessage(err));
        setWaiver(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [ownerType, ownerId, waiverId, reloadToken]);

  return { loading, error, waiver, refetch };
}

function extractAxiosMessage(err: unknown): string {
  if (!err || typeof err !== 'object') return 'Unknown error';
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const e: any = err;
  if (typeof e.response?.data === 'string' && e.response.data.length < 240) {
    return e.response.data;
  }
  if (typeof e.response?.data?.message === 'string') return e.response.data.message;
  if (typeof e.message === 'string') return e.message;
  return 'Unknown error';
}
