/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, length, groupBy, pipe, pluck, uniq } from 'ramda';
import { initialSortConfiguration } from './repositoriesConfigurationSlice';

const EMPTY_SET = new Set();

export const selectRepositoriesSlice = prop('repositories');

// Select current view type
export const selectCurrentView = createSelector(selectRepositoriesSlice, prop('currentView'));

// View-aware selectors that use currentView
export const selectOriginalRepositories = createSelector(
  selectRepositoriesSlice,
  selectCurrentView,
  (slice, currentView) => {
    const repos = slice?.originalRepositories?.[currentView];
    return repos == null ? [] : repos;
  }
);

export const selectRepositories = createSelector(selectRepositoriesSlice, selectCurrentView, (slice, currentView) => {
  const repos = slice?.repositories?.[currentView];
  return repos == null ? [] : repos;
});
export const selectRepositoriesByManagerInstanceId = createSelector(
  selectRepositories,
  groupBy(prop('managerInstanceId'))
);
export const selectRepositoriesLoading = createSelector(selectRepositoriesSlice, prop('loading'));
export const selectRepositoriesLoadError = createSelector(selectRepositoriesSlice, prop('loadError'));
export const selectRepositoriesDeleteError = createSelector(selectRepositoriesSlice, prop('deleteError'));
export const selectEditRepositoryManagerNameError = createSelector(
  selectRepositoriesSlice,
  prop('editRepositoryManagerNameError')
);
export const selectDeleteModal = createSelector(selectRepositoriesSlice, prop('showDeleteModal'));
export const selectShowEditRepositoryManagerNameModal = createSelector(
  selectRepositoriesSlice,
  prop('showEditRepositoryManagerNameModal')
);
export const selectSubmitMaskState = createSelector(selectRepositoriesSlice, prop('submitMaskState'));
export const selectDeleteModalInfo = createSelector(selectRepositoriesSlice, prop('deleteModalInfo'));
export const selectEditRepositoryManagerNameModalInfo = createSelector(
  selectRepositoriesSlice,
  prop('editRepositoryManagerNameModalInfo')
);
export const selectSortConfiguration = createSelector(
  selectRepositoriesSlice,
  selectCurrentView,
  (slice, currentView) => slice?.sortConfiguration?.[currentView] ?? initialSortConfiguration
);

export const selectRepositoriesLength = createSelector(selectRepositories, length);

export const selectRepositoryPublicIdFilter = createSelector(
  selectRepositoriesSlice,
  selectCurrentView,
  (slice, currentView) => slice?.repositoryPublicIdFilter?.[currentView] || ''
);

export const selectRepositoryFormats = createSelector(
  selectOriginalRepositories,
  pipe(pluck('repository'), pluck('format'), uniq)
);

export const selectRepositoryFormatsFilter = createSelector(
  selectRepositoriesSlice,
  selectCurrentView,
  (slice, currentView) => slice?.repositoryFormatsFilter?.[currentView] || EMPTY_SET
);
