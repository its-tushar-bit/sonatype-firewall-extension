/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

import {
  selectActionStageTypes,
  selectCliStagesWithInheritOrNoMonitorOption,
  selectSbomStageTypes,
} from './stagesSelectors';

import {
  createInheritOrNoMonitorOption,
  getMonitoredStage,
  getMonitoredStageFromAncestors,
  getSbomManagerMonitoredStageDetails,
} from 'MainRoot/OrgsAndPolicies/utility/monitoredStageUtil';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectRouterSlice, selectIsSbomManager, selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';

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
export const selectIsLegacyViolationSupported = createSelector(
  selectPolicyMonitoringSlice,
  prop('isLegacyViolationSupported')
);
export const selectLegacyViolationStatusMessage = createSelector(
  selectPolicyMonitoringSlice,
  prop('legacyViolationStatusMessage')
);

export const selectOriginalMonitoredStageFromFetchedData = createSelector(
  selectCliStagesWithInheritOrNoMonitorOption,
  selectSbomStageTypes,
  selectPolicyMonitoringByOwner,
  selectIsSbomManager,
  (cliStagesWithInheritOrNoMonitorOption, sbomStages, policyMonitoringByOwner, isSbomManager) => {
    if (!cliStagesWithInheritOrNoMonitorOption || !policyMonitoringByOwner) return undefined;

    if (!isSbomManager) {
      return getMonitoredStage(policyMonitoringByOwner[0].policyMonitorings, cliStagesWithInheritOrNoMonitorOption);
    } else {
      return getMonitoredStage(policyMonitoringByOwner[0].policyMonitorings, sbomStages);
    }
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
    const monitoredStage = getMonitoredStage(policyMonitoringByOwner[0].policyMonitorings, actionStages);
    const inheritOrNoMonitorOption = createInheritOrNoMonitorOption(policyMonitoringByOwner, actionStages);
    return monitoredStage || inheritOrNoMonitorOption;
  }
);

export const selectMonitoredStageFromSbomStages = createSelector(
  selectSbomStageTypes,
  selectPolicyMonitoringByOwner,
  (sbomStages, policyMonitoringByOwner) => {
    if (!sbomStages || !policyMonitoringByOwner) return undefined;
    return getMonitoredStageFromAncestors(policyMonitoringByOwner, sbomStages);
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

export const selectContinuousMonitoringIsDirty = createSelector(selectPolicyMonitoringSlice, prop('isDirty'));
export const selectContinuousMonitoringSubmitMaskState = createSelector(
  selectPolicyMonitoringSlice,
  prop('submitMaskState')
);

export const selectPolicyMonitoringLinkParams = createSelector(selectRouterSlice, (router) =>
  deriveEditRoute(router, 'monitor-policy')
);

export const selectSelectedStageLabelForSbomManager = createSelector(
  selectPolicyMonitoringByOwner,
  selectSbomStageTypes,
  selectIsApplication,
  (policyMonitoringByOwner, sbomStages, isAnApp) => {
    return getSbomManagerMonitoredStageDetails(policyMonitoringByOwner, sbomStages, isAnApp);
  }
);
