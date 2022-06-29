/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { compose, ifElse, isNil, pick, prop, propOr } from 'ramda';
import { selectIsRootOrganization, selectRouterSlice } from '../reduxUiRouter/routerSelectors';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';

export const selectPolicyViolationGrandfatheringSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  prop('policyViolationGrandfathering')
);

export const selectLoadError = createSelector(selectPolicyViolationGrandfatheringSlice, prop('loadError'));
export const selectLoading = createSelector(selectPolicyViolationGrandfatheringSlice, prop('loading'));
export const selectPolicyViolationGrandfathering = createSelector(
  selectPolicyViolationGrandfatheringSlice,
  prop('data')
);

export const selectPolicyViolationGrandfatheringConfig = createSelector(
  selectPolicyViolationGrandfathering,
  selectIsRootOrganization,
  (policyViolationGrandfathering, isRootOrg) => {
    if (!policyViolationGrandfathering) return {};
    const config = pick(
      ['inheritedFromOrganizationName', 'allowOverride', 'allowChange'],
      policyViolationGrandfathering
    );

    // The returned data contains the calculated value of the "enabled" flag based on the
    // current settings for the owner and its parents. For enabled values that are being
    // inherited, we need to adjust accordingly and null out the enabled value for this
    // particular owner (since the value is not coming from this owner but a parent).
    config.enabled = ifElse(
      compose(isNil, prop('inheritedFromOrganizationName')),
      prop('enabled'),
      () => null
    )(policyViolationGrandfathering);
    config.calculatedEnabled = policyViolationGrandfathering.enabled;

    // For the root organization, values that have not yet been set in the backend are treated
    // as false (as there's nowhere else to inherit from), so nulls need to be set to false.
    if (isRootOrg) {
      config.enabled = propOr(false, 'enabled')(config);
      config.calculatedEnabled = propOr(false, 'calculatedEnabled')(config);
    }
    return config;
  }
);

export const selectCalculatedEnabled = createSelector(
  selectPolicyViolationGrandfatheringConfig,
  prop('calculatedEnabled')
);

export const selectGrandfatheringStatusMessage = createSelector(
  selectPolicyViolationGrandfatheringConfig,
  (configuration) => {
    let msg = '';
    if (configuration.inheritedFromOrganizationName) {
      msg += `Inherit from ${configuration.inheritedFromOrganizationName} (`;
    }
    msg += 'Grandfathering is ' + (configuration.calculatedEnabled ? 'enabled' : 'disabled');
    if (configuration.inheritedFromOrganizationName) {
      msg += ')';
    }
    return msg;
  }
);

export const selectGrandfatheringLinkParams = createSelector(selectRouterSlice, (router) =>
  deriveEditRoute(router, 'violation-grandfathering-policy')
);
