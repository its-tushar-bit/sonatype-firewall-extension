/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectStagesSlice = prop('stages');
export const selectStagesDashboard = createSelector(selectStagesSlice, prop('dashboard'));
export const selectStagesLoadingError = createSelector(selectStagesDashboard, prop('error'));
export const selectStagesIsLoading = createSelector(selectStagesDashboard, prop('loading'));
export const selectStageTypes = createSelector(selectStagesDashboard, prop('stageTypes'));
