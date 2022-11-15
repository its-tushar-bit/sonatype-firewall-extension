/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../main/frontend/violation/violationReducer';

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
      expect(newState.loading).toBe(true);
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

  describe('VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED action', function () {
    it('resets to default state and sets the loading flag to true', function () {
      const initialState = {
        violationDetails: {},
        selectedViolationId: '123',
        violationDetailsError: 'foo',
        loading: false,
        vulnerabilityDetailsLoading: true,
        vulnerabilityDetails: {},
        vulnerabilityDetailsError: 'bla',
        otherProp: 'asdf',
        hasPermissionForAppWaivers: false,
      };

      const newState = reducer(initialState, {
        type: 'VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED',
      });

      expect(newState).toEqual({
        loading: true,
        violationDetailsError: null,
        violationDetails: null,
        selectedViolationId: null,
        vulnerabilityDetailsLoading: false,
        vulnerabilityDetails: null,
        vulnerabilityDetailsError: null,
        activeWaivers: [],
        expiredWaivers: [],
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
    it('unsets vulnerabilityDetailsError and vulnerabilityDetailsLoading and sets vulnerabilityDetails to the payload', function () {
      const initialState = {
        vulnerabilityDetails: {},
        vulnerabilityDetailsError: 'baz',
        vulnerabilityDetailsLoading: true,
        otherProp: 'asdf',
      };

      const newState = reducer(initialState, {
        type: 'VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED',
        payload: { foo: 'bar' },
      });

      expect(newState).toEqual({
        vulnerabilityDetailsLoading: false,
        vulnerabilityDetailsError: null,
        vulnerabilityDetails: { foo: 'bar' },
        otherProp: 'asdf',
      });
    });
  });

  describe('VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED', function () {
    it('unsets vulnerabilityDetailsLoading flag and sets the vulnerabilityDetailsError to the payload', function () {
      const initialState = {
        vulnerabilityDetailsLoading: true,
        vulnerabilityDetails: null,
        vulnerabilityDetailsError: null,
        otherProp: 'asdf',
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
      });
    });
  });

  describe('UI_ROUTER_ON_FINISH', function () {
    it('resets loading to true but keeps violationDetails', function () {
      const currentState = {
        loading: false,
        violationDetails: { prop: 'foo' },
      };

      const newState = reducer(currentState, {
        type: '@@reduxUiRouter/onFinish',
      });

      expect(newState.loading).toBe(true);
      expect(newState.violationDetails).toBe(currentState.violationDetails);
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
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });
});
