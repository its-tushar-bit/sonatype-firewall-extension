/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Mock the authorizationUtil module before importing actions
jest.mock('../../../../main/frontend/util/authorizationUtil', () => ({
  checkPermissions: jest.fn(),
}));

import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  loadSystemNotice,
  update,
  load,
} from '../../../../main/frontend/configuration/systemNoticeConfiguration/systemNoticeConfigurationActions';
import { checkPermissions } from '../../../../main/frontend/util/authorizationUtil';
import { getSystemNoticeFetchUrl, getSystemNoticeUrl } from '../../../../main/frontend/util/CLMLocation';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

describe('systemNoticeConfigurationActions', function () {
  let axiosMock;
  const systemNoticeFetchUrl = getSystemNoticeFetchUrl();
  const systemNoticeUpdateUrl = getSystemNoticeUrl();

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
    checkPermissions.mockClear();
  });

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
        axiosMock.onGet(systemNoticeFetchUrl).reply(200, { enabled: true, message: 'some message' });

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
        axiosMock.onGet(systemNoticeFetchUrl).reply(403);

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
    let store, state;

    beforeEach(() => {
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
        checkPermissions.mockReturnValue(Promise.resolve());
      });

      it('fires an SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED action', (done) => {
        axiosMock.onGet(systemNoticeFetchUrl).reply(200, { enabled: true, message: 'some message' });

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
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
        checkPermissions.mockImplementation(() => Promise.reject('system notice page authorization error'));

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(2);

          expect(actions[0].type).toBe('SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED');
          expect(actions[1].type).toBe('SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED');
          expect(actions[1].payload).toBe('system notice page authorization error');

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
      expect(axiosMock.history.put.length).toBe(1);
      expect(axiosMock.history.put[0].url).toBe(systemNoticeUpdateUrl);
      expect(JSON.parse(axiosMock.history.put[0].data)).toEqual({
        enabled: formState.enabled,
        message: formState.message.value,
      });
    });

    describe('after successful PUT call', function () {
      beforeEach(function () {
        axiosMock.onPut(systemNoticeUpdateUrl).reply(200, {});
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
        jest.useFakeTimers();

        store.dispatch(update()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe('SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE');

          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed PUT call', function () {
      beforeEach(function () {
        axiosMock.onPut(systemNoticeUpdateUrl).reply(403);
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
