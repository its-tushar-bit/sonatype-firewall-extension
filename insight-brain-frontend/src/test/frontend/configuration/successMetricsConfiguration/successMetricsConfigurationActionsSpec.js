/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  load,
  loadConfiguration,
  update,
  permissions,
  authErrorMessage,
} from '../../../../main/frontend/configuration/successMetricsConfiguration/successMetricsConfigurationActions';
import { getSuccessMetricsConfigUrl } from '../../../../main/frontend/util/CLMLocation';
import { getGlobalPermissionTestUrl } from '../../../../main/frontend/util/CLMContextLocation';

describe('successMetricsConfigurationActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const successMetricsConfigurationUrl = getSuccessMetricsConfigUrl();
  const permissionUrl = getGlobalPermissionTestUrl();

  describe('loadConfiguration', function () {
    let store, state;

    beforeEach(function () {
      state = {
        successMetricsConfiguration: {
          formState: {
            enabled: false,
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    it('immediately dispatches a SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [successMetricsConfigurationUrl]: Promise.resolve({
            data: { enabled: true },
          }),
        },
      });
      store.dispatch(loadConfiguration());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [successMetricsConfigurationUrl]: Promise.resolve({
              data: { enabled: true },
            }),
          },
        });

        store.dispatch(loadConfiguration()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED');
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe('SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED');
          expect(actions[1].payload).toEqual({ enabled: true });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches an SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [successMetricsConfigurationUrl]: Promise.reject({ status: 403 }),
          },
        });

        store.dispatch(loadConfiguration()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED');
          expect(actions[1].payload).toEqual('Error 403');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('load', () => {
    let store, state;

    beforeEach(function () {
      state = {
        successMetricsConfiguration: {
          formState: {
            enabled: false,
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    describe('after a successful PUT permission call', function () {
      it('dispatches an SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED action', function (done) {
        mockAxiosCalls({
          put: {
            [permissionUrl]: Promise.resolve({
              data: permissions,
            }),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(0);
      });
    });

    describe('after a failed PUT permission call', function () {
      it('dispatches an SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED action', function (done) {
        mockAxiosCalls({
          put: {
            [permissionUrl]: Promise.reject(authErrorMessage),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(1);
          expect(actions[0].type).toBe('SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED');
          expect(actions[0].payload).toEqual(authErrorMessage);
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(0);
      });
    });
  });

  describe('update', function () {
    let store, state;

    beforeEach(function () {
      state = {
        successMetricsConfiguration: {
          formState: {
            enabled: false,
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function () {
      expect(axios.put).toHaveBeenCalledWith(
        successMetricsConfigurationUrl,
        state.successMetricsConfiguration.formState
      );
    });

    it('immediately dispatches a SUCCESS_METRICS_CONFIGURATION_UPDATE_REQUESTED action', function () {
      mockAxiosCalls({
        put: {
          [successMetricsConfigurationUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(update());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SUCCESS_METRICS_CONFIGURATION_UPDATE_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful PUT call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          put: {
            [successMetricsConfigurationUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED', function (done) {
        store.dispatch(update()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED');
          expect(actions[1].payload).toBeUndefined();
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE after timeout', function (done) {
        store.dispatch(update()).then(() => {
          setTimeout(function () {
            actions = store.getActions();
            expect(actions.length).toBe(3);
            expect(actions[2].type).toBe('SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE');

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
            [successMetricsConfigurationUrl]: Promise.reject({ status: 403 }),
          },
        });
      });

      it('dispatches SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED action', function (done) {
        store.dispatch(update()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED');
          expect(actions[1].payload).toEqual('Error 403');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });
});
