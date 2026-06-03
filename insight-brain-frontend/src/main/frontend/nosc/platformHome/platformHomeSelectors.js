/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';

import { selectLicensedSolutions } from 'MainRoot/mainHeader/MenuBar/SolutionSwitcherContainer/solutionSwitcherSelectors';

const EMPTY_LICENSED_SOLUTIONS = Object.freeze([]);

/**
 * Identifier used for the synthetic "Platform Home" entry prepended to
 * the solution switcher. Callers can use this to recognize the entry
 * (for analytics, styling, or to suppress it in certain surfaces) rather
 * than doing a URL comparison.
 *
 * @type {string}
 */
export const PLATFORM_HOME_ENTRY_ID = 'platform-home';

/**
 * Read-side selector for the configured Platform Home URL exposed by the
 * backend on the system-information slice (see CLM-39608 and Story 2 of
 * the CLM-39639 plan, which adds `platformHomeUrl` to the system-info
 * response). Returns `null` for any deployment that hasn't configured a
 * Platform Home (most on-prem installs), including the common case where
 * the slice itself hasn't been populated yet.
 *
 * @param {object} [state] - Full Redux state.
 * @returns {string|null} The configured URL, or `null` when absent.
 */
export const selectPlatformHomeUrl = (state) => {
  const url = state?.systemInformation?.platformHomeUrl;
  return url == null ? null : url;
};

/**
 * Returns the switcher's licensed-solutions list, optionally augmented
 * with a synthetic "Platform Home" entry prepended at index 0.
 *
 * Behavior:
 *   - When `platformHomeUrl` is null / empty, the selector returns the
 *     input `licensedSolutions` array **by reference** when non-null.
 *     This reference stability matters because the downstream
 *     solution-switcher component memoizes on the array identity;
 *     returning a new array every render would defeat that and cause
 *     spurious re-renders.
 *   - When `platformHomeUrl` is configured, the selector returns a new
 *     array that does NOT share identity with the input (so downstream
 *     code sees the change) but also does not mutate the input.
 *   - A null or undefined `licensedSolutions` is treated as an empty
 *     list, so the Platform Home entry is still rendered while the
 *     backend request is in flight or has failed. When both the URL is
 *     unset and the list is null, a shared frozen empty array is
 *     returned (identity-stable across calls).
 *
 * Memoized via `createSelector` from `@reduxjs/toolkit` (matches the
 * repo-wide convention used by both `solutionSwitcherSelectors.js` and
 * `productFeaturesSelectors.js`).
 *
 * @param {object} state - Full Redux state.
 * @returns {Array<object>} Licensed-solutions list; Platform Home first
 *   when configured.
 */
export const selectLicensedSolutionsWithPlatformHome = createSelector(
  [selectPlatformHomeUrl, selectLicensedSolutions],
  (platformHomeUrl, licensedSolutions) => {
    if (!platformHomeUrl) {
      return licensedSolutions == null ? EMPTY_LICENSED_SOLUTIONS : licensedSolutions;
    }
    const safeLicensed = licensedSolutions == null ? EMPTY_LICENSED_SOLUTIONS : licensedSolutions;
    return [
      {
        id: PLATFORM_HOME_ENTRY_ID,
        name: 'Platform Home',
        url: platformHomeUrl,
        iconName: 'home',
        isPlatformHome: true,
      },
      ...safeLicensed,
    ];
  }
);
