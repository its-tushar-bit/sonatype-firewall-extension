/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Mock the authorizationUtil module before importing webhook actions
jest.mock('../../../../main/frontend/util/authorizationUtil', () => ({
  checkPermissions: jest.fn(),
}));

import axios from 'axios';
import '../../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { checkPermissions } from '../../../../main/frontend/util/authorizationUtil';
import {
  getWebhookEventTypesUrl,
  getProductFeaturesUrl,
  getWebhooksUrl,
  deleteWebhooksUrl,
} from '../../../../main/frontend/util/CLMLocation';
import {
  EDIT_WEBHOOK_LOAD_REQUESTED,
  EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED,
  EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED,
  EDIT_WEBHOOK_LOAD_FULFILLED,
  EDIT_WEBHOOK_LOAD_EDIT_FULFILLED,
  EDIT_WEBHOOK_LOAD_FAILED,
  EDIT_WEBHOOK_SAVE_REQUESTED,
  EDIT_WEBHOOK_SAVE_FULFILLED,
  EDIT_WEBHOOK_SAVE_FAILED,
  EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE,
  EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED,
  EDIT_WEBHOOK_DELETE_FULFILLED,
  EDIT_WEBHOOK_DELETE_REQUESTED,
  EDIT_WEBHOOK_DELETE_FAILED,
  EDIT_WEBHOOK_SET_IS_URL_HTTP,
  deleteWebhook,
  loadAddWebhookPage,
  loadEditWebhookPage,
  loadWebhookListPage,
  saveWebhook,
} from '../../../../main/frontend/configuration/webhook/webhookActions';
import { STATE_GO } from '../../../../main/frontend/reduxUiRouter/routerActions';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('webhookActions', () => {
  let axiosMock;
  let axiosGetSpy;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });
  beforeEach(function () {
    // Clear all mocks before each test
    jest.clearAllMocks();
    checkPermissions.mockClear();
    // Spy on axios.get to verify it's not called in unauthorized scenarios
    axiosGetSpy = jest.spyOn(axios, 'get');

    // Mock window.location and document.title to ensure Lifecycle context for all tests
    delete window.location;
    window.location = { pathname: '/admin/configuration/webhooks', hash: '' };
    Object.defineProperty(document, 'title', {
      writable: true,
      value: 'Webhooks - Nexus IQ Server'
    });
  });

  describe('loadEditWebhookPage', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissions.mockReturnValue(Promise.resolve());
      });

      it('fetches eventTypes, features, webhooks and fires EDIT_WEBHOOK_LOAD_EDIT_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter
        axiosMock.onGet(getWebhookEventTypesUrl('lifecycle')).reply(200, ['eventType1', 'eventType2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['webhooks-for-applications', 'webhooks-for-repositories']);
        axiosMock.onGet(getWebhooksUrl() + '?context=lifecycle').reply(200, [
          {
            id: '1',
            url: 'http://yetanother.com',
          },
          {
            id: '2',
            url: 'http://yetanother2.com',
          },
        ]);

        store.dispatch(loadEditWebhookPage('1')).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(6);
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED,
            payload: ['eventType1', 'eventType2'],
          });
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED,
            payload: [
              { id: '1', url: 'http://yetanother.com' },
              { id: '2', url: 'http://yetanother2.com' },
            ],
          });
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED,
            payload: true,
          });
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_SET_IS_URL_HTTP,
            payload: true,
          });
          expect(actions[actions.length - 1]).toEqual({
            type: EDIT_WEBHOOK_LOAD_EDIT_FULFILLED,
            payload: { id: '1', url: 'http://yetanother.com' },
          });
          done();
        });
        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('fires EDIT_WEBHOOK_LOAD_FAILED action if no webhook with predefined id exists', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter
        axiosMock.onGet(getWebhookEventTypesUrl('lifecycle')).reply(200, ['eventType1', 'eventType2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['webhooks-for-applications', 'webhooks-for-repositories']);
        axiosMock.onGet(getWebhooksUrl() + '?context=lifecycle').reply(200, [
          {
            id: '1',
            url: 'http://yetanother.com',
          },
          {
            id: '2',
            url: 'http://yetanother2.com',
          },
        ]);

        store.dispatch(loadEditWebhookPage('404')).then(() => {
          const actions = store.getActions();
          const lastAction = actions[actions.length - 1];

          expect(actions.length).toBe(5);

          expect(lastAction).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'Unable to locate webhook',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });
    });

    describe('when not authorized', () => {
      it('does not load webhook data and fires EDIT_WEBHOOK_LOAD_FAILED action with authorization error', (done) => {
        checkPermissions.mockImplementation(() => Promise.reject('webhook authorization error'));
        const store = SpecUtil.mockReduxStore();

        store.dispatch(loadEditWebhookPage()).then(() => {
          expect(axiosGetSpy).not.toHaveBeenCalled();
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'webhook authorization error',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });
    });
  });

  describe('loadAddWebhookPage', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissions.mockReturnValue(Promise.resolve());
      });

      it('fetches eventTypes and features and fires EDIT_WEBHOOK_LOAD_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter
        axiosMock.onGet(getWebhookEventTypesUrl('lifecycle')).reply(200, ['eventType1', 'eventType2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['webhooks-for-applications', 'webhooks-for-repositories']);

        store.dispatch(loadAddWebhookPage()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(4);
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED,
            payload: ['eventType1', 'eventType2'],
          });
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED,
            payload: true,
          });
          expect(actions[3]).toEqual({
            type: EDIT_WEBHOOK_LOAD_FULFILLED,
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('sets isAppWebhooksSupported to false if license does not support application webhooks', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock firewall context by setting window.location.hash
        delete window.location;
        window.location = { hash: '#/firewall/webhooks' };

        // Mock axios calls using axiosMockAdapter
        axiosMock.onGet(getWebhookEventTypesUrl('firewall')).reply(200, ['eventType1', 'eventType2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['webhooks-for-repositories']);

        store.dispatch(loadAddWebhookPage()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(4);
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED,
            payload: ['eventType1', 'eventType2'],
          });
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED,
            payload: false,
          });
          expect(actions[3]).toEqual({
            type: EDIT_WEBHOOK_LOAD_FULFILLED,
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('fires EDIT_WEBHOOK_LOAD_FAILED action on EventTypes fetch error', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter
        axiosMock.onGet(getWebhookEventTypesUrl('lifecycle')).reply(500, { message: 'failed to get event types' });
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['webhooks-for-applications', 'webhooks-for-repositories']);

        store.dispatch(loadAddWebhookPage()).then(() => {
          const actions = store.getActions(),
            lastAction = actions[actions.length - 1];
          expect(lastAction).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'failed to get event types',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('fires EDIT_WEBHOOK_LOAD_FAILED action on ProductFeatures fetch error', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter - this will not be called since product features fails first
        axiosMock.onGet(getWebhookEventTypesUrl('lifecycle')).reply(200, ['eventType1', 'eventType2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(500, { message: 'failed to get product features' });

        store.dispatch(loadAddWebhookPage()).then(() => {
          const actions = store.getActions(),
            lastAction = actions[actions.length - 1];
          expect(lastAction).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'failed to get product features',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('fires EDIT_WEBHOOK_LOAD_FAILED action when does not have expected product features', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter - eventTypes will not be called since product features check fails
        axiosMock.onGet(getWebhookEventTypesUrl('lifecycle')).reply(200, ['eventType1', 'eventType2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);

        store.dispatch(loadAddWebhookPage()).then(() => {
          const actions = store.getActions(),
            lastAction = actions[actions.length - 1];
          expect(lastAction).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'Webhooks feature is not supported by your license.',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });
    });

    describe('when not authorised', () => {
      it('does not load webhook data and fires EDIT_WEBHOOK_LOAD_FAILED action with authorisation error', (done) => {
        checkPermissions.mockImplementation(() => Promise.reject('webhook authorisation error'));
        const store = SpecUtil.mockReduxStore();

        store.dispatch(loadAddWebhookPage()).then(() => {
          expect(axiosGetSpy).not.toHaveBeenCalled();
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'webhook authorisation error',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });
    });
  });

  describe('loadWebhookListPage', () => {
    describe('when authorised', () => {
      beforeEach(() => {
        checkPermissions.mockReturnValue(Promise.resolve());
      });

      it('fetches webhooks and features and fires EDIT_WEBHOOK_LOAD_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter
        axiosMock.onGet(getWebhooksUrl() + '?context=lifecycle').reply(200, ['webhook1', 'webhook2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['webhooks-for-applications', 'webhooks-for-repositories']);

        store.dispatch(loadWebhookListPage()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(4);
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED,
            payload: ['webhook1', 'webhook2'],
          });
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED,
            payload: true,
          });
          expect(actions[3]).toEqual({
            type: EDIT_WEBHOOK_LOAD_FULFILLED,
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('sets isAppWebhooksSupported to false if license does not support application webhooks', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock firewall context by setting window.location.hash
        delete window.location;
        window.location = { hash: '#/firewall/webhooks' };

        // Mock axios calls using axiosMockAdapter
        axiosMock.onGet(getWebhooksUrl() + '?context=firewall').reply(200, ['webhook1', 'webhook2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['webhooks-for-repositories']);

        store.dispatch(loadWebhookListPage()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(4);
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED,
            payload: ['webhook1', 'webhook2'],
          });
          expect(actions).toHaveAction({
            type: EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED,
            payload: false,
          });
          expect(actions[3]).toEqual({
            type: EDIT_WEBHOOK_LOAD_FULFILLED,
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('fires EDIT_WEBHOOK_LOAD_FAILED action on webhooks fetch error', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter
        axiosMock.onGet(getWebhooksUrl() + '?context=lifecycle').reply(500, { message: 'failed to get webhooks' });
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['webhooks-for-applications', 'webhooks-for-repositories']);

        store.dispatch(loadWebhookListPage()).then(() => {
          const actions = store.getActions(),
            lastAction = actions[actions.length - 1];
          expect(lastAction).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'failed to get webhooks',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('fires EDIT_WEBHOOK_LOAD_FAILED action on ProductFeatures fetch error', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter - webhooks will not be called since product features fails first
        axiosMock.onGet(getWebhooksUrl() + '?context=lifecycle').reply(200, ['webhook1', 'webhook2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(500, { message: 'failed to get product features' });

        store.dispatch(loadWebhookListPage()).then(() => {
          const actions = store.getActions(),
            lastAction = actions[actions.length - 1];
          expect(lastAction).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'failed to get product features',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('fires EDIT_WEBHOOK_LOAD_FAILED action when does not have expected product features', (done) => {
        const store = SpecUtil.mockReduxStore();

        // Mock axios calls using axiosMockAdapter - webhooks will not be called since product features check fails
        axiosMock.onGet(getWebhooksUrl() + '?context=lifecycle').reply(200, ['webhook1', 'webhook2']);
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);

        store.dispatch(loadWebhookListPage()).then(() => {
          const actions = store.getActions(),
            lastAction = actions[actions.length - 1];
          expect(lastAction).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'Webhooks feature is not supported by your license.',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });
    });

    describe('when not authorised', () => {
      it('does not load webhook data and fires EDIT_WEBHOOK_LOAD_FAILED action with authorisation error', (done) => {
        checkPermissions.mockImplementation(() => Promise.reject('webhook authorisation error'));
        const store = SpecUtil.mockReduxStore();

        store.dispatch(loadWebhookListPage()).then(() => {
          expect(axiosGetSpy).not.toHaveBeenCalled();
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'webhook authorisation error',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });
    });
  });

  describe('saveWebhook', () => {
    let store;
    beforeEach(() => {
      const editWebhookState = {
        selectedEventTypes: ['foo', 'bar'],
        inputFields: {
          url: initUserInput('http://test'),
          description: initUserInput('test webhook'),
          secretKey: initUserInput('test-webhook-key'),
        },
        serverData: {},
        isAppWebhooksSupported: true, // Set to true for Lifecycle context
      };

      store = SpecUtil.mockReduxStore({ webhooks: editWebhookState });
    });

    it('fires EDIT_WEBHOOK_SAVE_FULFILLED, EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE and STATE_GO actions on success', (done) => {
      // Mock axios calls using axiosMockAdapter with context parameter
      axiosMock.onPost(getWebhooksUrl() + '?context=lifecycle').reply(200, 'success');
      jest.useFakeTimers();

      store.dispatch(saveWebhook()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        expect(store.getActions().length).toBe(4);
        expect(store.getActions()[1]).toEqual({ type: EDIT_WEBHOOK_SAVE_FULFILLED });
        expect(store.getActions()[2]).toEqual({ type: EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE });
        expect(store.getActions()[3]).toEqual({
          type: STATE_GO,
          payload: {
            to: 'listWebhooks',
            params: undefined,
            options: undefined,
          },
        });
        done();
      });

      expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_SAVE_REQUESTED });
    });

    it('fires EDIT_WEBHOOK_SAVE_FAILED action on error', (done) => {
      // Mock axios calls using axiosMockAdapter with context parameter
      axiosMock.onPost(getWebhooksUrl() + '?context=lifecycle').reply(500, { message: 'failed to save webhook' });

      store.dispatch(saveWebhook()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1]).toEqual({
          type: EDIT_WEBHOOK_SAVE_FAILED,
          payload: 'failed to save webhook',
        });

        done();
      });

      expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_SAVE_REQUESTED });
    });
  });

  describe('deleteWebhook', () => {
    let store;
    beforeEach(() => {
      const editWebhookState = {
        selectedEventTypes: ['foo', 'bar'],
        inputFields: {
          url: initUserInput('http://test'),
          description: initUserInput('test'),
          secretKey: initUserInput('testKey'),
        },
        serverData: {},
      };

      store = SpecUtil.mockReduxStore({ webhooks: editWebhookState });
    });

    it('fires EDIT_WEBHOOK_DELETE_REQUESTED, EDIT_WEBHOOK_DELETE_FULFILLED, EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE and STATE_GO actions on success', (done) => {
      // Mock axios calls using axiosMockAdapter
      axiosMock.onDelete(deleteWebhooksUrl('404')).reply(200, 'success');
      jest.useFakeTimers();

      store.dispatch(deleteWebhook('404')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        expect(store.getActions()).toHaveActionsInOrder([
          { type: EDIT_WEBHOOK_DELETE_REQUESTED },
          { type: EDIT_WEBHOOK_DELETE_FULFILLED },
          { type: EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE },
          {
            type: STATE_GO,
            payload: {
              to: 'listWebhooks',
              params: undefined,
              options: undefined,
            },
          },
        ]);

        done();
      });
    });

    it('fires EDIT_WEBHOOK_DELETE_FAILED action on error', (done) => {
      // Mock axios calls using axiosMockAdapter
      axiosMock.onDelete(deleteWebhooksUrl('404')).reply(500, { message: 'failed to delete webhook' });

      store.dispatch(deleteWebhook('404')).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          { type: EDIT_WEBHOOK_DELETE_REQUESTED },
          {
            type: EDIT_WEBHOOK_DELETE_FAILED,
            payload: 'failed to delete webhook',
          },
        ]);
        done();
      });
    });
  });
});
