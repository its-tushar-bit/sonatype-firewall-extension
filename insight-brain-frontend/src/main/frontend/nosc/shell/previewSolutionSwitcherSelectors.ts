/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';

/**
 * Mirrors the on-disk shape of `state.solutionSwitcher` as defined by
 * `mainHeader/MenuBar/SolutionSwitcherContainer/solutionSwitcherSlice.js`.
 * We keep the type local rather than importing it from Classic so this
 * file doesn't drag the entire Classic module graph into nosc/.
 */
export type LicensedSolution = {
  /** Stable id matching `SolutionIds` in
   *  @sonatype/solution-switcher-react-component. Used as the icon
   *  lookup key and as the React list key. */
  id: 'developer' | 'lifecycle' | 'repo' | 'firewall' | 'sbom' | 'guide';
  /** Display name. */
  name: string;
  /** Target url for the row. */
  url: string;
};

export type SolutionSwitcherSlice = {
  licensedSolutions: LicensedSolution[];
  isFetched: boolean;
  loading: boolean;
  loadError: string | null;
};

export const selectSolutionSwitcherSlice =
  (state: { solutionSwitcher: SolutionSwitcherSlice }): SolutionSwitcherSlice =>
    state.solutionSwitcher;

export const selectLicensedSolutions = createSelector(
  selectSolutionSwitcherSlice,
  (slice) => slice.licensedSolutions ?? []
);

export const selectSolutionSwitcherLoading = createSelector(
  selectSolutionSwitcherSlice,
  (slice) => slice.loading
);

export const selectSolutionSwitcherError = createSelector(
  selectSolutionSwitcherSlice,
  (slice) => slice.loadError
);
