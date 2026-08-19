/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { pick, prop } from 'ramda';
import { selectIsAdvancedLegalPackSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

export const selectComponentDetailsLicenseDetectionsTileSlice = prop('componentDetailsLicenseDetectionsTile');

export const selectLicenseDetectionsTileDataSlice = createSelector(
  selectComponentDetailsLicenseDetectionsTileSlice,
  selectIsAdvancedLegalPackSupported,
  (data, isAdvancedLegalPackSupported) => ({
    ...pick(
      [
        'licenseOverride',
        'declaredLicenses',
        'effectiveLicenses',
        'observedLicenses',
        'selectableLicenses',
        'allLicenses',
        'hiddenObservedLicenses',
        'supportAlpObservedLicenses',
        'loading',
        'loadError',
      ],
      data
    ),
    isAdvancedLegalPackSupported,
  })
);

export const selectShowEditLicensesPopover = createSelector(
  selectComponentDetailsLicenseDetectionsTileSlice,
  prop('showEditLicensesPopover')
);

export const selectEditLicensesForm = createSelector(
  selectComponentDetailsLicenseDetectionsTileSlice,
  prop('editLicensesForm')
);

export const selectEditLicensesFormIsDirty = createSelector(
  selectComponentDetailsLicenseDetectionsTileSlice,
  (state) => state.editLicensesForm.isDirty
);

export const selectIsUnsavedChangesModalActive = createSelector(
  selectComponentDetailsLicenseDetectionsTileSlice,
  (state) => state.editLicensesForm.showUnsavedChangesModal
);
