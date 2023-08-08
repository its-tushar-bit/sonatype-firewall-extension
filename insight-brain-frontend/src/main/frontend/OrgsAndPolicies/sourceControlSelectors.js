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

export const selectItemSubText = createSelector(
  selectSourceControl,
  selectEffectiveProvider,
  selectIsRootOrganization,
  selectIsApplication,
  selectSelectedOwnerName,
  (sourceControl, effectiveProvider, isRootOrg, isApp, ownerName) => {
    let text,
      token = sourceControl?.token.value,
      parentValue = sourceControl?.token.parentValue,
      parentName = sourceControl?.token.parentName,
      orgProvider = sourceControl?.provider ? sourceControl?.provider.value : null,
      provider = getProviderTypesMap()[effectiveProvider];

    if (!sourceControl || !effectiveProvider) {
      text = 'Source Control not configured';
    } else {
      if (isRootOrg) {
        text = 'Provides the default source control configuration settings';
      } else if (!!orgProvider && !token) {
        text = 'Inherit access token';
      } else if (!token) {
        text = `Inherit access token${parentValue ? ` from ${parentName}` : ''}${isApp ? ` (${provider})` : ''}`;
      } else {
        text = `Provides default access token for ${ownerName}${isApp ? ` (${provider})` : ''}`;
      }
    }
    return text;
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
