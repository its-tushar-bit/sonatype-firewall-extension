/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../main/frontend/violation/violationReducer';
import { clone } from 'ramda';

describe('violationReducer', function () {
  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = { type: 'UNKNOWN' };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState.violationDetails).toBe(null);
      expect(newState.selectedViolationId).toBe(null);
      expect(newState.loading).toBe(false);
      expect(newState.violationDetailsError).toBe(null);
      expect(newState.vulnerabilityDetailsLoading).toBe(false);
      expect(newState.vulnerabilityDetails).toBe(null);
      expect(newState.vulnerabilityDetailsError).toBe(null);
      expect(newState.activeWaivers).toEqual([]);
      expect(newState.expiredWaivers).toEqual([]);
    });

    it('is immutable', function () {
      const action = { type: 'UNKNOWN' };
      const state = reducer(undefined, action);

      // Overall state object
      expect(() => {
        state.newProp = 'newProp';
      }).toThrowError(TypeError);

      // Nested object-properties
      expect(() => {
        state.violationDetails = [];
      }).toThrowError(TypeError);

      expect(() => {
        state.loading = true;
      }).toThrowError(TypeError);

      expect(() => {
        state.violationDetailsError = 'Broke';
      }).toThrowError(TypeError);

      expect(() => {
        state.activeWaivers.push({});
      }).toThrowError(TypeError);

      expect(() => {
        state.expiredWaivers.push({});
      }).toThrowError(TypeError);
    });
  });

  describe('VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED action', function () {
    it('clears the store to its initial state', function () {
      const initialState = {
        violationDetails: {},
        selectedViolationId: '123',
        violationDetailsError: 'foo',
        loading: false,
        vulnerabilityDetailsLoading: true,
        vulnerabilityDetails: {},
        vulnerabilityDetailsError: 'bla',
        otherProp: 'asdf',
        activeWaivers: [123],
        expiredWaivers: [321],
        similarWaivers: [],
        hasPermissionForAppWaivers: false,
        hasEditIqPermission: false,
        isVulnerabilityDetailsOutdated: false,
        similarWaiversFilterSelectedIds: new Set([]),
        loadingSimilarWaivers: true,
        loadSimilarWaiversError: 'error',
      };

      const newState = reducer(initialState, {
        type: 'VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED',
      });

      expect(newState).toEqual({
        violationDetails: null,
        loading: false,
        violationDetailsError: null,
        vulnerabilityDetailsLoading: false,
        vulnerabilityDetails: null,
        vulnerabilityDetailsError: null,
        activeWaivers: [],
        expiredWaivers: [],
        autoWaiver: null,
        similarWaivers: [],
        selectedViolationId: null,
        hasPermissionForAppWaivers: false,
        hasEditIqPermission: false,
        isVulnerabilityDetailsOutdated: false,
        similarWaiversFilterSelectedIds: new Set([]),
        loadingApplicableWaivers: false,
        loadApplicableWaiversError: null,
        loadingSimilarWaivers: false,
        loadSimilarWaiversError: null,
      });
    });
  });

  describe('VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED action', function () {
    it('sets the loading flag to true and clear violationDetailsError flag', function () {
      const initialState = {
        violationDetails: {},
        selectedViolationId: '123',
        violationDetailsError: 'foo',
        loading: false,
        vulnerabilityDetailsLoading: true,
        vulnerabilityDetails: {},
        vulnerabilityDetailsError: 'bla',
        otherProp: 'asdf',
        activeWaivers: [123],
        expiredWaivers: [321],
        hasPermissionForAppWaivers: false,
      };

      const newState = reducer(initialState, {
        type: 'VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED',
      });

      expect(newState).toEqual({
        loading: true,
        violationDetailsError: null,
        violationDetails: {},
        selectedViolationId: '123',
        vulnerabilityDetailsLoading: true,
        vulnerabilityDetails: null,
        vulnerabilityDetailsError: 'bla',
        otherProp: 'asdf',
        activeWaivers: [123],
        expiredWaivers: [321],
        hasPermissionForAppWaivers: false,
      });
    });
  });

  describe('VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED', function () {
    it('unsets loading and violationDetailsError and sets violationDetails & activeWaivers to the payload', function () {
      const initialState = {
        violationDetailsError: 'baz',
        loading: true,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(initialState, {
        type: 'VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED',
      });

      expect(newState.violationDetailsError).toBeNull();
      expect(newState.loading).toBe(false);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('VIOLATION_LOAD_VIOLATION_DETAILS_FAILED', function () {
    it('unsets the loading flag and sets the violationDetailsError to the payload', function () {
      const initialState = {
        violationDetails: null,
        violationDetailsError: null,
        loading: true,
        otherProp: 'asdf',
      };

      const newState = reducer(initialState, {
        type: 'VIOLATION_LOAD_VIOLATION_DETAILS_FAILED',
        payload: 'ERRRRRRRRRRRRRRRRR',
      });

      expect(newState).toEqual({
        loading: false,
        violationDetailsError: 'ERRRRRRRRRRRRRRRRR',
        violationDetails: null,
        otherProp: 'asdf',
      });
    });
  });

  describe('VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED action', function () {
    it('sets vulnerabilityDetailsLoading flag to true', function () {
      const initialState = {
        violationDetails: {},
        loading: false,
        violationDetailsError: 'foo',
        vulnerabilityDetailsLoading: false,
        otherProp: 'asdf',
      };

      const newState = reducer(initialState, {
        type: 'VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED',
      });

      expect(newState).toEqual({
        violationDetails: {},
        loading: false,
        violationDetailsError: 'foo',
        vulnerabilityDetailsLoading: true,
        otherProp: 'asdf',
      });
    });
  });

  describe('VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED', function () {
    it(
      'unsets vulnerabilityDetailsError and vulnerabilityDetailsLoading and sets vulnerabilityDetails to the payload' +
        ' and hasEditIqPermission=true',
      function () {
        const initialState = {
          vulnerabilityDetails: {},
          vulnerabilityDetailsError: 'baz',
          vulnerabilityDetailsLoading: true,
          otherProp: 'asdf',
          hasEditIqPermission: undefined,
          isVulnerabilityDetailsOutdated: false,
        };

        const newState = reducer(initialState, {
          type: 'VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED',
          payload: { foo: 'bar', hasEditIqPermission: true },
        });

        expect(newState).toEqual({
          vulnerabilityDetailsLoading: false,
          vulnerabilityDetailsError: null,
          vulnerabilityDetails: { foo: 'bar', hasEditIqPermission: true },
          otherProp: 'asdf',
          hasEditIqPermission: true,
          isVulnerabilityDetailsOutdated: false,
        });
      }
    );
  });

  describe('VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED', function () {
    it('unsets vulnerabilityDetailsLoading flag and sets the vulnerabilityDetailsError to the payload', function () {
      const initialState = {
        vulnerabilityDetailsLoading: true,
        vulnerabilityDetails: null,
        vulnerabilityDetailsError: null,
        otherProp: 'asdf',
        isVulnerabilityDetailsOutdated: false,
      };

      const newState = reducer(initialState, {
        type: 'VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED',
        payload: 'ERRRRRRRRRRRRRRRRR',
      });

      expect(newState).toEqual({
        vulnerabilityDetailsLoading: false,
        vulnerabilityDetailsError: 'ERRRRRRRRRRRRRRRRR',
        vulnerabilityDetails: null,
        otherProp: 'asdf',
        isVulnerabilityDetailsOutdated: false,
      });
    });
  });

  describe('VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED', function () {
    it('sets the waivers in the state', function () {
      const state = {
        loading: true,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED',
        payload: {
          violationDetails: { foo: 'bar' },
          selectedViolationId: '123',
        },
      });

      expect(newState.violationDetails).toEqual({ foo: 'bar' });
      expect(newState.selectedViolationId).toEqual('123');
      expect(newState.loading).toBe(true);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED', function () {
    it('sets the waivers in the state', function () {
      const state = {
        loading: true,
        loadingApplicableWaivers: true,
        loadApplicableWaiversError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED',
        payload: {
          activeWaivers: ['foo'],
          expiredWaivers: ['bar'],
        },
      });

      expect(newState.activeWaivers).toEqual(['foo']);
      expect(newState.expiredWaivers).toEqual(['bar']);
      expect(newState.loading).toBe(true);
      expect(newState.loadingApplicableWaivers).toBe(false);
      expect(newState.loadApplicableWaiversError).toBe(null);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED', function () {
    it('sets error and loading states for applicable waivers', function () {
      const state = {
        loadingApplicableWaivers: false,
        loadApplicableWaiversError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED',
      });

      expect(newState.loadingApplicableWaivers).toBe(true);
      expect(newState.loadApplicableWaiversError).toBe(null);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('VIOLATION_FETCH_APPLICABLE_WAIVERS_FAILED', function () {
    it('sets error and loading states for applicable waivers', function () {
      const state = {
        loadingApplicableWaivers: true,
        loadApplicableWaiversError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'VIOLATION_FETCH_APPLICABLE_WAIVERS_FAILED',
        payload: 'some error',
      });

      expect(newState.loadingApplicableWaivers).toBe(false);
      expect(newState.loadApplicableWaiversError).toBe('some error');
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FULFILLED', function () {
    it('sets the waiver in the state', function () {
      const state = {
        loading: true,
        loadingApplicableWaivers: false,
        loadingApplicableAutoWaiver: true,
        loadApplicableWaiversError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FULFILLED',
        payload: {
          id: 'applicationPublicId',
        },
      });

      expect(newState.loading).toBe(true);
      expect(newState.loadingApplicableWaivers).toBe(false);
      expect(newState.loadApplicableWaiversError).toBe('error');
      expect(newState.loadingApplicableAutoWaiver).toBe(false);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_REQUESTED', function () {
    it('sets error and loading states for applicable auto waiver', function () {
      const state = {
        loadingApplicableAutoWaiver: false,
        loadApplicableAutoWaiverError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_REQUESTED',
      });

      expect(newState.loadingApplicableAutoWaiver).toBe(true);
      expect(newState.loadApplicableAutoWaiverError).toBe(null);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FAILED', function () {
    it('sets error and loading states for applicable waiver', function () {
      const state = {
        loadingApplicableAutoWaiver: true,
        loadApplicableAutoWaiverError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FAILED',
        payload: 'some error',
      });

      expect(newState.loadingApplicableAutoWaiver).toBe(false);
      expect(newState.loadApplicableAutoWaiverError).toBe('some error');
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED', function () {
    it('sets the similar waivers in the state and clears error and loading', function () {
      const state = {
        loadingSimilarWaivers: true,
        loadSimilarWaiversError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED',
        payload: ['foo'],
      });

      expect(newState.similarWaivers).toEqual(['foo']);
      expect(newState.loadingSimilarWaivers).toBe(false);
      expect(newState.loadSimilarWaiversError).toBe(null);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED', function () {
    it('sets the error and clears the loading for similar waivers', function () {
      const state = {
        loadingSimilarWaivers: true,
        loadSimilarWaiversError: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED',
        payload: 'some error',
      });

      expect(newState.loadingSimilarWaivers).toBe(false);
      expect(newState.loadSimilarWaiversError).toBe('some error');
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED', function () {
    it('sets the loading and clears the error for similar waivers', function () {
      const state = {
        loadingSimilarWaivers: false,
        loadSimilarWaiversError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED',
      });

      expect(newState.loadingSimilarWaivers).toBe(true);
      expect(newState.loadSimilarWaiversError).toBe(null);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('VIOLATION_SORT_SIMILAR_WAIVERS', function () {
    it('sets the similar waivers sorting', function () {
      const convertArrayToObj = (p, c) => [...p, { createTime: c }];
      const initialOrder = [
        '2024-01-11T15:32:35.849+0000',
        '2025-11-05T20:05:40.101+0000',
        '2020-04-21T05:10:11.002+0000',
      ];
      const ascOrder = clone(initialOrder).sort();
      const descOrder = clone(initialOrder).sort().reverse();

      const state = {
        similarWaivers: initialOrder.reduce(convertArrayToObj, []),
        otherProp: { prop: 'foo' },
      };

      const newStateNull = reducer(state, {
        type: 'VIOLATION_SORT_SIMILAR_WAIVERS',
        payload: null,
      });

      expect(newStateNull.similarWaivers).toEqual(ascOrder.reduce(convertArrayToObj, []));
      expect(newStateNull.otherProp).toBe(state.otherProp);

      const newStateAsc = reducer(state, {
        type: 'VIOLATION_SORT_SIMILAR_WAIVERS',
        payload: 'asc',
      });

      expect(newStateAsc.similarWaivers).toEqual(ascOrder.reduce(convertArrayToObj, []));
      expect(newStateAsc.otherProp).toBe(state.otherProp);

      const newStateDesc = reducer(state, {
        type: 'VIOLATION_SORT_SIMILAR_WAIVERS',
        payload: 'desc',
      });

      expect(newStateDesc.similarWaivers).toEqual(descOrder.reduce(convertArrayToObj, []));
      expect(newStateDesc.otherProp).toBe(state.otherProp);
    });
  });

  describe('VIOLATION_SET_FILTER_IDS_SIMILAR_WAIVERS', function () {
    it('sets the similar waivers filter id set', function () {
      const state = Object.freeze({
        otherProp: { prop: 'foo' },
      });

      const newState = reducer(state, {
        type: 'VIOLATION_SET_FILTER_IDS_SIMILAR_WAIVERS',
        payload: new Set([{ id: 'id', displayName: 'disp' }]),
      });

      expect([...newState.similarWaiversFilterSelectedIds]).toEqual([{ id: 'id', displayName: 'disp' }]);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });
});
