/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/advancedSearch/advancedSearchConfigReducer';

describe('advancedSearchConfigReducer', function () {
  let otherObject;

  beforeEach(function () {
    otherObject = { value: 'test value' };
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

      // formState
      expect(newState.formState.isEnabled).toBeFalsy();
      expect(newState.formState.lastIndexTime).toBeNull();
      expect(newState.formState.isFullIndexTriggered).toBeFalsy();

      // viewState
      expect(newState.viewState.loading).toBeTruthy();
      expect(newState.viewState.loadError).toBeNull();
      expect(newState.viewState.saveError).toBeNull();
      expect(newState.viewState.reIndexError).toBeNull();
      expect(newState.viewState.pollError).toBeNull();
      expect(newState.viewState.submitMaskState).toBeNull();
      expect(newState.viewState.submitMaskMessage).toBeNull();
      expect(newState.viewState.isDirty).toBeFalsy();

      expect(newState.currentlyPolling).toBeFalsy();

      expect(newState.serverData).toBeNull();
    });
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

  describe('ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: null,
        },
      });

      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED',
      });

      expect(newState.viewState.submitMaskState).toBe(false);

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED action', function () {
    it('sets submitMaskState to true', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: false,
        },
      });

      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED',
      });

      expect(newState.viewState.submitMaskState).toBe(true);

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_SAVE_FAILED action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: true,
        },
        formState: {},
      });

      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_SAVE_FAILED',
      });

      expect(newState.viewState.submitMaskState).toBeNull();

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: true,
        },
      });

      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE',
      });

      expect(newState.viewState.submitMaskState).toBeNull();

      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING action', function () {
    it('sets that polling is started', function () {
      const state = Object.freeze({
        other: otherObject,
        currentlyPolling: false,
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING',
        payload: true,
      });
      expect(newState.currentlyPolling).toBeTruthy();
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_TRIGGER_RE_INDEX action', function () {
    it('sets that a full index is triggered', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          other: otherObject,
          isFullIndexTriggered: false,
        },
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_TRIGGER_RE_INDEX',
      });
      expect(newState.formState.isFullIndexTriggered).toBeTruthy();
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.formState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_RE_INDEX_FAILED action', function () {
    it('sets the reIndexError and that a full index is not triggered', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          other: otherObject,
          isFullIndexTriggered: true,
        },
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_RE_INDEX_FAILED',
        payload: 'some error',
      });
      expect(newState.formState.isFullIndexTriggered).toBeFalsy();
      expect(newState.viewState.reIndexError).toBe('some error');
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.formState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_POLL_STATE_SUCCESS action', function () {
    it('updates the state and sets the pollError to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          pollError: 'error!',
        },
        formState: {
          isEnabled: true,
          lastIndexTime: null,
          isFullIndexTriggered: false,
        },
        serverData: otherObject,
      });
      const payload = {
        isEnabled: false,
        lastIndexTime: 'some time',
        isFullIndexTriggered: true,
      };
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_POLL_STATE_SUCCESS',
        payload: payload,
      });
      expect(newState.viewState.pollError).toBeNull();
      expect(newState.formState).not.toBeNull();
      expect(newState.formState.isEnabled).toBeTruthy();
      expect(newState.formState.lastIndexTime).toBe('some time');
      expect(newState.formState.isFullIndexTriggered).toBeTruthy();
      expect(newState.serverData).toBe(payload);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_POLL_STATE_FAILED action', function () {
    it('updates the state, sets that a full index is not triggered, sets the pollError, and that polling is stopped', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          other: otherObject,
          isFullIndexTriggered: true,
        },
        viewState: {
          other: otherObject,
          pollError: null,
        },
        currentlyPolling: true,
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_POLL_STATE_FAILED',
        payload: 'error!',
      });
      expect(newState.viewState.pollError).toBe('error!');
      expect(newState.currentlyPolling).toBeFalsy();
      expect(newState.formState.isFullIndexTriggered).toBeFalsy();
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.formState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED action', function () {
    it('updates to the initial state', function () {
      const state = Object.freeze({
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED',
      });
      // formState
      expect(newState.formState.isEnabled).toBeFalsy();
      expect(newState.formState.lastIndexTime).toBeNull();
      expect(newState.formState.isFullIndexTriggered).toBeFalsy();

      // viewState
      expect(newState.viewState.loading).toBeTruthy();
      expect(newState.viewState.loadError).toBeNull();
      expect(newState.viewState.saveError).toBeNull();
      expect(newState.viewState.reIndexError).toBeNull();
      expect(newState.viewState.pollError).toBeNull();
      expect(newState.viewState.submitMaskState).toBeNull();
      expect(newState.viewState.submitMaskMessage).toBeNull();
      expect(newState.viewState.isDirty).toBeFalsy();

      expect(newState.serverData).toBeNull();
      expect(newState.currentlyPolling).toBeFalsy();
    });

    it('does not modify an existing currentlyPolling', function () {
      const state = Object.freeze({
        other: otherObject,
        currentlyPolling: true,
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED',
      });
      expect(newState.currentlyPolling).toBeTruthy();
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED action', function () {
    it('updates the state and sets the error to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loading: true,
          loadError: 'error!',
          saveError: 'error!',
          reIndexError: 'error!',
          pollError: 'error!',
        },
        formState: otherObject,
        serverData: otherObject,
      });
      const payload = {
        isEnabled: false,
        lastIndexTime: 'some time',
        isFullIndexTriggered: true,
      };
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED',
        payload: payload,
      });
      expect(newState.viewState.loading).toBeFalsy();
      expect(newState.viewState.loadError).toBeNull();
      expect(newState.viewState.saveError).toBeNull();
      expect(newState.viewState.reIndexError).toBeNull();
      expect(newState.viewState.pollError).toBeNull();
      expect(newState.formState).toBe(payload);
      expect(newState.serverData).toBe(payload);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('ADVANCED_SEARCH_CONFIG_LOAD_FAILED action', function () {
    it('updates the state and sets the loadError to the payload', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loading: true,
          loadError: null,
        },
      });
      const newState = reduce(state, {
        type: 'ADVANCED_SEARCH_CONFIG_LOAD_FAILED',
        payload: 'error!',
      });
      expect(newState.viewState.loading).toBeFalsy();
      expect(newState.viewState.loadError).toBe('error!');
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });
});
