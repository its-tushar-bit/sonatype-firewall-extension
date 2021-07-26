/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import {
  selectPolicyViolation,
  selectLoadError,
  selectIsLoading,
} from '../applicationReport/applicationReportSelectors';

export const selectPolicyViolationError = createSelector(
  selectPolicyViolation,
  selectLoadError,
  selectIsLoading,
  (policyViolation, loadError, isLoading) => {
    const policyViolationNotFoundError = !policyViolation && !isLoading ? 'Error getting policy violation.' : '';
    return loadError || policyViolationNotFoundError;
  }
);
