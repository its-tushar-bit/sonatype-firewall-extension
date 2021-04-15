/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/mail/mailConfigReducer';

describe('mailConfigReducer', function () {
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

  describe('MAIL_CONFIG_SAVE_REQUESTED action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_SAVE_REQUESTED',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('MAIL_CONFIG_SAVE_FULFILLED action', function () {
    it('sets submitMaskState to true', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {},
        submitMaskState: false,
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_SAVE_FULFILLED',
        payload: {
          hostname: 'test.host',
          port: 42,
          systemEmail: 'foo@bar.com',
        },
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('MAIL_CONFIG_SAVE_FAILED action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
        formState: {},
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_SAVE_FAILED',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('MAIL_CONFIG_DELETE_REQUESTED action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_DELETE_REQUESTED',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('MAIL_CONFIG_DELETE_FULFILLED action', function () {
    it('sets submitMaskState to true', function () {
      const state = Object.freeze({
        submitMaskState: false,
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_DELETE_FULFILLED',
      });

      expect(newState.submitMaskState).toBe(true);
    });
  });

  describe('MAIL_CONFIG_DELETE_FAILED action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_DELETE_FAILED',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('MAIL_CONFIG_SEND_TEST_MAIL_REQUESTED action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_SEND_TEST_MAIL_REQUESTED',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('MAIL_CONFIG_SEND_TEST_MAIL_FULFILLED action', function () {
    it('sets submitMaskState to true', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: false,
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_SEND_TEST_MAIL_FULFILLED',
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('MAIL_CONFIG_SEND_TEST_MAIL_FAILED action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: false,
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_SEND_TEST_MAIL_FAILED',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
      });

      const newState = reduce(state, {
        type: 'MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
