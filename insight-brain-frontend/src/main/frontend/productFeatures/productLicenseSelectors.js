/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, propOr } from 'ramda';

import { isSbomManagerOnlyLicenseProduct } from 'MainRoot/sbomManager/sbomManagerUtil';

export const selectProductLicenseSlice = prop('productLicense');
export const selectLoadingProducts = createSelector(selectProductLicenseSlice, prop('loading'));
export const selectProductLicense = createSelector(selectProductLicenseSlice, prop('license'));
export const selectProducts = createSelector(selectProductLicense, propOr([], 'products'));

export const selectIsSbomManagerOnlyLicense = createSelector(selectProducts, isSbomManagerOnlyLicenseProduct);
