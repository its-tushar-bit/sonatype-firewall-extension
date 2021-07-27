/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../../main/frontend/configuration/proxy/proxyConfigReducer';

describe('proxyConfigReducer', () => {
  let otherObject;

  const serverData = {
    hostname: 'foo',
    username: 'user',
    port: 42,
    password: 'secret',
    passwordIsIncluded: true,
    excludeHosts: null,
  };

  beforeEach(() => {
    otherObject = { value: 'test value' };
  });

  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({ foo: 'bar' });

      const newState = reduce(state, { type: 'UNKNOWN' });
      expect(newState).toBe(state);
    });
  });

  describe('PROXY_CONFIG_SAVE_REQUESTED action', () => {
    it('sets submitMaskState to false', () => {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const { submitMaskState, other } = reduce(state, {
        type: 'PROXY_CONFIG_SAVE_REQUESTED',
      });

      expect(submitMaskState).toBe(false);
      expect(other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_SAVE_FULFILLED action', () => {
    it('sets submitMaskState to true', () => {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const { submitMaskState, other } = reduce(state, {
        type: 'PROXY_CONFIG_SAVE_FULFILLED',
        payload: serverData,
      });

      expect(submitMaskState).toBe(true);
      expect(other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_SAVE_FAILED action', () => {
    it('sets submitMaskState to null', () => {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const { submitMaskState, other } = reduce(state, {
        type: 'PROXY_CONFIG_SAVE_FAILED',
        payload: 'Error!',
      });

      expect(submitMaskState).toBe(null);
      expect(other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_DELETE_REQUESTED action', () => {
    it('sets deleteMaskState to false', () => {
      const state = Object.freeze({
        other: otherObject,
        deleteMaskState: null,
      });

      const { deleteMaskState, other } = reduce(state, {
        type: 'PROXY_CONFIG_DELETE_REQUESTED',
      });

      expect(deleteMaskState).toBe(false);
      expect(other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_DELETE_FULFILLED action', () => {
    it('sets deleteMaskState to true', () => {
      const state = Object.freeze({
        deleteMaskState: null,
      });

      const { deleteMaskState } = reduce(state, {
        type: 'PROXY_CONFIG_DELETE_FULFILLED',
      });

      expect(deleteMaskState).toBe(true);
    });
  });

  describe('PROXY_CONFIG_DELETE_FAILED action', () => {
    it('sets deleteMaskState to null', () => {
      const state = Object.freeze({
        other: otherObject,
        deleteMaskState: null,
      });

      const { deleteMaskState, other } = reduce(state, {
        type: 'PROXY_CONFIG_DELETE_FAILED',
        payload: 'Error!',
      });

      expect(deleteMaskState).toBe(null);
      expect(other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE action', () => {
    it('sets submitMaskState to null', () => {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const { submitMaskState, other } = reduce(state, {
        type: 'PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE',
      });

      expect(submitMaskState).toBe(null);
      expect(other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('PROXY_CONFIG_SET_HOSTNAME action', () => {
    it('sets isValid to false when using an invalid value', () => {
      const state = Object.freeze({
        formState: {
          hostname: { value: '' },
          port: { value: '' },
          username: { value: '' },
          password: { value: '' },
          excludeHosts: { value: '' },
        },
        isValid: true,
      });

      const { isValid } = reduce(state, {
        type: 'PROXY_CONFIG_SET_HOSTNAME',
        payload: 'sonatype.com/host',
      });

      expect(isValid).toBe(false);
    });

    it('sets validation errors on the hostname when using an invalid value', () => {
      const state = Object.freeze({
        formState: {
          hostname: { value: '' },
          port: { value: '' },
          username: { value: '' },
          password: { value: '' },
          excludeHosts: { value: '' },
        },
        isValid: true,
      });

      const { formState } = reduce(state, {
        type: 'PROXY_CONFIG_SET_HOSTNAME',
        payload: 'sonatype.com/host',
      });

      expect(formState.hostname.value).toEqual('sonatype.com/host');
      expect(formState.hostname.validationErrors).toEqual(['Invalid host name']);
    });

    it('sets the hostname value when using a valid hostname', () => {
      const state = Object.freeze({
        formState: {
          hostname: { value: 'host' },
          port: { value: '' },
          username: { value: '' },
          password: { value: '' },
          excludeHosts: { value: '' },
        },
        isValid: true,
      });

      const { formState } = reduce(state, {
        type: 'PROXY_CONFIG_SET_HOSTNAME',
        payload: 'sonatype.com',
      });

      expect(formState.hostname.value).toEqual('sonatype.com');
      expect(formState.hostname.validationErrors).toEqual([]);
    });
  });
});
