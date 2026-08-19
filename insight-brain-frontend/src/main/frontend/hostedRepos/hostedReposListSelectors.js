/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectHostedRepositoriesListSlice = (state) => prop('hostedReposList', state);

export const selectRepositories = createSelector(selectHostedRepositoriesListSlice, prop('repositories'));
export const selectLoading = createSelector(selectHostedRepositoriesListSlice, prop('loading'));
export const selectLoadError = createSelector(selectHostedRepositoriesListSlice, prop('loadError'));
export const selectSortConfiguration = createSelector(selectHostedRepositoriesListSlice, prop('sortConfiguration'));
export const selectRepositoryFormatsFilter = createSelector(
  selectHostedRepositoriesListSlice,
  prop('repositoryFormatsFilter')
);

export const selectManagerInstanceId = createSelector(selectHostedRepositoriesListSlice, prop('managerInstanceId'));
export const selectManagerBaseUrl = createSelector(selectHostedRepositoriesListSlice, prop('managerBaseUrl'));
export const selectManagerName = createSelector(selectHostedRepositoriesListSlice, prop('managerName'));

export const selectRepositoryManager = createSelector(
  selectManagerInstanceId,
  selectManagerBaseUrl,
  selectManagerName,
  (instanceId, baseUrl, name) => {
    if (!instanceId) {
      return null;
    }
    return { instanceId, baseUrl: baseUrl || null, name: name || null };
  }
);

export const selectAvailableFormats = createSelector(selectHostedRepositoriesListSlice, prop('availableFormats'));
export const selectSearchText = createSelector(selectHostedRepositoriesListSlice, prop('searchText'));
export const selectTotalCount = createSelector(selectHostedRepositoriesListSlice, prop('totalCount'));
