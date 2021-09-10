/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/componentDetails/componentDetailsReducer';

describe('componentDetailsReducer', function () {
  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = { type: 'UNKNOWN' };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState.pendingLoads).toEqual(new Set());
      expect(newState.loadError).toBe(null);
      expect(newState.labels).toEqual([]);
    });

    it('is immutable', function () {
      const action = { type: 'UNKNOWN' };
      const state = reduce(undefined, action);

      // Overall state object
      expect(() => {
        state.newProp = 'newProp';
      }).toThrowError(TypeError);

      // NOTE pendingLoads Set is not actually immutable, as Object.freeze doesn't work on Sets
    });
  });

  describe('LOAD_COMPONENT_LABELS_REQUESTED action', function () {
    it('adds "labels" pending load', function () {
      const state = { pendingLoads: new Set(), labels: [], loadError: null };
      const newState = reduce(state, {
        type: 'LOAD_COMPONENT_LABELS_REQUESTED',
      });
      expect(newState.pendingLoads.has('labels')).toBe(true);
    });
  });

  describe('LOAD_COMPONENT_LABELS_FULFILLED action', function () {
    it('adds labels value and removes "labels" pending load', function () {
      const state = { pendingLoads: new Set(), labels: [], loadError: null };
      const newState = reduce(state, {
        type: 'LOAD_COMPONENT_LABELS_FULFILLED',
        payload: [],
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.labels).toEqual([]);
      expect(newState.loadError).toBeNull();
    });
  });

  describe('LOAD_COMPONENT_LABELS_FAILED action', function () {
    it('adds loadError value and removes "labels" pending load', function () {
      const state = { pendingLoads: new Set(), labels: [], loadError: null };
      const newState = reduce(state, {
        type: 'LOAD_COMPONENT_LABELS_FAILED',
        payload: 'error',
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.loadError).toEqual('error');
    });

    it('clears error state on retry', function () {
      const state = { pendingLoads: new Set(), labels: [], loadError: null };
      const newState = reduce(state, {
        type: 'LOAD_COMPONENT_LABELS_FAILED',
        payload: 'error',
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.loadError).toEqual('error');

      const retryState = reduce(newState, {
        type: 'LOAD_COMPONENT_LABELS_FULFILLED',
        payload: [],
      });
      expect(retryState.loadError).toBeNull();
    });
  });
});
