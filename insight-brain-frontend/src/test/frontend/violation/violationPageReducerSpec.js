/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../main/frontend/violation/violationPageReducer';

describe('violationPageReducer', function() {
  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({foo: 'bar'});
      const action = {type: 'UNKNOWN'};
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reducer(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reducer(undefined, action);
      expect(newState.violationDetails).toBe(null);
      expect(newState.loading).toBe(false);
      expect(newState.error).toBe(null);
    });

    it('is immutable', function() {
      const action = {type: 'UNKNOWN'};
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
        state.error = 'Broke';
      }).toThrowError(TypeError);
    });
  });

  describe('LOAD_VIOLATION_REQUESTED action', function() {
    it('sets the loading flag to true', function() {
      const initialState = {
        violationDetails: {},
        error: 'foo',
        loading: false,
        otherProp: 'asdf'
      };

      const newState = reducer(initialState, { type: 'LOAD_VIOLATION_REQUESTED' });

      expect(newState).toEqual({
        loading: true,
        error: 'foo',
        violationDetails: {},
        otherProp: 'asdf'
      });
    });
  });

  describe('LOAD_VIOLATION_FULFILLED', function() {
    it('unsets loading and error and sets violationDetails to the payload', function() {
      const initialState = {
        violationDetails: {},
        error: 'baz',
        loading: false,
        otherProp: 'asdf'
      };

      const newState = reducer(initialState, {
        type: 'LOAD_VIOLATION_FULFILLED',
        payload: { foo: 'bar' }
      });

      expect(newState).toEqual({
        loading: false,
        error: null,
        violationDetails: { foo: 'bar' },
        otherProp: 'asdf'
      });
    });
  });

  describe('LOAD_VIOLATION_FAILED', function() {
    it('unsets the loading flag and sets the error to the payload', function() {
      const initialState = {
        violationDetails: null,
        error: null,
        loading: true,
        otherProp: 'asdf'
      };

      const newState = reducer(initialState, {
        type: 'LOAD_VIOLATION_FAILED',
        payload: 'ERRRRRRRRRRRRRRRRR'
      });

      expect(newState).toEqual({
        loading: false,
        error: 'ERRRRRRRRRRRRRRRRR',
        violationDetails: null,
        otherProp: 'asdf'
      });
    });
  });
});
