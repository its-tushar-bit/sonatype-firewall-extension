/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectRetentionSlice = createSelector(selectOrgsAndPoliciesSlice, prop('retention'));
export const selectLoading = createSelector(selectRetentionSlice, prop('loading'));
export const selectLoadError = createSelector(selectRetentionSlice, prop('loadError'));
export const selectApplicationReports = createSelector(selectRetentionSlice, prop('applicationReports'));
export const selectApplicationReportsStages = createSelector(selectApplicationReports, prop('stages'));
export const selectSuccessMetrics = createSelector(selectRetentionSlice, prop('successMetrics'));
