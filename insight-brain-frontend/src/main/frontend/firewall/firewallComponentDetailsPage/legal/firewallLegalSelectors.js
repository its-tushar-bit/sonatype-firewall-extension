/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { selectFirewallComponentDetailsPage } from 'MainRoot/firewall/firewallSelectors';
import { pickAll } from 'ramda';
import { selectIsAdvancedLegalPackSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

export const selectFirewallComponentLicensesState = createSelector(
  selectFirewallComponentDetailsPage,
  pickAll(['componentLicenses', 'componentLicensesError', 'isLoadingComponentLicenses'])
);

export const selectFirewallLicenseDetectionsTileDataSlice = createSelector(
  selectIsAdvancedLegalPackSupported,
  selectFirewallComponentLicensesState,
  (isAdvancedLegalPackSupported, licensesState) => {
    const { isLoadingComponentLicenses, componentLicenses, componentLicensesError } = licensesState;
    return {
      ...componentLicenses,
      loading: isLoadingComponentLicenses,
      loadError: componentLicensesError,
      isAdvancedLegalPackSupported,
    };
  }
);
