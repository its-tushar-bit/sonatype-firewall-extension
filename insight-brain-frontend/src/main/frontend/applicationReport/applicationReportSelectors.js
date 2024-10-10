/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop, isNil, isEmpty } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectRouterCurrentParams } from '../reduxUiRouter/routerSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectIsLatestReportForStageRequestPending } from 'MainRoot/applicationReport/latestReportForStageSelectors';

export const selectApplicationReportSlice = prop('applicationReport');
export const selectExactValueFilters = createSelector(selectApplicationReportSlice, prop('exactValueFilters'));
export const selectShowFilterPopover = createSelector(selectApplicationReportSlice, prop('showFilterPopover'));
export const selectSelectedReport = createSelector(selectApplicationReportSlice, prop('selectedReport'));
export const selectApplicationReportMetaData = createSelector(selectApplicationReportSlice, prop('metadata'));
export const selectIsPolicyTypeFilterEnabled = createSelector(
  selectApplicationReportSlice,
  prop('policyTypeFilterEnabled')
);

export const selectAllComponentsList = createSelector(selectSelectedReport, prop('allEntries'));
export const selectDisplayedComponentList = createSelector(selectSelectedReport, prop('displayedEntries'));
export const selectSelectedComponent = createSelector(
  selectRouterCurrentParams,
  selectAllComponentsList,
  ({ hash }, components = []) => components.find((component) => component.hash === hash)
);

export const selectAggregatedComponentsList = createSelector(selectSelectedReport, prop('aggregatedEntries'));
export const selectSelectedComponentInAggregatedList = createSelector(
  selectRouterCurrentParams,
  selectAggregatedComponentsList,
  ({ hash }, components = []) => components.find((component) => component.hash === hash)
);
export const selectSelectedComponentIndexInAggregatedList = createSelector(
  selectSelectedComponentInAggregatedList,
  selectAggregatedComponentsList,
  (component, list = []) => list.indexOf(component)
);
export const selectHasUnscannedComponents = createSelector(
  selectApplicationReportSlice,
  prop('reportHasUnscannedComponents')
);
export const selectUnscannedComponents = createSelector(selectApplicationReportSlice, prop('unscannedComponents'));
export const selectLoadError = createSelector(selectApplicationReportSlice, prop('loadError'));
export const selectIsLoading = createSelector(selectApplicationReportSlice, ({ pendingLoads }) => !!pendingLoads.size);

export const selectIsDependenciesLoading = createSelector(selectApplicationReportSlice, ({ pendingLoads }) =>
  pendingLoads.has('policy')
);

export const selectReportParameters = createSelector(selectApplicationReportSlice, prop('reportParameters'));

export const selectDependencyTreeData = createSelector(selectApplicationReportSlice, prop('dependencyTree'));
export const selectDependencyTreeIsAvailable = createSelector(selectDependencyTreeData, (tree) => !isNilOrEmpty(tree));

export const selectDependencyTreeUnavailableMessage = createSelector(selectDependencyTreeData, (tree) => {
  if (isNil(tree)) {
    return 'Please re-scan the application';
  }

  if (isEmpty(tree)) {
    return 'Dependency tree not available';
  }

  return '';
});

export const selectDependencyTreeIsOldReport = createSelector(selectDependencyTreeData, isNil);

export const selectDependencyTreeRouterParams = createSelector(
  selectApplicationReportSlice,
  prop('dependencyTreePageRouterParams')
);
export const selectDependencyTreeSearchTerm = createSelector(
  selectApplicationReportSlice,
  prop('dependencyTreeSearchTerm')
);
export const selectDisplayedDependencyTree = createSelector(
  selectApplicationReportSlice,
  prop('displayedDependencyTree')
);

export const selectIsAggregated = createSelector(selectApplicationReportSlice, prop('aggregate'));
export const selectSubstringFilters = createSelector(selectApplicationReportSlice, prop('substringFilters'));
export const selectSortConfiguration = createSelector(selectApplicationReportSlice, prop('sortConfiguration'));

export const selectReportStageId = (state) => selectApplicationReportSlice(state)?.metadata?.stageId;

export const selectApplicationReportLoading = createSelector(
  [selectApplicationReportSlice, selectIsLatestReportForStageRequestPending],
  (appReportRequests, latestReportRequestLoading) => {
    return (
      (!appReportRequests.loadError && (!!appReportRequests.pendingLoads.size || !appReportRequests.metadata)) ||
      latestReportRequestLoading
    );
  }
);

export const selectWaivedViolationCountFromAggregatedComponentList = createSelector(
  selectAggregatedComponentsList,
  (list) => (isNilOrEmpty(list) ? 0 : list.reduce((acc, component) => acc + component.waivedViolations, 0))
);
