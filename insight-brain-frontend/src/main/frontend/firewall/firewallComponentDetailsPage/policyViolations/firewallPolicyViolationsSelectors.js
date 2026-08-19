/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { flatten } from 'ramda';
import { selectFirewallComponentDetailsPage } from '../../firewallSelectors';

export const selectPolicyViolations = createSelector(
  selectFirewallComponentDetailsPage,
  (firewallComponentDetailsPage) => firewallComponentDetailsPage.policyViolations
);

export const selectSecurityPolicyViolations = createSelector(selectPolicyViolations, (violations) =>
  violations.filter((violation) => violation.policyThreatCategory === 'SECURITY')
);

export const selectLegalPolicyViolations = createSelector(selectPolicyViolations, (violations) =>
  violations.filter((violation) => violation.policyThreatCategory === 'LICENSE')
);

export const selectWaiversByOwner = createSelector(
  selectFirewallComponentDetailsPage,
  (componentDetailsPage) => componentDetailsPage.policyExistingWaivers?.waiversByOwner || []
);

export const selectWaivers = createSelector(selectWaiversByOwner, (items) => {
  return flatten(
    items.map((waiversWithOwner) =>
      waiversWithOwner.waivers.map((waiver) => ({
        ...waiver,
        policyWaiverId: waiver.id,
        scopeOwnerId: waiversWithOwner.ownerId,
        scopeOwnerType: waiversWithOwner.ownerType,
        scopeOwnerName: waiversWithOwner.ownerName,
      }))
    )
  );
});

export const selectDisplayName = createSelector(
  selectFirewallComponentDetailsPage,
  (componentDetailsPage) => componentDetailsPage.componentDetails.displayName
);

export const selectComponentName = createSelector(selectDisplayName, (componentName) => {
  return componentName?.parts?.reduce((prev, part) => prev + part.value, '') || '';
});

export const selectComponentNameWithoutVersion = createSelector(
  selectDisplayName,
  (displayName) => displayName?.name ?? ''
);
