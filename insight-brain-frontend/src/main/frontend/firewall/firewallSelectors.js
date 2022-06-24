/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop, pickAll } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectFirewall = prop('firewall');
export const selectFirewallCDP = createSelector(selectFirewall, prop('cdp'));
export const selectFirewallCDPRouteParams = createSelector(
  selectRouterCurrentParams,
  pickAll([
    'repositoryId',
    'componentIdentifier',
    'componentHash',
    'matchState',
    'proprietary',
    'identificationSource',
    'scanId',
    'tabId',
  ])
);
export const currentFirewallCDPComponentVersion = createSelector(
  selectFirewallCDP,
  (cdp) => cdp.componentDetails.componentIdentifier.coordinates.version
);
