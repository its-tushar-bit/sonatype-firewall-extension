/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { path, prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

const selectWaiverSlice = prop('waivers');
const selectBulkWaiveSlice = createSelector(selectWaiverSlice, path(['bulkWaive']));
const selectPermissionsSlice = createSelector(selectWaiverSlice, path(['permissions']));

export const selectBulkWaiverSelectedViolations = createSelector(selectBulkWaiveSlice, path(['selectedViolations']));

export const selectBulkWaiverCheckboxState = createSelector(selectBulkWaiveSlice, path(['checkboxState']));

export const selectBulkWaiverSelectAllChecked = createSelector(selectBulkWaiveSlice, path(['selectAllChecked']));

export const selectBulkWaiverConfiguration = createSelector(selectBulkWaiveSlice, path(['waiverConfiguration']));

const selectPermissionsByApplicationId = createSelector(selectPermissionsSlice, path(['byApplicationId']));

const selectPermissionsLoadingMap = createSelector(selectPermissionsSlice, path(['loading']));

const selectPermissionsErrorMap = createSelector(selectPermissionsSlice, path(['error']));

// Parameterized selectors for per-publicId access
export const selectCanWaivePolicyViolations = (state, publicId) => {
  const byApplicationId = selectPermissionsByApplicationId(state);
  return byApplicationId[publicId] || false;
};

export const selectPermissionsLoading = (state, publicId) => {
  const loadingMap = selectPermissionsLoadingMap(state);
  return loadingMap[publicId] || false;
};

export const selectPermissionsError = (state, publicId) => {
  const errorMap = selectPermissionsErrorMap(state);
  return errorMap[publicId] || null;
};
export const isUnknownComponent = (violation) => {
  return !violation.matchState || violation.matchState === 'unknown';
};

export const selectHasUnknownViolations = createSelector(selectBulkWaiverSelectedViolations, (violations) => {
  return violations.some(isUnknownComponent);
});

export const selectHasIdentifiedViolations = createSelector(selectBulkWaiverSelectedViolations, (violations) => {
  return violations.some((v) => !isUnknownComponent(v));
});

// Selector to check if there is a mix of unknown and identified violations
export const selectHasMixedViolations = createSelector(
  selectHasUnknownViolations,
  selectHasIdentifiedViolations,
  (hasUnknown, hasIdentified) => {
    return hasUnknown && hasIdentified;
  }
);

// Selector to check if all selected violations are unknown
export const selectOnlyUnknownViolations = createSelector(
  selectBulkWaiverSelectedViolations,
  selectHasIdentifiedViolations,
  (violations, hasIdentified) => {
    return violations.length > 0 && !hasIdentified;
  }
);
