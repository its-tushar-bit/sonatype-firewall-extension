/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

import { REDUCER_NAME } from './firewallOnboardingSlice';

export const selectFirewallOnboardingSlice = prop(REDUCER_NAME);
export const selectCurrentStep = createSelector(selectFirewallOnboardingSlice, prop('currentStep'));
export const selectSelectedRepositories = createSelector(selectFirewallOnboardingSlice, prop('selectedRepositories'));
export const selectUnconfiguredRepoManagersList = createSelector(
  selectFirewallOnboardingSlice,
  prop('unconfiguredRepoManagers')
);
export const selectUnconfiguredRepoManager = createSelector(
  selectUnconfiguredRepoManagersList,
  (unconfiguredRepoManagers) => {
    // return the first repo manager if there is any until we
    // can give support for multiples unconfigured repo managers
    return unconfiguredRepoManagers.repoManagers[0] ?? null;
  }
);
