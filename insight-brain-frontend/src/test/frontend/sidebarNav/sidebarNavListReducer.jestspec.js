/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../main/frontend/sidebarNav/sidebarNavListReducer';

describe('sidebarNavListReducer', function () {
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
      expect(newState.data).toEqual([]);
      expect(newState.loading).toBe(false);
      expect(newState.error).toBe(null);
      expect(newState.contentType).toBe('');
      expect(newState.sidebarId).toBe(null);
      expect(newState.sidebarReference).toBe(null);
    });

    it('is immutable', function () {
      const action = { type: 'UNKNOWN' };
      const state = reducer(undefined, action);

      // Overall state object
      expect(() => {
        state.newProp = 'newProp';
      }).toThrowError(TypeError);

      // array
      expect(() => {
        state.data.push('new data element');
      }).toThrowError(TypeError);

      expect(() => {
        state.loading = true;
      }).toThrowError(TypeError);

      expect(() => {
        state.error = 'Broke';
      }).toThrowError(TypeError);
    });
  });

  describe('LOAD_SIDEBAR_NAV_LIST_REQUESTED action', function () {
    it('sets the loading flag to true and sets only sidebar and contentType data from the payload on the state', () => {
      const initialState = {
        data: [],
        error: 'foo',
        loading: false,
        otherProp: 'asdf',
      };

      const newState = reducer(initialState, {
        type: 'LOAD_SIDEBAR_NAV_LIST_REQUESTED',
        payload: {
          sidebarId: 'sidebarId',
          sidebarReference: 'sidebarReference',
          contentType: 'contentType',
          foo: 'bar',
        },
      });

      expect(newState).toEqual({
        loading: true,
        error: 'foo',
        sidebarId: 'sidebarId',
        sidebarReference: 'sidebarReference',
        contentType: 'contentType',
        data: [],
        otherProp: 'asdf',
      });
    });
  });

  describe('LOAD_SIDEBAR_NAV_LIST_FULFILLED', function () {
    it('unsets loading and error and sets data', function () {
      const initialState = {
        data: [],
        error: 'baz',
        loading: true,
        otherProp: 'asdf',
      };

      const newState = reducer(initialState, {
        type: 'LOAD_SIDEBAR_NAV_LIST_FULFILLED',
        payload: {
          backButtonStateName: 'foo.bar.baz',
          contentType: 'violations',
          data: [{ foo: 'bar' }],
        },
      });

      expect(newState).toEqual({
        loading: false,
        error: null,
        data: [{ foo: 'bar' }],
        backButtonStateName: 'foo.bar.baz',
        contentType: 'violations',
        otherProp: 'asdf',
      });
    });
  });

  describe('LOAD_SIDEBAR_NAV_LIST_FAILED', function () {
    it('unsets the loading flag and sets the error to the payload', function () {
      const initialState = {
        data: [],
        error: null,
        loading: true,
        otherProp: 'asdf',
      };

      const newState = reducer(initialState, {
        type: 'LOAD_SIDEBAR_NAV_LIST_FAILED',
        payload: 'ERRRRRRRRRRRRRRRRR',
      });

      expect(newState).toEqual({
        loading: false,
        error: 'ERRRRRRRRRRRRRRRRR',
        data: [],
        otherProp: 'asdf',
      });
    });
  });
});
