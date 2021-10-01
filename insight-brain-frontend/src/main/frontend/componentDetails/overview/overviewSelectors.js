/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import {
  selectSelectedComponent,
  selectApplicationReportMetaData,
} from '../../applicationReport/applicationReportSelectors';
import { selectRouterCurrentParams } from '../../reduxUiRouter/routerSelectors';
import { getComponentVersionComparisonInfo } from '../componentDetailsUtils';

export const selectComponentDetailsOverviewSlice = prop('componentDetailsOverview');

export const selectVersionExplorerData = createSelector(
  selectComponentDetailsOverviewSlice,
  prop('versionExplorerData')
);

export const selectComponentDetailsRequestData = createSelector(
  selectSelectedComponent,
  selectApplicationReportMetaData,
  selectRouterCurrentParams,
  (component, metadata, params) => ({
    clientType: 'ci',
    ownerType: 'application',
    ownerId: params.publicId,
    matchState: component.matchState,
    proprietary: component.proprietary,
    identificationSource: component.identificationSource,
    componentIdentifier: stringifyComponentIdentifier(component.componentIdentifier, component.matchState),
    hash: component.hash,
    scanId: params.scanId,
  })
);

function stringifyComponentIdentifier(componentIdentifier, matchState) {
  const coordinates = !matchState || matchState === 'unknown' ? null : componentIdentifier.coordinates;
  return coordinates ? JSON.stringify({ format: componentIdentifier.format, coordinates }) : null;
}

export const selectVersionExplorerRequestData = createSelector(
  selectSelectedComponent,
  selectApplicationReportMetaData,
  selectComponentDetailsRequestData,
  (component, metadata, data) => ({
    ...data,
    stageId: metadata.stageId,
    dependencyType: component.derivedDependencyType,
  })
);

export const selectRemediationData = createSelector(
  selectSelectedComponent,
  selectApplicationReportMetaData,
  (component, metadata) => ({
    currentVersion:
      !component.matchState || component.matchState === 'unknown'
        ? 'unknown'
        : component.componentIdentifier.coordinates.version,
    stageId: metadata.stageId,
  })
);

export const selectInnerSourceProducerData = createSelector(
  selectComponentDetailsOverviewSlice,
  prop('innerSourceProducerData')
);

export const selectInnerSourceProducerUrl = createSelector(selectInnerSourceProducerData, prop('reportUrl'));

export const selectShowInnerSourceProducerReportModal = createSelector(
  selectInnerSourceProducerData,
  prop('showInnerSourceProducerReportModal')
);

export const selectInsufficientPermission = createSelector(
  selectInnerSourceProducerData,
  prop('insufficientPermission')
);

export const selectShowInsufficientPermissionsModal = createSelector(
  selectInnerSourceProducerData,
  prop('showInnerSourcePermissionsModal')
);

export const selectLatestInnerSourceComponentVersion = createSelector(
  selectInnerSourceProducerData,
  prop('latestInnerSourceComponentVersion')
);

export const selectCurrentVersionDetails = createSelector(selectVersionExplorerData, prop('currentVersionDetails'));

export const selectSelectedVersionDetails = createSelector(
  selectComponentDetailsOverviewSlice,
  // This is just a placeholder for actual state, to be implemented as part of CLM-19434
  prop('selectedVersionDetails')
);

export const selectCurrentVersionComparisonData = createSelector(
  selectCurrentVersionDetails,
  getComponentVersionComparisonInfo
);

export const selectSelectedVersionComparisonData = createSelector(
  selectSelectedVersionDetails,
  getComponentVersionComparisonInfo
);
