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
  effectiveAuthenticationType,
  GITHUB_APP_NOT_CONFIGURED_MESSAGE,
  hasConfiguredGitHubApp,
  PROVIDERS_WITH_USERNAME,
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

    // Don't show banner if GitHub App authentication is selected (local or inherited)
    const provider = effectiveProvider(sourceControl, serverSourceControl);
    const isGitHubProvider = provider === 'github';

    // Check EFFECTIVE auth type (including inheritance)
    // If authenticationType is inherited, use parentValue instead of value
    const effectiveAuthType = effectiveAuthenticationType(sourceControl);

    const isGitHubWithAppAuth = isGitHubProvider && effectiveAuthType === AUTHENTICATION_TYPES.GITHUB_APP;

    if (isGitHubWithAppAuth) {
      return false;
    }

    return (
      isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp) &&
      !sourceControl?.token?.rscValue?.trimmedValue
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
    const isGitHub = provider === 'github';

    if (isGitHub && !sourceControl.authenticationType?.isInherited) {
      if (!sourceControl.authenticationType?.value) {
        return 'Please select an authentication method (GitHub App or Personal Access Token)';
      }
    }

    const isGitHubWithAppAuth =
      isGitHub && effectiveAuthenticationType(sourceControl) === AUTHENTICATION_TYPES.GITHUB_APP;
    const isTokenRequired = !isGitHubWithAppAuth;

    // GitHub App must be configured when GITHUB_APP auth is selected
    if (isGitHubWithAppAuth) {
      const hasGitHubApps = sourceControl.githubApps?.isInherited
        ? hasConfiguredGitHubApp(sourceControl.githubApps?.parentValue)
        : (sourceControl.githubApps?.localCount ?? 0) > 0 || hasConfiguredGitHubApp(sourceControl.githubApps?.value);

      if (!hasGitHubApps) {
        return GITHUB_APP_NOT_CONFIGURED_MESSAGE;
      }
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

    if (
      isApp &&
      isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp) &&
      !sourceControl?.token?.rscValue?.trimmedValue
    ) {
      return GLOBAL_FORM_VALIDATION_ERROR;
    }

    // Cross-provider token validation: inherited token is only incompatible when the user
    // is still relying on it. Once provider inheritance is off and a local token is entered,
    // validation should follow the local field state instead of the stale inherit flag.
    // SKIP this check for GitHub App since it doesn't use token at all.
    const isTokenInherited = sourceControl?.token?.isInherited;
    const isProviderInherited = sourceControl?.provider?.isInherited;
    const parentProvider = serverSourceControl?.provider?.parentValue?.value;
    const currentProvider = sourceControl?.provider?.rscValue?.value;

    if (
      !isGitHubWithAppAuth &&
      isTokenInherited &&
      !isProviderInherited &&
      currentProvider &&
      parentProvider &&
      currentProvider !== parentProvider
    ) {
      return GLOBAL_FORM_VALIDATION_ERROR;
    }

    // Check if provider requires username (only Bitbucket and Azure)
    const providerNeedsUsername = PROVIDERS_WITH_USERNAME.includes(provider);

    const validatableFields = [
      !sourceControl.provider?.isInherited && sourceControl.provider?.rscValue,
      // Username validation: only for providers that require it (Bitbucket, Azure)
      providerNeedsUsername &&
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

export const selectHasPendingGitHubAppReturn = createSelector(
  selectSourceControlConfigurationSlice,
  prop('hasPendingGitHubAppReturn')
);

export const selectSourceControl = createSelector(selectSourceControlConfigurationSlice, prop('sourceControl'));

export const selectHasEditPermission = createSelector(selectSourceControlConfigurationSlice, prop('hasEditPermission'));
