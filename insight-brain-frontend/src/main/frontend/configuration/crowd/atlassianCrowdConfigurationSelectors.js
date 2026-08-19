/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { FAKE_PASSWORD } from './util';

export const selectAtlassianCrowdConfigurationSlice = prop('atlassianCrowdConfiguration');
export const selectFormState = createSelector(selectAtlassianCrowdConfigurationSlice, prop('formState'));
export const selectLoading = createSelector(selectAtlassianCrowdConfigurationSlice, prop('loading'));
export const selectSubmitMaskState = createSelector(selectAtlassianCrowdConfigurationSlice, prop('submitMaskState'));
export const selectSubmitMaskMessage = createSelector(
  selectAtlassianCrowdConfigurationSlice,
  prop('submitMaskMessage')
);
export const selectIsDirty = createSelector(selectAtlassianCrowdConfigurationSlice, prop('isDirty'));
export const selectDeleteMaskState = createSelector(selectAtlassianCrowdConfigurationSlice, prop('deleteMaskState'));
export const selectTestError = createSelector(selectAtlassianCrowdConfigurationSlice, prop('testError'));
export const selectLoadError = createSelector(selectAtlassianCrowdConfigurationSlice, prop('loadError'));
export const selectUpdateError = createSelector(selectAtlassianCrowdConfigurationSlice, prop('updateError'));
export const selectDeleteError = createSelector(selectAtlassianCrowdConfigurationSlice, prop('deleteError'));

export const selectMustReenterPassword = createSelector(
  selectAtlassianCrowdConfigurationSlice,
  ({ serverData, formState }) => {
    if (serverData) {
      const serverUrl = formState.serverUrl.trimmedValue,
        applicationPassword = formState.applicationPassword.trimmedValue,
        savedServerUrl = serverData.serverUrl;
      return serverUrl !== savedServerUrl && applicationPassword === FAKE_PASSWORD;
    } else {
      return false;
    }
  }
);

export const selectHasAllRequiredData = createSelector(
  selectAtlassianCrowdConfigurationSlice,
  ({ formState: { serverUrl, applicationName, applicationPassword } }) =>
    !!(serverUrl.trimmedValue && applicationName.trimmedValue && applicationPassword.trimmedValue)
);

export const selectTestSuccessMessage = createSelector(
  selectAtlassianCrowdConfigurationSlice,
  prop('testSuccessMessage')
);
export const selectServerData = createSelector(selectAtlassianCrowdConfigurationSlice, prop('serverData'));

export const selectShowModal = createSelector(selectAtlassianCrowdConfigurationSlice, prop('showModal'));
