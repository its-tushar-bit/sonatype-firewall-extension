/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectRepositoryComponentsSlice = prop('repositoryComponents');

export const selectComponents = createSelector(selectRepositoryComponentsSlice, prop('components'));
export const selectTotalCount = createSelector(selectRepositoryComponentsSlice, prop('totalCount'));
export const selectPageSize = createSelector(selectRepositoryComponentsSlice, prop('pageSize'));
export const selectCurrentPage = createSelector(selectRepositoryComponentsSlice, prop('currentPage'));
export const selectFilter = createSelector(selectRepositoryComponentsSlice, prop('filter'));
export const selectLoading = createSelector(selectRepositoryComponentsSlice, prop('loading'));
export const selectError = createSelector(selectRepositoryComponentsSlice, prop('error'));
