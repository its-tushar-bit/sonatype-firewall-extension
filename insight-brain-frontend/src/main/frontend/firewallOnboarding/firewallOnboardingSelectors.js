/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { groupRepositoriesByTypes, groupAndSortByFormat } from './firewallOnboardingUtils';

export const selectFirewallOnboardingSlice = prop('firewallOnboarding');
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

export const selectRepositories = createSelector(selectFirewallOnboardingSlice, prop('repositories'));
export const selectRepositoriesList = createSelector(selectRepositories, prop('list'));
export const selectSupportedFormats = createSelector(selectFirewallOnboardingSlice, prop('supportedFormats'));

export const selectRepositoriesByType = createSelector(
  selectRepositoriesList,
  selectSupportedFormats,
  (_, repositoryType) => repositoryType,
  (list, formats, repositoryType) => {
    const repositoriesByType = groupRepositoriesByTypes(list);
    for (let type in repositoriesByType) {
      if (repositoriesByType.hasOwnProperty(type)) {
        repositoriesByType[type] = groupAndSortByFormat(repositoriesByType[type], formats);
      }
    }
    return repositoriesByType[repositoryType] ?? [];
  }
);

export const selectTotalEnabledRepositoriesByTypeAndProp = createSelector(
  selectRepositoriesList,
  (_, type) => type,
  (_, __, propName) => propName,
  (list, type, propName = 'quarantineEnabled') => {
    return list.reduce((count, repo) => {
      if (repo[propName] && repo.repositoryType === type) {
        return count + 1;
      }
      return count;
    }, 0);
  }
);
