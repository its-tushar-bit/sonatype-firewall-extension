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
  selectApplicationReportSlice,
} from '../../applicationReport/applicationReportSelectors';
import { selectComponentDetails } from '../../componentDetails/componentDetailsSelectors';
import { selectRouterCurrentParams } from '../../reduxUiRouter/routerSelectors';
import { getComponentVersionComparisonInfo } from '../componentDetailsUtils';
import { stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';

export const selectComponentDetailsOverviewSlice = prop('componentDetailsOverview');

export const selectExpanded = createSelector(selectComponentDetailsOverviewSlice, prop('expanded'));

export const selectVersionExplorerData = createSelector(
  selectComponentDetailsOverviewSlice,
  prop('versionExplorerData')
);

export const selectSelectedVersionData = createSelector(
  selectComponentDetailsOverviewSlice,
  prop('selectedVersionData')
);

export const selectComponentDetailsRequestData = createSelector(
  selectSelectedComponent,
  selectRouterCurrentParams,
  (component, params) => ({
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

export const selectVersionExplorerRequestData = createSelector(
  selectSelectedComponent,
  selectApplicationReportMetaData,
  selectComponentDetailsRequestData,
  (component, metadata, data) => ({
    ...data,
    stageId: metadata.stageId,
    dependencyType: component.innerSource ? 'innersource' : component.derivedDependencyType,
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
export const selectCurrentVersion = createSelector(
  selectCurrentVersionDetails,
  (details) => details?.componentIdentifier?.coordinates?.version || details?.version
);

export const selectSelectedVersionDetails = createSelector(selectSelectedVersionData, prop('selectedVersionDetails'));
export const selectAvailableVersions = createSelector(selectVersionExplorerData, prop('versions'));
export const selectSelectedVersion = createSelector(selectSelectedVersionData, prop('selectedVersion'));
export const selectSelectedVersionDetailsByVersionId = createSelector(
  selectAvailableVersions,
  selectSelectedVersion,
  (versions, selectedVersion) =>
    versions.find((version) => version.componentIdentifier?.coordinates?.version === selectedVersion)
);

export const selectCurrentVersionComparisonData = createSelector(
  selectCurrentVersionDetails,
  getComponentVersionComparisonInfo
);

export const selectSelectedVersionComparisonData = createSelector(
  selectSelectedVersionDetails,
  getComponentVersionComparisonInfo
);

export const selectComponentDetailsSelectedRequestData = createSelector(
  selectSelectedVersionDetailsByVersionId,
  selectRouterCurrentParams,
  (component, params) => ({
    clientType: 'ci',
    ownerType: 'application',
    ownerId: params.publicId,
    matchState: component.matchState,
    proprietary: component.proprietary,
    identificationSource: component.identificationSource,
    componentIdentifier: stringifyComponentIdentifier(component.componentIdentifier, component.matchState),
    hash: undefined,
    scanId: params.scanId,
  })
);

export const selectShowComponentCoordinatesPopover = createSelector(
  selectComponentDetailsOverviewSlice,
  prop('showComponentCoordinatesPopover')
);

export const selectisLoadingApplicationReportOrComponentDetails = createSelector(
  selectApplicationReportSlice,
  selectComponentDetails,
  (applicationReportSlice, componentDetails) => !!applicationReportSlice.pendingLoads.size || !componentDetails
);
