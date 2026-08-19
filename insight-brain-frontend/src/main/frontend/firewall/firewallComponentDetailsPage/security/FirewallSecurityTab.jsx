/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectSecurityPolicyViolations } from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/firewallPolicyViolationsSelectors.js';
import FirewallPolicyViolationsTile from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTile';
import VulnerabilitiesTableTile from 'MainRoot/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTableTile';
import { selectVulnerabilitiesSortedSlice } from 'MainRoot/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSelectors';
import {
  selectFirewallComponentDetailsPage,
  selectFirewallComponentDetailsPageRouteParams,
} from 'MainRoot/firewall/firewallSelectors';
import { loadComponentDetails } from 'MainRoot/firewall/firewallActions';
import { actions as vulnerabilitiesActions } from 'MainRoot/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSlice';

export default function FirewallSecurityTab() {
  const dispatch = useDispatch();
  const violations = useSelector(selectSecurityPolicyViolations);
  const vulnerabilities = useSelector(selectVulnerabilitiesSortedSlice),
    { isLoadingComponentDetails, componentDetailsError } = useSelector(selectFirewallComponentDetailsPage);
  const routeParams = useSelector(selectFirewallComponentDetailsPageRouteParams);

  return (
    <>
      <FirewallPolicyViolationsTile violations={violations} showProxyState title="Security Violations" />
      <VulnerabilitiesTableTile
        {...{
          vulnerabilities,
          isLoadingComponentDetails,
          componentDetailsLoadError: componentDetailsError,
          loadComponentDetails: () => dispatch(loadComponentDetails(routeParams)),
          loadVulnerabilities: () => dispatch(vulnerabilitiesActions.loadFirewallVulnerabilities()),
          toggleVulnerabilityPopoverWithEffects: (vulnerabilityRefId) =>
            dispatch(vulnerabilitiesActions.toggleVulnerabilityPopoverWithEffects(vulnerabilityRefId)),
        }}
      />
    </>
  );
}
