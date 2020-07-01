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
});
