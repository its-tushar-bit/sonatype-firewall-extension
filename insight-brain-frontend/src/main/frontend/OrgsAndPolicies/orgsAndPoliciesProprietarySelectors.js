/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { length, prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectProprietarySlice = createSelector(selectOrgsAndPoliciesSlice, prop('proprietary'));

export const selectLoadError = createSelector(selectProprietarySlice, prop('loadError'));
export const selectSubmitError = createSelector(selectProprietarySlice, prop('submitError'));
export const selectIsDirty = createSelector(selectProprietarySlice, prop('isDirty'));
export const selectIsLoading = createSelector(selectProprietarySlice, prop('loading'));
export const selectLocalMatchers = createSelector(selectProprietarySlice, prop('localMatchers'));
export const selectProprietaryConfigs = createSelector(selectProprietarySlice, prop('proprietaryConfigs'));
export const selectCurrentConfigs = createSelector(selectProprietarySlice, prop('currentConfig'));

export const selectPackageMatcher = createSelector(selectProprietarySlice, prop('packageMatcher'));
export const selectRegexMatcher = createSelector(selectProprietarySlice, prop('regexMatcher'));
export const selectMatcherType = createSelector(selectProprietarySlice, prop('matcherType'));
export const selectPropietaryConfigLocalMatchersCount = createSelector(selectLocalMatchers, length);
export const selectPropietaryConfigInheritedMatchersCount = createSelector(
  selectProprietaryConfigs,
  (propietaryConfigs = []) => {
    return propietaryConfigs.reduce((counter, configOwner, index) => {
      const config = configOwner.proprietaryConfig;
      const matcherTotal = config.packages.length + config.regexes.length;
      return index > 0 ? (counter += matcherTotal) : counter;
    }, 0);
  }
);
