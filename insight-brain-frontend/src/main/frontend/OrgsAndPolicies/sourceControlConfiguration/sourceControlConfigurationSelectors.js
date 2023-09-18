/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';
import { prop } from 'ramda';
import { isAccessTokenRequiredOnNode } from './utils';
import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectSourceControlConfigurationSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  prop('sourceControlConfiguration')
);

export const selectIsAccessTokenRequiredOnNode = createSelector(
  selectSourceControlConfigurationSlice,
  selectIsApplication,
  (sourceControlConfiguration, isApp) =>
    isAccessTokenRequiredOnNode(
      sourceControlConfiguration.sourceControl,
      sourceControlConfiguration.serverSourceControl,
      isApp
    ) && !sourceControlConfiguration.sourceControl?.token.rscValue.value
);

export const selectValidationError = createSelector(
  selectSourceControlConfigurationSlice,
  selectIsApplication,
  ({ sourceControl }, isApp) => {
    if (!sourceControl) return null;
    const validatableFields = [
      !sourceControl.provider.isInherited && sourceControl.provider.rscValue,
      (!sourceControl.username.isInherited || !sourceControl.provider.isInherited) && sourceControl.username.rscValue,
      (!sourceControl.token.isInherited || !sourceControl.provider.isInherited) && sourceControl.token.rscValue,
      !sourceControl.baseBranch.isInherited && sourceControl.baseBranch.rscValue,
    ];
    if (isApp) validatableFields.push(sourceControl.repositoryUrl);
    const isValidationError = validatableFields.some((property) => property?.validationErrors?.length >= 1);

    return isValidationError ? GLOBAL_FORM_VALIDATION_ERROR : null;
  }
);

export const selectIsLoading = createSelector(selectSourceControlConfigurationSlice, prop('formLoading'));
