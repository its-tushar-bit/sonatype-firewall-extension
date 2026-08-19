/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectOidcConfigurationSlice = prop('oidcConfiguration');

export const selectConfigurationValues = createSelector(selectOidcConfigurationSlice, prop('configurationValues'));

export const selectLoadError = createSelector(selectOidcConfigurationSlice, prop('loadError'));

export const selectIsLoading = createSelector(selectOidcConfigurationSlice, prop('isLoading'));

export const selectSubmitState = createSelector(selectOidcConfigurationSlice, prop('submitState'));

export const selectIsConfigured = createSelector(selectOidcConfigurationSlice, prop('isConfigured'));

export const selectSubmitMaskError = createSelector(selectOidcConfigurationSlice, prop('submitMaskError'));

export const selectLoadedConfigurationValues = createSelector(
  selectOidcConfigurationSlice,
  prop('loadedConfigurationValues')
);

export const selectIsDeleteModalShown = createSelector(selectOidcConfigurationSlice, prop('isDeleteModalShown'));

export const selectIsDirty = createSelector(selectOidcConfigurationSlice, prop('isDirty'));
