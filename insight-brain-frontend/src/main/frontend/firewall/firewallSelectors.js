/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop, pickAll } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectFirewall = prop('firewall');
export const selectRoiMetrics = createSelector(selectFirewall, prop('roiMetricsState'));
export const selectFirewallComponentDetailsPage = createSelector(selectFirewall, (firewall) => {
  return firewall.componentDetailsPage;
});

export const selectFirewallPolicyViolations = createSelector(
  selectFirewallComponentDetailsPage,
  prop('policyViolations')
);

export const selectFirewallComponentDetailsPageRouteParams = createSelector(
  selectRouterCurrentParams,
  pickAll([
    'repositoryId',
    'componentIdentifier',
    'componentHash',
    'matchState',
    'tabId',
    'pathname',
    'componentDisplayName',
  ])
);

export const currentFirewallComponentDetailsPageComponentVersion = createSelector(
  selectFirewallComponentDetailsPage,
  (componentDetailsPage) => componentDetailsPage.componentDetails.componentIdentifier.coordinates.version
);

export const selectFirewallComponentDetailsIsLoading = createSelector(
  selectFirewallComponentDetailsPage,
  prop('isLoadingComponentDetails')
);

export const selectFirewallComponentName = createSelector(selectFirewallComponentDetailsPage, ({ componentDetails }) =>
  componentDetails?.displayName?.parts?.reduce((fullName, part) => fullName + part.value, '')
);

export const selectFirewallPolicyName = createSelector(selectFirewallComponentDetailsPage, prop('firewallPolicyName'));

export const selectFirewallThreatLevel = createSelector(
  selectFirewallComponentDetailsPage,
  prop('firewallThreatLevel')
);

export const selectFirewallIsLoading = createSelector(
  selectFirewallComponentDetailsPage,
  prop('firewallViolationDetailsLoading')
);

export const selectFirewallLoadingError = createSelector(
  selectFirewallComponentDetailsPage,
  prop('firewallViolationDetailsError')
);

export const selectHasPermissionToAddWaivers = createSelector(
  selectFirewallComponentDetailsPage,
  prop('hasWaivePermission')
);

export const selectComponentDetails = createSelector(selectFirewallComponentDetailsPage, prop('componentDetails'));

export const selectAddWaiverFromFirewallRedirectionProps = createSelector(
  selectFirewallComponentDetailsPageRouteParams,
  selectComponentDetails,
  (firewallComponentDetailsPageRouteParams, componentDetails) => ({
    ...firewallComponentDetailsPageRouteParams,
    identificationSource: componentDetails?.identificationSource,
  })
);
