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
} from '../../../../../main/frontend/util/CLMLocation';
import {
  EDIT_WEBHOOK_LOAD_REQUESTED,
  EDIT_WEBHOOK_LOAD_FULFILLED,
  EDIT_WEBHOOK_LOAD_FAILED,
  EDIT_WEBHOOK_SAVE_REQUESTED,
  EDIT_WEBHOOK_SAVE_FULFILLED,
  EDIT_WEBHOOK_SAVE_FAILED,
  EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE,
} from '../../../../../main/frontend/configuration/webhook/editWebhook/webhooksActions';
import { STATE_GO } from '../../../../../main/frontend/reduxUiRouter/routerActions';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('webhooksActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let loadWebhookData, saveWebhook, checkPermissionsSpy;

  beforeEach(function () {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const module = require('inject-loader!../../../../../main/frontend/configuration/webhook/editWebhook/webhooksActions')(
      {
        '../../../util/authorizationUtil': {
          checkPermissions: checkPermissionsSpy,
        },
      }
    );
    loadWebhookData = module.loadWebhookData;
    saveWebhook = module.saveWebhook;
  });

  describe('loadWebhookData', () => {
    describe('when authorised', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fires EDIT_WEBHOOK_LOAD_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [getWebhookEventTypesUrl()]: Promise.resolve({ data: ['eventType1', 'eventType2'] }),
            [getProductFeaturesUrl()]: Promise.resolve({ data: ['feature1', 'feature2'] }),
          },
        });

        store.dispatch(loadWebhookData()).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: EDIT_WEBHOOK_LOAD_FULFILLED,
            payload: {
              eventTypes: ['eventType1', 'eventType2'],
              productFeatures: ['feature1', 'feature2'],
            },
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });

      it('fires EDIT_WEBHOOK_LOAD_FAILED action on error', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [getWebhookEventTypesUrl()]: Promise.reject({ response: 'failed to get event types' }),
            [getProductFeaturesUrl()]: Promise.resolve({ data: ['feature1', 'feature2'] }),
          },
        });

        store.dispatch(loadWebhookData()).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: EDIT_WEBHOOK_LOAD_FAILED,
            payload: 'failed to get event types',
          });
          done();
        });

        expect(store.getActions()[0]).toEqual({ type: EDIT_WEBHOOK_LOAD_REQUESTED });
      });
    });

    describe('when not authorised', () => {
      it('does not load webhook data and fires EDIT_WEBHOOK_LOAD_FAILED action with authorisation error', (done) => {
        checkPermissionsSpy.and.returnValue(Promise.reject('webhook authorisation error'));
        const store = SpecUtil.mockReduxStore();

        mockAxiosCalls({ get: {} });

        store.dispatch(loadWebhookData()).then(() => {
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
      };

      store = SpecUtil.mockReduxStore({ editWebhook: editWebhookState });
    });

    it('fires EDIT_WEBHOOK_SAVE_FULFILLED, EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE and STATE_GO actions on success', (done) => {
      mockAxiosCalls({
        post: {
          [getWebhooksUrl()]: Promise.resolve({ data: 'success' }),
        },
      });

      store.dispatch(saveWebhook()).then(() => {
        setTimeout(function () {
          expect(store.getActions().length).toBe(4);
          expect(store.getActions()[1]).toEqual({ type: EDIT_WEBHOOK_SAVE_FULFILLED });
          expect(store.getActions()[2]).toEqual({ type: EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE });
          expect(store.getActions()[3]).toEqual({
            type: STATE_GO,
            payload: {
              to: 'webhooks.list',
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
          [getWebhooksUrl()]: Promise.reject({ response: 'failed to save webhook' }),
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
});
