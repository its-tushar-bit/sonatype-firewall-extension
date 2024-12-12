/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectComponentPagination,
  selectSelectedComponent,
  selectSelectedComponentIndex,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';

describe('selectComponentDetails', () => {
  const uiRouterStateService = {
    href: jest.fn((routeName, params) => `/route/${params.componentHash}`),
  };

  const createState = () => ({
    router: { currentState: { name: 'routeName' }, currentParams: { componentHash: 'hash3' } },
    sbomComponentDetailsPage: {
      componentDetailsPaginationData: {
        pagination: {
          currentPage: 0,
          pageCount: 3,
        },
        pagesData: {
          0: [{ hash: 'hash1' }, { hash: 'hash2' }, { hash: 'hash3' }],
          1: [{ hash: 'hash4' }, { hash: 'hash5' }, { hash: 'hash6' }],
        },
        totalNumberOfComponents: 6,
      },
    },
  });

  describe('selectComponentPagination', () => {
    it('should return correct pagination data', () => {
      const state = createState();
      const result = selectComponentPagination(state, uiRouterStateService);
      expect(result).toEqual({
        next: '/route/hash4',
        prev: '/route/hash2',
        currentPage: 3,
        pageCount: 6,
      });
    });

    it('should return null if index is undefined', () => {
      const state = createState();
      const stateWithUndefinedIndex = {
        ...state,
        sbomComponentDetailsPage: {
          ...state.sbomComponentDetailsPage,
          componentDetailsPaginationData: {
            ...state.sbomComponentDetailsPage.componentDetailsPaginationData,
            pagesData: {
              1: [],
            },
          },
        },
      };
      const result = selectComponentPagination(stateWithUndefinedIndex, uiRouterStateService);
      expect(result).toBeNull();
    });
  });

  describe('selectSelectedComponent', () => {
    it('should return the selected component based on componentHash', () => {
      const state = createState();
      const result = selectSelectedComponent(state);
      expect(result).toEqual({ hash: 'hash3' });
    });

    it('should return undefined if componentHash does not match any component', () => {
      const state = createState();
      const stateWithInvalidHash = {
        ...state,
        router: { ...state.router, currentParams: { componentHash: 'invalidHash' } },
      };
      const result = selectSelectedComponent(stateWithInvalidHash);
      expect(result).toBeUndefined();
    });
  });

  describe('selectSelectedComponentIndex', () => {
    it('should return the index of the selected component', () => {
      const state = createState();
      const selectedIndex = selectSelectedComponentIndex(state);
      expect(selectedIndex).toBe(2);
    });

    it('should return -1 if the selected component is not found', () => {
      const state = createState();
      const stateWithInvalidHash = {
        ...state,
        router: { ...state.router, currentParams: { componentHash: 'invalidHash' } },
      };
      const selectedIndex = selectSelectedComponentIndex(stateWithInvalidHash);
      expect(selectedIndex).toBe(-1);
    });
  });
});
