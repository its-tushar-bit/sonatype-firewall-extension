/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop, pick } from 'ramda';
import { compose, createSelector } from '@reduxjs/toolkit';
import { groupAndSortByFormat } from './firewallOnboardingUtils';

/** @type {(state: any) => import('./types').FirewallOnboardingState} */
export const selectFirewallOnboardingSlice = prop('firewallOnboarding');
export const selectCurrentStep = createSelector(selectFirewallOnboardingSlice, prop('currentStep'));
export const selectShowWelcomeScreen = createSelector(selectFirewallOnboardingSlice, prop('showWelcomeScreen'));
export const selectUnconfiguredRepoManagersList = createSelector(
  selectFirewallOnboardingSlice,
  prop('unconfiguredRepoManagers')
);
export const selectUnconfiguredRepoManager = createSelector(
  selectUnconfiguredRepoManagersList,
  (unconfiguredRepoManagers) => {
    // return the first repo manager if there is any until we
    // can give support for multiples unconfigured repo managers
    return unconfiguredRepoManagers.repoManagers?.[0] ?? null;
  }
);

export const selectRepositories = createSelector(selectFirewallOnboardingSlice, prop('repositories'));
export const selectRepositoriesList = createSelector(selectRepositories, prop('list'));
export const selectSupportedFormats = createSelector(selectFirewallOnboardingSlice, prop('supportedFormats'));

export const selectProxyRepositoriesList = createSelector(selectRepositoriesList, (list) => {
  return list?.filter((repo) => repo.repositoryType === 'proxy') || [];
});

export const selectHostedRepositoriesList = createSelector(selectRepositoriesList, (list) => {
  return list?.filter((repo) => repo.repositoryType === 'hosted') || [];
});

export const selectRepositoriesByType = createSelector(
  selectRepositoriesList,
  selectSupportedFormats,
  (_, repositoryType) => repositoryType,
  (list, supportedFormats, repositoryType) => {
    const repositoriesWithType = list?.filter((repo) => repo.repositoryType === repositoryType) ?? [];
    return groupAndSortByFormat(repositoriesWithType, supportedFormats ?? []);
  }
);

export const selectTotalEnabledRepositoriesByTypeAndProp = createSelector(
  selectRepositoriesList,
  selectSupportedFormats,
  (_, type) => type,
  (_, __, propName) => propName,
  (list, supportedFormats, type, propName = 'quarantineEnabled') => {
    return list?.reduce((count, repo) => {
      if (repo[propName] && repo.repositoryType === type && supportedFormats.includes(repo.format)) {
        return count + 1;
      }
      return count;
    }, 0);
  }
);

export const selectProtectionRules = createSelector(
  selectFirewallOnboardingSlice,
  compose(pick(['supplyChainAttacksProtectionEnabled', 'namespaceConfusionProtectionEnabled']), prop('protectionRules'))
);

export const selectLaunchFirewall = createSelector(selectFirewallOnboardingSlice, prop('launchFirewall'));
