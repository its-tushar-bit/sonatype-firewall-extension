/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import * as R from 'ramda';

import { isSbomManagerOnlyLicenseProduct } from 'MainRoot/sbomManager/sbomManagerUtil';

export const PRODUCT_LICENSES = Object.freeze({
  lifecycle: ['Sonatype Lifecycle', 'Sonatype Lifecycle SaaS'],
  firewall: [
    'Sonatype Repository Firewall',
    'Sonatype Firewall for Artifactory',
    'Sonatype Lifecycle Firewall SaaS',
    'Sonatype Lifecycle Firewall Cloud',
  ],
  sbomManager: ['Sonatype SBOM Manager', 'Sonatype SBOM Manager SaaS'],
});

export const selectProductLicenseSlice = R.prop('productLicense');
export const selectLoadingProducts = createSelector(selectProductLicenseSlice, R.prop('loading'));
export const selectProductLicense = createSelector(selectProductLicenseSlice, R.prop('license'));
export const selectProducts = createSelector(selectProductLicense, R.propOr([], 'products'));

export const selectIsSbomManagerOnlyLicense = createSelector(selectProducts, isSbomManagerOnlyLicenseProduct);

export const isFirewallOnlyLicenseProduct = (products) =>
  R.length(products) > 0 && R.all(R.includes(R.__, PRODUCT_LICENSES.firewall), products);

export const isNotFirewallLicenseProduct = (products) =>
  R.length(products) > 0 && R.none(R.includes(R.__, PRODUCT_LICENSES.firewall), products);

export const selectIsFirewallOnlyLicense = createSelector(selectProducts, isFirewallOnlyLicenseProduct);

export const selectHasLifecycleLicense = createSelector(
  selectProducts,
  R.any(R.includes(R.__, PRODUCT_LICENSES.lifecycle))
);

export const selectHasFirewallLicense = createSelector(
  selectProducts,
  R.any(R.includes(R.__, PRODUCT_LICENSES.firewall))
);

export const selectHasSbomManagerLicense = createSelector(
  selectProducts,
  R.any(R.includes(R.__, PRODUCT_LICENSES.sbomManager))
);
