/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { getComponentVersionComparisonInfo } from 'MainRoot/componentDetails/componentDetailsUtils';

export const selectRiskRemediationSlice = prop('quarantinedReportRiskRemediation');

export const selectCurrentVersion = createSelector(selectRiskRemediationSlice, prop('currentVersion'));

export const selectVersionExplorerData = createSelector(selectRiskRemediationSlice, prop('versionExplorerData'));

export const selectSelectedVersionData = createSelector(selectRiskRemediationSlice, prop('selectedVersionData'));

export const selectSelectedVersion = createSelector(selectSelectedVersionData, prop('selectedVersion'));

export const selectCurrentVersionDetails = createSelector(selectVersionExplorerData, prop('currentVersionDetails'));

export const selectSelectedVersionDetails = createSelector(selectSelectedVersionData, prop('selectedVersionDetails'));

export const selectCurrentVersionComparisonData = createSelector(
  selectCurrentVersionDetails,
  getComponentVersionComparisonInfo
);

export const selectSelectedVersionComparisonData = createSelector(
  selectSelectedVersionDetails,
  getComponentVersionComparisonInfo
);
