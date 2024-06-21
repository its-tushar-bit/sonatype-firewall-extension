/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  SORT_DIRECTION,
} from 'MainRoot/sbomManager/features/dashboard/recentlyImportedSbomsTile/recentlyImportedSbomsTileSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('RecentlyImportedSbomsTile', () => {
  const initialState = Object.freeze({
    loading: true,
    loadingErrorMessage: null,
    sboms: null,
    sortDirection: SORT_DIRECTION.UNSORTED,
  });

  const sbomTemplate = ({ applicationName, importDate }) =>
    Object.freeze({
      applicationName,
      publicApplicationId: applicationName + '-id',
      sbomVersion: '1.2.3',
      specification: 'SPDX',
      importDate,
      criticalCount: 1,
      highCount: 2,
      mediumCount: 3,
      lowCount: 4,
    });

  const generateResponse = (parameters) => parameters.map(sbomTemplate);

  const parameters = [
    {
      applicationName: 'alice',
      importDate: '2024-01-03T00:00:00.000+00:00',
    },
    {
      applicationName: 'bob',
      importDate: '2024-01-02T00:00:00.000+00:00',
    },
    {
      applicationName: 'chesire',
      importDate: '2024-01-01T00:00:00.000+00:00',
    },
  ];

  describe('recentlyImportedSbomsTile/loadRecentlyImportedSboms', function () {
    it('/pending', () => {
      const newState = reducer(initialState, {
        type: 'recentlyImportedSbomsTile/loadRecentlyImportedSboms/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.loadingErrorMessage).toBe(null);
      expect(newState.sboms).toBe(null);
      expect(newState.sortDirection).toBe(SORT_DIRECTION.UNSORTED);
    });

    it('/fulfilled', () => {
      const sboms = generateResponse(parameters);
      const newState = reducer(initialState, {
        type: 'recentlyImportedSbomsTile/loadRecentlyImportedSboms/fulfilled',
        payload: sboms,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadingErrorMessage).toBe(null);
      expect(newState.sboms).toEqual(sboms);
    });

    it('/rejected', () => {
      const payload = {
        response: {
          data: 'payload-error-message',
        },
      };

      const newState = reducer(initialState, {
        type: 'recentlyImportedSbomsTile/loadRecentlyImportedSboms/rejected',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadingErrorMessage).toBe('payload-error-message');
      expect(newState.sboms).toBe(null);
    });
  });

  describe('cycleNextSortDirection', () => {
    it('should cycle the next sort direction', () => {
      const newState = reducer(initialState, {
        type: 'recentlyImportedSbomsTile/cycleNextSortDirection',
      });
      expect(newState.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState1 = reducer(newState, {
        type: 'recentlyImportedSbomsTile/cycleNextSortDirection',
      });
      expect(newState1.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState2 = reducer(newState1, {
        type: 'recentlyImportedSbomsTile/cycleNextSortDirection',
      });
      expect(newState2.sortDirection).toBe(SORT_DIRECTION.UNSORTED);
    });
  });

  describe('sortSboms', () => {
    it('should sort by date descending when sort direction is "unsorted"', () => {
      const state = {
        sortDirection: SORT_DIRECTION.UNSORTED,
        sboms: generateResponse([
          { applicationName: 'Alice', importDate: '2024-01-01T00:00:00.000+00:00' },
          { applicationName: 'Bob', importDate: '2024-01-02T00:00:00.000+00:00' },
          { applicationName: 'Chesire', importDate: '2024-01-03T00:00:00.000+00:00' },
        ]),
      };
      const newState = reducer(state, {
        type: 'recentlyImportedSbomsTile/sortSboms',
      });
      expect(newState.sboms).toEqual(
        generateResponse([
          { applicationName: 'Chesire', importDate: '2024-01-03T00:00:00.000+00:00' },
          { applicationName: 'Bob', importDate: '2024-01-02T00:00:00.000+00:00' },
          { applicationName: 'Alice', importDate: '2024-01-01T00:00:00.000+00:00' },
        ])
      );
    });

    it('should sort by applicationName ascending when sort direction is "ascending"', () => {
      const state = {
        sortDirection: SORT_DIRECTION.ASC,
        sboms: generateResponse([
          { applicationName: 'Chesire', importDate: '2024-01-00T00:00:00.000+00:00' },
          { applicationName: 'Bob', importDate: '2024-01-00T00:00:00.000+00:00' },
          { applicationName: 'Alice', importDate: '2024-01-00T00:00:00.000+00:00' },
        ]),
      };
      const newState = reducer(state, {
        type: 'recentlyImportedSbomsTile/sortSboms',
      });
      expect(newState.sboms).toEqual(
        generateResponse([
          { applicationName: 'Alice', importDate: '2024-01-00T00:00:00.000+00:00' },
          { applicationName: 'Bob', importDate: '2024-01-00T00:00:00.000+00:00' },
          { applicationName: 'Chesire', importDate: '2024-01-00T00:00:00.000+00:00' },
        ])
      );
    });

    it('should sort by applicationName descending when sort direction is "descending"', () => {
      const state = {
        sortDirection: SORT_DIRECTION.DESC,
        sboms: generateResponse([
          { applicationName: 'Alice', importDate: '2024-01-00T00:00:00.000+00:00' },
          { applicationName: 'Bob', importDate: '2024-01-00T00:00:00.000+00:00' },
          { applicationName: 'Chesire', importDate: '2024-01-00T00:00:00.000+00:00' },
        ]),
      };
      const newState = reducer(state, {
        type: 'recentlyImportedSbomsTile/sortSboms',
      });
      expect(newState.sboms).toEqual(
        generateResponse([
          { applicationName: 'Chesire', importDate: '2024-01-00T00:00:00.000+00:00' },
          { applicationName: 'Bob', importDate: '2024-01-00T00:00:00.000+00:00' },
          { applicationName: 'Alice', importDate: '2024-01-00T00:00:00.000+00:00' },
        ])
      );
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const sboms = generateResponse(parameters);
      const state = Object.freeze({
        loading: false,
        loadingErrorMessage: 'payload-error-message',
        sboms,
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
