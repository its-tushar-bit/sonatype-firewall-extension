/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce, {
  initialState,
} from '../../../../../main/frontend/configuration/webhook/editWebhook/editWebhookReducer';
import {
  EDIT_WEBHOOK_LOAD_REQUESTED,
  EDIT_WEBHOOK_LOAD_FAILED,
  EDIT_WEBHOOK_LOAD_FULFILLED,
  EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
  EDIT_WEBHOOK_SET_URL,
  EDIT_WEBHOOK_SET_DESCRIPTION,
  EDIT_WEBHOOK_SET_SECRET_KEY,
  EDIT_WEBHOOK_SAVE_REQUESTED,
  EDIT_WEBHOOK_SAVE_FULFILLED,
  EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE,
  EDIT_WEBHOOK_SAVE_FAILED,
} from '../../../../../main/frontend/configuration/webhook/editWebhook/webhooksActions';

describe('editWebhookReducer', () => {
  let otherObject;

  beforeEach(() => {
    otherObject = { value: 'test value' };
  });

  describe(EDIT_WEBHOOK_LOAD_REQUESTED + ' action', () => {
    it('resets to initialState', () => {
      const state = {
        other: otherObject,
      };

      const newState = reduce(state, { type: EDIT_WEBHOOK_LOAD_REQUESTED });
      expect(newState).toBe(initialState);
    });
  });

  describe(EDIT_WEBHOOK_LOAD_FAILED + ' action', () => {
    it('resets isLoading and sets loadError to the payload', () => {
      const state = {
        isLoading: true,
        loadError: null,
        other: otherObject,
      };

      const { isLoading, loadError, other } = reduce(state, {
        type: EDIT_WEBHOOK_LOAD_FAILED,
        payload: 'webhook error',
      });
      expect(isLoading).toBe(false);
      expect(loadError).toBe('webhook error');
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_LOAD_FULFILLED + ' action', () => {
    it('resets isLoading and loadError and sets data from payload', () => {
      const state = {
        isLoading: true,
        loadError: 'error',
        availableEventTypes: [],
        isAppWebhooksSupported: false,
        other: otherObject,
      };

      const payload = {
        eventTypes: ['eventType1', 'eventType2'],
        productFeatures: ['webhooks-for-applications', 'foo', 'bar'],
      };

      const { isLoading, loadError, availableEventTypes, isAppWebhooksSupported, other } = reduce(state, {
        type: EDIT_WEBHOOK_LOAD_FULFILLED,
        payload,
      });
      expect(isLoading).toBe(false);
      expect(loadError).toBe(null);
      expect(availableEventTypes).toBe(payload.eventTypes);
      expect(isAppWebhooksSupported).toBe(true);
      expect(other).toBe(otherObject);
    });

    it('sets isAppWebhooksSupported to false if application webhooks are not supported ', () => {
      const state = {
        isAppWebhooksSupported: true,
        other: otherObject,
      };

      const payload = {
        productFeatures: ['webhooks-for-repositories', 'foo', 'bar'],
      };

      const { isAppWebhooksSupported, other } = reduce(state, {
        type: EDIT_WEBHOOK_LOAD_FULFILLED,
        payload,
      });
      expect(isAppWebhooksSupported).toBe(false);
      expect(other).toBe(otherObject);
    });

    it('sets loadError if both application and repo webhooks are not supported', () => {
      const state = {
        loadError: null,
        other: otherObject,
      };

      const payload = {
        productFeatures: ['foo', 'bar'],
      };

      const { loadError, other } = reduce(state, {
        type: EDIT_WEBHOOK_LOAD_FULFILLED,
        payload,
      });
      expect(loadError).toBe('Webhooks feature is not supported by your license.');
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_TOGGLE_EVENT_TYPE + ' action', () => {
    it('removes event type if already exists', () => {
      const state = {
        selectedEventTypes: ['foo', 'bar', 'baz'],
        other: otherObject,
      };

      const { selectedEventTypes, other } = reduce(state, {
        type: EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
        payload: 'bar',
      });
      expect(selectedEventTypes).toEqual(['foo', 'baz']);
      expect(selectedEventTypes).not.toBe(state.selectedEventTypes);
      expect(other).toBe(otherObject);
    });

    it('adds event type if does not exists', () => {
      const state = {
        selectedEventTypes: ['foo', 'bar', 'baz'],
        other: otherObject,
      };

      const { selectedEventTypes, other } = reduce(state, {
        type: EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
        payload: 'new',
      });
      expect(selectedEventTypes).toEqual(['foo', 'bar', 'baz', 'new']);
      expect(selectedEventTypes).not.toBe(state.selectedEventTypes);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_SET_URL + ' action', () => {
    it('sets url userInput with no validationErrors if payload starts with http://', () => {
      const state = {
        inputFields: {
          url: null,
          other: otherObject,
        },
        other: otherObject,
      };

      const { inputFields, other } = reduce(state, {
        type: EDIT_WEBHOOK_SET_URL,
        payload: 'http://foo',
      });

      expect(other).toBe(otherObject);
      expect(inputFields.other).toBe(otherObject);
      expect(inputFields.url.value).toBe('http://foo');
      expect(inputFields.url.validationErrors).toEqual([]);
    });

    it('sets url userInput with no validationErrors if payload starts with https://', () => {
      const state = {
        inputFields: {
          url: null,
          other: otherObject,
        },
        other: otherObject,
      };

      const { inputFields, other } = reduce(state, {
        type: EDIT_WEBHOOK_SET_URL,
        payload: 'https://foo',
      });

      expect(other).toBe(otherObject);
      expect(inputFields.other).toBe(otherObject);
      expect(inputFields.url.value).toBe('https://foo');
      expect(inputFields.url.validationErrors).toEqual([]);
    });

    it('sets url userInput with "Must be non-empty" error if payload is empty', () => {
      const state = {
        inputFields: {
          url: null,
          other: otherObject,
        },
        other: otherObject,
      };

      const { inputFields, other } = reduce(state, {
        type: EDIT_WEBHOOK_SET_URL,
        payload: '',
      });

      expect(other).toBe(otherObject);
      expect(inputFields.other).toBe(otherObject);
      expect(inputFields.url.value).toBe('');
      expect(inputFields.url.validationErrors).toEqual(['Must be non-empty']);
    });

    it('sets url userInput with validation error if payload prefix is invalid', () => {
      const state = {
        inputFields: {
          url: null,
          other: otherObject,
        },
        other: otherObject,
      };

      const { inputFields, other } = reduce(state, {
        type: EDIT_WEBHOOK_SET_URL,
        payload: 'http:/foo',
      });

      expect(other).toBe(otherObject);
      expect(inputFields.other).toBe(otherObject);
      expect(inputFields.url.value).toBe('http:/foo');
      expect(inputFields.url.validationErrors).toEqual(['Webhook URL must start with http:// or https://']);
    });
  });

  describe(EDIT_WEBHOOK_SET_DESCRIPTION + ' action', () => {
    it('sets description userInput', () => {
      const state = {
        inputFields: {
          description: null,
          other: otherObject,
        },
        other: otherObject,
      };

      const { inputFields, other } = reduce(state, {
        type: EDIT_WEBHOOK_SET_DESCRIPTION,
        payload: 'bar',
      });

      expect(other).toBe(otherObject);
      expect(inputFields.other).toBe(otherObject);
      expect(inputFields.description.value).toBe('bar');
    });
  });

  describe(EDIT_WEBHOOK_SET_SECRET_KEY + ' action', () => {
    it('sets secretKey userInput', () => {
      const state = {
        inputFields: {
          secretKey: null,
          other: otherObject,
        },
        other: otherObject,
      };

      const { inputFields, other } = reduce(state, {
        type: EDIT_WEBHOOK_SET_SECRET_KEY,
        payload: 'bar',
      });

      expect(other).toBe(otherObject);
      expect(inputFields.other).toBe(otherObject);
      expect(inputFields.secretKey.value).toBe('bar');
    });
  });

  describe(EDIT_WEBHOOK_SAVE_REQUESTED + ' action', () => {
    it('sets submitMaskState to false', () => {
      const state = {
        submitMaskState: null,
        other: otherObject,
      };

      const { submitMaskState, other } = reduce(state, { type: EDIT_WEBHOOK_SAVE_REQUESTED });

      expect(submitMaskState).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_SAVE_FULFILLED + ' action', () => {
    it('resets saveError and sets submitMaskState to true', () => {
      const state = {
        submitMaskState: null,
        saveError: 'error',
        other: otherObject,
      };

      const { submitMaskState, saveError, other } = reduce(state, { type: EDIT_WEBHOOK_SAVE_FULFILLED });

      expect(submitMaskState).toBe(true);
      expect(saveError).toBe(null);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_SAVE_FAILED + ' action', () => {
    it('resets submitMaskState and sets saveError to payload', () => {
      const state = {
        submitMaskState: null,
        other: otherObject,
      };

      const { submitMaskState, saveError, other } = reduce(state, {
        type: EDIT_WEBHOOK_SAVE_FAILED,
        payload: 'save webhook error',
      });

      expect(submitMaskState).toBe(null);
      expect(saveError).toBe('save webhook error');
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE + ' action', () => {
    it('resets submitMaskState', () => {
      const state = {
        submitMaskState: true,
        other: otherObject,
      };

      const { submitMaskState, other } = reduce(state, { type: EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE });

      expect(submitMaskState).toBe(null);
      expect(other).toBe(otherObject);
    });
  });
});
