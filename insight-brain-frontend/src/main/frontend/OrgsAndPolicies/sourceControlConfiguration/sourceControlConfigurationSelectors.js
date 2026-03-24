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
import { selectIsGithubAppAuthenticationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

export const selectSourceControlConfigurationSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  prop('sourceControlConfiguration')
);

export const selectIsAccessTokenRequiredOnNode = createSelector(
  selectSourceControlConfigurationSlice,
  selectIsApplication,
  selectIsGithubAppAuthenticationEnabled,
  (sourceControlConfiguration, isApp, isGithubAppAuthenticationEnabled) => {
    const { sourceControl, serverSourceControl } = sourceControlConfiguration;

    // Don't show banner if GitHub App authentication is selected (local or inherited)
    const provider = effectiveProvider(sourceControl, serverSourceControl);
    const isGitHubProvider = provider === 'github';

    // Check EFFECTIVE auth type (including inheritance)
    // If authenticationType is inherited, use parentValue instead of value
    const effectiveAuthType = sourceControl?.authenticationType?.isInherited
      ? sourceControl?.authenticationType?.parentValue
      : sourceControl?.authenticationType?.value;

    const isGitHubWithAppAuth = isGitHubProvider && effectiveAuthType === AUTHENTICATION_TYPES.GITHUB_APP;

    if (isGitHubWithAppAuth) {
      return false;
    }

    return (
      isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp, isGithubAppAuthenticationEnabled) &&
      !sourceControl?.token?.rscValue?.value
    );
  }
);

export const selectValidationError = createSelector(
  selectSourceControlConfigurationSlice,
  selectIsApplication,
  selectIsGithubAppAuthenticationEnabled,
  ({ sourceControl, serverSourceControl }, isApp, isGithubAppAuthenticationEnabled) => {
    if (!sourceControl) return null;

    // Only skip token validation if Provider is GitHub and Authentication type is GITHUB_APP
    const provider = effectiveProvider(sourceControl, serverSourceControl);
    const isGitHub = provider === 'github';

    if (isGitHub && !sourceControl.authenticationType?.isInherited && isGithubAppAuthenticationEnabled) {
      if (!sourceControl.authenticationType?.value) {
        return 'Please select an authentication method (GitHub App or Personal Access Token)';
      }
    }

    const isGitHubWithAppAuth = isGitHub && sourceControl.authenticationType?.value === AUTHENTICATION_TYPES.GITHUB_APP;
    const isTokenRequired = !isGitHubWithAppAuth;

    // GitHub App must be configured when GITHUB_APP auth is selected
    const githubAppInstallationId = sourceControl.githubApp?.isInherited
      ? sourceControl.githubApp?.parentValue?.installationId
      : sourceControl.githubApp?.value?.installationId;

    if (isGitHubWithAppAuth && !githubAppInstallationId) {
      return GITHUB_APP_NOT_CONFIGURED_MESSAGE;
    }

    // When PAT is overridden, token must be provided at this level (not parent's)
    if (
      isGitHub &&
      !sourceControl.authenticationType?.isInherited &&
      sourceControl.authenticationType?.value === AUTHENTICATION_TYPES.PAT
    ) {
      const hasTokenValue = sourceControl.token?.rscValue?.trimmedValue;
      if (!hasTokenValue) {
        return 'Access Token is required when using Personal Access Token authentication';
      }
    }

    // Cross-provider token validation: inherited token incompatible with new provider
    const isTokenInherited = sourceControl?.token?.isInherited;
    const isProviderInherited = sourceControl?.provider?.isInherited;
    const parentProvider = serverSourceControl?.provider?.parentValue?.value;
    const currentProvider = sourceControl?.provider?.rscValue?.value;

    if (
      isTokenInherited &&
      !isProviderInherited &&
      currentProvider &&
      parentProvider &&
      currentProvider !== parentProvider
    ) {
      return GLOBAL_FORM_VALIDATION_ERROR;
    }

    const validatableFields = [
      !sourceControl.provider?.isInherited && sourceControl.provider?.rscValue,
      (!sourceControl.username?.isInherited || !sourceControl.provider?.isInherited) &&
        sourceControl.username?.rscValue,
      isTokenRequired && !sourceControl.token?.isInherited && sourceControl.token?.rscValue,
      !sourceControl.baseBranch?.isInherited && sourceControl.baseBranch?.rscValue,
      !sourceControl.closePrAfterDays?.isInherited &&
        sourceControl?.closePrAfterDaysOpenEnabled?.value &&
        sourceControl.closePrAfterDays?.rscValue,
    ];
    if (isApp) validatableFields.push(sourceControl?.repositoryUrl);
    const isValidationError = validatableFields.some((property) => property?.validationErrors?.length >= 1);

    return isValidationError ? GLOBAL_FORM_VALIDATION_ERROR : null;
  }
);

export const selectIsLoading = createSelector(selectSourceControlConfigurationSlice, prop('formLoading'));

export const selectShowGitHubAppSuccessModal = createSelector(
  selectSourceControlConfigurationSlice,
  prop('showGitHubAppSuccessModal')
);

export const selectShowGitHubAppReplacedAlert = createSelector(
  selectSourceControlConfigurationSlice,
  prop('showGitHubAppReplacedAlert')
);

export const selectIsGitHubAppReplacement = createSelector(
  selectSourceControlConfigurationSlice,
  prop('isGitHubAppReplacement')
);

export const selectSourceControl = createSelector(selectSourceControlConfigurationSlice, prop('sourceControl'));
