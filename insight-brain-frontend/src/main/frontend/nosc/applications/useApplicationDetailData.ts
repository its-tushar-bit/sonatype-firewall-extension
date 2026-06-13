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
  fetchApplicationReports,
  reset,
  resetPolicyThreats,
  resetRawReport,
  selectApplicationPolicyThreatsState,
  selectApplicationRawReportState,
  selectApplicationReportsState,
} from './applicationDetailSlice';
import {
  ApiApplicationReport,
  PolicyThreatsResponse,
  RawReportResponse,
} from './applicationDetailTypes';

export interface UseApplicationDetailDataArgs {
  /** Internal application id from `GET /rest/application/{publicId}` — gates
   *  the per-stage reports fetch. Undefined until the app metadata resolves. */
  readonly applicationInternalId: string | undefined;
  /** Route publicId — gates (with scanId) the policythreats + raw fetches. */
  readonly publicId: string;
  /** Scan id parsed from the latest report — null until reports resolve / when
   *  the app has never been scanned. */
  readonly scanId: string | null;
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
 * Orchestrates the three Application Detail fetches through Redux (CLM-39709,
 * review #7). Mirrors `usePreviewNewestRisksData`: dispatch each thunk when its
 * inputs are present and its status is idle; expose a retry per fetch.
 *
 * Dependency chain:
 *   - reports needs `applicationInternalId`.
 *   - policyThreats + rawReport need `publicId` + `scanId`; they clear back to
 *     idle whenever there is no scanId (e.g. an app that's never been scanned),
 *     preserving the original component's semantics.
 *
 * When the application changes (publicId / internal id), the whole slice is
 * reset and the fetches re-run so a different app never shows stale data.
 */
export function useApplicationDetailData({
  applicationInternalId,
  publicId,
  scanId,
}: UseApplicationDetailDataArgs): UseApplicationDetailDataResult {
  const dispatch = useDispatch();
  const reportsState = useSelector(selectApplicationReportsState);
  const policyState = useSelector(selectApplicationPolicyThreatsState);
  const rawState = useSelector(selectApplicationRawReportState);

  // Reset all fetches when the user navigates to a different application so the
  // previous app's data doesn't flash before the new fetches resolve. Skipped
  // on first mount (no previous key to compare against).
  const appKeyRef = useRef<string | undefined>(undefined);
  useEffect(() => {
    const key = `${publicId}|${applicationInternalId ?? ''}`;
    if (appKeyRef.current !== undefined && appKeyRef.current !== key) {
      dispatch(reset());
    }
    appKeyRef.current = key;
  }, [dispatch, publicId, applicationInternalId]);

  // Each fetch is dispatched at most ONCE per distinct input key. Tracking the
  // attempted key in a ref (rather than relying solely on status === 'idle')
  // means status/render churn — or a spurious reset — can never re-trigger an
  // already-attempted fetch, so a report that errors (e.g. a 404) does not
  // re-fire in a loop. A genuine input change (new app / new scanId) changes
  // the key and re-fetches; an explicit Retry re-dispatches directly.

  // reports — gated on the internal id.
  const reportsKeyRef = useRef<string | null>(null);
  useEffect(() => {
    if (!applicationInternalId) return;
    if (reportsState.status === 'idle' && reportsKeyRef.current !== applicationInternalId) {
      reportsKeyRef.current = applicationInternalId;
      dispatch(fetchApplicationReports({ applicationInternalId }));
    }
  }, [dispatch, applicationInternalId, reportsState.status]);

  // policythreats — gated on publicId + scanId; clear to idle when no scanId.
  const policyKeyRef = useRef<string | null>(null);
  useEffect(() => {
    if (!publicId || !scanId) {
      policyKeyRef.current = null;
      if (policyState.status !== 'idle') {
        dispatch(resetPolicyThreats());
      }
      return;
    }
    const key = `${publicId}|${scanId}`;
    if (policyState.status === 'idle' && policyKeyRef.current !== key) {
      policyKeyRef.current = key;
      dispatch(fetchApplicationPolicyThreats({ publicId, scanId }));
    }
  }, [dispatch, publicId, scanId, policyState.status]);

  // raw report — same scanId dependency as policythreats; runs in parallel.
  const rawKeyRef = useRef<string | null>(null);
  useEffect(() => {
    if (!publicId || !scanId) {
      rawKeyRef.current = null;
      if (rawState.status !== 'idle') {
        dispatch(resetRawReport());
      }
      return;
    }
    const key = `${publicId}|${scanId}`;
    if (rawState.status === 'idle' && rawKeyRef.current !== key) {
      rawKeyRef.current = key;
      dispatch(fetchApplicationRawReport({ publicId, scanId }));
    }
  }, [dispatch, publicId, scanId, rawState.status]);

  const retryReports = useCallback(() => {
    if (applicationInternalId && reportsState.status !== 'loading') {
      dispatch(fetchApplicationReports({ applicationInternalId }));
    }
  }, [dispatch, applicationInternalId, reportsState.status]);

  const retryPolicy = useCallback(() => {
    if (publicId && scanId && policyState.status !== 'loading') {
      dispatch(fetchApplicationPolicyThreats({ publicId, scanId }));
    }
  }, [dispatch, publicId, scanId, policyState.status]);

  const retryRaw = useCallback(() => {
    if (publicId && scanId && rawState.status !== 'loading') {
      dispatch(fetchApplicationRawReport({ publicId, scanId }));
    }
  }, [dispatch, publicId, scanId, rawState.status]);

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
