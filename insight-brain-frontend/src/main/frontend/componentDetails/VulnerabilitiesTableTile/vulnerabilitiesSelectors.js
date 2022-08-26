/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectSelectedComponent } from 'MainRoot/applicationReport/applicationReportSelectors';
import { selectSelectedComponent as selectFirewallSelectedComponent } from 'MainRoot/firewall/firewallComponentDetailsPage/overview/firewallOverviewSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';

export const selectVulnerabilitiesSlice = prop('componentDetailsVulnerabilities');

export const selectVulnerabilitiesSortedSlice = createSelector(selectVulnerabilitiesSlice, (vulnerabilitiesSlice) => {
  if (vulnerabilitiesSlice.vulnerabilities.data) {
    const sortedVulnerabilities = [...vulnerabilitiesSlice.vulnerabilities.data];
    sortedVulnerabilities.sort((a, b) => b.severity - a.severity);
    return { ...vulnerabilitiesSlice.vulnerabilities, data: sortedVulnerabilities };
  }
  return vulnerabilitiesSlice.vulnerabilities;
});

export const selectShowVulnerabilityDetailPopover = createSelector(
  selectVulnerabilitiesSlice,
  prop('showVulnerabilityDetailPopover')
);

export const selectVulnerabilityDetailsSlice = createSelector(selectVulnerabilitiesSlice, prop('vulnerabilityDetails'));

export const selectVulnerabityRefId = createSelector(selectVulnerabilitiesSlice, prop('selectedRefId'));

export const selectVulnerabilitiesRequestData = createSelector(
  selectSelectedComponent,
  selectRouterCurrentParams,
  (component, params) => ({
    clientType: 'ci',
    ownerType: 'application',
    ownerId: params.publicId,
    componentIdentifier: stringifyComponentIdentifier(component.componentIdentifier, component.matchState),
    hash: component.hash,
    identificationSource: component.identificationSource,
    scanId: params.scanId,
  })
);

export const selectFirewallVulnerabilitiesRequestData = createSelector(
  selectFirewallSelectedComponent,
  selectRouterCurrentParams,
  (component, params) => ({
    clientType: 'ci',
    ownerType: 'repository',
    ownerId: params.repositoryId,
    componentIdentifier: stringifyComponentIdentifier(component.componentIdentifier, component.matchState),
    hash: component.hash,
    identificationSource: component.identificationSource,
    scanId: params.scanId,
  })
);

export const selectFirewallPolicyThreatsRequestData = createSelector(
  selectFirewallSelectedComponent,
  selectRouterCurrentParams,
  (component, routerParams) => ({
    pathname: component.pathname,
    repositoryId: routerParams.ownerId,
  })
);

export const selectSelectedVulnerability = createSelector(
  selectVulnerabilitiesSortedSlice,
  selectVulnerabityRefId,
  (vulnerabilities, selectedRefId) =>
    !selectedRefId ? null : vulnerabilities.data.find((vulnerability) => vulnerability.refId === selectedRefId)
);

export const selectVulnerabilityOverrideFormData = createSelector(
  selectVulnerabilitiesSlice,
  prop('vulnerabilitySecurityOverride')
);
