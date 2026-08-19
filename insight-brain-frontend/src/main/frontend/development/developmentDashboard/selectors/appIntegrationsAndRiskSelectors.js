/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

const integrationsSlice = prop('integrations');

export const selectAppIntegrationsAndRiskSlice = createSelector(integrationsSlice, prop('appIntegrationsAndRisk'));

export const selectPageSize = createSelector(selectAppIntegrationsAndRiskSlice, prop('pageSize'));

export const selectCurrentPage = createSelector(selectAppIntegrationsAndRiskSlice, prop('currentPage'));

export const selectSort = createSelector(selectAppIntegrationsAndRiskSlice, prop('sort'));

export const selectNameFilter = createSelector(selectAppIntegrationsAndRiskSlice, prop('nameFilter'));

export const selectCiCdFilter = createSelector(selectAppIntegrationsAndRiskSlice, prop('ciCdFilter'));

export const selectScmFilter = createSelector(selectAppIntegrationsAndRiskSlice, prop('scmFilter'));
