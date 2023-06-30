/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

const integrationsSlice = prop('integrations');

export const appsWithoutCiIntegrationsSelector = createSelector(integrationsSlice, prop('appsWithoutCiIntegrations'));

export const selectPageSize = createSelector(appsWithoutCiIntegrationsSelector, prop('pageSize'));

export const selectCurrentPage = createSelector(appsWithoutCiIntegrationsSelector, prop('currentPage'));

export const selectSort = createSelector(appsWithoutCiIntegrationsSelector, prop('sort'));

export const selectFilter = createSelector(appsWithoutCiIntegrationsSelector, prop('filter'));
