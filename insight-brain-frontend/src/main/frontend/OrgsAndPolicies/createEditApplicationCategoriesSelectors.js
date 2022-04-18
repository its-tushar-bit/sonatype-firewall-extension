/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { isNil, path, prop } from 'ramda';
import { selectRouterCurrentParams } from '../reduxUiRouter/routerSelectors';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectApplicationCategoriesSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  path(['applicationCategories', 'createEdit'])
);
export const selectIsEditMode = createSelector(selectRouterCurrentParams, ({ categoryId }) => !isNil(categoryId));
export const selectAppCategoryOwners = createSelector(selectApplicationCategoriesSlice, prop('appCategoryOwners'));
export const selectIsLoading = createSelector(selectApplicationCategoriesSlice, prop('loading'));
export const selectLoadError = createSelector(selectApplicationCategoriesSlice, prop('loadError'));
export const selectIsDirty = createSelector(selectApplicationCategoriesSlice, prop('isDirty'));
export const selectCurrentCategory = createSelector(selectApplicationCategoriesSlice, prop('currentCategory'));
export const selectDeleteModal = createSelector(selectApplicationCategoriesSlice, prop('deleteModal'));
export const selectAssociatedApplicationNames = createSelector(selectDeleteModal, prop('associatedApplicationNames'));
export const selectTagPolicyList = createSelector(selectDeleteModal, prop('tagPolicyList'));
export const selectSiblings = createSelector(selectApplicationCategoriesSlice, prop('siblings'));
