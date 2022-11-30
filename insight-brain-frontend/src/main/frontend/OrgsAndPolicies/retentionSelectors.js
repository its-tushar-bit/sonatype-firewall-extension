/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';
import { hasValidationErrors } from '@sonatype/react-shared-components';

export const selectRetentionSlice = createSelector(selectOrgsAndPoliciesSlice, prop('retention'));
export const selectLoading = createSelector(selectRetentionSlice, prop('loading'));
export const selectLoadError = createSelector(selectRetentionSlice, prop('loadError'));
export const selectSuccessMetrics = createSelector(selectRetentionSlice, prop('successMetrics'));
export const selectApplicationReports = createSelector(selectRetentionSlice, prop('applicationReports'));
export const selectApplicationReportsStages = createSelector(selectApplicationReports, prop('stages'));
export const selectApplicationReportsParent = createSelector(selectRetentionSlice, prop('applicationReportsParent'));
export const selectApplicationReportsParentStages = createSelector(selectApplicationReportsParent, prop('stages'));
export const selectApplicationReportsServerData = createSelector(
  selectRetentionSlice,
  prop('applicationReportsServerData')
);
export const selectApplicationReportsStagesServerData = createSelector(
  selectApplicationReportsServerData,
  prop('stages')
);
export const selectValidationErrors = createSelector(selectRetentionSlice, ({ validationErrors }) => {
  let error = null;
  if (validationErrors) {
    for (const stage in validationErrors) {
      const { age, count } = validationErrors[stage];
      if (hasValidationErrors(age) || hasValidationErrors(count)) {
        error = GLOBAL_FORM_VALIDATION_ERROR;
        break;
      }
    }
  }

  return error;
});
