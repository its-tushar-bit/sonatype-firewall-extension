/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardModule from '../../../main/frontend/dashboard/dashboard.module';

describe('dashboardReducer', function () {
  var reduce, otherObject;

  beforeEach(angular.mock.module(dashboardModule.name));

  beforeEach(inject(function ($injector) {
    reduce = $injector.get('dashboardReducer');
    otherObject = { value: 'test value' };
  }));

  describe('unknown action', function () {
    it('returns original state', function () {
      var state = Object.freeze({ foo: 'bar' });
      var action = {
        type: 'UNKNOWN',
      };
      var newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      var action = {
        type: 'UNKNOWN',
      };
      var newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default sortFields', function () {
      var action = {
        type: 'UNKNOWN',
      };
      var newState = reduce(undefined, action);
      expect(newState.violations.sortFields).toEqual(['-firstOccurrenceTime', '-threatLevel']);
      expect(newState.components.sortFields).toEqual(['-score']);
      expect(newState.applications.sortFields).toEqual(['-totalApplicationRisk.totalRisk']);
    });
  });

  describe('LOAD_FILTER_REQUESTED action', function () {
    testResetsResults({
      type: 'LOAD_FILTER_REQUESTED',
    });
  });

  describe('APPLY_FILTER_REQUESTED action', function () {
    testResetsResults({
      type: 'APPLY_FILTER_REQUESTED',
    });
  });

  function testResetsResults(action) {
    it('resets results', function () {
      var state = Object.freeze({
        violations: { results: [], numResults: 3, error: 'foo' },
        components: { results: [], numResults: 3, error: 'foo' },
        applications: { results: [], numResults: 3, error: 'foo' },
        other: otherObject,
      });
      var newState = reduce(state, action);
      expect(newState).toEqual({
        violations: { results: null, numResults: null, error: null },
        components: { results: null, numResults: null, error: null },
        applications: { results: null, numResults: null, error: null },
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  }

  describe('LOAD_RESULTS_REQUESTED action', function () {
    it('resets violations state', function () {
      var state = Object.freeze({
        violations: { results: [], numResults: 0, error: 'foo' },
        components: { results: [], error: 'foo' },
        applications: { results: [], error: 'foo' },
        other: otherObject,
      });
      var action = { type: 'LOAD_RESULTS_REQUESTED', payload: 'violations' };
      var newState = reduce(state, action);
      expect(newState.violations.results).toBeNull();
      expect(newState.violations.numResults).toBe(0);
      expect(newState.violations.error).toBeNull();
      expect(newState.components).toBe(state.components);
      expect(newState.applications).toBe(state.applications);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('resets applications state', function () {
      var state = Object.freeze({
        violations: { results: [], error: 'foo' },
        components: { results: [], error: 'foo' },
        applications: { results: [], numResults: 0, error: 'foo' },
        other: otherObject,
      });
      var action = { type: 'LOAD_RESULTS_REQUESTED', payload: 'applications' };
      var newState = reduce(state, action);
      expect(newState.applications.results).toBeNull();
      expect(newState.applications.numResults).toBe(0);
      expect(newState.applications.error).toBeNull();
      expect(newState.components).toBe(state.components);
      expect(newState.violations).toBe(state.violations);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('resets components state', function () {
      var state = Object.freeze({
        violations: { results: [], error: 'foo' },
        components: { results: [], numResults: 0, error: 'foo' },
        applications: { results: [], error: 'foo' },
        other: otherObject,
      });
      var action = { type: 'LOAD_RESULTS_REQUESTED', payload: 'components' };
      var newState = reduce(state, action);
      expect(newState.components.results).toBeNull();
      expect(newState.components.numResults).toBe(0);
      expect(newState.components.error).toBeNull();
      expect(newState.applications).toBe(state.applications);
      expect(newState.violations).toBe(state.violations);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_RESULTS_FULFILLED action', function () {
    it('updates violations results', function () {
      var state = Object.freeze({
        violations: { results: null, numResults: null },
        components: { results: [] },
        applications: { results: [] },
        other: otherObject,
      });
      var action = {
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'violations',
          results: [],
          numResults: 0,
        },
      };
      var newState = reduce(state, action);
      expect(newState.violations.results).toBe(action.payload.results);
      expect(newState.violations.numResults).toBe(action.payload.numResults);
      expect(newState.violations.classyBrew).toBeUndefined();
      expect(newState.components).toBe(state.components);
      expect(newState.applications).toBe(state.applications);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('updates components results and classyBrew', function () {
      var state = Object.freeze({
        violations: { results: [] },
        components: { results: null, numResults: 0, classyBrew: null },
        applications: { results: [] },
        other: otherObject,
      });
      var action = {
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'components',
          results: [],
          numResults: 0,
          classyBrew: {},
        },
      };
      var newState = reduce(state, action);
      expect(newState.components.results).toBe(action.payload.results);
      expect(newState.components.numResults).toBe(action.payload.numResults);
      expect(newState.components.classyBrew).toBe(action.payload.classyBrew);
      expect(newState.violations).toBe(state.violations);
      expect(newState.applications).toBe(state.applications);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('updates applications results and classyBrew', function () {
      var state = Object.freeze({
        violations: { results: [] },
        components: { results: [] },
        applications: { results: null, numResults: null, classyBrew: null },
        other: otherObject,
      });
      var action = {
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'applications',
          results: [],
          numResults: 0,
          classyBrew: {},
        },
      };
      var newState = reduce(state, action);
      expect(newState.applications.results).toBe(action.payload.results);
      expect(newState.applications.numResults).toBe(action.payload.numResults);
      expect(newState.applications.classyBrew).toBe(action.payload.classyBrew);
      expect(newState.violations).toBe(state.violations);
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_RESULTS_FAILED action', function () {
    it('sets error in violations state', function () {
      var state = Object.freeze({
        violations: { error: null },
        components: { error: {} },
        applications: { error: {} },
        currentTab: 'violations',
        other: otherObject,
      });
      var action = {
        type: 'LOAD_RESULTS_FAILED',
        payload: {
          resultsType: 'violations',
          error: 'error',
        },
      };
      var newState = reduce(state, action);
      expect(newState.violations.error).toBe(action.payload.error);
      expect(newState.components).toBe(state.components);
      expect(newState.applications).toBe(state.applications);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets error in components state', function () {
      var state = Object.freeze({
        violations: { error: {} },
        components: { error: null },
        applications: { error: {} },
        currentTab: 'components',
        other: otherObject,
      });
      var action = {
        type: 'LOAD_RESULTS_FAILED',
        payload: {
          resultsType: 'components',
          error: 'error',
        },
      };
      var newState = reduce(state, action);
      expect(newState.components.error).toBe(action.payload.error);
      expect(newState.violations).toBe(state.violations);
      expect(newState.applications).toBe(state.applications);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets error in applications state', function () {
      var state = Object.freeze({
        violations: { error: {} },
        components: { error: {} },
        applications: { error: null },
        currentTab: 'applications',
        other: otherObject,
      });
      var action = {
        type: 'LOAD_RESULTS_FAILED',
        payload: {
          resultsType: 'applications',
          error: 'error',
        },
      };
      var newState = reduce(state, action);
      expect(newState.applications.error).toBe(action.payload.error);
      expect(newState.violations).toBe(state.violations);
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SORT_RESULTS_REQUESTED action', function () {
    it('updates violations sortFields', function () {
      var state = Object.freeze({
        violations: {
          sortFields: ['-firstOccurrenceTime', '-threatLevel'],
          other: otherObject,
        },
        components: { sortFields: ['-score'] },
        applications: { sortFields: ['-totalApplicationRisk.totalRisk'] },
        currentTab: 'violations',
      });
      var action = {
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'violations',
          sortFields: ['foo', '-bar'],
        },
      };
      var newState = reduce(state, action);
      expect(newState.violations.sortFields).toBe(action.payload.sortFields);
      expect(newState.violations.other).toBe(otherObject); // other properties are not modified
      expect(newState.components.sortFields).toBe(state.components.sortFields);
      expect(newState.applications.sortFields).toBe(state.applications.sortFields);
    });

    it('updates components sortFields', function () {
      var state = Object.freeze({
        violations: { sortFields: ['-firstOccurrenceTime', '-threatLevel'] },
        components: {
          sortFields: ['-score'],
          other: otherObject,
        },
        applications: { sortFields: ['-totalApplicationRisk.totalRisk'] },
        currentTab: 'components',
      });
      var action = {
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'components',
          sortFields: ['foo', '-bar'],
        },
      };
      var newState = reduce(state, action);
      expect(newState.components.sortFields).toBe(action.payload.sortFields);
      expect(newState.components.other).toBe(otherObject); // other properties are not modified
      expect(newState.violations.sortFields).toBe(state.violations.sortFields);
      expect(newState.applications.sortFields).toBe(state.applications.sortFields);
    });

    it('updates applications sortFields', function () {
      var state = Object.freeze({
        violations: { sortFields: ['-firstOccurrenceTime', '-threatLevel'] },
        components: { sortFields: ['-score'] },
        applications: {
          sortFields: ['-totalApplicationRisk.totalRisk'],
          other: otherObject,
        },
        currentTab: 'applications',
      });
      var action = {
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'applications',
          sortFields: ['foo', '-bar'],
        },
      };
      var newState = reduce(state, action);
      expect(newState.applications.sortFields).toBe(action.payload.sortFields);
      expect(newState.applications.other).toBe(otherObject); // other properties are not modified
      expect(newState.violations.sortFields).toBe(state.violations.sortFields);
      expect(newState.components.sortFields).toBe(state.components.sortFields);
    });
  });

  describe('SORT_RESULTS_FULFILLED action', function () {
    it('updates violations results', function () {
      var state = Object.freeze({
        violations: { results: null },
        components: { results: [] },
        applications: { results: [] },
        other: otherObject,
      });
      var action = {
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'violations',
          results: [],
        },
      };
      var newState = reduce(state, action);
      expect(newState.violations.results).toBe(action.payload.results);
      expect(newState.violations.classyBrew).toBeUndefined();
      expect(newState.components).toBe(state.components);
      expect(newState.applications).toBe(state.applications);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('updates components results and does not affect classyBrew', function () {
      var expectedBrew = { data: 'original classyBrew' };
      var state = Object.freeze({
        violations: { results: [] },
        components: { results: null, classyBrew: expectedBrew },
        applications: { results: [] },
        other: otherObject,
      });
      var action = {
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'components',
          results: [],
        },
      };
      var newState = reduce(state, action);
      expect(newState.components.results).toBe(action.payload.results);
      expect(newState.components.classyBrew).toBe(expectedBrew);
      expect(newState.violations).toBe(state.violations);
      expect(newState.applications).toBe(state.applications);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('updates applications results does not affect classyBrew', function () {
      var expectedBrew = { data: 'original classyBrew' };
      var state = Object.freeze({
        violations: { results: [] },
        components: { results: [] },
        applications: { results: null, classyBrew: expectedBrew },
        other: otherObject,
      });
      var action = {
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'applications',
          results: [],
        },
      };
      var newState = reduce(state, action);
      expect(newState.applications.results).toBe(action.payload.results);
      expect(newState.applications.classyBrew).toBe(expectedBrew);
      expect(newState.violations).toBe(state.violations);
      expect(newState.components).toBe(state.components);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('@@reduxUiRouter/onFinish action', function () {
    it('sets currentTab when navigating to violations tab', function () {
      var state = Object.freeze({ currentTab: 'foo', other: otherObject });
      var action = {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'dashboard.overview.violations',
          },
        },
      };
      var newState = reduce(state, action);
      expect(newState.currentTab).toEqual('violations');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets currentTab when navigating to components tab', function () {
      var state = Object.freeze({ currentTab: 'foo', other: otherObject });
      var action = {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'dashboard.overview.components',
          },
        },
      };
      var newState = reduce(state, action);
      expect(newState.currentTab).toEqual('components');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets currentTab when navigating to applications tab', function () {
      var state = Object.freeze({ currentTab: 'foo', other: otherObject });
      var action = {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'dashboard.overview.applications',
          },
        },
      };
      var newState = reduce(state, action);
      expect(newState.currentTab).toEqual('applications');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets currentTab to "violations" when navigating to violation details page', function () {
      var state = Object.freeze({ currentTab: 'foo', other: otherObject });
      var action = {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'dashboard.violation',
          },
        },
      };
      var newState = reduce(state, action);
      expect(newState.currentTab).toEqual('violations');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('does not change currentTab when navigating to other pages', function () {
      var state = Object.freeze({ currentTab: 'foo' });
      var action = {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'other.page',
          },
        },
      };
      var newState = reduce(state, action);
      expect(newState.currentTab).toBe('foo');
    });
  });
});
