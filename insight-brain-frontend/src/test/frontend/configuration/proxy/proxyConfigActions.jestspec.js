/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { save, del } from '../../../../main/frontend/configuration/proxy/proxyConfigActions';
import { getProxyConfigUrl } from '../../../../main/frontend/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('proxyConfigActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const proxyConfigUrl = getProxyConfigUrl();

  let store, state;

  beforeEach(() => {
    state = {
      proxyConfig: {
        formState: {
          hostname: {
            trimmedValue: 'foo',
          },
          username: {
            trimmedValue: 'user',
          },
          port: {
            trimmedValue: 42,
          },
          excludeHosts: {
            trimmedValue: '',
          },
          password: {
            value: 'secret',
          },
        },
      },
    };

    store = SpecUtil.mockReduxStore(state);
    jest.useFakeTimers();
  });

  afterEach(() => jest.useRealTimers());

  describe('save', () => {
    const serverData = {
      hostname: 'foo',
      username: 'user',
      port: 42,
      password: 'secret',
      passwordIsIncluded: true,
      excludeHosts: null,
    };

    afterEach(() => {
      expect(axios.put).toHaveBeenCalledWith(proxyConfigUrl, serverData);
    });

    it('immediately dispatches a PROXY_CONFIG_SAVE_REQUESTED action', () => {
      mockAxiosCalls({
        put: {
          [proxyConfigUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(save());

      const actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions).toHaveAction({
        type: 'PROXY_CONFIG_SAVE_REQUESTED',
        payload: undefined,
      });
    });

    describe('after successful PUT call', () => {
      beforeEach(() => {
        mockAxiosCalls({
          put: {
            [proxyConfigUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches PROXY_CONFIG_SAVE_FULFILLED', (done) => {
        store.dispatch(save()).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: 'PROXY_CONFIG_SAVE_REQUESTED' },
            { type: 'PROXY_CONFIG_SAVE_FULFILLED', payload: { ...serverData } },
          ]);

          done();
        });
      });

      it('dispatches PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE after timeout', (done) => {
        store.dispatch(save()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          const actions = store.getActions();

          expect(actions.length).toBe(3);
          expect(actions).toHaveActionsInOrder([
            { type: 'PROXY_CONFIG_SAVE_REQUESTED' },
            { type: 'PROXY_CONFIG_SAVE_FULFILLED', payload: { ...serverData } },
            { type: 'PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE' },
          ]);

          done();
        });
      });
    });

    describe('after failed PUT call', () => {
      beforeEach(() => {
        mockAxiosCalls({
          put: {
            [proxyConfigUrl]: () => Promise.reject('error!'),
          },
        });
      });

      it('dispatches PROXY_CONFIG_SAVE_FAILED action', (done) => {
        store.dispatch(save()).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: 'PROXY_CONFIG_SAVE_REQUESTED' },
            { type: 'PROXY_CONFIG_SAVE_FAILED', payload: 'error!' },
          ]);

          done();
        });
      });
    });
  });

  describe('del', () => {
    afterEach(() => {
      expect(axios.delete).toHaveBeenCalledWith(proxyConfigUrl);
    });

    it('immediately dispatches a PROXY_CONFIG_DELETE_REQUESTED action', () => {
      mockAxiosCalls({
        del: {
          [proxyConfigUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(del());

      const actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions).toHaveAction({
        type: 'PROXY_CONFIG_DELETE_REQUESTED',
        payload: undefined,
      });
    });

    describe('after successful DELETE call', () => {
      const noop = () => {};
      beforeEach(() => {
        mockAxiosCalls({
          del: {
            [proxyConfigUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches PROXY_CONFIG_DELETE_FULFILLED', (done) => {
        store.dispatch(del(noop)).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: 'PROXY_CONFIG_DELETE_REQUESTED' },
            { type: 'PROXY_CONFIG_DELETE_FULFILLED', payload: undefined },
          ]);

          done();
        });
      });

      it('dispatches PROXY_CONFIG_DELETE_MASK_TIMER_DONE after timeout', (done) => {
        store.dispatch(del(noop)).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          const actions = store.getActions();

          expect(actions.length).toBe(3);
          expect(actions).toHaveActionsInOrder([
            { type: 'PROXY_CONFIG_DELETE_REQUESTED' },
            { type: 'PROXY_CONFIG_DELETE_FULFILLED', payload: undefined },
            { type: 'PROXY_CONFIG_DELETE_MASK_TIMER_DONE' },
          ]);

          done();
        });
      });
    });

    describe('after failed DELETE call', () => {
      beforeEach(() => {
        mockAxiosCalls({
          del: {
            [proxyConfigUrl]: () => Promise.reject('error!'),
          },
        });
      });

      it('dispatches PROXY_CONFIG_DELETE_FAILED action', (done) => {
        store.dispatch(del()).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: 'PROXY_CONFIG_DELETE_REQUESTED' },
            { type: 'PROXY_CONFIG_DELETE_FAILED', payload: 'error!' },
          ]);

          done();
        });
      });
    });
  });
});
