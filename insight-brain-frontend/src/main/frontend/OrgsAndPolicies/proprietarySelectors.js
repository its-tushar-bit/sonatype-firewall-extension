/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { length, prop, propEq, not, filter, compose } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectProprietarySlice = createSelector(selectOrgsAndPoliciesSlice, prop('proprietary'));

export const selectLoadError = createSelector(selectProprietarySlice, prop('loadError'));
export const selectSubmitError = createSelector(selectProprietarySlice, prop('submitError'));
export const selectIsDirty = createSelector(selectProprietarySlice, prop('isDirty'));
export const selectIsLoading = createSelector(selectProprietarySlice, prop('loading'));
export const selectLocalMatchers = createSelector(selectProprietarySlice, prop('localMatchers'));
export const selectProprietaryConfigs = createSelector(selectProprietarySlice, prop('proprietaryConfigs'));
export const selectCurrentConfigs = createSelector(selectProprietarySlice, prop('currentConfig'));
export const selectProprietarySubmitMaskState = createSelector(selectProprietarySlice, prop('submitMaskState'));

export const selectMatcherValue = createSelector(selectProprietarySlice, prop('matcherValue'));
export const selectMatcherType = createSelector(selectProprietarySlice, prop('matcherType'));
export const selectProprietaryConfigLocalMatchersCount = createSelector(selectLocalMatchers, length);
export const selectProprietaryConfigInheritedMatchersCount = createSelector(
  selectProprietaryConfigs,
  (proprietaryConfigs = []) => {
    return proprietaryConfigs.reduce((counter, configOwner, index) => {
      const config = configOwner.proprietaryConfig;
      const matcherTotal = config.packages.length + config.regexes.length;
      return index > 0 ? (counter += matcherTotal) : counter;
    }, 0);
  }
);
export const selectProprietaryOtherConfigs = createSelector(
  selectProprietarySlice,
  ({ proprietaryConfigs = [], currentConfig }) =>
    filter(compose(not, propEq('ownerId', currentConfig?.ownerId)), proprietaryConfigs)
);
