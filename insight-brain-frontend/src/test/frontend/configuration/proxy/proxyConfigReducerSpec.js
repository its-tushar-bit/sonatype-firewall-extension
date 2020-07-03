/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/proxy/proxyConfigReducer';

describe('proxyConfigReducer', function() {
  let otherObject;

  const serverData = {
    hostname: 'foo',
    username: 'user',
    port: 42,
    password: 'secret',
    passwordIsIncluded: true,
    excludeHosts: null
  };

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

  describe('PROXY_CONFIG_SAVE_REQUESTED action', function() {
    it('sets submitMaskState to false', function() {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null
      });

      const newState = reduce(state, {
        type: 'PROXY_CONFIG_SAVE_REQUESTED'
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_SAVE_FULFILLED action', function() {
    it('sets submitMaskState to true', function() {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null
      });

      const newState = reduce(state, {
        type: 'PROXY_CONFIG_SAVE_FULFILLED',
        payload: serverData
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_SAVE_FAILED action', function() {
    it('sets submitMaskState to null', function() {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null
      });

      const newState = reduce(state, {
        type: 'PROXY_CONFIG_SAVE_FAILED',
        payload: 'Error!'
      });

      expect(newState.submitMaskState).toBe(null);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_DELETE_REQUESTED action', function() {
    it('sets submitMaskState to false', function() {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null
      });

      const newState = reduce(state, {
        type: 'PROXY_CONFIG_DELETE_REQUESTED'
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_DELETE_FULFILLED action', function() {
    it('sets submitMaskState to true', function() {
      const state = Object.freeze({
        submitMaskState: null
      });

      const newState = reduce(state, {
        type: 'PROXY_CONFIG_DELETE_FULFILLED'
      });

      expect(newState.submitMaskState).toBe(true);
    });
  });

  describe('PROXY_CONFIG_DELETE_FAILED action', function() {
    it('sets submitMaskState to null', function() {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null
      });

      const newState = reduce(state, {
        type: 'PROXY_CONFIG_DELETE_FAILED',
        payload: 'Error!'
      });

      expect(newState.submitMaskState).toBe(null);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE action', function() {
    it('sets submitMaskState to null', function() {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null
      });

      const newState = reduce(state, {
        type: 'PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE'
      });

      expect(newState.submitMaskState).toBe(null);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
