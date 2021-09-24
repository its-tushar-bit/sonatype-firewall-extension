/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  loadSystemNotice,
  update,
} from '../../../../main/frontend/configuration/systemNoticeConfiguration/systemNoticeConfigurationActions';
import { getSystemNoticeFetchUrl, getSystemNoticeUrl } from '../../../../main/frontend/util/CLMLocation';

describe('systemNoticeConfigurationActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const systemNoticeFetchUrl = getSystemNoticeFetchUrl();
  const systemNoticeUpdateUrl = getSystemNoticeUrl();

  describe('loadSystemNotice', function () {
    let store, state;

    beforeEach(function () {
      state = {
        systemNoticeConfiguration: {
          formState: {
            enabled: false,
            message: {
              value: '',
              trimmedValue: '',
              isPristine: true,
            },
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    describe('after a successful GET call', function () {
      it('dispatches SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [systemNoticeFetchUrl]: Promise.resolve({
              data: { enabled: true, message: 'some message' },
            }),
          },
        });

        store.dispatch(loadSystemNotice()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED');
          expect(actions[1].payload).toEqual({ enabled: true, message: 'some message' });
          done();
        });

        let actions = store.getActions();

        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED');
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [systemNoticeFetchUrl]: () => Promise.reject({ status: 403 }),
          },
        });

        store.dispatch(loadSystemNotice()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED');
          expect(actions[1].payload).toEqual('Error 403');
          done();
        });

        let actions = store.getActions();

        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED');
      });
    });
  });

  describe('load', () => {
    let checkPermissionsSpy, load, store, state;

    beforeEach(() => {
      checkPermissionsSpy = jasmine.createSpy('checkPermissions');
      const module = require('inject-loader!../../../../main/frontend/configuration/systemNoticeConfiguration/systemNoticeConfigurationActions')(
        {
          '../../util/authorizationUtil': {
            checkPermissions: checkPermissionsSpy,
          },
        }
      );

      load = module.load;

      state = {
        systemNoticeConfiguration: {
          formState: {
            enabled: false,
            message: {
              value: '',
              trimmedValue: '',
              isPristine: true,
            },
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fires an SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED action', (done) => {
        mockAxiosCalls({
          get: {
            [systemNoticeFetchUrl]: Promise.resolve({
              data: { enabled: true, message: 'some message' },
            }),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe('SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED');
      });
    });

    describe('when not authorized', () => {
      it('fires an SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED action', (done) => {
        checkPermissionsSpy.and.callFake(() => Promise.reject('system notice page authorization error'));
        const store = SpecUtil.mockReduxStore();

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(2);

          expect(actions).toHaveActionsInOrder([
            { type: 'SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED' },
            { type: 'SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED', payload: 'system notice page authorization error' },
          ]);

          done();
        });
      });
    });
  });

  describe('update', function () {
    let store, state;

    beforeEach(function () {
      state = {
        systemNoticeConfiguration: {
          formState: {
            enabled: false,
            message: 'some',
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function () {
      const { formState } = state.systemNoticeConfiguration;
      expect(axios.put).toHaveBeenCalledWith(systemNoticeUpdateUrl, {
        enabled: formState.enabled,
        message: formState.message.value,
      });
    });

    describe('after successful PUT call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          put: {
            [systemNoticeUpdateUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches SYSTEM_NOTICE_CONFIGURATION_UPDATE_FULFILLED', function (done) {
        store.dispatch(update()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_FULFILLED');
          done();
        });

        let actions = store.getActions();

        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_REQUESTED');
      });

      it('dispatches SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE after timeout', function (done) {
        store.dispatch(update()).then(() => {
          setTimeout(function () {
            actions = store.getActions();
            expect(actions.length).toBe(3);
            expect(actions[2].type).toBe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE');

            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed PUT call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          put: {
            [systemNoticeUpdateUrl]: () => Promise.reject({ status: 403 }),
          },
        });
      });

      it('dispatches SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED action', function (done) {
        store.dispatch(update()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED');
          expect(actions[1].payload).toEqual('Error 403');
          done();
        });

        let actions = store.getActions();

        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_REQUESTED');
      });
    });
  });
});
