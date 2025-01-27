/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { all, includes, length, none, prop, propOr } from 'ramda';

import { isSbomManagerOnlyLicenseProduct } from 'MainRoot/sbomManager/sbomManagerUtil';

const firewallLicenseProducts = [
  'Sonatype Repository Firewall',
  'Sonatype Firewall for Artifactory',
  'Sonatype Lifecycle Firewall SaaS',
  'Sonatype Lifecycle Firewall Cloud',
];

export const selectProductLicenseSlice = prop('productLicense');
export const selectLoadingProducts = createSelector(selectProductLicenseSlice, prop('loading'));
export const selectProductLicense = createSelector(selectProductLicenseSlice, prop('license'));
export const selectProducts = createSelector(selectProductLicense, propOr([], 'products'));

export const selectIsSbomManagerOnlyLicense = createSelector(selectProducts, isSbomManagerOnlyLicenseProduct);

export const isFirewallOnlyLicenseProduct = (products) =>
  length(products) > 0 && all((product) => includes(product, firewallLicenseProducts), products);

export const isNotFirewallLicenseProduct = (products) =>
  length(products) > 0 && none((product) => includes(product, firewallLicenseProducts), products);

export const selectIsFirewallOnlyLicense = createSelector(selectProducts, isFirewallOnlyLicenseProduct);
