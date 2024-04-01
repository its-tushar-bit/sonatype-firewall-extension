/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectComponentsSbomTileSlice = prop('componentsBillOfMaterialsTile');
export const selectComponentsSbomsResults = createSelector(selectComponentsSbomTileSlice, prop('results'));
export const selectComponentsSbomsResultsIsLoading = createSelector(selectComponentsSbomTileSlice, prop('loading'));
export const selectSortDir = createSelector(selectComponentsSbomTileSlice, prop('sortDir'));
export const selectComponentsSbomsResultsError = createSelector(selectComponentsSbomTileSlice, prop('error'));
