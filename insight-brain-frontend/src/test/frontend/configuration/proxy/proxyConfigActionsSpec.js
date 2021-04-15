/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  save,
  del,
} from '../../../../main/frontend/configuration/proxy/proxyConfigActions';
import { getProxyConfigUrl } from '../../../../main/frontend/util/CLMLocation';

describe('proxyConfigActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
    proxyConfigUrl = getProxyConfigUrl();

  let store, state;

  beforeEach(function () {
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
  });

  describe('save', function () {
    const serverData = {
      hostname: 'foo',
      username: 'user',
      port: 42,
      password: 'secret',
      passwordIsIncluded: true,
      excludeHosts: null,
    };

    afterEach(function () {
      expect(axios.put).toHaveBeenCalledWith(proxyConfigUrl, serverData);
    });

    it('immediately dispatches a PROXY_CONFIG_SAVE_REQUESTED action', function () {
      mockAxiosCalls({
        put: {
          [proxyConfigUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(save());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('PROXY_CONFIG_SAVE_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful PUT call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          put: {
            [proxyConfigUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches PROXY_CONFIG_SAVE_FULFILLED', function (done) {
        store.dispatch(save()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('PROXY_CONFIG_SAVE_FULFILLED');
          expect(actions[1].payload).toEqual(serverData);
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE after timeout', function (done) {
        store.dispatch(save()).then(() => {
          setTimeout(function () {
            actions = store.getActions();
            expect(actions.length).toBe(3);
            expect(actions[2].type).toBe('PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE');

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
            [proxyConfigUrl]: Promise.reject('error!'),
          },
        });
      });

      it('dispatches PROXY_CONFIG_SAVE_FAILED action', function (done) {
        store.dispatch(save()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('PROXY_CONFIG_SAVE_FAILED');
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('del', function () {
    afterEach(function () {
      expect(axios.delete).toHaveBeenCalledWith(proxyConfigUrl);
    });

    it('immediately dispatches a PROXY_CONFIG_DELETE_REQUESTED action', function () {
      mockAxiosCalls({
        del: {
          [proxyConfigUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(del());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('PROXY_CONFIG_DELETE_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful DELETE call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          del: {
            [proxyConfigUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches PROXY_CONFIG_DELETE_FULFILLED', function (done) {
        store.dispatch(del()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('PROXY_CONFIG_DELETE_FULFILLED');
          expect(actions[1].payload).toBeUndefined();
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE after timeout', function (done) {
        store.dispatch(del()).then(() => {
          setTimeout(function () {
            actions = store.getActions();
            expect(actions.length).toBe(3);
            expect(actions[2].type).toBe('PROXY_CONFIG_SUBMIT_MASK_TIMER_DONE');

            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed DELETE call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          del: {
            [proxyConfigUrl]: Promise.reject('error!'),
          },
        });
      });

      it('dispatches PROXY_CONFIG_DELETE_FAILED action', function (done) {
        store.dispatch(del()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('PROXY_CONFIG_DELETE_FAILED');
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });
});
