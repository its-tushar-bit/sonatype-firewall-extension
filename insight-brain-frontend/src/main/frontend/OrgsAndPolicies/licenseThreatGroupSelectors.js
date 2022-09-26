/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, isNil } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectLicenseThreatGroupSlice = createSelector(selectOrgsAndPoliciesSlice, prop('licenseThreatGroups'));
export const selectIsLoading = createSelector(selectLicenseThreatGroupSlice, prop('loading'));
export const selectLicenseThreatGroupLoadError = createSelector(selectLicenseThreatGroupSlice, prop('loadError'));
export const selectLicenseThreatGroupSubmitError = createSelector(selectLicenseThreatGroupSlice, prop('submitError'));
export const selectLicenseThreatGroupIsDirty = createSelector(selectLicenseThreatGroupSlice, prop('isDirty'));
export const selectSubmitMaskState = createSelector(selectLicenseThreatGroupSlice, prop('submitMaskState'));
export const selectDeleteMaskState = createSelector(selectLicenseThreatGroupSlice, prop('deleteMaskState'));
export const selectDeleteError = createSelector(selectLicenseThreatGroupSlice, prop('deleteError'));

export const selectValidationError = createSelector(selectLicenseThreatGroupSlice, prop('validationError'));
export const selectLicenseThreatGroupIsEditMode = createSelector(
  selectRouterCurrentParams,
  (currentParams) => !isNil(prop('licenseThreatGroupId', currentParams))
);
export const selectLicenseThreatGroupId = createSelector(selectRouterCurrentParams, prop('licenseThreatGroupId'));
export const selectCurrentLicenseThreatGroup = createSelector(
  selectLicenseThreatGroupSlice,
  prop('currentLicenseThreatGroup')
);
export const selectNextLicenseThreatGroup = createSelector(
  selectLicenseThreatGroupSlice,
  prop('nextLicenseThreatGroup')
);
export const selectApplicableLicenseThreatGroup = createSelector(
  selectLicenseThreatGroupSlice,
  prop('applicableLicenseThreatGroups')
);
export const selectLicenseThreatGroupSiblings = createSelector(selectLicenseThreatGroupSlice, prop('siblings'));
export const selectAvailableLicenses = createSelector(selectLicenseThreatGroupSlice, prop('availableLicenses'));
export const selectDirtyLicenseThreatGroup = createSelector(selectLicenseThreatGroupSlice, prop('dirtyLTG'));
