/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import {
  selectApplicationId as selectApplicationPublicId,
  selectRouterCurrentParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  FAKE_PASSWORD,
  MISSING_OR_INVALID_DATA_MESSAGE,
  MUST_REENTER_PASSWORD_MESSAGE,
  NO_CHANGES_MESSAGE,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';
import { getOriginalValues } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalUtil';

// Visible for testing
export const selectOrganizationId = createSelector(selectRouterCurrentParams, prop('organizationId'));

// Visible for testing
export const selectApplicationId = createSelector(selectRouterCurrentParams, prop('applicationId'));

export const selectOwnerTypeAndOwnerId = createSelector(
  selectOrganizationId,
  selectApplicationId,
  selectApplicationPublicId,
  (organizationId, applicationId, applicationPublicId) => {
    if (!organizationId && !applicationId && !applicationPublicId) {
      return undefined;
    }
    return {
      ownerType: organizationId ? 'organization' : 'application',
      ownerId: organizationId ?? applicationId ?? applicationPublicId,
    };
  }
);

export const selectArtifactoryRepositoryConfigurationModalSlice = prop('artifactoryRepositoryConfigurationModal');

export const selectArtifactoryConnectionId = createSelector(
  selectArtifactoryRepositoryConfigurationModalSlice,
  prop('artifactoryConnectionId')
);

export const selectIsUpdate = createSelector(
  selectArtifactoryConnectionId,
  (artifactoryConnectionId) => !!artifactoryConnectionId
);

// Visible for testing
export const selectServerData = createSelector(selectArtifactoryRepositoryConfigurationModalSlice, prop('serverData'));

export const selectFormState = createSelector(selectArtifactoryRepositoryConfigurationModalSlice, prop('formState'));

// Visible for testing
export const selectOriginalValues = createSelector(selectServerData, getOriginalValues);

export const selectIsDirty = createSelector(selectFormState, selectOriginalValues, (formState, originalValues) => {
  if (formState.baseUrlState.trimmedValue !== originalValues.baseUrl) {
    return true;
  }
  if (formState.isAnonymous !== originalValues.isAnonymous) {
    return true;
  }
  if (!formState.isAnonymous) {
    if (formState.usernameState.trimmedValue !== originalValues.username) {
      return true;
    }
    if (formState.passwordState.trimmedValue !== originalValues.password) {
      return true;
    }
  }
  return false;
});

export const selectHasAllRequiredData = createSelector(selectFormState, (formState) => {
  if (!formState.baseUrlState.trimmedValue) {
    return false;
  }
  if (!formState.isAnonymous && (!formState.usernameState.trimmedValue || !formState.passwordState.trimmedValue)) {
    return false;
  }
  return true;
});

// Visible for testing
export const selectIsPasswordNeededAndNotEntered = createSelector(selectFormState, (formState) => {
  return (
    !formState.isAnonymous &&
    formState.passwordState.isPristine &&
    formState.passwordState.trimmedValue === FAKE_PASSWORD
  );
});

export const selectValidationErrors = createSelector(
  selectHasAllRequiredData,
  selectIsDirty,
  selectIsPasswordNeededAndNotEntered,
  (hasAllRequiredData, isDirty, isPasswordNeededAndNotEntered) => {
    if (!hasAllRequiredData) {
      return MISSING_OR_INVALID_DATA_MESSAGE;
    }
    if (!isDirty) {
      return NO_CHANGES_MESSAGE;
    }
    if (isPasswordNeededAndNotEntered) {
      return MUST_REENTER_PASSWORD_MESSAGE;
    }
    return null;
  }
);
