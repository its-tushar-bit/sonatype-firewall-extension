/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce, { initialState } from '../../../../main/frontend/configuration/webhook/webhookReducer';
import {
  EDIT_WEBHOOK_LOAD_REQUESTED,
  EDIT_WEBHOOK_LOAD_FAILED,
  EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED,
  EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED,
  EDIT_WEBHOOK_LOAD_FULFILLED,
  EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
  EDIT_WEBHOOK_SET_URL,
  EDIT_WEBHOOK_SET_DESCRIPTION,
  EDIT_WEBHOOK_SET_SECRET_KEY,
  EDIT_WEBHOOK_SAVE_REQUESTED,
  EDIT_WEBHOOK_SAVE_FULFILLED,
  EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE,
  EDIT_WEBHOOK_SAVE_FAILED,
  EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED,
  EDIT_WEBHOOK_DELETE_REQUESTED,
  EDIT_WEBHOOK_DELETE_FULFILLED,
  EDIT_WEBHOOK_DELETE_FAILED,
} from '../../../../main/frontend/configuration/webhook/webhookActions';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('webhookReducer', () => {
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

  describe(EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED + ' action', () => {
    it('sets availableEventTypes from payload', () => {
      const state = {
        availableEventTypes: [],
        other: otherObject,
      };

      const payload = ['eventType1', 'eventType2'];

      const { availableEventTypes, other } = reduce(state, {
        type: EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED,
        payload,
      });
      expect(availableEventTypes).toBe(payload);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED + ' action', () => {
    it('sets isAppWebhooksSupported from payload', () => {
      const state = {
        isAppWebhooksSupported: false,
        other: otherObject,
      };

      const { isAppWebhooksSupported, other } = reduce(state, {
        type: EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED,
        payload: true,
      });
      expect(isAppWebhooksSupported).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED + ' action', () => {
    it('sets webhooks from payload', () => {
      const state = {
        webhooks: null,
        other: otherObject,
      };

      const payload = ['webhook1', 'webhook2'];

      const { webhooks, other } = reduce(state, {
        type: EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED,
        payload,
      });
      expect(webhooks).toBe(payload);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_LOAD_FULFILLED + ' action', () => {
    it('resets isLoading and loadError', () => {
      const state = {
        isLoading: true,
        loadError: 'error',
        other: otherObject,
      };

      const { isLoading, loadError, other } = reduce(state, {
        type: EDIT_WEBHOOK_LOAD_FULFILLED,
      });
      expect(isLoading).toBe(false);
      expect(loadError).toBe(null);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_TOGGLE_EVENT_TYPE + ' action', () => {
    it('removes event type if already exists', () => {
      const state = {
        selectedEventTypes: ['foo', 'bar', 'baz'],
        other: otherObject,
        inputFields: {
          url: initUserInput(''),
          description: initUserInput(''),
          secretKey: initUserInput(''),
        },
        serverData: {},
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
        inputFields: {
          url: initUserInput(''),
          description: initUserInput(''),
          secretKey: initUserInput(''),
        },
        serverData: {},
      };

      const { selectedEventTypes, other } = reduce(state, {
        type: EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
        payload: 'new',
      });
      expect(selectedEventTypes).toEqual(['foo', 'bar', 'baz', 'new']);
      expect(selectedEventTypes).not.toBe(state.selectedEventTypes);
      expect(other).toBe(otherObject);
    });

    it('sets isDirty to true', () => {
      const { isDirty } = reduce(initialState, {
        type: EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
        payload: 'new',
      });
      expect(isDirty).toEqual(true);
    });
  });

  describe(EDIT_WEBHOOK_SET_URL + ' action', () => {
    it('sets url userInput with no validationErrors if payload starts with http://', () => {
      const state = {
        inputFields: {
          url: null,
          other: otherObject,
        },
        serverData: {},
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
        serverData: {},
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
          url: initUserInput(''),
          description: initUserInput(''),
          secretKey: initUserInput(''),
          other: otherObject,
        },
        serverData: {},
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
        serverData: {},
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

    it('sets isDirty to true', () => {
      const { isDirty } = reduce(initialState, {
        type: EDIT_WEBHOOK_SET_URL,
        payload: 'http:/foo',
      });
      expect(isDirty).toEqual(true);
    });
  });

  describe(EDIT_WEBHOOK_SET_DESCRIPTION + ' action', () => {
    it('sets description userInput', () => {
      const state = {
        inputFields: {
          url: initUserInput(''),
          description: initUserInput(''),
          secretKey: initUserInput(''),
          other: otherObject,
        },
        serverData: {},
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

    it('sets isDirty to true', () => {
      const { isDirty } = reduce(initialState, {
        type: EDIT_WEBHOOK_SET_DESCRIPTION,
        payload: 'bar',
      });
      expect(isDirty).toEqual(true);
    });
  });

  describe(EDIT_WEBHOOK_SET_SECRET_KEY + ' action', () => {
    it('sets secretKey userInput', () => {
      const state = {
        inputFields: {
          url: initUserInput(''),
          description: initUserInput(''),
          secretKey: initUserInput(''),
          other: otherObject,
        },
        serverData: {},
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

    it('sets isDirty to true', () => {
      const { isDirty } = reduce(initialState, {
        type: EDIT_WEBHOOK_SET_SECRET_KEY,
        payload: 'bar',
      });
      expect(isDirty).toEqual(true);
    });
  });

  describe(EDIT_WEBHOOK_SAVE_REQUESTED + ' action', () => {
    it('sets updateMaskState to false', () => {
      const state = {
        updateMaskState: null,
        deleteMaskState: null,
        other: otherObject,
      };

      const { updateMaskState, other } = reduce(state, { type: EDIT_WEBHOOK_SAVE_REQUESTED });

      expect(updateMaskState).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_SAVE_FULFILLED + ' action', () => {
    it('resets saveError and sets updateMaskState to true', () => {
      const state = {
        updateMaskState: null,
        deleteMaskState: null,
        saveError: 'error',
        other: otherObject,
      };

      const { updateMaskState, saveError, other } = reduce(state, { type: EDIT_WEBHOOK_SAVE_FULFILLED });

      expect(updateMaskState).toBe(true);
      expect(saveError).toBe(null);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_SAVE_FAILED + ' action', () => {
    it('resets updateMaskState and sets saveError to payload', () => {
      const state = {
        updateMaskState: false,
        other: otherObject,
      };

      const { updateMaskState, saveError, other } = reduce(state, {
        type: EDIT_WEBHOOK_SAVE_FAILED,
        payload: 'save webhook error',
      });

      expect(updateMaskState).toBe(null);
      expect(saveError).toBe('save webhook error');
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE + ' action', () => {
    it('resets updateMaskState and deleteMaskState', () => {
      const state = {
        updateMaskState: true,
        deleteMaskState: true,
        other: otherObject,
      };

      const { updateMaskState, deleteMaskState, other } = reduce(state, { type: EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE });

      expect(updateMaskState).toBe(null);
      expect(deleteMaskState).toBe(null);
      expect(other).toBe(otherObject);
    });
  });

  describe(EDIT_WEBHOOK_DELETE_REQUESTED + ' action', () => {
    it('set deleteMaskState to false', () => {
      const state = {
        deleteMaskState: true,
      };

      const { deleteMaskState } = reduce(state, { type: EDIT_WEBHOOK_DELETE_REQUESTED });

      expect(deleteMaskState).toBe(false);
    });
  });

  describe(EDIT_WEBHOOK_DELETE_FULFILLED + ' action', () => {
    let newState;

    beforeAll(() => {
      const state = {
        deleteMaskState: null,
      };

      newState = reduce(state, { type: EDIT_WEBHOOK_DELETE_FULFILLED });
    });

    it('set deleteMaskState to true', () => {
      expect(newState.deleteMaskState).toBe(true);
    });
    it('set deleteError to null', () => {
      expect(newState.deleteError).toBe(null);
    });
  });
  describe(EDIT_WEBHOOK_DELETE_FAILED + ' action', () => {
    const errorMsg = 'failed to delete webhook';
    let newState;

    beforeAll(() => {
      const state = {
        deleteMaskState: null,
      };

      newState = reduce(state, { type: EDIT_WEBHOOK_DELETE_FAILED, payload: errorMsg });
    });

    it('set deleteMaskState to null', () => {
      expect(newState.deleteMaskState).toBe(null);
    });
    it('set deleteError to error message', () => {
      expect(newState.deleteError).toBe(errorMsg);
    });
  });

  describe('isDirty', () => {
    describe('is set to false', () => {
      it('when serverData is empty and all inputFields is empty', () => {
        const state = {
          inputFields: {
            url: initUserInput('http://foo'),
            description: initUserInput(''),
            secretKey: initUserInput(''),
          },
          serverData: {},
          other: otherObject,
        };

        const { isDirty } = reduce(state, {
          type: EDIT_WEBHOOK_SET_URL,
          payload: '',
        });
        expect(isDirty).toEqual(false);
      });
      it('when serverData is empty and selectedEventTypes is empty', () => {
        const state = {
          selectedEventTypes: ['bar'],
          inputFields: {
            url: initUserInput(''),
            description: initUserInput(''),
            secretKey: initUserInput(''),
          },
          serverData: {},
        };

        const { isDirty } = reduce(state, {
          type: EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
          payload: 'bar',
        });
        expect(isDirty).toEqual(false);
      });

      it('when inputFields is equals to serverData', () => {
        const state = {
          selectedEventTypes: ['bar'],
          inputFields: {
            url: initUserInput('http://bar'),
            description: initUserInput(''),
            secretKey: initUserInput(''),
          },
          serverData: {
            url: 'http://foo',
            description: '',
            secretKey: '',
            eventTypes: ['bar'],
          },
        };

        const { isDirty } = reduce(state, {
          type: EDIT_WEBHOOK_SET_URL,
          payload: 'http://foo',
        });
        expect(isDirty).toEqual(false);
      });
      it('when selectedEventTypes is equals to serverData.eventTypes', () => {
        const state = {
          selectedEventTypes: ['bar'],
          other: otherObject,
          inputFields: {
            url: initUserInput('http://foo'),
            description: initUserInput(''),
            secretKey: initUserInput(''),
          },
          serverData: {
            eventTypes: ['foo', 'bar'],
            url: 'http://foo',
            description: '',
            secretKey: '',
          },
        };

        const { isDirty } = reduce(state, {
          type: EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
          payload: 'foo',
        });
        expect(isDirty).toEqual(false);
      });
    });
    describe('is set to true', () => {
      it('when serverData is empty and some inputFields is not empty', () => {
        const state = {
          inputFields: {
            url: initUserInput(''),
            description: initUserInput(''),
            secretKey: initUserInput(''),
          },
          serverData: {},
          other: otherObject,
        };

        const { isDirty } = reduce(state, {
          type: EDIT_WEBHOOK_SET_URL,
          payload: 'http://foo',
        });
        expect(isDirty).toEqual(true);
      });
      it('when serverData is empty and some selectedEventTypes is not empty', () => {
        const state = {
          selectedEventTypes: [''],
          inputFields: {
            url: initUserInput(''),
            description: initUserInput(''),
            secretKey: initUserInput(''),
          },
          serverData: {},
        };

        const { isDirty } = reduce(state, {
          type: EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
          payload: 'foo',
        });
        expect(isDirty).toEqual(true);
      });
      it('when inputFields is different from serverData', () => {
        const state = {
          selectedEventTypes: ['bar'],
          inputFields: {
            url: initUserInput('http://foo'),
            description: initUserInput(''),
            secretKey: initUserInput(''),
          },
          serverData: {
            url: 'http://foo',
            description: '',
            secretKey: '',
            eventTypes: ['bar'],
          },
        };

        const { isDirty } = reduce(state, {
          type: EDIT_WEBHOOK_SET_URL,
          payload: 'http://bar',
        });
        expect(isDirty).toEqual(true);
      });
      it('when selectedEventTypes is different from serverData.eventTypes', () => {
        const state = {
          selectedEventTypes: ['bar'],
          other: otherObject,
          inputFields: {
            url: initUserInput('http://foo'),
            description: initUserInput(''),
            secretKey: initUserInput(''),
          },
          serverData: {
            eventTypes: ['bar'],
            url: 'http://foo',
            description: '',
            secretKey: '',
          },
        };

        const { isDirty } = reduce(state, {
          type: EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
          payload: 'foo',
        });
        expect(isDirty).toEqual(true);
      });
    });
  });
});
