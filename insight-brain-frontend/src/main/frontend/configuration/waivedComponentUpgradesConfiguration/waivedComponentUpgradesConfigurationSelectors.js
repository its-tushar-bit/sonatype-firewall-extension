/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectWaivedComponentUpgradesConfigurationSlice = prop('waivedComponentUpgradesConfiguration');

export const selectFormState = createSelector(selectWaivedComponentUpgradesConfigurationSlice, prop('formState'));
export const selectLoading = createSelector(selectWaivedComponentUpgradesConfigurationSlice, prop('loading'));
export const selectSubmitMaskState = createSelector(
  selectWaivedComponentUpgradesConfigurationSlice,
  prop('submitMaskState')
);
export const selectIsDirty = createSelector(selectWaivedComponentUpgradesConfigurationSlice, prop('isDirty'));
export const selectLoadError = createSelector(selectWaivedComponentUpgradesConfigurationSlice, prop('loadError'));
export const selectUpdateError = createSelector(selectWaivedComponentUpgradesConfigurationSlice, prop('updateError'));
