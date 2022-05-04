/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, isNil } from 'ramda';

import { selectRouterCurrentParams } from '../reduxUiRouter/routerSelectors';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import { eqValues } from 'MainRoot/util/jsUtil';

export const selectPolicySlice = createSelector(selectOrgsAndPoliciesSlice, prop('policy'));

export const selectIsEditMode = createSelector(selectRouterCurrentParams, ({ policyId }) => !isNil(policyId));

export const selectIsOrgOwner = createSelector(selectPolicySlice, prop('isOrgOwner'));

export const selectReadOnly = createSelector(selectPolicySlice, prop('readOnly'));

export const selectSiblings = createSelector(selectPolicySlice, prop('siblings'));

export const selectSubmitError = createSelector(selectPolicySlice, prop('submitError'));

export const selectIsRootOrg = createSelector(selectPolicySlice, prop('isRootOrg'));

export const selectOriginalProxyStageAction = createSelector(selectPolicySlice, prop('originalProxyStageAction'));

export const selectLoadError = createSelector(selectPolicySlice, prop('loadError'));

export const selectLoading = createSelector(selectPolicySlice, prop('loading'));

export const selectDeleteModal = createSelector(selectPolicySlice, prop('deleteModal'));

export const selectCurrentPolicy = createSelector(selectPolicySlice, prop('currentPolicy'));

export const selectIsDirty = createSelector(selectPolicySlice, prop('isDirty'));

export const selectHasPolicyCategories = createSelector(selectPolicySlice, prop('hasPolicyCategories'));

export const selectOriginalCategories = createSelector(selectPolicySlice, prop('originalCategories'));

export const selectCategories = createSelector(selectPolicySlice, prop('categories'));

export const selectOriginalHasPolicyCategories = createSelector(selectPolicySlice, prop('originalHasPolicyCategories'));

export const selectIsInheritanceDirty = createSelector(
  selectIsOrgOwner,
  selectHasPolicyCategories,
  selectOriginalHasPolicyCategories,
  selectCategories,
  selectOriginalCategories,
  (isOrgOwner, hasPolicyCategories, originalHasPolicyCategories, categories, originalCategories) =>
    isOrgOwner &&
    ((hasPolicyCategories && !eqValues(originalCategories, categories)) ||
      originalHasPolicyCategories !== hasPolicyCategories)
);

export const selectCurrentPolicyActions = createSelector(selectCurrentPolicy, prop('actions'));

export const selectShouldShowQuarantineWarning = createSelector(
  selectCurrentPolicyActions,
  selectOriginalProxyStageAction,
  selectIsRootOrg,
  (actions, originalProxyStageAction, isRootOrg) =>
    actions?.proxy === 'fail' && originalProxyStageAction !== 'fail' && isRootOrg
);

export const selectIsCurrentPolicyDirty = createSelector(
  selectIsDirty,
  selectIsInheritanceDirty,
  (isDirty, isInheritanceDirty) => isDirty || isInheritanceDirty
);

export const selectCurrentPolicyOwner = createSelector(selectPolicySlice, prop('currentPolicyOwner'));
export const selectCurrentPolicyOwnerName = createSelector(selectCurrentPolicyOwner, prop('name'));
