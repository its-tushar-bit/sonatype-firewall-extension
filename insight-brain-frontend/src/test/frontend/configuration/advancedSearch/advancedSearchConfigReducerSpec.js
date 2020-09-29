/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/advancedSearch/advancedSearchConfigReducer';

describe('advancedSearchConfigReducer', function() {
  let otherObject;

  beforeEach(function() {
    otherObject = {value: 'test value'};
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function() {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);

      // formState
      expect(newState.formState.isEnabled).toBeFalsy();
      expect(newState.formState.lastIndexTime).toBeNull();
      expect(newState.formState.isFullIndexTriggered).toBeFalsy();

      // viewState
      expect(newState.viewState.loading).toBeTruthy();
      expect(newState.viewState.error).toBeNull();
      expect(newState.viewState.submitMaskState).toBeNull();
      expect(newState.viewState.submitMaskMessage).toBeNull();
      expect(newState.viewState.isDirty).toBeFalsy();

      expect(newState.serverData).toBeNull();
    });
  });

  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({foo: 'bar'});
      const action = {
        type: 'UNKNOWN'
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED action', function() {
    it('sets submitMaskState to false', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: null
        }
      });

      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED'
      });

      expect(newState.viewState.submitMaskState).toBe(false);

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED action', function() {
    it('sets submitMaskState to true', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: false
        }
      });

      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED'
      });

      expect(newState.viewState.submitMaskState).toBe(true);

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_SAVE_FAILED action', function() {
    it('sets submitMaskState to null', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: true
        },
        formState: {}
      });

      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_SAVE_FAILED'
      });

      expect(newState.viewState.submitMaskState).toBeNull();

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE action', function() {
    it('sets submitMaskState to null', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: true
        }
      });

      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE'
      });

      expect(newState.viewState.submitMaskState).toBeNull();

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_TRIGGER_RE_INDEX action', function() {
    it('sets that a full index is triggered', function() {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          other: otherObject,
          isFullIndexTriggered: false
        }
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_TRIGGER_RE_INDEX'
      });
      expect(newState.formState.isFullIndexTriggered).toBeTruthy();
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.formState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_RE_INDEX_FAILED action', function() {
    it('sets the error and that a full index is not triggered', function() {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          other: otherObject,
          isFullIndexTriggered: true
        }
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_RE_INDEX_FAILED',
        payload: 'some error'
      });
      expect(newState.formState.isFullIndexTriggered).toBeFalsy();
      expect(newState.viewState.error).toBe('some error');
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.formState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_POLL_STATE_SUCCESS action', function() {
    it('updates the state and sets the error to null', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          error: 'error!'
        },
        formState: otherObject,
        serverData: otherObject
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_POLL_STATE_SUCCESS',
        payload: 'payload'
      });
      expect(newState.viewState.error).toBeNull();
      expect(newState.formState).toBe('payload');
      expect(newState.serverData).toBe('payload');
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_POLL_STATE_FAILED action', function() {
    it('updates the state, sets that a full index is not triggered, and sets the error', function() {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          other: otherObject,
          isFullIndexTriggered: true
        },
        viewState: {
          other: otherObject,
          error: null
        }
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_POLL_STATE_FAILED',
        payload: 'error!'
      });
      expect(newState.viewState.error).toBe('error!');
      expect(newState.formState.isFullIndexTriggered).toBeFalsy();
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.formState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });
});
