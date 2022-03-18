/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectPolicyMonitoringSlice = createSelector(selectOrgsAndPoliciesSlice, prop('policyMonitoring'));
export const selectPolicyMonitoringLoading = createSelector(selectPolicyMonitoringSlice, prop('loading'));
export const selectPolicyMonitoringLoadError = createSelector(selectPolicyMonitoringSlice, prop('loadError'));
export const selectPolicyMonitoringSubmitError = createSelector(selectPolicyMonitoringSlice, prop('submitError'));
export const selectPoliciesByOwner = createSelector(selectPolicyMonitoringSlice, prop('policiesByOwner'));
export const selectPolicyMonitoringByOwner = createSelector(
  selectPolicyMonitoringSlice,
  prop('policyMonitoringByOwner')
);
export const selectPolicyMonitoringOriginalStage = createSelector(selectPolicyMonitoringSlice, prop('originalStage'));
export const selectPolicyMonitoringMonitoredStage = createSelector(selectPolicyMonitoringSlice, prop('monitoredStage'));
export const selectIsMonitoringSupported = createSelector(selectPolicyMonitoringSlice, prop('isMonitoringSupported'));
export const selectPolicyMonitoringStages = createSelector(selectPolicyMonitoringSlice, prop('stages'));
export const selectPolicyMonitoringActionStages = createSelector(selectPolicyMonitoringSlice, prop('actionStages'));
export const selectIsGrandfatheringSupported = createSelector(
  selectPolicyMonitoringSlice,
  prop('isGrandfatheringSupported')
);
export const selectPolicyMonitoringOwnerName = createSelector(
  selectPolicyMonitoringByOwner,
  (policyMonitoringByOwner = []) => {
    return policyMonitoringByOwner[0]?.ownerName;
  }
);
export const selectGrandfatheringStatusMessage = createSelector(
  selectPolicyMonitoringSlice,
  prop('grandfatheringStatusMessage')
);
