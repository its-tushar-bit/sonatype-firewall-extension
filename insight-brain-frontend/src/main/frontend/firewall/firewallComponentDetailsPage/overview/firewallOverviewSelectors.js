/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { selectRouterCurrentParams } from '../../../reduxUiRouter/routerSelectors';
import { stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';
import { selectSelectedVersionDetailsByVersionId } from 'MainRoot/componentDetails/overview/overviewSelectors';
import {
  selectFirewallComponentDetailsPage,
  selectFirewallComponentDetailsPageRouteParams,
} from '../../firewallSelectors';

export const selectSelectedComponent = createSelector(
  selectRouterCurrentParams,
  selectFirewallComponentDetailsPage,
  (routeParams, firewallComponentDetailsPage) => {
    return {
      ...firewallComponentDetailsPage.componentDetails,
      routeParams: routeParams.proprietary === 'true' ? true : false,
      scanId: routeParams.scanId,
      ownerId: routeParams.repositoryId,
    };
  }
);

export const selectComponentDetailsRequestData = createSelector(
  selectSelectedComponent,
  selectRouterCurrentParams,
  (component, params) => {
    return {
      clientType: 'ci',
      ownerType: 'repository',
      ownerId: params.repositoryId,
      matchState: component.matchState,
      proprietary: component.proprietary ?? false,
      componentIdentifier: stringifyComponentIdentifier(component.componentIdentifier, component.matchState),
      hash: component.hash,
      scanId: params.scanId,
    };
  }
);

export const selectComponentDetailsSelectedRequestData = createSelector(
  selectSelectedVersionDetailsByVersionId,
  selectRouterCurrentParams,
  (component, params) => ({
    clientType: 'ci',
    ownerType: 'repository',
    ownerId: params.repositoryId,
    matchState: component.matchState,
    componentIdentifier: stringifyComponentIdentifier(component.componentIdentifier, component.matchState),
    hash: undefined,
    scanId: params.scanId,
  })
);

export const selectVersionExplorerRequestData = createSelector(selectComponentDetailsRequestData, (data) => ({
  ...data,
  stageId: 'proxy',
}));

export const selectComponentDetailsFromParams = createSelector(
  selectFirewallComponentDetailsPageRouteParams,
  (params) => {
    return {
      clientType: 'ci',
      ownerType: 'repository',
      ownerId: params.repositoryId,
      matchState: params.matchState,
      proprietary: params.proprietary,
      componentIdentifier: params.componentIdentifier,
      hash: params.componentHash,
      stageId: 'proxy',
    };
  }
);
