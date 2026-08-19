/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { selectProductEdition, selectLoadingProducts } from './productLicenseSelectors';

// These must match CLMLicenseManager.PRODUCT_LIFECYCLE_PRO / PRODUCT_LIFECYCLE_ENTERPRISE on the backend.
export const TIER_LIFECYCLE_PRO = 'Lifecycle Pro';
export const TIER_LIFECYCLE_ENTERPRISE = 'Lifecycle Enterprise';

export const selectTierLoading = selectLoadingProducts;

export const selectIsPro = createSelector(selectProductEdition, (edition) => edition === TIER_LIFECYCLE_PRO);

export const selectIsEnterprise = createSelector(
  selectProductEdition,
  (edition) => edition === TIER_LIFECYCLE_ENTERPRISE
);
