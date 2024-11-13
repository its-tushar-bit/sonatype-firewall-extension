/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectBaseUrlConfigurationSlice = prop('baseUrlConfiguration');

export const selectFormState = createSelector(selectBaseUrlConfigurationSlice, prop('formState'));
export const selectLoading = createSelector(selectBaseUrlConfigurationSlice, prop('loading'));
export const selectSubmitMaskState = createSelector(selectBaseUrlConfigurationSlice, prop('submitMaskState'));
export const selectSubmitMaskMessage = createSelector(selectBaseUrlConfigurationSlice, prop('submitMaskMessage'));
export const selectIsDirty = createSelector(selectBaseUrlConfigurationSlice, prop('isDirty'));
export const selectDeleteMaskState = createSelector(selectBaseUrlConfigurationSlice, prop('deleteMaskState'));
export const selectLoadError = createSelector(selectBaseUrlConfigurationSlice, prop('loadError'));
export const selectUpdateError = createSelector(selectBaseUrlConfigurationSlice, prop('updateError'));
export const selectDeleteError = createSelector(selectBaseUrlConfigurationSlice, prop('deleteError'));
export const selectShouldDisplayNotice = createSelector(selectBaseUrlConfigurationSlice, prop('shouldDisplayNotice'));
export const selectHasAllRequiredFields = createSelector(
  selectBaseUrlConfigurationSlice,
  ({ formState: { baseUrl } }) => !!baseUrl.trimmedValue
);

export const selectServerData = createSelector(selectBaseUrlConfigurationSlice, prop('serverData'));
export const selectShowDeleteModal = createSelector(selectBaseUrlConfigurationSlice, prop('showDeleteModal'));
