/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { selectFirewallComponentDetailsPage } from '../../firewallSelectors';

export const selectPolicyViolations = createSelector(
  selectFirewallComponentDetailsPage,
  (firewallComponentDetailsPage) => firewallComponentDetailsPage.policyViolations
);

export const selectSecurityPolicyViolations = createSelector(selectPolicyViolations, (violations) =>
  violations.filter((violation) => violation.policyThreatCategory === 'SECURITY')
);
