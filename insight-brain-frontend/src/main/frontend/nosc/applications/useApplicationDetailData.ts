/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  ApplicationDetailFetchStatus,
  fetchApplicationPolicyThreats,
  fetchApplicationRawReport,
  loadApplicationDetail,
  reset,
  selectApplicationPolicyThreatsState,
  selectApplicationRawReportState,
  selectApplicationReportsState,
} from './applicationDetailSlice';
import { ApiApplicationReport, PolicyThreatsResponse, RawReportResponse } from './applicationDetailTypes';
import { extractScanId, pickLatestReport } from './applicationDetailUtils';

export interface UseApplicationDetailDataArgs {
  /** Internal application id from `GET /rest/application/{publicId}`. */
  readonly applicationInternalId: string | undefined;
  /** Route publicId — passed through to the orchestrating thunk. */
  readonly publicId: string;
}

export interface UseApplicationDetailDataResult {
  readonly reports: ReadonlyArray<ApiApplicationReport> | null;
  readonly reportsStatus: ApplicationDetailFetchStatus;
  readonly policyThreats: PolicyThreatsResponse | null;
  readonly policyStatus: ApplicationDetailFetchStatus;
  readonly rawReport: RawReportResponse | null;
  readonly rawStatus: ApplicationDetailFetchStatus;
  readonly retryReports: () => void;
  readonly retryPolicy: () => void;
  readonly retryRaw: () => void;
}

/**
 * Dispatches the single `loadApplicationDetail` thunk when the application
 * changes (CLM-40901). Tab child routes can call this hook directly or read
 * from {@link applicationDetailSelectors} via `useSelector`.
 */
export function useApplicationDetailData({
  applicationInternalId,
  publicId,
}: UseApplicationDetailDataArgs): UseApplicationDetailDataResult {
  const dispatch = useDispatch();
  const reportsState = useSelector(selectApplicationReportsState);
  const policyState = useSelector(selectApplicationPolicyThreatsState);
  const rawState = useSelector(selectApplicationRawReportState);

  // Only clear slice data when the *identity* of the loaded application changes.
  // A bare `reset()` on every effect run (e.g. a parent re-render handing a new
  // `applicationInternalId` reference, or a publicId object identity change)
  // would blank good data to skeletons before the refetch resolves — the same
  // flash the detail slice's `activeKey` guard avoids. Tracking the previously
  // loaded identity keeps in-flight data on screen across benign re-runs.
  const loadedIdentityRef = useRef<string | null>(null);
  /** Prevents StrictMode / re-render double-dispatch for the same application identity. */
  const loadInitiatedRef = useRef<string | null>(null);

  useEffect(() => {
    if (!applicationInternalId) return;
    const identity = `${applicationInternalId}|${publicId}`;
    if (loadedIdentityRef.current !== identity) {
      dispatch(reset());
      loadedIdentityRef.current = identity;
      loadInitiatedRef.current = null;
    }
    if (loadInitiatedRef.current === identity) return;
    loadInitiatedRef.current = identity;
    void dispatch(loadApplicationDetail({ applicationInternalId, publicId }));
  }, [dispatch, applicationInternalId, publicId]);

  // Retry the whole chain, not just reports: reports gate the scanId that
  // policyThreats + rawReport depend on, and the orchestrating thunk re-runs
  // those downstream fetches once reports resolve. Dispatching only
  // fetchApplicationReports would leave policyThreats/rawReport stuck at idle.
  const retryReports = useCallback(() => {
    if (!applicationInternalId || reportsState.status === 'loading') return;
    void dispatch(loadApplicationDetail({ applicationInternalId, publicId }));
  }, [dispatch, applicationInternalId, publicId, reportsState.status]);

  const retryPolicy = useCallback(() => {
    const latest = pickLatestReport(reportsState.data ?? []);
    const scanId = latest ? extractScanId(latest) : null;
    if (!scanId) {
      if (!applicationInternalId || reportsState.status === 'loading') return;
      void dispatch(loadApplicationDetail({ applicationInternalId, publicId }));
      return;
    }
    if (policyState.status === 'loading') return;
    void dispatch(fetchApplicationPolicyThreats({ publicId, scanId }));
  }, [dispatch, applicationInternalId, publicId, policyState.status, reportsState.data, reportsState.status]);

  const retryRaw = useCallback(() => {
    const latest = pickLatestReport(reportsState.data ?? []);
    const scanId = latest ? extractScanId(latest) : null;
    if (!scanId) {
      if (!applicationInternalId || reportsState.status === 'loading') return;
      void dispatch(loadApplicationDetail({ applicationInternalId, publicId }));
      return;
    }
    if (rawState.status === 'loading') return;
    void dispatch(fetchApplicationRawReport({ publicId, scanId }));
  }, [dispatch, applicationInternalId, publicId, rawState.status, reportsState.data, reportsState.status]);

  return {
    reports: reportsState.data,
    reportsStatus: reportsState.status,
    policyThreats: policyState.data,
    policyStatus: policyState.status,
    rawReport: rawState.data,
    rawStatus: rawState.status,
    retryReports,
    retryPolicy,
    retryRaw,
  };
}
