/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import { selectActionStageTypes, selectCliStagesWithInheritOrNoMonitorOption } from './orgsAndPoliciesStagesSelectors';
import {
  createInheritOrNoMonitorOption,
  getMonitoredStage,
} from 'MainRoot/owner.manager/utility/monitored.stage.service';

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

export const selectOriginalMonitoredStageFromFetchedData = createSelector(
  selectCliStagesWithInheritOrNoMonitorOption,
  selectPolicyMonitoringByOwner,
  (cliStagesWithInheritOrNoMonitorOption, policyMonitoringByOwner) => {
    if (!cliStagesWithInheritOrNoMonitorOption || !policyMonitoringByOwner) return undefined;

    return getMonitoredStage(policyMonitoringByOwner[0].policyMonitoring, cliStagesWithInheritOrNoMonitorOption);
  }
);

export const selectLastSavedMonitoredStage = createSelector(
  selectOriginalMonitoredStageFromFetchedData,
  selectPolicyMonitoringOriginalStage,
  (originalMonitoredStage, lastSavedMonitoredStage) => {
    return lastSavedMonitoredStage || originalMonitoredStage;
  }
);

export const selectSelectedMonitoredStage = createSelector(
  selectPolicyMonitoringMonitoredStage,
  selectLastSavedMonitoredStage,
  (selectedMonitoredStage, lastSavedMonitoredStage) => {
    return selectedMonitoredStage || lastSavedMonitoredStage;
  }
);

export const selectMonitoredStageFromActionStages = createSelector(
  selectActionStageTypes,
  selectPolicyMonitoringByOwner,
  (actionStages, policyMonitoringByOwner) => {
    if (!actionStages || !policyMonitoringByOwner) return undefined;
    const monitoredStage = getMonitoredStage(policyMonitoringByOwner[0].policyMonitoring, actionStages);
    const inheritOrNoMonitorOption = createInheritOrNoMonitorOption(policyMonitoringByOwner, actionStages);

    return monitoredStage || inheritOrNoMonitorOption;
  }
);

export const selectPoliciesByOwnerWithEnforcementActions = createSelector(
  selectActionStageTypes,
  selectPoliciesByOwner,
  (actionStages, policiesByOwner) => {
    if (!actionStages || !policiesByOwner) return undefined;

    return policiesByOwner.map((policyOwner, index) => {
      const policies = policyOwner.policies.map(function (policy) {
        const enforcementAction = {};
        actionStages.forEach((actionStage) => {
          if (policy.actions[actionStage.stageTypeId]) {
            enforcementAction[actionStage.stageTypeId] = policy.actions[actionStage.stageTypeId];
          }
        });
        return { ...policy, enforcementAction };
      });
      return { ...policyOwner, policies, inherited: index > 0 };
    });
  }
);
