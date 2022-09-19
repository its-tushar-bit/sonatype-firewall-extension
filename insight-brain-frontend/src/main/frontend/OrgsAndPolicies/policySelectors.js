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

export const selectHasEditIqPermission = createSelector(selectPolicySlice, prop('hasEditIqPermission'));

export const selectIsOrgOwner = createSelector(selectPolicySlice, prop('isOrgOwner'));

export const selectIsInherited = createSelector(selectPolicySlice, prop('isInherited'));

export const selectSiblings = createSelector(selectPolicySlice, prop('siblings'));

export const selectSubmitError = createSelector(selectPolicySlice, prop('submitError'));

export const selectIsRootOrg = createSelector(selectPolicySlice, prop('isRootOrg'));

export const selectOriginalProxyStageAction = createSelector(selectPolicySlice, prop('originalProxyStageAction'));

export const selectPolicyLoadError = createSelector(selectPolicySlice, prop('loadError'));

export const selectCategoriesForPolicyLoadError = createSelector(
  selectPolicySlice,
  prop('categoriesForPolicyLoadError')
);

export const selectLoadError = createSelector(
  selectPolicyLoadError,
  selectCategoriesForPolicyLoadError,
  (policyLoadError, categoriesForPolicyLoadError) => {
    return policyLoadError || categoriesForPolicyLoadError;
  }
);

export const selectLoading = createSelector(selectPolicySlice, prop('loading'));

export const selectDeleteModal = createSelector(selectPolicySlice, prop('deleteModal'));

export const selectCurrentPolicy = createSelector(selectPolicySlice, prop('currentPolicy'));

export const selectIsActionOverrideEnabled = createSelector(
  selectIsInherited,
  selectCurrentPolicy,
  (isInherited, currentPolicy) => isInherited && currentPolicy.policyActionsOverrideAllowed
);

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
export const selectOriginalPolicy = createSelector(selectPolicySlice, prop('originalPolicy'));

export const selectOverrideActionsFlag = createSelector(selectPolicySlice, prop('overrideActionsFlag'));
export const selectOriginalOverrideActionsFlag = createSelector(selectPolicySlice, prop('originalOverrideActionsFlag'));

export const selectOverrideNeedsToBeRemoved = createSelector(
  selectOriginalOverrideActionsFlag,
  selectOverrideActionsFlag,
  (originalOverrideFlag, overrideFlag) => originalOverrideFlag && !overrideFlag
);

export const selectPolicyTile = createSelector(selectPolicySlice, prop('policyTile'));
export const selectPoliciesByOwner = createSelector(selectPolicyTile, prop('policiesByOwner'));
export const selectPolicyTileLoading = createSelector(selectPolicyTile, prop('loading'));
export const selectPolicyTileLoadError = createSelector(selectPolicyTile, prop('loadError'));
export const selectPolicyTileSorting = createSelector(selectPolicyTile, prop('sorting'));
