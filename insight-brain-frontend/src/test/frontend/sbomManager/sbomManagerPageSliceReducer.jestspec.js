/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/sbomManager/sbomManagerPageSlice';

describe('SbomManagerReducer', () => {
  let initialState;

  beforeEach(() => {
    const dummyAction = { type: 'DUMMY_ACTION' };
    initialState = reducer(undefined, dummyAction);
  });

  describe('initial state', () => {
    it('has default field values', function () {
      expect(initialState.showSbomManagerSidebar).toBeFalsy();
    });
  });

  describe('unknown action', () => {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };

      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('UI_ROUTER_ON_FINISH action', () => {
    const oldState = Object.freeze({
      showSbomManagerSidebar: false,
    });

    it('should set the showSbomManagerSidebar to true when path includes sbomManager', () => {
      const newState = reducer(oldState, {
        type: '@@reduxUiRouter/onFinish',
        payload: { toState: { name: 'sbomManager' } },
      });

      expect(newState).toEqual({
        showSbomManagerSidebar: true,
      });
    });

    it('should set the showSbomManagerSidebar to false when path does not include sbomManager', () => {
      const newState = reducer(oldState, {
        type: '@@reduxUiRouter/onFinish',
        payload: { toState: { name: 'otherPath' } },
      });

      expect(newState).toEqual({
        showSbomManagerSidebar: false,
      });
    });
  });
});
