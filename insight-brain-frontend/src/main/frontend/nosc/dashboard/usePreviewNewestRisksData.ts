/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchPreviewNewestRisks,
  selectPreviewNewestRisksState,
  type NewestRiskViolationRow,
  type PreviewNewestRisksStatus,
} from './previewDashboardNewestRisksSlice';

export interface UsePreviewNewestRisksDataResult {
  readonly status: PreviewNewestRisksStatus;
  readonly violations: ReadonlyArray<NewestRiskViolationRow>;
  readonly error: Error | null;
  readonly retry: () => void;
}

/**
 * Ensures a single shared `newestRisks` fetch backs all Overview tiles that
 * aggregate violation rows. RTK deduplicates concurrent dispatches.
 */
export function usePreviewNewestRisksData(): UsePreviewNewestRisksDataResult {
  const dispatch = useDispatch();
  const { status, violations, error: errorMessage } = useSelector(selectPreviewNewestRisksState);

  useEffect(() => {
    if (status === 'idle') {
      dispatch(fetchPreviewNewestRisks());
    }
  }, [dispatch, status]);

  const retry = useCallback(() => {
    dispatch(fetchPreviewNewestRisks());
  }, [dispatch]);

  const error = errorMessage ? new Error(errorMessage) : null;

  return { status, violations, error, retry };
}
