/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectSbomsTile,
  selectSbomsResults,
  selectError,
  selectDeleteError,
  selectCurrentPage,
  selectShowDeleteModal,
  selectDeleteMaskState,
  selectSortDir,
  selectPageCount,
  selectVersionForActions,
  selectApplicationId,
  selectLoading,
} from 'MainRoot/OrgsAndPolicies/ownerSummary/sbomsTile/sbomsTileSelectors.js';

describe('sbomTileSelectors returns the correct state for the following selector:', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        sbomsTile: {
          results: null,
          numResults: null,
          loading: false,
          error: null,
          deleteError: null,
          currentPage: 0,
          pageCount: 0,
          selectedVersionForActions: null,
          applicationId: null,
          sortDir: 'desc',
          deleteMaskState: null,
          showDeleteModal: false,
        },
      },
    };
  });

  it('selectSbomsTile', () => {
    expect(selectSbomsTile(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile);
  });

  it('selectSbomsResults', () => {
    expect(selectSbomsResults(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.results);
  });

  it('selectError', () => {
    expect(selectError(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.error);
  });

  it('selectDeleteError', () => {
    expect(selectDeleteError(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.deleteError);
  });

  it('selectCurrentPage', () => {
    expect(selectCurrentPage(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.currentPage);
  });

  it('selectShowDeleteModal', () => {
    expect(selectShowDeleteModal(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.showDeleteModal);
  });

  it('selectDeleteMaskState', () => {
    expect(selectDeleteMaskState(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.deleteMaskState);
  });

  it('selectSortDir', () => {
    expect(selectSortDir(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.sortDir);
  });

  it('selectPageCount', () => {
    expect(selectPageCount(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.pageCount);
  });

  it('selectVersionForActions', () => {
    expect(selectVersionForActions(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.selectedVersionForActions);
  });

  it('selectApplicationId', () => {
    expect(selectApplicationId(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.applicationId);
  });

  it('selectLoading', () => {
    expect(selectLoading(mockState)).toEqual(mockState.orgsAndPolicies.sbomsTile.loading);
  });
});
