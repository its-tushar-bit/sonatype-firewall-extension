/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../main/frontend/dashboard/dashboardReducer';

describe('dashboardReducer', () => {
  let otherObject;

  beforeEach(() => {
    otherObject = { value: 'test value' };
  });

  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({ foo: 'bar' });
      const newState = reduce(state, { type: 'UNKNOWN' });

      expect(newState).toBe(state);
    });
  });

  describe('initial state', () => {
    it('is used if no state is provided', () => {
      const newState = reduce(undefined, { type: 'UNKNOWN' });

      expect(newState).not.toBeUndefined();
    });

    it('has default sortFields', () => {
      const { violations, components, applications } = reduce(undefined, { type: 'UNKNOWN' });

      expect(violations.sortFields).toEqual(['-firstOccurrenceTime', '-threatLevel']);
      expect(components.sortFields).toEqual(['-score']);
      expect(applications.sortFields).toEqual(['-totalApplicationRisk.totalRisk']);
    });

    describe('Initial page tests', () => {
      it('shows page 0 for the violation table', () => {
        const { violations } = reduce(undefined, { type: 'UNKNOWN' });
        expect(violations.hasMultiplePages).toBe(false);
        expect(violations.page).toBeNull();
      });

      it('shows page 0 for the applications table', () => {
        const { applications } = reduce(undefined, { type: 'UNKNOWN' });
        expect(applications.hasMultiplePages).toBe(false);
        expect(applications.page).toBeNull();
      });
    });
  });

  const testResetsResults = (action) => {
    it('resets results', () => {
      const state = Object.freeze({
        violations: { results: [], hasMultiplePages: false, error: 'foo' },
        components: { results: [], hasMultiplePages: false, error: 'foo' },
        applications: { results: [], hasMultiplePages: false, error: 'foo' },
        waivers: { results: [], hasMultiplePages: false, error: 'foo' },
        waiverRequests: { results: [], hasMultiplePages: false, error: 'foo' },
        other: otherObject,
      });
      const newState = reduce(state, action);
      expect(newState).toEqual({
        violations: { results: null, hasMultiplePages: null, error: null },
        components: { results: null, hasMultiplePages: null, error: null },
        applications: { results: null, hasMultiplePages: null, error: null },
        waivers: { results: null, hasMultiplePages: null, error: null },
        waiverRequests: { results: null, hasMultiplePages: null, error: null },
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  };

  describe('LOAD_FILTER_REQUESTED action', () => {
    testResetsResults({
      type: 'LOAD_FILTER_REQUESTED',
    });
  });

  describe('APPLY_FILTER_REQUESTED action', () => {
    testResetsResults({
      type: 'RESET_ALL_TABS',
    });
  });

  describe('LOAD_RESULTS_REQUESTED action', () => {
    it('resets violations state', () => {
      const state = Object.freeze({
        violations: { results: [], error: 'foo' },
        components: { results: [], error: 'foo' },
        applications: { results: [], error: 'foo' },
        waivers: { results: [], error: 'foo' },
        other: otherObject,
      });
      const { violations, components, applications, waivers, other } = reduce(state, {
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'violations',
      });

      expect(violations.results).toBeNull();
      expect(violations.error).toBeNull();
      expect(components).toBe(state.components);
      expect(applications).toBe(state.applications);
      expect(waivers).toBe(state.waivers);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('resets applications state', () => {
      const state = Object.freeze({
        violations: { results: [], error: 'foo' },
        components: { results: [], error: 'foo' },
        applications: { results: [], error: 'foo' },
        waivers: { results: [], error: 'foo' },
        other: otherObject,
      });
      const { applications, components, violations, waivers, other } = reduce(state, {
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'applications',
      });

      expect(applications.results).toBeNull();
      expect(applications.error).toBeNull();
      expect(components).toBe(state.components);
      expect(violations).toBe(state.violations);
      expect(waivers).toBe(state.waivers);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('resets components state', () => {
      const state = Object.freeze({
        violations: { results: [], error: 'foo' },
        components: { results: [], error: 'foo' },
        applications: { results: [], error: 'foo' },
        waivers: { results: [], error: 'foo' },
        other: otherObject,
      });
      const { components, applications, violations, waivers, other } = reduce(state, {
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'components',
      });

      expect(components.results).toBeNull();
      expect(components.error).toBeNull();
      expect(applications).toBe(state.applications);
      expect(violations).toBe(state.violations);
      expect(waivers).toBe(state.waivers);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('resets waivers state', () => {
      const state = Object.freeze({
        violations: { results: [], error: 'foo' },
        components: { results: [], error: 'foo' },
        applications: { results: [], error: 'foo' },
        waivers: { results: [], error: 'foo' },
        other: otherObject,
      });
      const { components, applications, violations, waivers, other } = reduce(state, {
        type: 'LOAD_RESULTS_REQUESTED',
        payload: 'waivers',
      });

      expect(waivers.results).toBeNull();
      expect(waivers.error).toBeNull();
      expect(components).toBe(state.components);
      expect(applications).toBe(state.applications);
      expect(violations).toBe(state.violations);
      expect(other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_RESULTS_FULFILLED action', () => {
    it('updates violations results', () => {
      const state = Object.freeze({
        violations: { results: null },
        components: { results: [] },
        applications: { results: [] },
        waivers: { results: [] },
        other: otherObject,
      });
      const action = {
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'violations',
          results: [],
        },
      };
      const { violations, components, applications, waivers, other } = reduce(state, action);

      expect(violations.results).toBe(action.payload.results);
      expect(violations.classyBrew).toBeUndefined();
      expect(components).toBe(state.components);
      expect(applications).toBe(state.applications);
      expect(waivers).toBe(state.waivers);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('updates components results and classyBrew', () => {
      const state = Object.freeze({
        violations: { results: [] },
        components: { results: null, classyBrew: null },
        applications: { results: [] },
        waivers: { results: [] },
        other: otherObject,
      });
      const action = {
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'components',
          results: [],
          classyBrew: {},
        },
      };
      const { components, violations, applications, waivers, other } = reduce(state, action);

      expect(components.results).toBe(action.payload.results);
      expect(components.classyBrew).toBe(action.payload.classyBrew);
      expect(violations).toBe(state.violations);
      expect(applications).toBe(state.applications);
      expect(waivers).toBe(state.waivers);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('updates applications results and classyBrew', () => {
      const state = Object.freeze({
        violations: { results: [] },
        components: { results: [] },
        applications: { results: null, classyBrew: null },
        waivers: { results: [] },
        other: otherObject,
      });
      const action = {
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'applications',
          results: [],
          classyBrew: {},
        },
      };
      const { components, violations, applications, waivers, other } = reduce(state, action);

      expect(applications.results).toBe(action.payload.results);
      expect(applications.classyBrew).toBe(action.payload.classyBrew);
      expect(violations).toBe(state.violations);
      expect(components).toBe(state.components);
      expect(waivers).toBe(state.waivers);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('updates waivers results without classyBrew', () => {
      const state = Object.freeze({
        violations: { results: [] },
        components: { results: [] },
        applications: { results: [] },
        waivers: { results: null },
        other: otherObject,
      });
      const action = {
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: 'waivers',
          results: [
            {
              id: 'id',
              ownerName: 'ownerName',
              ownerType: 'application',
            },
          ],
          classyBrew: {},
        },
      };
      const { components, violations, applications, waivers, other } = reduce(state, action);
      const expectedWaiversResults = [{ ...action.payload.results[0], scope: 'Application - ownerName' }];

      expect(waivers.results).toEqual(expectedWaiversResults);
      expect(violations).toBe(state.violations);
      expect(components).toBe(state.components);
      expect(applications).toBe(state.applications);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('updates violations pagination with results', () => {
      testPaginationWithResults('violations');
    });

    it('updates waivers pagination with results', () => {
      testPaginationWithResults('waivers');
    });

    it('updates components pagination with results', () => {
      testPaginationWithResults('components');
    });

    it('updates applications pagination with results', () => {
      testPaginationWithResults('applications');
    });

    const testPaginationWithResults = (resultsType) => {
      const state = Object.freeze({
        [resultsType]: { results: null, hasMultiplePages: false, hasNextPage: false, page: null },
      });
      const action = {
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: resultsType,
          results: [...Array(10).keys()],
          hasNextPage: false,
        },
      };
      const result = reduce(state, action)[resultsType];

      expect(result.hasMultiplePages).toBe(false);
      expect(result.page).toBe(0);
    };

    it('updates violations pagination without results', () => {
      testPaginationWithoutResults('violations');
    });

    it('updates waivers pagination without results', () => {
      testPaginationWithoutResults('waivers');
    });

    it('updates components pagination without results', () => {
      testPaginationWithoutResults('components');
    });

    it('updates applications pagination without results', () => {
      testPaginationWithoutResults('applications');
    });

    const testPaginationWithoutResults = (resultsType) => {
      const state = Object.freeze({
        [resultsType]: { results: null, hasMultiplePages: false, hasNextPage: false, page: 0 },
      });
      const action = {
        type: 'LOAD_RESULTS_FULFILLED',
        payload: {
          resultsType: resultsType,
          results: [],
          hasNextPage: false,
        },
      };
      const result = reduce(state, action)[resultsType];

      expect(result.hasMultiplePages).toBe(false);
      expect(result.page).toBe(0);
    };
  });

  describe('LOAD_RESULTS_FAILED action', () => {
    it('sets error in violations state', () => {
      const state = Object.freeze({
        violations: { error: null },
        components: { error: {} },
        applications: { error: {} },
        waivers: { error: {} },
        currentTab: 'violations',
        other: otherObject,
      });
      const action = {
        type: 'LOAD_RESULTS_FAILED',
        payload: {
          resultsType: 'violations',
          error: 'error',
        },
      };
      const { components, violations, applications, waivers, other } = reduce(state, action);

      expect(violations.error).toBe(action.payload.error);
      expect(components).toBe(state.components);
      expect(applications).toBe(state.applications);
      expect(waivers).toBe(state.waivers);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('sets error in components state', () => {
      const state = Object.freeze({
        violations: { error: {} },
        components: { error: null },
        applications: { error: {} },
        waivers: { error: {} },
        currentTab: 'components',
        other: otherObject,
      });
      const action = {
        type: 'LOAD_RESULTS_FAILED',
        payload: {
          resultsType: 'components',
          error: 'error',
        },
      };
      const { components, violations, applications, waivers, other } = reduce(state, action);

      expect(components.error).toBe(action.payload.error);
      expect(violations).toBe(state.violations);
      expect(applications).toBe(state.applications);
      expect(waivers).toBe(state.waivers);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('sets error in applications state', () => {
      const state = Object.freeze({
        violations: { error: {} },
        components: { error: {} },
        applications: { error: null },
        waivers: { error: {} },
        currentTab: 'applications',
        other: otherObject,
      });
      const action = {
        type: 'LOAD_RESULTS_FAILED',
        payload: {
          resultsType: 'applications',
          error: 'error',
        },
      };
      const { components, violations, applications, waivers, other } = reduce(state, action);

      expect(applications.error).toBe(action.payload.error);
      expect(violations).toBe(state.violations);
      expect(components).toBe(state.components);
      expect(waivers).toBe(state.waivers);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('sets error in waivers state', () => {
      const state = Object.freeze({
        violations: { error: {} },
        components: { error: {} },
        applications: { error: {} },
        waivers: { error: null },
        currentTab: 'waivers',
        other: otherObject,
      });
      const action = {
        type: 'LOAD_RESULTS_FAILED',
        payload: {
          resultsType: 'waivers',
          error: 'error',
        },
      };
      const { components, violations, applications, waivers, other } = reduce(state, action);

      expect(waivers.error).toBe(action.payload.error);
      expect(violations).toBe(state.violations);
      expect(components).toBe(state.components);
      expect(applications).toBe(state.applications);
      expect(other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SORT_RESULTS_REQUESTED action', () => {
    it('updates violations sortFields', () => {
      const state = Object.freeze({
        violations: {
          sortFields: ['-firstOccurrenceTime', '-threatLevel'],
          page: 10,
          other: otherObject,
        },
        components: { sortFields: ['-score'] },
        applications: { sortFields: ['-totalApplicationRisk.totalRisk'] },
        currentTab: 'violations',
      });
      const action = {
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'violations',
          sortFields: ['foo', '-bar'],
        },
      };
      const { components, violations, applications } = reduce(state, action);

      expect(violations.sortFields).toBe(action.payload.sortFields);
      expect(violations.page).toBe(0);
      expect(violations.other).toBe(otherObject); // other properties are not modified
      expect(components.sortFields).toBe(state.components.sortFields);
      expect(applications.sortFields).toBe(state.applications.sortFields);
    });

    it('updates components sortFields', () => {
      const state = Object.freeze({
        violations: { sortFields: ['-firstOccurrenceTime', '-threatLevel'] },
        components: {
          sortFields: ['-score'],
          other: otherObject,
        },
        applications: { sortFields: ['-totalApplicationRisk.totalRisk'] },
        currentTab: 'components',
      });
      const action = {
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'components',
          sortFields: ['foo', '-bar'],
        },
      };
      const { components, violations, applications } = reduce(state, action);

      expect(components.sortFields).toBe(action.payload.sortFields);
      expect(components.other).toBe(otherObject); // other properties are not modified
      expect(violations.sortFields).toBe(state.violations.sortFields);
      expect(applications.sortFields).toBe(state.applications.sortFields);
    });

    it('updates applications sortFields', () => {
      const state = Object.freeze({
        violations: { sortFields: ['-firstOccurrenceTime', '-threatLevel'] },
        components: { sortFields: ['-score'] },
        applications: {
          sortFields: ['-totalApplicationRisk.totalRisk'],
          page: 10,
          other: otherObject,
        },
        currentTab: 'applications',
      });
      const action = {
        type: 'SORT_RESULTS_REQUESTED',
        payload: {
          resultsType: 'applications',
          sortFields: ['foo', '-bar'],
        },
      };
      const { components, violations, applications } = reduce(state, action);

      expect(applications.sortFields).toBe(action.payload.sortFields);
      expect(applications.page).toBe(0);
      expect(applications.other).toBe(otherObject); // other properties are not modified
      expect(violations.sortFields).toBe(state.violations.sortFields);
      expect(components.sortFields).toBe(state.components.sortFields);
    });
  });

  describe('SORT_RESULTS_FULFILLED action', () => {
    it('updates violations results', () => {
      const state = Object.freeze({
        violations: { results: null },
        components: { results: [] },
        applications: { results: [] },
        other: otherObject,
      });
      const action = {
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'violations',
          results: [],
        },
      };
      const { components, violations, applications, other } = reduce(state, action);

      expect(violations.results).toBe(action.payload.results);
      expect(violations.classyBrew).toBeUndefined();
      expect(components).toBe(state.components);
      expect(applications).toBe(state.applications);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('updates components results and does not affect classyBrew', () => {
      const expectedBrew = { data: 'original classyBrew' };
      const state = Object.freeze({
        violations: { results: [] },
        components: { results: null, classyBrew: expectedBrew },
        applications: { results: [] },
        other: otherObject,
      });
      const action = {
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'components',
          results: [],
        },
      };
      const { components, violations, applications, other } = reduce(state, action);

      expect(components.results).toBe(action.payload.results);
      expect(components.classyBrew).toBe(expectedBrew);
      expect(violations).toBe(state.violations);
      expect(applications).toBe(state.applications);
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('updates applications results does not affect classyBrew', () => {
      const expectedBrew = { data: 'original classyBrew' };
      const state = Object.freeze({
        violations: { results: [] },
        components: { results: [] },
        applications: { results: null, classyBrew: expectedBrew },
        other: otherObject,
      });
      const action = {
        type: 'SORT_RESULTS_FULFILLED',
        payload: {
          resultsType: 'applications',
          results: [],
        },
      };

      const { applications, violations, components, other } = reduce(state, action);

      expect(applications.results).toBe(action.payload.results);
      expect(applications.classyBrew).toBe(expectedBrew);
      expect(violations).toBe(state.violations);
      expect(components).toBe(state.components);
      expect(other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('@@reduxUiRouter/onFinish action', () => {
    it('sets currentTab when navigating to violations tab', () => {
      const state = Object.freeze({ currentTab: 'foo', other: otherObject });
      const { currentTab, other } = reduce(state, {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'dashboard.overview.violations',
          },
        },
      });

      expect(currentTab).toEqual('violations');
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('sets currentTab when navigating to components tab', () => {
      const state = Object.freeze({ currentTab: 'foo', other: otherObject });
      const { currentTab, other } = reduce(state, {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'dashboard.overview.components',
          },
        },
      });

      expect(currentTab).toEqual('components');
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('sets currentTab when navigating to applications tab', () => {
      const state = Object.freeze({ currentTab: 'foo', other: otherObject });
      const { currentTab, other } = reduce(state, {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'dashboard.overview.applications',
          },
        },
      });

      expect(currentTab).toEqual('applications');
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('sets currentTab when navigating to waivers tab', () => {
      const state = Object.freeze({ currentTab: 'foo', other: otherObject });
      const { currentTab, other } = reduce(state, {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'dashboard.overview.waivers',
          },
        },
      });

      expect(currentTab).toEqual('waivers');
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('sets currentTab when navigating to waivers details page', () => {
      const state = Object.freeze({ currentTab: 'foo', other: otherObject });
      const { currentTab, other } = reduce(state, {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'waiver.details',
          },
        },
      });

      expect(currentTab).toEqual('waivers');
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('sets currentTab to "violations" when navigating to violation details page', () => {
      const state = Object.freeze({ currentTab: 'foo', other: otherObject });
      const { currentTab, other } = reduce(state, {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'dashboard.violation',
          },
        },
      });

      expect(currentTab).toEqual('violations');
      expect(other).toBe(otherObject); // other properties are not modified
    });

    it('does not change currentTab when navigating to other pages', () => {
      const state = Object.freeze({ currentTab: 'foo' });
      const { currentTab } = reduce(state, {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          toState: {
            name: 'other.page',
          },
        },
      });

      expect(currentTab).toBe('foo');
    });
  });
});
