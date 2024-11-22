/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectCurrentPage,
  selectLoadError,
  selectLoading,
  selectOtherVersions,
  selectOtherVersionsSlice,
  selectPageCount,
  selectPageSize,
  selectRouterCurrentParams,
  selectSortAsc,
  selectToken,
} from 'MainRoot/quarantinedComponentReport/otherVersionsTile/otherVersionsSelectors';

describe('otherVersionsSelectors', () => {
  describe('selectLoading', () => {
    const loading = {
      loading: false,
    };

    it('is composed from the following selectors', () => {
      expect(selectLoading.dependencies).toEqual([selectOtherVersionsSlice]);
    });

    it('selects loading from the root of the state', () => {
      const actualSelection = selectLoading.resultFunc(loading);

      expect(actualSelection).toBe(loading.loading);
    });
  });

  describe('selectLoadError', () => {
    const loadError = {
      loadError: null,
    };

    it('is composed from the following selectors', () => {
      expect(selectLoadError.dependencies).toEqual([selectOtherVersionsSlice]);
    });

    it('selects loading from the root of the state', () => {
      const actualSelection = selectLoadError.resultFunc(loadError);

      expect(actualSelection).toBe(loadError.loadError);
    });
  });

  describe('selectOtherVersions', () => {
    const otherVersions = {
      otherVersions: ['a1 : b1 : 2', 'a1 : b1 : 3', 'a1 : b1 : 4'],
    };

    it('is composed from the following selectors', () => {
      expect(selectOtherVersions.dependencies).toEqual([selectOtherVersionsSlice]);
    });

    it('selects loading from the root of the state', () => {
      const actualSelection = selectOtherVersions.resultFunc(otherVersions);

      expect(actualSelection).toBe(otherVersions.otherVersions);
    });
  });

  describe('selectPageCount', () => {
    const pageCount = {
      pageCount: 10,
    };

    it('is composed from the following selectors', () => {
      expect(selectPageCount.dependencies).toEqual([selectOtherVersionsSlice]);
    });

    it('selects loading from the root of the state', () => {
      const actualSelection = selectPageCount.resultFunc(pageCount);

      expect(actualSelection).toBe(pageCount.pageCount);
    });
  });

  describe('selectPageSize', () => {
    const pageSize = {
      pageSize: 5,
    };

    it('is composed from the following selectors', () => {
      expect(selectPageSize.dependencies).toEqual([selectOtherVersionsSlice]);
    });

    it('selects loading from the root of the state', () => {
      const actualSelection = selectPageSize.resultFunc(pageSize);

      expect(actualSelection).toBe(pageSize.pageSize);
    });
  });

  describe('selectCurrentPage', () => {
    const currentPage = {
      currentPage: 10,
    };

    it('is composed from the following selectors', () => {
      expect(selectCurrentPage.dependencies).toEqual([selectOtherVersionsSlice]);
    });

    it('selects loading from the root of the state', () => {
      const actualSelection = selectCurrentPage.resultFunc(currentPage);

      expect(actualSelection).toBe(currentPage.currentPage);
    });
  });

  describe('selectSortAsc', () => {
    const sortAsc = {
      sortAsc: true,
    };

    it('is composed from the following selectors', () => {
      expect(selectSortAsc.dependencies).toEqual([selectOtherVersionsSlice]);
    });

    it('selects loading from the root of the state', () => {
      const actualSelection = selectSortAsc.resultFunc(sortAsc);

      expect(actualSelection).toBe(sortAsc.sortAsc);
    });
  });

  describe('selectToken', () => {
    const token = {
      token: 'token',
    };

    it('is composed from the following selectors', () => {
      expect(selectToken.dependencies).toEqual([selectRouterCurrentParams]);
    });

    it('selects loading from the root of the state', () => {
      const actualSelection = selectToken.resultFunc(token);

      expect(actualSelection).toBe(token.token);
    });
  });
});
