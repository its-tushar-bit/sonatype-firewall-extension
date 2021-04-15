/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import legalDashboardReducer from '../../../../main/frontend/legal/dashboard/legalDashboardReducer';
import {
  LEGAL_DASHBOARD_LOAD_RESULTS_FAILED,
  LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED,
  LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED,
  LEGAL_DASHBOARD_FETCH_BACKEND_PAGE,
  LEGAL_DASHBOARD_CHANGE_SORT_FIELD,
} from '../../../../main/frontend/legal/dashboard/legalDashboardActions';

const otherObject = { value: 'test value' };

describe('legalDashboardReducer', function () {
  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = legalDashboardReducer(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = legalDashboardReducer(undefined, action);

      expect(newState.loading).toBeFalsy();
      expect(newState.loadError).toBeNull();
      expect(newState.applications).toEqual({
        results: [],
        error: null,
        sortField: null,
        totalResultsCount: 0,
        backendPage: 1,
        loading: false,
      });
      expect(newState.components).toEqual({
        results: [],
        error: null,
        sortField: null,
      });
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = { foo: 'bar' };
      const action = {
        type: 'UNKNOWN',
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED action', function () {
    it('resets applications state', function () {
      const state = Object.freeze({
        components: { results: [], error: 'foo' },
        applications: { results: [], numResults: 0, error: 'foo' },
        other: otherObject,
      });
      const action = {
        type: LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED,
        payload: 'applications',
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.applications.results).toEqual([]);
      expect(newState.applications.error).toBeNull();
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED action', function () {
    it('updates applications results and classyBrew', function () {
      const state = Object.freeze({
        components: { results: [] },
        applications: { results: [] },
        other: otherObject,
      });
      const action = {
        type: LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED,
        payload: {
          resultsType: 'applications',
          results: {
            results: [{ foo: 'bar' }],
            totalResultsCount: 1,
          },
        },
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.applications.results).toBe(action.payload.results.results);
      expect(newState.applications.totalResultsCount).toBe(action.payload.results.totalResultsCount);
      expect(newState.applications.loading).toBeFalsy();
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
  describe('LEGAL_DASHBOARD_LOAD_RESULTS_FAILED action', function () {
    it('sets error in applications state', function () {
      const state = Object.freeze({
        components: { error: {} },
        applications: { error: null },
        currentTab: 'applications',
        other: otherObject,
      });
      const action = {
        type: LEGAL_DASHBOARD_LOAD_RESULTS_FAILED,
        payload: {
          resultsType: 'applications',
          error: 'error',
        },
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.applications.error).toBe(action.payload.error);
      expect(newState.applications.loading).toBeFalsy();
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
  describe('LEGAL_DASHBOARD_FETCH_BACKEND_PAGE action', function () {
    it('sets the backend page in applications state', function () {
      const state = Object.freeze({
        components: { error: {} },
        applications: { error: null },
        currentTab: 'applications',
        other: otherObject,
      });
      const action = {
        type: LEGAL_DASHBOARD_FETCH_BACKEND_PAGE,
        payload: {
          resultsType: 'applications',
          page: 10,
        },
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.applications.backendPage).toBe(action.payload.page);
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
  describe('LEGAL_DASHBOARD_CHANGE_SORT_FIELD action', function () {
    it('sets the sort field in applications state', function () {
      const state = Object.freeze({
        components: { error: {} },
        applications: { error: null },
        currentTab: 'applications',
        other: otherObject,
      });
      const action = {
        type: LEGAL_DASHBOARD_CHANGE_SORT_FIELD,
        payload: {
          resultsType: 'applications',
          sortField: 'LAST_SCAN_TIME_ASC',
        },
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.applications.sortField).toBe(action.payload.sortField);
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
