/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  loadConfiguration,
  update,
} from '../../../../main/frontend/configuration/successMetricsConfiguration/successMetricsConfigurationActions';
import { getSuccessMetricsConfigUrl } from '../../../../main/frontend/util/CLMLocation';

describe('successMetricsConfigurationActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const successMetricsConfigurationUrl = getSuccessMetricsConfigUrl();

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
            [successMetricsConfigurationUrl]: () => Promise.reject({ status: 403 }),
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
    let checkPermissionsSpy, load, store, state;

    beforeEach(() => {
      checkPermissionsSpy = jasmine.createSpy('checkPermissions');
      const module = require('inject-loader!../../../../main/frontend/configuration/successMetricsConfiguration/successMetricsConfigurationActions')(
        {
          '../../util/authorizationUtil': {
            checkPermissions: checkPermissionsSpy,
          },
        }
      );

      load = module.load;

      state = {
        successMetricsConfiguration: {
          formState: {
            enabled: false,
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fires an SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED action', (done) => {
        mockAxiosCalls({
          get: {
            [successMetricsConfigurationUrl]: Promise.resolve({
              data: { enabled: true },
            }),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe('SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED');
      });
    });

    describe('when not authorized', () => {
      it('fires an SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED action', (done) => {
        checkPermissionsSpy.and.callFake(() => Promise.reject('success metrics config page authorization error'));

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(2);

          expect(actions).toHaveActionsInOrder([
            { type: 'SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED' },
            {
              type: 'SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED',
              payload: 'success metrics config page authorization error',
            },
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
            [successMetricsConfigurationUrl]: () => Promise.reject({ status: 403 }),
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
