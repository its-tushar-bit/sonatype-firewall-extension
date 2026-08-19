/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/violation/waiveTransitiveViolationsSlice';

describe('waiveTransitiveViolationsReducer', function () {
  let otherObject;

  beforeEach(function () {
    otherObject = { value: 'test value' };
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('waiveTransitiveViolationsReducer/save/pending action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const newState = reduce(state, {
        type: 'waiveTransitiveViolationsReducer/save/pending',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('waiveTransitiveViolationsReducer/save/fulfilled action', function () {
    it('sets submitMaskState to true', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: false,
      });

      const newState = reduce(state, {
        type: 'waiveTransitiveViolationsReducer/save/fulfilled',
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('waiveTransitiveViolationsReducer/save/rejected action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
      });

      const newState = reduce(state, {
        type: 'waiveTransitiveViolationsReducer/save/rejected',
        payload: {
          response: 'someErrorResponse',
        },
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.saveError).toBe('someErrorResponse');
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('waiveTransitiveViolationsReducer/submitMaskTimerDone action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
      });

      const newState = reduce(state, {
        type: 'waiveTransitiveViolationsReducer/submitMaskTimerDone',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('waiveTransitiveViolationsReducer/cancel action', function () {
    it('returns the initial state', function () {
      const state = Object.freeze({
        other: otherObject,
      });

      const newState = reduce(state, {
        type: 'waiveTransitiveViolationsReducer/cancel',
      });

      expect(newState).toEqual({
        scope: null,
        expiration: 'never',
        comments: '',
        submitMaskState: null,
        saveError: null,
      });
    });
  });

  describe('waiveTransitiveViolationsReducer/setScope', function () {
    it('sets the scope', function () {
      const state = Object.freeze({ other: otherObject });

      const newState = reduce(state, {
        type: 'waiveTransitiveViolationsReducer/setScope',
        payload: 'someScope',
      });

      expect(newState.scope).toBe('someScope');
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('waiveTransitiveViolationsReducer/setExpiration', function () {
    it('sets the expiration', function () {
      const state = Object.freeze({ other: otherObject });

      const newState = reduce(state, {
        type: 'waiveTransitiveViolationsReducer/setExpiration',
        payload: 'someExpiration',
      });

      expect(newState.expiration).toBe('someExpiration');
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('waiveTransitiveViolationsReducer/setComments', function () {
    it('sets the comments', function () {
      const state = Object.freeze({ other: otherObject });

      const newState = reduce(state, {
        type: 'waiveTransitiveViolationsReducer/setComments',
        payload: 'someComments',
      });

      expect(newState.comments).toBe('someComments');
      expect(newState.other).toBe(otherObject);
    });
  });
});
