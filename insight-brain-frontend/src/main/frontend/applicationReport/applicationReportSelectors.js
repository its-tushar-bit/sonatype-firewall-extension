/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectRouterCurrentParams } from '../reduxUiRouter/routerSelectors';

export const selectApplicationReportSlice = prop('applicationReport');
export const selectSelectedReport = createSelector(selectApplicationReportSlice, prop('selectedReport'));
export const selectApplicationReportMetaData = createSelector(selectApplicationReportSlice, prop('metadata'));

export const selectAllComponentsList = createSelector(selectSelectedReport, prop('allEntries'));
export const selectDisplayedComponentList = createSelector(selectSelectedReport, prop('displayedEntries'));
export const selectSelectedComponent = createSelector(
  selectRouterCurrentParams,
  selectDisplayedComponentList,
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
export const selectLoadError = createSelector(selectApplicationReportSlice, prop('loadError'));
export const selectIsLoading = createSelector(selectApplicationReportSlice, ({ pendingLoads }) => !!pendingLoads.size);

export const selectReportParameters = createSelector(selectApplicationReportSlice, prop('reportParameters'));
