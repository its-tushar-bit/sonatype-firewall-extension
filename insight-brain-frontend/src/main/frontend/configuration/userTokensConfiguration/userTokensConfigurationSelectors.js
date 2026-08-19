/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectUserTokensConfigurationSlice = prop('userTokensConfiguration');

export const selectFormState = createSelector(selectUserTokensConfigurationSlice, prop('formState'));
export const selectLoading = createSelector(selectUserTokensConfigurationSlice, prop('loading'));
export const selectSubmitMaskState = createSelector(selectUserTokensConfigurationSlice, prop('submitMaskState'));
export const selectIsDirty = createSelector(selectUserTokensConfigurationSlice, prop('isDirty'));
export const selectLoadError = createSelector(selectUserTokensConfigurationSlice, prop('loadError'));
export const selectUpdateError = createSelector(selectUserTokensConfigurationSlice, prop('updateError'));
