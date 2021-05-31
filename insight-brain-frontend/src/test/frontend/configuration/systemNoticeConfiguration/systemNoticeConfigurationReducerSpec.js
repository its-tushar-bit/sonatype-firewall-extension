/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import reduce, {
  DEFAULT_SYSTEM_NOTICE,
} from '../../../../main/frontend/configuration/systemNoticeConfiguration/systemNoticeConfigurationReducer';

describe('systemNoticeConfigurationReducer', function () {
  let otherObject;

  beforeEach(function () {
    otherObject = { value: 'test value' };
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_REQUESTED action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: null,
        },
      });

      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_UPDATE_REQUESTED',
      });

      expect(newState.viewState.submitMaskState).toBe(false);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_FULFILLED action', function () {
    it('sets submitMaskState to true', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: false,
        },
        formState: {
          enabled: true,
          message: {
            value: 'text',
            trimmedValue: 'text',
            isPristine: false,
          },
        },
      });

      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_UPDATE_FULFILLED',
      });

      expect(newState.viewState.submitMaskState).toBe(true);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: true,
        },
        formState: {},
        serverData: {
          enabled: false,
          message: '',
        },
      });

      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED',
        payload: 'update error',
      });

      expect(newState.viewState.submitMaskState).toBeNull();

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });

    it('sets updateError to the payload', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {},
        formState: {},
        serverData: {
          enabled: false,
        },
      });

      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED',
        payload: 'update error',
      });

      expect(newState.viewState.updateError).toEqual('update error');

      expect(newState.other).toBe(otherObject);
    });

    it('does not reset formState values', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {},
        formState: {
          enabled: true,
          message: 'message',
        },
        serverData: {
          enabled: false,
          message: 'old message',
        },
      });

      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED',
        payload: 'update error',
      });

      expect(newState.formState.enabled).toBe(true);
      expect(newState.formState.message).toBe('message');

      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          submitMaskState: true,
        },
      });

      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE',
      });

      expect(newState.viewState.submitMaskState).toBeNull();

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toBe(otherObject);
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED action', function () {
    it('updates the state and sets loading, loadError and mask to default', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          loading: false,
          loadError: 'error',
          submitMaskState: true,
        },
      });
      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED',
      });

      expect(newState.viewState.loading).toEqual(true);
      expect(newState.viewState.loadError).toBeNull();
      expect(newState.viewState.submitMaskState).toBeNull();
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED action', function () {
    it('updates the state and sets the error to null', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loading: true,
          loadError: 'error',
          updateError: 'error',
        },
        formState: {},
        serverData: {},
      });
      const payload = {
        enabled: true,
        message: 'message from server',
      };
      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED',
        payload: payload,
      });

      expect(newState.viewState.loading).toBe(false);
      expect(newState.viewState.loadError).toBeNull();
      expect(newState.viewState.updateError).toBeNull();
      expect(newState.formState).toEqual({
        enabled: payload.enabled,
        message: textInputStateHelpers.initialState(payload.message),
      });
      expect(newState.serverData).toBe(payload);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED action', function () {
    it('updates the state and sets the loadError to the payload', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loading: true,
          loadError: null,
        },
        formState: {
          enabled: true,
          message: {
            value: 'text',
            trimmedValue: 'text',
            isPristine: true,
          },
        },
      });
      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED',
        payload: 'page error',
      });

      expect(newState.viewState.loading).toBe(false);
      expect(newState.viewState.loadError).toBe('page error');

      expect(newState.formState).toEqual(state.formState);

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED action', function () {
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
        type: 'SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED',
        payload: 'error',
      });

      expect(newState.viewState.loading).toBe(false);
      expect(newState.viewState.loadError).toBe('error');

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });

    it('updates formState with DEFAULT_SYSTEM_NOTICE', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loading: true,
          loadError: null,
        },
        formState: {
          enabled: false,
          message: 'old message',
        },
      });
      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED',
        payload: 'error',
      });

      expect(newState.viewState.loading).toBe(false);
      expect(newState.formState.enabled).toBe(DEFAULT_SYSTEM_NOTICE.enabled);
      expect(newState.formState.message).toEqual({
        value: DEFAULT_SYSTEM_NOTICE.message,
        trimmedValue: DEFAULT_SYSTEM_NOTICE.message,
        isPristine: true,
        validationErrors: null,
      });

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_RESET_FORM action', function () {
    it('updates the state and resets formState to initial', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          isDirty: true,
        },
        formState: {
          enabled: false,
          message: {
            value: 'new message',
            trimmedValue: 'new message',
          },
        },
        serverData: {
          enabled: true,
          message: 'old message',
        },
      });
      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_RESET_FORM',
      });

      expect(newState.viewState.isDirty).toBe(false);
      expect(newState.formState.enabled).toBe(true);
      expect(newState.formState.message).toEqual({
        value: 'old message',
        trimmedValue: 'old message',
        isPristine: true,
        validationErrors: null,
      });

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_TOGGLE_ENABLED action', function () {
    it('updates the state and toggles enabled formState value', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          isDirty: false,
        },
        formState: {
          enabled: false,
          message: {
            value: 'new message',
            trimmedValue: 'new message',
          },
        },
        serverData: {
          enabled: true,
          message: 'old message',
        },
      });
      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_TOGGLE_ENABLED',
      });

      expect(newState.viewState.isDirty).toBe(true);
      expect(newState.formState.enabled).toBe(true);
      expect(newState.formState.message).toEqual({
        value: 'new message',
        trimmedValue: 'new message',
      });

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });

  describe('SYSTEM_NOTICE_CONFIGURATION_SET_MESSAGE action', function () {
    it('updates the state and sets message to formState value', function () {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          isDirty: false,
        },
        formState: {
          enabled: false,
          message: {
            value: 'old message',
            trimmedValue: 'old message',
            isPristine: true,
            validationErrors: null,
          },
        },
        serverData: {
          enabled: true,
          message: 'old message',
        },
      });
      const newState = reduce(state, {
        type: 'SYSTEM_NOTICE_CONFIGURATION_SET_MESSAGE',
        payload: 'new message',
      });

      expect(newState.viewState.isDirty).toBe(true);
      expect(newState.formState.enabled).toBe(false);
      expect(newState.formState.message).toEqual({
        value: 'new message',
        trimmedValue: 'new message',
        isPristine: false,
        validationErrors: null,
      });

      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });
});
