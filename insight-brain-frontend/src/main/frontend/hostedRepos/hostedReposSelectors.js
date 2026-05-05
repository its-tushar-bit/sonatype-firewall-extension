/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectHostedReposSlice = prop('hostedRepos');

export const selectRepositoryManagers = createSelector(
  selectHostedReposSlice,
  prop('repositoryManagers')
);

export const selectLoading = createSelector(
  selectHostedReposSlice,
  prop('loading')
);

export const selectError = createSelector(
  selectHostedReposSlice,
  prop('error')
);
