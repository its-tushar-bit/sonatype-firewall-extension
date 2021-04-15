/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../../main/frontend/waivers/deleteWaiverModal/deleteWaiverReducer';

describe('deleteWaiverReducer', function () {
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
      const state = reducer(undefined, action);
      expect(state.waiverToDelete).toBeNull();
      expect(state.deleteWaiverSaving).toBeNull();
      expect(state.deleteWaiverError).toBeNull();
    });

    it('is immutable', function () {
      const action = { type: 'UNKNOWN' };
      const state = reducer(undefined, action);

      expect(() => {
        state.newProp = 'newProp';
      }).toThrowError(TypeError);

      expect(() => {
        state.deleteWaiverSaving = false;
      }).toThrowError(TypeError);
    });
  });

  describe('WAIVERS_SET_WAIVER_TO_DELETE action', function () {
    it('sets the waiverToDelete from the payload and clears flags', function () {
      const initialState = {
        waiverToDelete: null,
        deleteWaiverSaving: false,
        deleteWaiverError: null,
        otherProp: { prop: 'foo' },
      };
      const action = {
        type: 'WAIVERS_SET_WAIVER_TO_DELETE',
        payload: { waiverId: 'foo' },
      };

      const newState = reducer(initialState, action);
      expect(newState.waiverToDelete).toEqual({ waiverId: 'foo' });
      expect(newState.otherProp).toBeUndefined();
      expect(newState.deleteWaiverSaving).toBeNull();
      expect(newState.deleteWaiverError).toBeNull();
    });
  });

  describe('WAIVERS_HIDE_DELETE_WAIVER_MODAL action', function () {
    it('resets state to initial conditions', function () {
      const state = {
        waiverToDelete: null,
        deleteWaiverSaving: true,
        deleteWaiverError: 'Error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'WAIVERS_HIDE_DELETE_WAIVER_MODAL',
      });
      expect(newState.waiverToDelete).toBeNull();
      expect(newState.deleteWaiverSaving).toBeNull();
      expect(newState.deleteWaiverError).toBeNull();
      expect(newState.otherProp).not.toBeDefined();
    });
  });

  describe('WAIVERS_DELETE_WAIVER_REQUESTED action', function () {
    it('sets the deleteWaiverSaving flag to false', function () {
      const state = {
        deleteWaiverSaving: null,
        otherProp: { prop: 'foo' },
      };
      const newState = reducer(state, {
        type: 'WAIVERS_DELETE_WAIVER_REQUESTED',
      });

      expect(newState.deleteWaiverSaving).toBe(false);
      expect(newState.otherProp).toEqual(state.otherProp);
    });

    it('clears any error that may have previously been in the state', function () {
      const state = {
        deleteWaiverError: 'err',
        otherProp: { prop: 'foo' },
      };
      const newState = reducer(state, {
        type: 'WAIVERS_DELETE_WAIVER_REQUESTED',
      });

      expect(newState.deleteWaiverError).toBeNull();
      expect(newState.otherProp).toEqual(state.otherProp);
    });
  });

  describe('WAIVERS_DELETE_WAIVER_FAILED action', function () {
    it('sets the deleteWaiverError prop and toggles deleteWaiverSaving to null ', function () {
      const state = {
        deleteWaiverSaving: false,
        deleteWaiverError: null,
        otherProp: { prop: 'foo' },
      };
      const newState = reducer(state, {
        type: 'WAIVERS_DELETE_WAIVER_FAILED',
        payload: 'Foo!',
      });
      expect(newState.deleteWaiverError).toEqual('Foo!');
      expect(newState.otherProp).toEqual(state.otherProp);
      expect(newState.deleteWaiverSaving).toBeNull();
    });
  });

  describe('WAIVERS_DELETE_WAIVER_FULFILLED action', function () {
    it('sets the deleteWaiverSaving prop to true', function () {
      const state = {
        deleteWaiverSaving: false,
        deleteWaiverError: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'WAIVERS_DELETE_WAIVER_FULFILLED',
      });
      expect(newState.deleteWaiverSaving).toBe(true);
      expect(newState.otherProp).toBe(state.otherProp);
    });
  });

  describe('WAIVERS_DELETE_MASK_TIMER_DONE action', function () {
    it('resets state to initial conditions', function () {
      const state = {
        waiverToDelete: { waiverId: 'foo' },
        deleteWaiverSaving: true,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(state, {
        type: 'WAIVERS_DELETE_MASK_TIMER_DONE',
      });
      expect(newState.waiverToDelete).toBeNull();
      expect(newState.deleteWaiverSaving).toBeNull();
      expect(newState.deleteWaiverError).toBeNull();
      expect(newState.otherProp).not.toBeDefined();
    });
  });

  describe('UI_ROUTER_ON_FINISH action', function () {
    it('clears state on onFinish', function () {
      const currentState = {
        waiverToDelete: { waiverId: 'foo' },
        deleteWaiverSaving: true,
        deleteWaiverError: 'Some Err',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(currentState, {
        type: '@@reduxUiRouter/onFinish',
      });

      expect(newState.waiverToDelete).toBeNull();
      expect(newState.deleteWaiverSaving).toBeNull();
      expect(newState.deleteWaiverError).toBeNull();
      expect(newState.otherProp).toBeUndefined();
    });
  });
});
