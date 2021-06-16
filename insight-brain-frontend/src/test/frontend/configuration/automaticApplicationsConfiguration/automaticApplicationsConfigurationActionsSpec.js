/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import axios from 'axios';
import {
  AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED,
  AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED,
  AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED,
} from '../../../../main/frontend/configuration/automaticApplicationsConfiguration/automaticApplicationsConfigurationActions';
import {
  getAutomaticApplicationsConfigurationUrl,
  getOrganizationsUrl,
} from '../../../../main/frontend/util/CLMLocation';

describe('AutomaticApplicationConfigurationActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const OrganizationsUrl = getOrganizationsUrl();
  const AutomaticApplicationsConfigurationUrl = getAutomaticApplicationsConfigurationUrl();
  let checkPermissionsSpy, load, update;

  beforeEach(() => {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const actionsModule = require('inject-loader!../../../../main/frontend/configuration/automaticApplicationsConfiguration/automaticApplicationsConfigurationActions')(
      {
        '../../util/authorizationUtil': {
          checkPermissions: checkPermissionsSpy,
        },
      }
    );
    load = actionsModule.load;
    update = actionsModule.update;
  });

  describe('load', function () {
    let store;

    beforeEach(() => {
      checkPermissionsSpy.and.returnValue(Promise.resolve());
      store = SpecUtil.mockReduxStore();
    });

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED', function (done) {
      store.dispatch(load()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED,
        });
        done();
      });
    });
    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED on permissions error', function (done) {
      const errorMsg = 'authorization error';
      checkPermissionsSpy.and.returnValue(Promise.reject(errorMsg));

      store.dispatch(load()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED,
          payload: errorMsg,
        });
        done();
      });
    });

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED on fetch organizations error', function (done) {
      const errorMsg = 'error fetching organizations';
      mockAxiosCalls({
        get: {
          [OrganizationsUrl]: Promise.reject(errorMsg),
        },
      });

      store.dispatch(load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED');
        expect(actions[1].payload).toBe(errorMsg);
        done();
      });
    });
    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED on fetch automaticApplicationsConfiguration error', function (done) {
      const errorMsg = 'error fetching automaticApplicationsConfiguration';
      mockAxiosCalls({
        get: {
          [AutomaticApplicationsConfigurationUrl]: Promise.reject(errorMsg),
        },
      });

      store.dispatch(load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED');
        expect(actions[1].payload).toBe(errorMsg);
        done();
      });
    });
    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED on success', function (done) {
      const organizations = [{ id: '1', name: 'test' }];
      const automaticApplicationsConfiguration = { enabled: true };
      mockAxiosCalls({
        get: {
          [OrganizationsUrl]: Promise.resolve({ data: organizations }),
          [AutomaticApplicationsConfigurationUrl]: Promise.resolve({ data: automaticApplicationsConfiguration }),
        },
      });

      store.dispatch(load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED');
        expect(actions[1]).toEqual({
          type: 'AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED',
          payload: { organizations, automaticApplicationsConfiguration },
        });
        done();
      });
    });
  });
  describe('update', function () {
    let store;

    beforeEach(() => {
      checkPermissionsSpy.and.returnValue(Promise.resolve());
      store = SpecUtil.mockReduxStore({ automaticApplicationsConfiguration: { formState: {} } });
    });

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED', function (done) {
      store.dispatch(update()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED,
        });
        done();
      });
    });
    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED on error', function (done) {
      const errorMsg = 'error updating';
      mockAxiosCalls({
        put: {
          [AutomaticApplicationsConfigurationUrl]: Promise.reject(errorMsg),
        },
      });

      store.dispatch(update()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED');
        expect(actions[1].payload).toBe(errorMsg);
        done();
      });
    });
    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED on success', function (done) {
      mockAxiosCalls({
        put: {
          [AutomaticApplicationsConfigurationUrl]: Promise.resolve(),
        },
      });

      store.dispatch(update()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED');
        done();
      });
    });
    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE on success', function (done) {
      mockAxiosCalls({
        put: {
          [AutomaticApplicationsConfigurationUrl]: Promise.resolve(),
        },
      });

      store.dispatch(update()).then(() => {
        setTimeout(function () {
          const actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED');
          expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED');
          expect(actions[2].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE');
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });
  });
});
