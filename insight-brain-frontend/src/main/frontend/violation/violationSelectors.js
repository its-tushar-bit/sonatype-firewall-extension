/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { hasPath, prop } from 'ramda';

export const selectViolationSlice = prop('violation');
const selectTransitiveViolationsSlice = prop('transitiveViolations');

export const selectViolationLoadingError = createSelector(selectViolationSlice, prop('violationDetailsError'));
export const selectViolationIsLoading = createSelector(selectViolationSlice, prop('loading'));
export const selectViolationDetails = createSelector(selectViolationSlice, prop('violationDetails'));
export const selectViolationPolicyName = createSelector(selectViolationDetails, prop('policyName'));
export const selectViolationThreatLevel = createSelector(selectViolationDetails, prop('threatLevel'));
export const selectPolicyExists = createSelector(selectViolationDetails, hasPath(['policyOwner', 'ownerId']));

export const selectTransitiveViolationsData = createSelector(
  selectTransitiveViolationsSlice,
  (transitiveViolationsSlice) => transitiveViolationsSlice?.componentTransitivePolicyViolations?.data?.violations
);

export const selectApplicableWaivers = createSelector(selectViolationSlice, ({ activeWaivers, expiredWaivers }) => {
  return { activeWaivers, expiredWaivers };
});

export const selectHasPermissionForAppWaivers = createSelector(
  selectViolationSlice,
  prop('hasPermissionForAppWaivers')
);
