/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
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
  deleteWebhook,
} from '../../../../main/frontend/configuration/webhook/webhookActions';
import { STATE_GO } from '../../../../main/frontend/reduxUiRouter/routerActions';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('webhookActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let loadAddWebhookPage, loadEditWebhookPage, loadWebhookListPage, saveWebhook, checkPermissionsSpy;

  beforeEach(function () {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const module = require('inject-loader!../../../../main/frontend/configuration/webhook/webhookActions')({
      '../../util/authorizationUtil': {
        checkPermissions: checkPermissionsSpy,
      },
    });
    loadAddWebhookPage = module.loadAddWebhookPage;
    loadEditWebhookPage = module.loadEditWebhookPage;
    loadWebhookListPage = module.loadWebhookListPage;
    saveWebhook = module.saveWebhook;
  });

  describe('loadEditWebhookPage', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fetches eventTypes, features, webhooks and fires EDIT_WEBHOOK_LOAD_EDIT_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [getWebhookEventTypesUrl()]: Promise.resolve({ data: ['eventType1', 'eventType2'] }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['webhooks-for-applications', 'webhooks-for-repositories'],
            }),
            [getWebhooksUrl()]: Promise.resolve({
              data: [
                {
                  id: '1',
                  url: 'http://yetanother.com',
                },
                {
                  id: '2',
                  url: 'http://yetanother2.com',
                },
              ],
            }),
          },
        });

        store.dispatch(loadEditWebhookPage('1')).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(5);

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
          expect(actions[4]).toEqual({
            type: EDIT_WEBHOOK_LOAD_EDIT_FULFILLED,
            payload: { id: '1', url: 'http://yetanother.com' },
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('fires EDIT_WEBHOOK_LOAD_FAILED action if no webhook with predefined id exists', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [getWebhookEventTypesUrl()]: Promise.resolve({ data: ['eventType1', 'eventType2'] }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['webhooks-for-applications', 'webhooks-for-repositories'],
            }),
            [getWebhooksUrl()]: Promise.resolve({
              data: [
                {
                  id: '1',
                  url: 'http://yetanother.com',
                },
                {
                  id: '2',
                  url: 'http://yetanother2.com',
                },
              ],
            }),
          },
        });

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
        checkPermissionsSpy.and.callFake(() => Promise.reject('webhook authorization error'));
        const store = SpecUtil.mockReduxStore();

        mockAxiosCalls({ get: {} });

        store.dispatch(loadEditWebhookPage()).then(() => {
          expect(axios.get).not.toHaveBeenCalled();
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
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fetches eventTypes and features and fires EDIT_WEBHOOK_LOAD_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [getWebhookEventTypesUrl()]: Promise.resolve({ data: ['eventType1', 'eventType2'] }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['webhooks-for-applications', 'webhooks-for-repositories'],
            }),
          },
        });

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
        mockAxiosCalls({
          get: {
            [getWebhookEventTypesUrl()]: Promise.resolve({ data: ['eventType1', 'eventType2'] }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['webhooks-for-repositories'],
            }),
          },
        });

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
        mockAxiosCalls({
          get: {
            [getWebhookEventTypesUrl()]: () => Promise.reject({ response: 'failed to get event types' }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['webhooks-for-applications', 'webhooks-for-repositories'],
            }),
          },
        });

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
        mockAxiosCalls({
          get: {
            [getWebhookEventTypesUrl()]: Promise.resolve({ data: ['eventType1', 'eventType2'] }),
            [getProductFeaturesUrl()]: () => Promise.reject({ response: 'failed to get product features' }),
          },
        });

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
        mockAxiosCalls({
          get: {
            [getWebhookEventTypesUrl()]: Promise.resolve({ data: ['eventType1', 'eventType2'] }),
            [getProductFeaturesUrl()]: Promise.resolve({ data: [] }),
          },
        });

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
        checkPermissionsSpy.and.callFake(() => Promise.reject('webhook authorisation error'));
        const store = SpecUtil.mockReduxStore();

        mockAxiosCalls({ get: {} });

        store.dispatch(loadAddWebhookPage()).then(() => {
          expect(axios.get).not.toHaveBeenCalled();
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
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fetches webhooks and features and fires EDIT_WEBHOOK_LOAD_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [getWebhooksUrl()]: Promise.resolve({ data: ['webhook1', 'webhook2'] }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['webhooks-for-applications', 'webhooks-for-repositories'],
            }),
          },
        });

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
        mockAxiosCalls({
          get: {
            [getWebhooksUrl()]: Promise.resolve({ data: ['webhook1', 'webhook2'] }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['webhooks-for-repositories'],
            }),
          },
        });

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
        mockAxiosCalls({
          get: {
            [getWebhooksUrl()]: () => Promise.reject({ response: 'failed to get webhooks' }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['webhooks-for-applications', 'webhooks-for-repositories'],
            }),
          },
        });

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
        mockAxiosCalls({
          get: {
            [getWebhooksUrl()]: Promise.resolve({ data: ['webhook1', 'webhook2'] }),
            [getProductFeaturesUrl()]: () => Promise.reject({ response: 'failed to get product features' }),
          },
        });

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
        mockAxiosCalls({
          get: {
            [getWebhooksUrl()]: Promise.resolve({ data: ['webhook1', 'webhook2'] }),
            [getProductFeaturesUrl()]: Promise.resolve({ data: [] }),
          },
        });

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
        checkPermissionsSpy.and.callFake(() => Promise.reject('webhook authorisation error'));
        const store = SpecUtil.mockReduxStore();

        mockAxiosCalls({ get: {} });

        store.dispatch(loadWebhookListPage()).then(() => {
          expect(axios.get).not.toHaveBeenCalled();
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
      };

      store = SpecUtil.mockReduxStore({ webhooks: editWebhookState });
    });

    it('fires EDIT_WEBHOOK_SAVE_FULFILLED, EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE and STATE_GO actions on success', (done) => {
      mockAxiosCalls({
        post: {
          [getWebhooksUrl()]: Promise.resolve({ data: 'success' }),
        },
      });

      store.dispatch(saveWebhook()).then(() => {
        setTimeout(function () {
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
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_SAVE_REQUESTED });
    });

    it('fires EDIT_WEBHOOK_SAVE_FAILED action on error', (done) => {
      mockAxiosCalls({
        post: {
          [getWebhooksUrl()]: () => Promise.reject({ response: 'failed to save webhook' }),
        },
      });

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
      mockAxiosCalls({
        del: {
          [deleteWebhooksUrl('404')]: Promise.resolve({ data: 'success' }),
        },
      });

      store.dispatch(deleteWebhook('404')).then(() => {
        setTimeout(function () {
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
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires EDIT_WEBHOOK_DELETE_FAILED action on error', (done) => {
      mockAxiosCalls({
        del: {
          [deleteWebhooksUrl('404')]: () => Promise.reject({ response: 'failed to delete webhook' }),
        },
      });

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
