/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initState } from '../../../main/frontend/waivers/manageWaiversReducer';
import {
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED,
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED,
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED,
  WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED,
  WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED,
  WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED,
  WAIVERS_SET_IS_REQUEST_WAIVER_POPOVER_SHOWN,
} from '../../../main/frontend/waivers/waiverActions';
import { UI_ROUTER_ON_FINISH } from '../../../main/frontend/reduxUiRouter/routerActions';

describe('manageWaiversReducer', function () {
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
      expect(newState).toBe(initState);
    });

    it('is immutable', function () {
      const action = { type: 'UNKNOWN' };
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
        state.loadManageWaiversDataError = 'error';
      }).toThrowError(TypeError);
    });
  });

  describe('WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED action', function () {
    it('sets loadingManageWaiversData to true', function () {
      const state = {
        loadingManageWaiversData: false,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED,
      });

      expect(newState.loadingManageWaiversData).toBe(true);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED action', function () {
    it('sets loadManageWaiversDataError and resets loadingManageWaiversData', function () {
      const state = {
        loadingManageWaiversData: true,
        loadManageWaiversDataError: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED,
        payload: 'load manage waivers data error',
      });

      expect(newState.loadingManageWaiversData).toBe(initState.loadingManageWaiversData);
      expect(newState.loadManageWaiversDataError).toBe('load manage waivers data error');
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED action', function () {
    it('sets hasPermissionForAppWaivers and resets loadingManageWaiversData and loadManageWaiversDataError', function () {
      const state = {
        loadingManageWaiversData: true,
        loadManageWaiversDataError: 'error',
        hasPermissionForAppWaivers: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED,
        payload: true,
      });

      expect(newState.loadingManageWaiversData).toBe(initState.loadingManageWaiversData);
      expect(newState.loadManageWaiversDataError).toBe(initState.loadManageWaiversDataError);
      expect(newState.hasPermissionForAppWaivers).toBe(true);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED action', function () {
    it('sets loadingApplicableWaivers to true and loadApplicableWaiversError to null ', function () {
      const state = {
        loadingApplicableWaivers: false,
        loadApplicableWaiversError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED,
      });

      expect(newState.loadingApplicableWaivers).toBe(true);
      expect(newState.loadApplicableWaiversError).toBeNull();
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED action', function () {
    it('sets loadingApplicableWaivers to false and loadApplicableWaiversError to null ', function () {
      const state = {
        loadingApplicableWaivers: true,
        loadApplicableWaiversError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED,
      });

      expect(newState.loadingApplicableWaivers).toBe(false);
      expect(newState.loadApplicableWaiversError).toBeNull();
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED action', function () {
    it('sets loadingApplicableWaivers to false and loadApplicableWaiversError to null ', function () {
      const state = {
        loadingApplicableWaivers: true,
        loadApplicableWaiversError: 'error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED,
        payload: 'load applicable waivers error',
      });

      expect(newState.loadingApplicableWaivers).toBe(false);
      expect(newState.loadApplicableWaiversError).toBe('load applicable waivers error');
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_SET_IS_REQUEST_WAIVER_POPOVER_SHOWN action', function () {
    it('sets isRequestWaiverPopoverShown to true', function () {
      const state = {
        isRequestWaiverPopoverShown: false,
      };

      const newState = reducer(state, {
        type: WAIVERS_SET_IS_REQUEST_WAIVER_POPOVER_SHOWN,
        payload: true,
      });

      expect(newState.isRequestWaiverPopoverShown).toBe(true);
    });

    it('sets isRequestWaiverPopoverShown to false', function () {
      const state = {
        isRequestWaiverPopoverShown: true,
      };

      const newState = reducer(state, {
        type: WAIVERS_SET_IS_REQUEST_WAIVER_POPOVER_SHOWN,
        payload: false,
      });

      expect(newState.isRequestWaiverPopoverShown).toBe(false);
    });
  });

  describe('UI_ROUTER_ON_FINISH action', function () {
    it('resets state to initState', function () {
      const state = {
        loadingManageWaiversData: true,
        loadManageWaiversDataError: 'error',
        hasPermissionForAppWaivers: true,
        previousRouterStateNameForComponentDetails: 'applicationReport.componentDetails.legal',
      };

      const newState = reducer(state, {
        type: UI_ROUTER_ON_FINISH,
      });

      expect(newState).toEqual({
        ...initState,
        previousRouterStateNameForComponentDetails: state.previousRouterStateNameForComponentDetails,
      });
    });
  });
});
