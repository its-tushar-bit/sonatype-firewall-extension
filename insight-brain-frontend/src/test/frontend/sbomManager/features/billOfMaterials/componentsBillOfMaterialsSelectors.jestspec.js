/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectComponentsSbomTileSlice,
  selectComponentsSbomsResults,
  selectComponentsSbomsResultsIsLoading,
  selectSortDir,
  selectComponentsSbomsResultsError,
} from 'MainRoot/sbomManager/features/componentsTile/componentsBillOfMaterialsSelectors.js';

let mockState;

describe('sbomTileSelectors returns the correct state for the following selector:', () => {
  beforeEach(() => {
    mockState = {
      componentsBillOfMaterialsTile: {
        results: null,
        loading: false,
        error: null,
        sortDir: 'asc',
      },
    };
  });

  it('selectComponentsSbomTileSlice', () => {
    expect(selectComponentsSbomTileSlice(mockState)).toEqual(mockState.componentsBillOfMaterialsTile);
  });

  it('selectComponentsSbomsResults', () => {
    expect(selectComponentsSbomsResults(mockState)).toEqual(mockState.componentsBillOfMaterialsTile.results);
  });

  it('selectComponentsSbomsResultsIsLoading', () => {
    expect(selectComponentsSbomsResultsIsLoading(mockState)).toEqual(mockState.componentsBillOfMaterialsTile.loading);
  });

  it('selectSortDir', () => {
    expect(selectSortDir(mockState)).toEqual(mockState.componentsBillOfMaterialsTile.sortDir);
  });

  it('selectComponentsSbomsResultsError', () => {
    expect(selectComponentsSbomsResultsError(mockState)).toEqual(mockState.componentsBillOfMaterialsTile.error);
  });
});
