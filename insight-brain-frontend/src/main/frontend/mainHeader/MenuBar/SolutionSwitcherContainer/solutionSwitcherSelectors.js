/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectSolutionSwitcherSlice = prop('solutionSwitcher');

export const selectLicensedSolutions = createSelector(selectSolutionSwitcherSlice, prop('licensedSolutions'));
export const selectIsFetched = createSelector(selectSolutionSwitcherSlice, prop('isFetched'));
export const selectLoading = createSelector(selectSolutionSwitcherSlice, prop('loading'));

export const selectAiDeveloperLicensedSolution = createSelector(selectLicensedSolutions, (licensedSolutions) =>
  // 'guide' must match GUIDE_SOLUTION_ID in solutionSwitcherSlice.js — no cross-import with Guide code (see CLAUDE.md).
  licensedSolutions.find((solution) => solution.id === 'guide')
);

export const selectIsAiDeveloperEntitled = createSelector(selectAiDeveloperLicensedSolution, (aiDeveloperSolution) =>
  Boolean(aiDeveloperSolution)
);

export const selectAiDeveloperUrl = createSelector(
  selectAiDeveloperLicensedSolution,
  (aiDeveloperSolution) => aiDeveloperSolution?.url
);
