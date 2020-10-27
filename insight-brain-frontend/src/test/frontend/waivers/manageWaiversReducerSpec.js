/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initState } from '../../../main/frontend/waivers/manageWaiversReducer';
import {
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED,
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED,
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED
} from '../../../main/frontend/waivers/waiverActions';
import { UI_ROUTER_ON_FINISH } from '../../../main/frontend/reduxUiRouter/routerActions';

describe('manageWaiversReducer', function() {
  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({ foo: 'bar' });
      const action = { type: 'UNKNOWN' };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState).toBe(initState);
    });

    it('is immutable', function() {
      const action = {type: 'UNKNOWN'};
      const state = reducer(undefined, action);

      expect(() => {
        state.newProp = 'newProp';
      }).toThrowError(TypeError);

      expect(() => {
        state.hasPermissionForAppWaivers = true;
      }).toThrowError(TypeError);

      expect(() => {
        state.loading = true;
      }).toThrowError(TypeError);

      expect(() => {
        state.loadError = 'error';
      }).toThrowError(TypeError);
    });
  });

  describe('WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED action', function() {
    it('sets loading to true', function() {
      const state = {
        loading: false,
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(state, {
        type: WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED
      });

      expect(newState.loading).toBe(true);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED action', function() {
    it('sets loadError and resets loading', function() {
      const state = {
        loading: true,
        loadError: null,
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(state, {
        type: WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED,
        payload: 'load manage waivers data error'
      });

      expect(newState.loading).toBe(initState.loading);
      expect(newState.loadError).toBe('load manage waivers data error');
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED action', function() {
    it('sets hasPermissionForAppWaivers and resets loading and loadError', function() {
      const state = {
        loading: true,
        loadError: 'error',
        hasPermissionForAppWaivers: null,
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(state, {
        type: WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED,
        payload: true
      });

      expect(newState.loading).toBe(initState.loading);
      expect(newState.loadError).toBe(initState.loadError);
      expect(newState.hasPermissionForAppWaivers).toBe(true);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('UI_ROUTER_ON_FINISH action', function() {
    it('resets state to initState', function() {
      const state = {
        loading: true,
        loadError: 'error',
        hasPermissionForAppWaivers: true
      };

      const newState = reducer(state, {
        type: UI_ROUTER_ON_FINISH
      });

      expect(newState).toEqual(initState);
    });
  });
});
