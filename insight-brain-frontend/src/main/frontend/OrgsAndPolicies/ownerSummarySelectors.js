/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectOwnerSummarySlice = createSelector(selectOrgsAndPoliciesSlice, prop('ownerSummary'));
export const selectLoading = createSelector(selectOwnerSummarySlice, prop('loading'));
export const selectLoadError = createSelector(selectOwnerSummarySlice, prop('loadError'));
export const selectHasEditIqPermission = createSelector(selectOwnerSummarySlice, prop('hasEditIqPermission'));
