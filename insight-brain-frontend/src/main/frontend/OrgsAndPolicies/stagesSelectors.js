/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { path, prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import { createInheritOrNoMonitorOption } from 'MainRoot/OrgsAndPolicies/utility/monitoredStageUtil';

export const selectStagesSlice = createSelector(selectOrgsAndPoliciesSlice, prop('stages'));

export const selectCliStages = createSelector(selectStagesSlice, prop('cli'));
export const selectCliStageTypes = createSelector(selectCliStages, prop('stageTypes'));
export const selectCliStagesLoadError = createSelector(selectCliStages, prop('error'));
export const selectCliStagesIsLoading = createSelector(selectCliStages, prop('loading'));

export const selectActionStages = createSelector(selectStagesSlice, prop('action'));
export const selectActionStageTypes = createSelector(selectActionStages, prop('stageTypes'));
export const selectActionStagesLoadError = createSelector(selectActionStages, prop('error'));
export const selectActionStagesIsLoading = createSelector(selectActionStages, prop('loading'));

export const selectDashboardStages = createSelector(selectStagesSlice, prop('dashboard'));
export const selectDashboardStageTypes = createSelector(selectDashboardStages, prop('stageTypes'));
export const selectDashboardStagesLoadError = createSelector(selectDashboardStages, prop('error'));
export const selectDashboardStagesIsLoading = createSelector(selectDashboardStages, prop('loading'));

export const selectSbomStages = createSelector(selectStagesSlice, prop('sbom'));
export const selectSbomStageTypes = createSelector(selectSbomStages, prop('stageTypes'));
export const selectSbomStagesLoadError = createSelector(selectSbomStages, prop('error'));
export const selectSbomStagesIsLoading = createSelector(selectSbomStages, prop('loading'));

export const selectCliStagesWithInheritOrNoMonitorOption = createSelector(
  selectCliStageTypes,
  path(['orgsAndPolicies', 'policyMonitoring', 'policyMonitoringByOwner']),
  (cliStages, policyMonitoringByOwner) => {
    if (!cliStages || !policyMonitoringByOwner) return undefined;

    return [createInheritOrNoMonitorOption(policyMonitoringByOwner, cliStages), ...cliStages];
  }
);
