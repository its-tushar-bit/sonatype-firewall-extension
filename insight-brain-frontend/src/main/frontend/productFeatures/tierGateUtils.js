/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';

/**
 * Creates a memoized selector that suppresses the dirty state when the feature
 * is not entitled. When a Pro user has no entitlement for a tier-gated feature,
 * no form on that feature's pages is saveable — the UI renders either a
 * read-only view (Custom preview) or a "Changes can't be saved" preview form
 * ("Preview Add ..."). In both cases a truthy isDirty is never actionable and
 * should not trigger the unsaved-changes modal.
 *
 * @param {Function} originalIsDirty - The original isDirty selector
 * @param {Function} hasFeatureSelector - Selector that returns true if the feature is entitled
 * @returns {Function} A memoized selector that returns the appropriate dirty state
 */
export const createTierGatedDirtySelector = (originalIsDirty, hasFeatureSelector) => {
  return createSelector(hasFeatureSelector, originalIsDirty, (hasFeature, isDirty) => {
    if (!hasFeature) {
      return false;
    }
    return isDirty;
  });
};
