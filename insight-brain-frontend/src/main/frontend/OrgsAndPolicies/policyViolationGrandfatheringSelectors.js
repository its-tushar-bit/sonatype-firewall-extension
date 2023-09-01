/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { pick, prop } from 'ramda';
import { selectIsRootOrganization, selectRouterSlice } from '../reduxUiRouter/routerSelectors';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';

export const selectPolicyViolationGrandfatheringSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  prop('policyViolationGrandfathering')
);

export const selectRoot = createSelector(selectOrgsAndPoliciesSlice, prop('root'));

export const selectLoadError = createSelector(selectPolicyViolationGrandfatheringSlice, prop('loadError'));
export const selectLoading = createSelector(selectPolicyViolationGrandfatheringSlice, prop('loading'));
export const selectPolicyViolationGrandfathering = createSelector(
  selectPolicyViolationGrandfatheringSlice,
  prop('data')
);
export const selectPolicyViolationGrandfatheringServerData = createSelector(
  selectPolicyViolationGrandfatheringSlice,
  prop('serverData')
);

export const selectPolicyViolationGrandfatheringConfig = createSelector(
  selectPolicyViolationGrandfathering,
  selectIsRootOrganization,
  selectRoot,
  (policyViolationGrandfathering, isRootOrg, root) => {
    if (!policyViolationGrandfathering) return {};
    const config = pick(
      ['inheritedFromOrganizationName', 'allowOverride', 'allowChange'],
      policyViolationGrandfathering
    );

    if (root.selectedOwner.organizationName) {
      config.organizationName = root.selectedOwner.organizationName;
    } else if (root.selectedOwner.parentOrganizationId) {
      config.organizationName = 'Root Organization';
    } else {
      config.organizationName = 'parent';
    }

    // The returned data contains the calculated value of the "enabled" flag based on the
    // current settings for the owner and its parents. For enabled values that are being
    // inherited, we need to adjust accordingly and null out the enabled value for this
    // particular owner (since the value is not coming from this owner but a parent).
    config.enabled = policyViolationGrandfathering.inheritedFromOrganizationName
      ? null
      : policyViolationGrandfathering.enabled;

    config.calculatedEnabled = policyViolationGrandfathering.enabled;

    // For the root organization, values that have not yet been set in the backend are treated
    // as false (as there's nowhere else to inherit from), so nulls need to be set to false.
    if (isRootOrg) {
      config.enabled = !!config.enabled;
      config.calculatedEnabled = !!config.calculatedEnabled;
    }
    return config;
  }
);

export const selectCalculatedEnabled = createSelector(
  selectPolicyViolationGrandfatheringConfig,
  prop('calculatedEnabled')
);

export const selectLegacyViolationsStatusMessage = createSelector(
  selectPolicyViolationGrandfatheringConfig,
  (configuration) => {
    const message = 'Legacy violations are ' + (configuration.calculatedEnabled ? 'enabled' : 'disabled');
    return configuration.inheritedFromOrganizationName
      ? message + ` (Inheriting from ${configuration.inheritedFromOrganizationName})`
      : message;
  }
);

export const selectParentLegacyViolationStatus = createSelector(
  selectPolicyViolationGrandfatheringServerData,
  (serverData) => (serverData?.enabledInParent ? 'Enabled' : 'Disabled')
);

export const selectGrandfatheringLinkParams = createSelector(selectRouterSlice, (router) =>
  deriveEditRoute(router, 'violation-grandfathering-policy')
);
