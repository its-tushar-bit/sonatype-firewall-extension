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
  LEGAL_DASHBOARD_SET_PAGE,
  LEGAL_DASHBOARD_COMPONENT_SEARCH,
  LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE,
} from '../../../../main/frontend/legal/dashboard/legalDashboardActions';
import {
  LEGAL_DASHBOARD_APPLY_FILTER_FAILED,
  LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED,
  LEGAL_DASHBOARD_APPLY_SAVED_FILTER_FAILED,
  LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED,
} from 'MainRoot/legal/dashboard/filter/legalDashboardFilterActions';

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
        page: 0,
        backendPage: 1,
        loading: true,
      });
      expect(newState.components).toEqual({
        results: [],
        page: 0,
        backendPage: 1,
        error: null,
        sortField: null,
        loading: true,
        componentNameToSearch: '',
        componentSearchInput: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
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

  describe('LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED action', function () {
    it('resets all tabs state except pagination and preserves the loading flag', function () {
      const state = Object.freeze({
        components: { results: [1, 2], numResults: 2, error: 'foo', page: 1, backendPage: 2, loading: true },
        applications: { results: [3, 4], numResults: 2, error: 'foo', page: 3, backendPage: 4, loading: true },
        other: otherObject,
      });
      const action = { type: LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED };
      const newState = legalDashboardReducer(state, action);
      expect(newState.components.results).toEqual([]);
      expect(newState.components.error).toBeNull();
      expect(newState.components.page).toBe(1);
      expect(newState.components.backendPage).toBe(2);
      expect(newState.applications.results).toEqual([]);
      expect(newState.applications.error).toBeNull();
      expect(newState.applications.page).toBe(3);
      expect(newState.applications.backendPage).toBe(4);
      expect(newState.applications.loading).toBe(true);
      expect(newState.components.loading).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('preserves loading:false so a standalone filter reload does not strand the results spinner', function () {
      const state = Object.freeze({
        components: { results: [1, 2], error: 'foo', page: 1, backendPage: 2, loading: false },
        applications: { results: [3, 4], error: 'foo', page: 3, backendPage: 4, loading: false },
        other: otherObject,
      });
      const action = { type: LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED };
      const newState = legalDashboardReducer(state, action);
      expect(newState.applications.loading).toBe(false);
      expect(newState.components.loading).toBe(false);
    });
  });

  describe('LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED action', function () {
    it('resets all tabs state including pagination', function () {
      const state = Object.freeze({
        components: { results: [1, 2], numResults: 2, error: 'foo', page: 1, backendPage: 2 },
        applications: { results: [3, 4], numResults: 2, error: 'foo', page: 3, backendPage: 4 },
        other: otherObject,
      });
      const action = { type: LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED };
      const newState = legalDashboardReducer(state, action);
      expect(newState.components.results).toEqual([]);
      expect(newState.components.error).toBeNull();
      expect(newState.components.page).toBe(0);
      expect(newState.components.backendPage).toBe(1);
      expect(newState.applications.results).toEqual([]);
      expect(newState.applications.error).toBeNull();
      expect(newState.applications.page).toBe(0);
      expect(newState.applications.backendPage).toBe(1);
      expect(newState.applications.loading).toBe(true);
      expect(newState.components.loading).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
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
      expect(newState.applications.loading).toBe(true);
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('filter apply failure actions', function () {
    [LEGAL_DASHBOARD_APPLY_FILTER_FAILED, LEGAL_DASHBOARD_APPLY_SAVED_FILTER_FAILED].forEach(function (type) {
      it(`clears the loading flag on both tabs for ${type}`, function () {
        const state = Object.freeze({
          components: { results: [], loading: true },
          applications: { results: [], loading: true },
          other: otherObject,
        });
        const newState = legalDashboardReducer(state, { type, payload: 'error message' });
        expect(newState.applications.loading).toBe(false);
        expect(newState.components.loading).toBe(false);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
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
  describe('LEGAL_DASHBOARD_SET_PAGE action', function () {
    it('sets the page in applications state', function () {
      const state = Object.freeze({
        components: { error: {} },
        applications: { error: null },
        currentTab: 'applications',
        other: otherObject,
      });
      const action = {
        type: LEGAL_DASHBOARD_SET_PAGE,
        payload: {
          resultsType: 'applications',
          page: 11,
        },
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.applications.page).toBe(action.payload.page);
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
    it('sets the page in components state', function () {
      const state = Object.freeze({
        applications: { error: {} },
        components: { error: null },
        currentTab: 'applications',
        other: otherObject,
      });
      const action = {
        type: LEGAL_DASHBOARD_SET_PAGE,
        payload: {
          resultsType: 'components',
          page: 5,
        },
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.components.page).toBe(action.payload.page);
      expect(newState.applications).toBe(state.applications);
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
  describe('LEGAL_DASHBOARD_COMPONENT_SEARCH action', function () {
    it('sets the search string for components', function () {
      const searchString = 'aws';
      const state = Object.freeze({
        components: {
          error: {},
          componentNameToSearch: 'searchString',
          componentSearchInput: {
            isPristine: false,
            value: searchString,
            trimmedValue: searchString,
            validationErrors: null,
          },
        },
        applications: { error: null },
        currentTab: 'components',
        other: otherObject,
      });
      const action = {
        type: LEGAL_DASHBOARD_COMPONENT_SEARCH,
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.components.componentNameToSearch).toEqual(searchString);
      expect(newState.actions).toBe(state.actions);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
  describe('LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE action', function () {
    it('sets the string for components search box', function () {
      const state = Object.freeze({
        components: {
          error: {},
          componentNameToSearch: '',
          componentSearchInput: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
        },
        applications: { error: null },
        currentTab: 'components',
        other: otherObject,
      });
      const searchString = 'searchString';
      const action = {
        type: LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE,
        payload: searchString,
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.components.componentSearchInput.value).toEqual(searchString);
      expect(newState.components.componentSearchInput.trimmedValue).toEqual(searchString);
      expect(newState.components.componentSearchInput.isPristine).toEqual(false);
      expect(newState.actions).toBe(state.actions);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets an empty string as valid on componentSearchInput', function () {
      const state = Object.freeze({
        components: {
          error: {},
          componentNameToSearch: 'searchString',
          componentSearchInput: {
            isPristine: false,
            value: 'searchString',
            trimmedValue: 'searchString',
            validationErrors: null,
          },
        },
        applications: { error: null },
        currentTab: '',
        other: otherObject,
      });
      const searchString = '';
      const action = {
        type: LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE,
        payload: searchString,
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.components.componentSearchInput.value).toEqual(searchString);
      expect(newState.components.componentSearchInput.trimmedValue).toEqual(searchString);
      expect(newState.components.componentSearchInput.isPristine).toEqual(false);
      expect(newState.actions).toBe(state.actions);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets an invalid string on componentSearchInput', function () {
      const state = Object.freeze({
        components: {
          error: {},
          componentNameToSearch: 'searchString',
          componentSearchInput: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
        },
        applications: { error: null },
        currentTab: '',
        other: otherObject,
      });
      const searchString = '12';
      const action = {
        type: LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE,
        payload: searchString,
      };
      const newState = legalDashboardReducer(state, action);
      expect(newState.components.componentSearchInput.value).toEqual(searchString);
      expect(newState.components.componentSearchInput.trimmedValue).toEqual(searchString);
      expect(newState.components.componentSearchInput.isPristine).toEqual(false);
      expect(newState.components.componentSearchInput.validationErrors).toEqual(
        'You must input at least three characters to search'
      );
      expect(newState.actions).toBe(state.actions);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
