/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';
import { prop } from 'ramda';
import {
  isAccessTokenRequiredOnNode,
  AUTHENTICATION_TYPES,
  effectiveProvider,
  GITHUB_APP_NOT_CONFIGURED_MESSAGE,
} from './utils';
import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectSourceControlConfigurationSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  prop('sourceControlConfiguration')
);

export const selectIsAccessTokenRequiredOnNode = createSelector(
  selectSourceControlConfigurationSlice,
  selectIsApplication,
  (sourceControlConfiguration, isApp) => {
    const { sourceControl, serverSourceControl } = sourceControlConfiguration;

    // Don't show banner if GitHub App authentication is selected
    const provider = effectiveProvider(sourceControl, serverSourceControl);
    const isGitHubWithAppAuth =
      provider === 'github' && sourceControl?.authenticationType?.value === AUTHENTICATION_TYPES.GITHUB_APP;

    if (isGitHubWithAppAuth) {
      return false;
    }

    return (
      isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp) && !sourceControl?.token.rscValue.value
    );
  }
);

export const selectValidationError = createSelector(
  selectSourceControlConfigurationSlice,
  selectIsApplication,
  ({ sourceControl, serverSourceControl }, isApp) => {
    if (!sourceControl) return null;

    // Only skip token validation if Provider is GitHub and Authentication type is GITHUB_APP
    const provider = effectiveProvider(sourceControl, serverSourceControl);
    const isGitHubWithAppAuth =
      provider === 'github' && sourceControl.authenticationType?.value === AUTHENTICATION_TYPES.GITHUB_APP;
    const isTokenRequired = !isGitHubWithAppAuth;

    // Validate that GitHub App is configured when GITHUB_APP authentication is selected
    if (isGitHubWithAppAuth && !sourceControl.githubApp?.value?.installationId) {
      return GITHUB_APP_NOT_CONFIGURED_MESSAGE;
    }

    const validatableFields = [
      !sourceControl.provider.isInherited && sourceControl.provider.rscValue,
      (!sourceControl.username.isInherited || !sourceControl.provider.isInherited) && sourceControl.username.rscValue,
      isTokenRequired &&
        (!sourceControl.token.isInherited || !sourceControl.provider.isInherited) &&
        sourceControl.token.rscValue,
      !sourceControl.baseBranch.isInherited && sourceControl.baseBranch.rscValue,
      !sourceControl.closePrAfterDays.isInherited &&
        sourceControl?.closePrAfterDaysOpenEnabled.value &&
        sourceControl.closePrAfterDays.rscValue,
    ];
    if (isApp) validatableFields.push(sourceControl.repositoryUrl);
    const isValidationError = validatableFields.some((property) => property?.validationErrors?.length >= 1);

    return isValidationError ? GLOBAL_FORM_VALIDATION_ERROR : null;
  }
);

export const selectIsLoading = createSelector(selectSourceControlConfigurationSlice, prop('formLoading'));

export const selectShowGitHubAppSuccessModal = createSelector(
  selectSourceControlConfigurationSlice,
  prop('showGitHubAppSuccessModal')
);

export const selectSourceControl = createSelector(selectSourceControlConfigurationSlice, prop('sourceControl'));
