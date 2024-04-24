/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectBillOfMaterialsPage = prop('billOfMaterialsPage');
export const selectInternalApplicationId = createSelector(selectBillOfMaterialsPage, prop('internalAppId'));
export const selectInternalApplicationIdIsLoading = createSelector(selectBillOfMaterialsPage, prop('loading'));
export const selectInternalApplicationIdError = createSelector(selectBillOfMaterialsPage, prop('errorInternalAppId'));
export const selectSbomVersions = createSelector(selectBillOfMaterialsPage, prop('sbomVersions'));
export const selectErrorSbomVersions = createSelector(selectBillOfMaterialsPage, prop('errorSbomVersions'));
