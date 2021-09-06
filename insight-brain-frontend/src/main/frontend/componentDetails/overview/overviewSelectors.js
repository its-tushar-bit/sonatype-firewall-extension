/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectSelectedComponent } from '../../applicationReport/applicationReportSelectors';
import { selectRouterCurrentParams } from '../../reduxUiRouter/routerSelectors';

export const selectComponentDetailsOverviewSlice = prop('componentDetailsOverview');

export const selectComponentDetailsOverviewVersionExplorerSlice = createSelector(
  selectComponentDetailsOverviewSlice,
  prop('graphExplorerData')
);

export const selectVersionExplorerRequestData = createSelector(
  selectSelectedComponent,
  selectRouterCurrentParams,
  (component, params) => ({
    clientType: 'ci',
    ownerType: 'application',
    ownerId: params.publicId,
    matchState: component.matchState,
    proprietary: component.proprietary,
    identificationSource: component.identificationSource,
    componentIdentifier: {
      componentType:
        !component.matchState || component.matchState === 'unknown' ? '' : component.componentIdentifier.format,
      coordinates: component.componentIdentifier.coordinates,
    },
    hash: component.hash,
    scanId: params.scanId,
  })
);
