/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { always, compose, equals, has, ifElse, path, prop } from 'ramda';
import { valueFromHierarchy } from 'MainRoot/configuration/scmOnboarding/utils/providers';
import { selectOrgsAndPoliciesSlice, selectSelectedOwnerName } from './orgsAndPoliciesSelectors';
import { selectIsOrganization, selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { getProviderTypesMap } from 'MainRoot/util/sourceControlUtils';
import { selectIsApplication } from '../reduxUiRouter/routerSelectors';
import { AUTHENTICATION_TYPES } from './sourceControlConfiguration/utils';

export const selectSourceControlSlice = createSelector(selectOrgsAndPoliciesSlice, prop('sourceControl'));

export const selectLoadError = createSelector(selectSourceControlSlice, prop('loadError'));
export const selectLoading = createSelector(selectSourceControlSlice, prop('loading'));
export const selectSourceControl = createSelector(selectSourceControlSlice, prop('data'));

export const selectEffectiveProvider = createSelector(
  selectSourceControl,
  ifElse(has('provider'), compose(valueFromHierarchy, prop('provider')), () => null)
);

export const selectItemText = createSelector(
  selectSourceControl,
  selectEffectiveProvider,
  selectIsOrganization,
  (sourceControl, effectiveProvider, isOrg) => {
    let text = '';
    if (sourceControl && effectiveProvider) {
      if (isOrg) {
        text = getProviderTypesMap()[effectiveProvider];
      } else {
        text = sourceControl?.repositoryUrl ? sourceControl?.repositoryUrl : 'Repository URL needed';
      }
    }
    return text;
  }
);

const SOURCE_CONTROL_FIELDS = {
  GITHUB_APP: 'githubApp',
  INSTALLATION_ID: 'installationId',
};

const PROVIDER_AUTH_CONFIGS = {
  github: {
    appAuthType: AUTHENTICATION_TYPES.GITHUB_APP,
    appFieldName: SOURCE_CONTROL_FIELDS.GITHUB_APP,
    appIdField: SOURCE_CONTROL_FIELDS.INSTALLATION_ID,
    authMethodLabel: 'GitHub App',
  },
};

/**
 * Checks if App-based authentication is configured locally at the current organization level.
 *
 * In the organization hierarchy, "local" refers to configuration set directly on the current
 * organization (sourceControl.authenticationType.value), as opposed to configuration inherited
 * from a parent organization (sourceControl.authenticationType.parentValue).
 *
 * @param {Object} sourceControl - The source control configuration object containing auth settings
 * @param {string} effectiveProvider - The source control provider (e.g., 'github', 'gitlab', 'bitbucket')
 * @returns {boolean} True if App authentication is configured locally with a valid installation ID
 */
const isLocalAppAuthConfigured = (sourceControl, effectiveProvider) => {
  const providerConfig = PROVIDER_AUTH_CONFIGS[effectiveProvider];
  if (!providerConfig) {
    return false;
  }

  const localAuthType = sourceControl?.authenticationType?.value;
  const hasAppAuth = localAuthType === providerConfig.appAuthType;
  const appData = sourceControl?.[providerConfig.appFieldName]?.value;
  const hasAppId = Boolean(appData?.[providerConfig.appIdField]);

  return hasAppAuth && hasAppId;
};

/**
 * Checks if App-based authentication is being inherited from a parent organization.
 *
 * @param {Object} sourceControl - The source control configuration object containing auth settings
 * @param {string} effectiveProvider - The source control provider (e.g., 'github', 'gitlab', 'bitbucket')
 * @returns {boolean} True if parent organization has App authentication configured
 */
const isInheritingAppAuthFromParent = (sourceControl, effectiveProvider) => {
  const providerConfig = PROVIDER_AUTH_CONFIGS[effectiveProvider];
  if (!providerConfig) {
    return false;
  }

  const parentAuthType = sourceControl?.authenticationType?.parentValue;
  return parentAuthType === providerConfig.appAuthType;
};

const getAuthMethodLabel = (effectiveProvider) => {
  return PROVIDER_AUTH_CONFIGS[effectiveProvider]?.authMethodLabel || 'App';
};

const createMessages = (ownerName, providerSuffix, parentName, authMethodLabel) => ({
  inheritAppAuth: () => `Inherit authentication method: ${authMethodLabel}`,
  inheritAccessToken: () => `Inherit access token${providerSuffix}`,
  inheritAccessTokenFrom: () => `Inherit access token from ${parentName}${providerSuffix}`,
  providesAppAuth: () => `Provides default authentication method: ${authMethodLabel} for ${ownerName}${providerSuffix}`,
  providesAccessToken: () => `Provides default access token for ${ownerName}${providerSuffix}`,
});

const AUTH_STATES = {
  LOCAL_TOKEN_WITH_APP: 'LOCAL_TOKEN_WITH_APP',
  LOCAL_TOKEN_PAT: 'LOCAL_TOKEN_PAT',
  LOCAL_APP_NO_TOKEN: 'LOCAL_APP_NO_TOKEN',
  INHERIT_APP: 'INHERIT_APP',
  INHERIT_ORG_PROVIDER: 'INHERIT_ORG_PROVIDER',
  INHERIT_KNOWN_PARENT: 'INHERIT_KNOWN_PARENT',
  INHERIT_DEFAULT: 'INHERIT_DEFAULT',
};

const MESSAGE_TABLE = {
  [AUTH_STATES.LOCAL_TOKEN_WITH_APP]: (messages) => messages.providesAppAuth(),
  [AUTH_STATES.LOCAL_TOKEN_PAT]: (messages) => messages.providesAccessToken(),
  [AUTH_STATES.LOCAL_APP_NO_TOKEN]: (messages) => messages.providesAppAuth(),
  [AUTH_STATES.INHERIT_APP]: (messages) => messages.inheritAppAuth(),
  [AUTH_STATES.INHERIT_ORG_PROVIDER]: (messages) => messages.inheritAccessToken(),
  [AUTH_STATES.INHERIT_KNOWN_PARENT]: (messages) => messages.inheritAccessTokenFrom(),
  [AUTH_STATES.INHERIT_DEFAULT]: (messages) => messages.inheritAccessToken(),
};

const determineAuthState = (authState) => {
  const { hasLocalToken, hasLocalAppAuth, hasParentAppAuth, hasKnownParentToken, hasOrgProvider } = authState;

  if (hasLocalToken && hasLocalAppAuth) return AUTH_STATES.LOCAL_TOKEN_WITH_APP;
  if (hasLocalToken) return AUTH_STATES.LOCAL_TOKEN_PAT;
  if (hasLocalAppAuth) return AUTH_STATES.LOCAL_APP_NO_TOKEN;
  if (hasParentAppAuth) return AUTH_STATES.INHERIT_APP;
  if (hasOrgProvider) return AUTH_STATES.INHERIT_ORG_PROVIDER;
  if (hasKnownParentToken) return AUTH_STATES.INHERIT_KNOWN_PARENT;
  return AUTH_STATES.INHERIT_DEFAULT;
};

const selectAuthenticationMessage = (authState, messages) => {
  const state = determineAuthState(authState);
  const messageGenerator = MESSAGE_TABLE[state];
  return messageGenerator(messages);
};

export const selectItemSubText = createSelector(
  selectSourceControl,
  selectEffectiveProvider,
  selectIsRootOrganization,
  selectIsApplication,
  selectSelectedOwnerName,
  (sourceControl, effectiveProvider, isRootOrg, isApp, ownerName) => {
    if (!sourceControl || !effectiveProvider) {
      return 'Source Control not configured';
    }

    if (isRootOrg) {
      return 'Provides the default source control configuration settings';
    }

    const token = sourceControl?.token.value;
    const parentValue = sourceControl?.token.parentValue;
    const parentName = sourceControl?.token.parentName;
    const orgProvider = sourceControl?.provider?.value;
    const provider = getProviderTypesMap()[effectiveProvider];
    const providerSuffix = isApp && provider ? ` (${provider})` : '';

    const hasLocalAppAuth = isLocalAppAuthConfigured(sourceControl, effectiveProvider);
    const hasParentAppAuthConfigured = isInheritingAppAuthFromParent(sourceControl, effectiveProvider);
    const authMethodLabel = getAuthMethodLabel(effectiveProvider);
    const messages = createMessages(ownerName, providerSuffix, parentName, authMethodLabel);

    const authState = {
      hasLocalToken: Boolean(token),
      hasLocalAppAuth,
      hasParentAppAuth: hasParentAppAuthConfigured,
      hasKnownParentToken: Boolean(parentValue && parentName),
      hasOrgProvider: Boolean(orgProvider),
    };

    return selectAuthenticationMessage(authState, messages);
  }
);

export const selectRepositoryUrl = createSelector(selectSourceControl, prop('repositoryUrl'));

const ifProviderIsAzureSetItToGit = ifElse(equals('azure'), always('git'), (provider) => provider);
const getScmProviderValue = ifElse(
  path(['provider', 'value']),
  path(['provider', 'value']),
  path(['provider', 'parentValue'])
);

export const selectScmProviderIcon = createSelector(
  selectSourceControl,
  compose(
    // no Font Awesome icon for Azure, use Microsoft instead once FA v5 is available (eg: React migration)
    // see: https://github.com/FortAwesome/Font-Awesome/issues/14058
    ifProviderIsAzureSetItToGit,
    getScmProviderValue
  )
);
