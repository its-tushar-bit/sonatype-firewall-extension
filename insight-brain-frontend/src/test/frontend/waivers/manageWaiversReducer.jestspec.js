/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initState } from '../../../main/frontend/waivers/manageWaiversReducer';
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
    });
  });

  describe('UI_ROUTER_ON_FINISH action', function () {
    it('resets state to initState', function () {
      const state = {
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
