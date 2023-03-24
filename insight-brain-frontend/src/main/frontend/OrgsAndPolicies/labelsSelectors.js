/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, isNil } from 'ramda';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import { hasValidationErrors, GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';

export const selectLabelsSlice = createSelector(selectOrgsAndPoliciesSlice, prop('labels'));
export const selectApplicableLabels = createSelector(selectLabelsSlice, prop('applicableLabels'));
export const selectInheritedLabelsOpen = createSelector(selectLabelsSlice, prop('inheritedLabelsOpen'));
export const selectLabelsLoading = createSelector(selectLabelsSlice, prop('loading'));
export const selectLabelsLoadError = createSelector(selectLabelsSlice, prop('loadError'));
export const selectLabelsSubmitError = createSelector(selectLabelsSlice, prop('submitError'));
export const selectLabelsIsDirty = createSelector(selectLabelsSlice, prop('isDirty'));
export const selectLabelsSubmitMaskState = createSelector(selectLabelsSlice, prop('submitMaskState'));
export const selectLabelsIsEditMode = createSelector(
  selectRouterCurrentParams,
  (currentParams) => !isNil(currentParams.labelId)
);
export const selectLabelsSiblings = createSelector(selectLabelsSlice, prop('siblings'));
export const selectLabelsCurrentLabel = createSelector(selectLabelsSlice, prop('currentLabel'));
export const selectPrevOwnerType = createSelector(selectLabelsSlice, prop('ownerType'));
export const selectPrevOwnerId = createSelector(selectLabelsSlice, prop('ownerId'));

export const selectValidationError = createSelector(selectLabelsSlice, ({ currentLabel }) =>
  hasValidationErrors(currentLabel.label.validationErrors) ||
  hasValidationErrors(currentLabel.description.validationErrors)
    ? GLOBAL_FORM_VALIDATION_ERROR
    : null
);

export const selectLabelsDeleteMaskState = createSelector(selectLabelsSlice, prop('deleteMaskState'));
export const selectLabelsDeleteError = createSelector(selectLabelsSlice, prop('deleteError'));
