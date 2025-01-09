/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
  SORT_BY_FIELDS,
  SORT_DIRECTION,
  defaultSortConfiguration,
} from 'MainRoot/OrgsAndPolicies/ownerSummary/sbomsTile/sbomsTileSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

const APP_ID = 'abc123';

describe('sbomTileSlice reducers have the correct state when the following reducer is dispatched', function () {
  it('sbomsTile/setCurrentPage', () => {
    const state = {
      currentPage: 1,
    };

    const newState = reducer(state, {
      type: 'sbomsTile/setCurrentPage',
      payload: 10,
    });

    expect(newState.currentPage).toBe(10);
  });

  describe('sbomsTile/toggleSortDir', function () {
    it('desc to asc', () => {
      const state = {
        sortDir: 'desc',
      };

      const newState = reducer(state, {
        type: 'sbomsTile/toggleSortDir',
      });

      expect(newState.sortDir).toBe('asc');
    });

    it('asc to desc', () => {
      const state = {
        sortDir: 'asc',
      };

      const newState = reducer(state, {
        type: 'sbomsTile/toggleSortDir',
      });

      expect(newState.sortDir).toBe('desc');
    });
  });

  describe('sbomsTile/setSelectedVersionForActions', function () {
    it('when selectedVersionForActions !== payload', () => {
      const state = {
        selectedVersionForActions: null,
      };

      const newState = reducer(state, {
        type: 'sbomsTile/setSelectedVersionForActions',
        payload: APP_ID,
      });

      expect(newState.selectedVersionForActions).toBe(APP_ID);
    });

    it('when selectedVersionForActions === payload', () => {
      const state = {
        sortDir: 'asc',
      };

      const newState = reducer(state, {
        type: 'sbomsTile/toggleSortDir',
      });

      expect(newState.sortDir).toBe('desc');
    });
  });

  describe('sbomsTile/loadSbomTableData', function () {
    it('/pending', () => {
      const state = {
        results: null,
        loading: false,
      };

      const newState = reducer(state, {
        type: 'sbomsTile/loadSbomTableData/pending',
      });

      expect(newState.results).toBe(null);
      expect(newState.loading).toBe(true);
    });

    it('/failed', () => {
      const state = {
        error: null,
        loading: false,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomsTile/loadSbomTableData/rejected',
        payload: payload,
      });

      expect(newState.error).toBe('payload error');
      expect(newState.loading).toBe(false);
    });

    it('/fulfilled', () => {
      const state = {
        sboms: null,
        sbomsTotalCount: null,
        loading: false,
        pageCount: 0,
        applicationId: null,
      };

      const sbomResult = {
        applicationVersion: 'v1',
        spec: 'SPDX',
        specVersion: '2.1',
        importDate: '2020-01-01T00:00:00.000+00:00',
        none: 0,
        low: 0,
        medium: 0,
        high: 0,
        critical: 0,
      };

      const payload = {
        totalResultsCount: 1,
        results: [sbomResult],
        applicationId: APP_ID,
      };

      const newState = reducer(state, {
        type: 'sbomsTile/loadSbomTableData/fulfilled',
        payload: payload,
      });

      expect(newState.sboms).toHaveLength(1);
      expect(newState.sboms[0].applicationVersion).toBe('v1');
      expect(newState.loading).toBe(false);
      expect(newState.pageCount).toBe(1);
      expect(newState.applicationId).toBe(APP_ID);
    });
  });

  describe('sbomsTile/deleteSbomFromTable', function () {
    it('/pending', () => {
      const state = {
        deleteMaskState: null,
      };

      const newState = reducer(state, {
        type: 'sbomsTile/deleteSbomFromTable/pending',
      });

      expect(newState.deleteMaskState).toBe(false);
    });

    it('/failed', () => {
      const state = {
        deleteError: null,
        deleteMaskState: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomsTile/deleteSbomFromTable/rejected',
        payload: payload,
      });

      expect(newState.deleteError).toBe('payload error');
      expect(newState.deleteMaskState).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        deleteError: null,
        deleteMaskState: null,
      };

      const newState = reducer(state, {
        type: 'sbomsTile/deleteSbomFromTable/fulfilled',
      });

      expect(newState.deleteError).toBe(null);
      expect(newState.deleteMaskState).toBe(true);
    });
  });

  describe('setSortByAndCycleDirection', () => {
    it('should only cycle between ASC and DESC when sortBy is set to the default field', () => {
      const state = Object.freeze({ ...initialState });

      const newState1 = reducer(state, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState3 = reducer(newState2, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState3.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState3.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
    });

    it('should cycle a non-default field correctly', () => {
      const state = Object.freeze({ ...initialState });

      const newState1 = reducer(state, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.releaseStatus,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.releaseStatus);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.releaseStatus,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.releaseStatus);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState3 = reducer(newState2, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.releaseStatus,
      });

      expect(newState3.sortConfiguration).toEqual(defaultSortConfiguration);
    });

    it('should cycle a non-default field correctly after cycling the default field', () => {
      const state = Object.freeze({ ...initialState });

      const newState0 = reducer(state, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.releaseStatus,
      });

      expect(newState0.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.releaseStatus);
      expect(newState0.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState1 = reducer(newState0, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState2 = reducer(newState1, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState3 = reducer(newState2, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.releaseStatus,
      });

      expect(newState3.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.releaseStatus);
      expect(newState3.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
    });

    it('should cycle the default field correctly after cycling a non-default field', () => {
      const state = Object.freeze({ ...initialState });

      const newState0 = reducer(state, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.releaseStatus,
      });

      expect(newState0.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.releaseStatus);
      expect(newState0.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState1 = reducer(newState0, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.releaseStatus,
      });

      expect(newState1.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.releaseStatus);
      expect(newState1.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

      const newState2 = reducer(newState1, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState2.sortConfiguration.sortBy).toBe(SORT_BY_FIELDS.importDate);
      expect(newState2.sortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

      const newState3 = reducer(newState2, {
        type: 'sbomsTile/setSortByAndCycleDirection',
        payload: SORT_BY_FIELDS.importDate,
      });

      expect(newState3.sortConfiguration).toEqual(defaultSortConfiguration);
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        results: [1, 2, 3],
        numResults: 20,
        loading: true,
        error: 'some error',
        deleteError: 'delete error',
        currentPage: 10,
        pageCount: 2,
        selectedVersionForActions: '1.0',
        applicationId: 'someAppId',
        sortDir: 'asc',
        deleteMaskState: true,
        showDeleteModal: true,
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
