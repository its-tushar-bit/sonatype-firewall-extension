/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export const selectSbomsTile = createSelector(selectOrgsAndPoliciesSlice, prop('sbomsTile'));
export const selectSbomsResults = createSelector(selectSbomsTile, prop('results'));
export const selectError = createSelector(selectSbomsTile, prop('error'));
export const selectDeleteError = createSelector(selectSbomsTile, prop('deleteError'));
export const selectCurrentPage = createSelector(selectSbomsTile, prop('currentPage'));
export const selectShowDeleteModal = createSelector(selectSbomsTile, prop('showDeleteModal'));
export const selectDeleteMaskState = createSelector(selectSbomsTile, prop('deleteMaskState'));
export const selectSortDir = createSelector(selectSbomsTile, prop('sortDir'));
export const selectPageCount = createSelector(selectSbomsTile, prop('pageCount'));
export const selectVersionForActions = createSelector(selectSbomsTile, prop('selectedVersionForActions'));
export const selectApplicationId = createSelector(selectSbomsTile, prop('applicationId'));
export const selectLoading = createSelector(selectSbomsTile, prop('loading'));
