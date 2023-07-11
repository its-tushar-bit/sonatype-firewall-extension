/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, ifElse, isNil, always, identity, compose, length, groupBy, pipe, pluck, uniq } from 'ramda';

export const selectRepositoriesSlice = prop('repositories');

export const selectOriginalRepositories = createSelector(
  selectRepositoriesSlice,
  compose(ifElse(isNil(), always([]), identity), prop('originalRepositories'))
);
export const selectRepositories = createSelector(
  selectRepositoriesSlice,
  compose(ifElse(isNil(), always([]), identity), prop('repositories'))
);
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
export const selectSortConfiguration = createSelector(selectRepositoriesSlice, prop('sortConfiguration'));

export const selectRepositoriesLength = createSelector(selectRepositories, length);

export const selectRepositoryPublicIdFilter = createSelector(selectRepositoriesSlice, prop('repositoryPublicIdFilter'));

export const selectRepositoryFormats = createSelector(
  selectOriginalRepositories,
  pipe(pluck('repository'), pluck('format'), uniq)
);
export const selectRepositoryFormatsFilter = createSelector(selectRepositoriesSlice, prop('repositoryFormatsFilter'));
