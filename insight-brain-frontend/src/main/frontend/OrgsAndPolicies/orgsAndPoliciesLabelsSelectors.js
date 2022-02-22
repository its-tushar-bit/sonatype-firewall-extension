/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, isNil } from 'ramda';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectLabelsSlice = createSelector(selectOrgsAndPoliciesSlice, prop('labels'));
export const selectApplicableLabels = createSelector(selectLabelsSlice, prop('applicableLabels'));
export const selectLabelsLoading = createSelector(selectLabelsSlice, prop('loading'));
export const selectLabelsLoadError = createSelector(selectLabelsSlice, prop('loadError'));
export const selectLabelsSubmitError = createSelector(selectLabelsSlice, prop('submitError'));
export const selectLabelsIsDirty = createSelector(selectLabelsSlice, prop('isDirty'));
export const selectLabelsIsEditMode = createSelector(
  selectRouterCurrentParams,
  (currentParams) => !isNil(currentParams.labelId)
);
export const selectLabelsSiblings = createSelector(selectLabelsSlice, prop('siblings'));
export const selectLabelsCurrentLabel = createSelector(selectLabelsSlice, prop('currentLabel'));
