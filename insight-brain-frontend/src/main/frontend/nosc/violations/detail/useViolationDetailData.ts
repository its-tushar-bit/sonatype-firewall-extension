/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { loadViolationDetail, reset } from 'MainRoot/nosc/violations/detail/violationDetailSlice';
import type { FetchStatus } from 'MainRoot/nosc/violations/detail/violationDetailSlice';
import {
  selectViolationDetailIdentityState,
  selectViolationDetailVulnerabilitySummaryState,
  selectViolationDetailWaiversState,
  selectViolationHasPermissionForAppWaivers,
} from 'MainRoot/nosc/violations/detail/violationDetailSelectors';
import {
  ApplicableWaiverDTO,
  ViolationDetailsDTO,
  VulnerabilitySummaryDTO,
} from 'MainRoot/nosc/violations/detail/violationDetailTypes';

export interface UseViolationDetailDataArgs {
  readonly violationId: string | undefined;
}

export interface UseViolationDetailDataResult {
  readonly identity: ViolationDetailsDTO | null;
  readonly identityStatus: FetchStatus;
  readonly identityError: string | null;
  readonly activeWaivers: ReadonlyArray<ApplicableWaiverDTO>;
  readonly expiredWaivers: ReadonlyArray<ApplicableWaiverDTO>;
  readonly waiversStatus: FetchStatus;
  readonly waiversError: string | null;
  readonly vulnerabilitySummary: VulnerabilitySummaryDTO | null;
  readonly vulnerabilitySummaryStatus: FetchStatus;
  readonly vulnerabilitySummaryError: string | null;
  readonly hasPermissionForAppWaivers: boolean | null;
  readonly retry: () => void;
}

export function useViolationDetailData({ violationId }: UseViolationDetailDataArgs): UseViolationDetailDataResult {
  const dispatch = useDispatch<any>();
  const identityState = useSelector(selectViolationDetailIdentityState);
  const waiversState = useSelector(selectViolationDetailWaiversState);
  const vulnerabilitySummaryState = useSelector(selectViolationDetailVulnerabilitySummaryState);
  const hasPermissionForAppWaivers = useSelector(selectViolationHasPermissionForAppWaivers);
  const loadInitiatedRef = useRef<string | null>(null);

  useEffect(() => {
    if (!violationId) {
      dispatch(reset());
      loadInitiatedRef.current = null;
      return;
    }

    if (loadInitiatedRef.current === violationId) return;
    loadInitiatedRef.current = violationId;
    void dispatch(loadViolationDetail(violationId));
  }, [dispatch, violationId]);

  const retry = useCallback(() => {
    if (!violationId || identityState.status === 'loading') return;
    loadInitiatedRef.current = violationId;
    void dispatch(loadViolationDetail(violationId));
  }, [dispatch, identityState.status, violationId]);

  return {
    identity: identityState.data,
    identityStatus: identityState.status,
    identityError: identityState.error,
    activeWaivers: waiversState.active,
    expiredWaivers: waiversState.expired,
    waiversStatus: waiversState.status,
    waiversError: waiversState.error,
    vulnerabilitySummary: vulnerabilitySummaryState.data,
    vulnerabilitySummaryStatus: vulnerabilitySummaryState.status,
    vulnerabilitySummaryError: vulnerabilitySummaryState.error,
    hasPermissionForAppWaivers,
    retry,
  };
}
