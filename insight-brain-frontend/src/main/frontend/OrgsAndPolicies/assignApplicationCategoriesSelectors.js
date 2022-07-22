/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { any, compose, map, path, prop, propEq, isEmpty, not } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectAssignApplicationCategoriesSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  path(['applicationCategories', 'assign'])
);

export const selectLoadingApplicableCategories = createSelector(
  selectAssignApplicationCategoriesSlice,
  prop('loadingApplicableCategories')
);
export const selectLoadApplicableCategoriesError = createSelector(
  selectAssignApplicationCategoriesSlice,
  prop('loadApplicableCategoriesError')
);
export const selectApplicableCategories = createSelector(
  selectAssignApplicationCategoriesSlice,
  prop('applicableCategories')
);

export const selectLoadingAppliedCategories = createSelector(
  selectAssignApplicationCategoriesSlice,
  prop('loadingAppliedCategories')
);
export const selectLoadAppliedCategoriesError = createSelector(
  selectAssignApplicationCategoriesSlice,
  prop('loadAppliedCategoriesError')
);
export const selectAppliedCategories = createSelector(
  selectAssignApplicationCategoriesSlice,
  prop('appliedCategories')
);

export const selectSubmitApplyCategoriesError = createSelector(
  selectAssignApplicationCategoriesSlice,
  prop('submitError')
);

export const selectAssignAppCategoriesSubmitMaskState = createSelector(
  selectAssignApplicationCategoriesSlice,
  prop('submitMaskState')
);

export const selectIsDirty = createSelector(selectAssignApplicationCategoriesSlice, prop('isDirty'));

export const selectCategories = createSelector(
  selectApplicableCategories,
  selectAppliedCategories,
  (applicableCategories, appliedCategories) => {
    return map((applicableCategory) => {
      const isApplied = any(propEq('id', applicableCategory.id), appliedCategories);
      return { ...applicableCategory, isApplied };
    }, applicableCategories);
  }
);

export const selectAreAnyCategoriesDefined = createSelector(selectApplicableCategories, compose(not, isEmpty));
