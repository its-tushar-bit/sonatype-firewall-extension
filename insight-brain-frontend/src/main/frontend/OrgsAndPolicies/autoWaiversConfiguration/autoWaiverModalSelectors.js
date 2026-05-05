/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { pick, prop, path } from 'ramda';
import { selectOrgsAndPoliciesSlice } from '../orgsAndPoliciesSelectors';
import { createTierGatedDirtySelector } from 'MainRoot/productFeatures/tierGateUtils';
import { selectHasAutoWaiverManagement } from 'MainRoot/productFeatures/productFeaturesSelectors';

export const selectAutoWaiverModalSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  path(['autoWaivers', 'autoWaiverModal'])
);

export const selectWaiver = createSelector(selectAutoWaiverModalSlice, prop('data'));

export const selectAutoWaiverDetails = createSelector(selectWaiver, (waiver) => {
  if (!waiver) {
    return {};
  }

  return pick(['pathForward', 'reachability', 'threatLevel', 'scope', 'isInherited', 'autoPolicyWaiverId'], waiver);
});

const selectIsDirtyInternal = createSelector(selectAutoWaiverModalSlice, prop('isDirty'));

export const selectAutoWaiverModalIsDirty = createTierGatedDirtySelector(
  selectIsDirtyInternal,
  selectHasAutoWaiverManagement
);
