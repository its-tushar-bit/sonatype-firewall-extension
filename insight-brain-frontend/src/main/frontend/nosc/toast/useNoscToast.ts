/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';

export type NoscToastType = 'success' | 'error' | 'info' | 'warning';

/**
 * Dispatch NOSC (and Classic-shared) toasts via the existing Redux toast slice.
 * Nexus One renders them with {@link NoscToastHost}; Classic uses NxToastContainer.
 */
export function useNoscToast(): {
  readonly showToast: (type: NoscToastType, message: string) => void;
  readonly success: (message: string) => void;
  readonly error: (message: string) => void;
} {
  const dispatch = useDispatch();

  const showToast = useCallback(
    (type: NoscToastType, message: string): void => {
      dispatch(toastActions.addToast({ type, message }));
    },
    [dispatch],
  );

  const success = useCallback(
    (message: string): void => showToast('success', message),
    [showToast],
  );

  const error = useCallback(
    (message: string): void => showToast('error', message),
    [showToast],
  );

  return { showToast, success, error };
}
