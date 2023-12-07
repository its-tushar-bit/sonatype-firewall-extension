/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { isNil, path, prop } from 'ramda';
import { selectRouterCurrentParams } from '../../reduxUiRouter/routerSelectors';
import { selectOrgsAndPoliciesSlice } from '../orgsAndPoliciesSelectors';
import { hasValidationErrors, GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';
import { selectOwnersFlattenEntries } from '../ownerSideNav/ownerSideNavSelectors';

export const selectApplicationCategoriesSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  path(['applicationCategories', 'createEdit'])
);
export const selectIsEditMode = createSelector(selectRouterCurrentParams, ({ categoryId }) => !isNil(categoryId));
export const selectAppCategoryOwners = createSelector(selectApplicationCategoriesSlice, prop('appCategoryOwners'));
export const selectIsLoading = createSelector(selectApplicationCategoriesSlice, prop('loading'));
export const selectLoadError = createSelector(selectApplicationCategoriesSlice, prop('loadError'));
export const selectSubmitError = createSelector(selectApplicationCategoriesSlice, prop('submitError'));
export const selectIsDirty = createSelector(selectApplicationCategoriesSlice, prop('isDirty'));
export const selectCurrentCategory = createSelector(selectApplicationCategoriesSlice, prop('currentCategory'));
export const selectDeleteModal = createSelector(selectApplicationCategoriesSlice, prop('deleteModal'));
export const selectTagPolicyList = createSelector(selectDeleteModal, prop('tagPolicyList'));
export const selectApplicationTags = createSelector(selectDeleteModal, prop('applicationTags'));

export const selectSiblings = createSelector(selectApplicationCategoriesSlice, prop('siblings'));
export const selectSubmitMaskState = createSelector(selectApplicationCategoriesSlice, prop('submitMaskState'));
export const selectValidationError = createSelector(selectApplicationCategoriesSlice, ({ currentCategory }) =>
  hasValidationErrors(currentCategory.name.validationErrors) ||
  hasValidationErrors(currentCategory.description.validationErrors)
    ? GLOBAL_FORM_VALIDATION_ERROR
    : null
);

export const selectDeleteMaskState = createSelector(selectApplicationCategoriesSlice, prop('deleteMaskState'));
export const selectDeleteError = createSelector(selectApplicationCategoriesSlice, prop('deleteError'));

const getAssociatedApplicationNames = (applicationTags, { applications: allApplications }, { categoryId }) => {
  const associatedApplicationNames = [];

  applicationTags?.forEach((tag) => {
    if (tag.tagId === categoryId) {
      allApplications.forEach((application) => {
        if (application.id === tag.applicationId) {
          associatedApplicationNames.push(application.name);
        }
      });
    }
  });

  return associatedApplicationNames;
};

export const selectAssociatedApplicationNames = createSelector(
  selectApplicationTags,
  selectOwnersFlattenEntries,
  selectRouterCurrentParams,
  getAssociatedApplicationNames
);
