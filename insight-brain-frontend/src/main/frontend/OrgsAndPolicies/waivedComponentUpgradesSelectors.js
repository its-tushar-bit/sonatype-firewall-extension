/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';

export const selectWaivedComponentUpgrades = prop('waivedComponentUpgrades');

export const selectConfiguredStage = createSelector(selectWaivedComponentUpgrades, prop('configuredStage'));

export const selectUpgradeMonitoringLinkParams = createSelector(selectRouterSlice, (router) =>
  deriveEditRoute(router, 'monitor-component-upgrades')
);
