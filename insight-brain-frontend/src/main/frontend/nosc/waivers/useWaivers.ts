/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchNoscWaiverDetail,
  fetchNoscWaiversList,
  resetNoscWaiverDetail,
  selectNoscWaiverDetailState,
  selectNoscWaiversListHasEntry,
  selectNoscWaiversListState,
  waiverDetailKey,
  waiversListKey,
  type WaiversListRequest,
} from './noscWaiversSlice';
import type { PolicyWaiverDTO, PolicyWaiverDetailDTO } from './waiverTypes';

/**
 * Nexus One waiver-data hooks (CLM-39545 / CLM-40901). Thin wrappers over
 * {@link noscWaiversSlice} — list and detail fetches run through Redux thunks
 * instead of component-local `useState` + `axios`.
 */

export type UseWaiversListOptions = WaiversListRequest;

export interface UseWaiversListResult {
  /** True only on the first fetch for this listKey (no cached entry yet). */
  loading: boolean;
  /** True during a background refetch while stale waivers remain visible. */
  refreshing: boolean;
  error: string | null;
  waivers: ReadonlyArray<PolicyWaiverDTO>;
  hasNextPage: boolean;
  refetch: () => void;
}

export function useWaiversList(options: UseWaiversListOptions = {}): UseWaiversListResult {
  const dispatch = useDispatch();
  const {
    applicationInternalId,
    pageSize,
    page,
    includeAutoWaivers = true,
  } = options;

  const listKey = useMemo(
    () =>
      waiversListKey({
        applicationInternalId,
        pageSize,
        page,
        includeAutoWaivers,
      }),
    [applicationInternalId, pageSize, page, includeAutoWaivers],
  );

  const listState = useSelector((state) => selectNoscWaiversListState(state, listKey));
  const hasEntry = useSelector((state) => selectNoscWaiversListHasEntry(state, listKey));

  // `listKey` is a deterministic function of exactly these option fields, so the
  // primitives below are the only inputs that can change the request. Dispatching
  // the request object directly (rather than via a ref) keeps the effect and
  // `refetch` in sync without indirection.
  const scopedToApplication = 'applicationInternalId' in options;
  const canFetch = !scopedToApplication || !!applicationInternalId;

  const fetchList = useCallback(() => {
    if (!canFetch) return;
    void dispatch(
      fetchNoscWaiversList({
        applicationInternalId,
        pageSize,
        page,
        includeAutoWaivers,
      }),
    );
  }, [dispatch, canFetch, applicationInternalId, pageSize, page, includeAutoWaivers]);

  useEffect(() => {
    if (!canFetch) return;
    fetchList();
    // Stale-while-revalidate: cached entries render immediately on navigate-back,
    // then refetch in the background. This gives instant page loads when switching
    // between apps while still keeping data fresh. The `pending` reducer sets
    // `status:'loading'` on the existing entry, so UI can show a subtle refresh
    // indicator if desired — but the stale waivers remain visible during refetch.
    //
    // No cleanup reset: the cache is keyed by `listKey`, so a stale entry is
    // harmless and key-scoped. Deleting on unmount would clobber a co-mounted
    // consumer sharing the same key (the collision the keyed cache exists to prevent).
  }, [canFetch, fetchList]);

  const isLoading = listState.status === 'loading';
  const waiverCount = listState.waivers.length;

  return {
    // No cache entry yet, or fetching a new page key with no rows to show.
    loading: !hasEntry || (isLoading && waiverCount === 0),
    // Stale-while-revalidate: keep prior rows visible during background refetch.
    refreshing: hasEntry && isLoading && waiverCount > 0,
    error: listState.error,
    waivers: listState.waivers,
    hasNextPage: listState.hasNextPage,
    refetch: fetchList,
  };
}

export interface UseWaiverDetailResult {
  loading: boolean;
  error: string | null;
  waiver: PolicyWaiverDetailDTO | null;
  refetch: () => void;
}

/**
 * Hook for fetching a single waiver's detail. Uses a single global `detail` slot
 * (unlike `listsByKey` for list fetches) because only one detail page mounts at
 * a time. The `activeKey` guard prevents stale responses from overwriting newer
 * data. If a second detail consumer is added in the future (e.g., a dashboard
 * preview), the slice should be updated to key the detail slot by `waiverDetailKey`
 * for symmetrical multi-consumer support.
 */
export function useWaiverDetail(
  ownerType: string | null,
  ownerId: string | null,
  waiverId: string | null,
  isAutoWaiver = false,
): UseWaiverDetailResult {
  const dispatch = useDispatch();
  const detailState = useSelector(selectNoscWaiverDetailState);

  const missingIdentifier = !ownerType || !ownerId || !waiverId;
  const activeDetailKey = waiverDetailKey(ownerType, ownerId, waiverId);
  const detailMatchesRequest =
    !missingIdentifier && detailState.activeKey === activeDetailKey;

  const refetch = useCallback(() => {
    if (missingIdentifier) return;
    dispatch(resetNoscWaiverDetail());
    void dispatch(fetchNoscWaiverDetail({ ownerType, ownerId, waiverId, isAutoWaiver }));
  }, [dispatch, missingIdentifier, ownerType, ownerId, waiverId, isAutoWaiver]);

  useEffect(() => {
    refetch();
  }, [refetch]);

  if (missingIdentifier) {
    return {
      loading: false,
      error: 'Missing waiver identifier',
      waiver: null,
      refetch,
    };
  }

  return {
    loading: detailState.status === 'loading' || !detailMatchesRequest,
    error: detailMatchesRequest ? detailState.error : null,
    waiver: detailMatchesRequest ? detailState.waiver : null,
    refetch,
  };
}
