/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { pick, prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectWaiversSlice = createSelector(selectOrgsAndPoliciesSlice, prop('waivers'));
export const selectWaivers = createSelector(selectWaiversSlice, prop('data'));

export const selectWaiversConfig = createSelector(selectWaivers, (waiver) => {
  if (!waiver) return {};
  const config = pick(['isInherited', 'isAutoWaiverEnabled', 'autoPolicyWaiverOwnerName'], waiver);
  return config;
});

export const selectWaiversConfigPage = createSelector(selectWaivers, (waiver) => {
  if (!waiver) return {};
  const config = pick(['pathForward', 'reachability', 'threatLevel', 'isInherited', 'autoPolicyWaiverId'], waiver);
  return config;
});

export const selectWaiversStatusMessage = createSelector(selectWaiversConfig, (configuration) => {
  const message = 'Automated Waivers are ' + (configuration.isAutoWaiverEnabled ? 'enabled' : 'disabled');
  return configuration.isInherited
    ? message + ` (Inheriting from ${configuration.autoPolicyWaiverOwnerName})`
    : message;
});
