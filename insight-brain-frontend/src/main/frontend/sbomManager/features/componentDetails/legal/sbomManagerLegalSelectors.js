/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { pick } from 'ramda';
import { selectSbomComponentDetails } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';
import {
  selectIsAdvancedLegalPackSupported,
  selectIsAlpForSbomManagerEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectLicenseDetectionsTileDataSlice } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSelectors';
export const selectSbomManagerComponentDetailsLoadingState = createSelector(
  selectSbomComponentDetails,
  pick(['loading', 'loadError'])
);

export const selectSbomManagerLicenseDetectionsTileDataSlice = createSelector(
  selectSbomManagerComponentDetailsLoadingState,
  selectIsAdvancedLegalPackSupported,
  selectIsAlpForSbomManagerEnabled,
  selectLicenseDetectionsTileDataSlice,
  ({ loading, loadError }, isAdvancedLegalPackSupported, isAlpForSbomManagerEnabled, licenseDetectionsTileData) => {
    const declaredLicenses = licenseDetectionsTileData.declaredLicenses;
    const effectiveLicenses = licenseDetectionsTileData.effectiveLicenses ?? declaredLicenses;
    return {
      loading: loading || licenseDetectionsTileData.loading,
      loadError: loadError || licenseDetectionsTileData.loadError,
      isLoadingComponentDetails: loading,
      componentDetailsLoadError: loadError,
      declaredLicenses,
      effectiveLicenses,
      observedLicenses: licenseDetectionsTileData.observedLicenses,
      licenseOverride: licenseDetectionsTileData.licenseOverride,
      selectableLicenses: licenseDetectionsTileData.selectableLicenses,
      allLicenses: licenseDetectionsTileData.allLicenses,
      hiddenObservedLicenses: licenseDetectionsTileData.hiddenObservedLicenses ?? false,
      supportAlpObservedLicenses: licenseDetectionsTileData.supportAlpObservedLicenses ?? false,
      isAdvancedLegalPackSupported: isAdvancedLegalPackSupported && isAlpForSbomManagerEnabled,
    };
  }
);
