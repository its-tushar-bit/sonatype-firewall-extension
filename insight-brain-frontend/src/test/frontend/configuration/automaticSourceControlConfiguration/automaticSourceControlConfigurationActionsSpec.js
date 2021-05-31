/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import {
  load,
  update,
  authErrorMessage,
} from '../../../../main/frontend/configuration/automaticSourceControlConfiguration/automaticSourceControlConfigurationActions';
import { getGlobalPermissionTestUrl } from '../../../../main/frontend/util/CLMContextLocation';
import { getAutomaticSourceControlConfigurationUrl } from '../../../../main/frontend/util/CLMLocation';

describe('AutomaticSourceControlConfigurationActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const automaticSourceControlControlConfigurationUrl = getAutomaticSourceControlConfigurationUrl();
  const globalPermissionTestUrl = getGlobalPermissionTestUrl();

  let store, state, actions;

  beforeEach(function () {
    state = {
      automaticSourceControlConfiguration: {
        formState: {
          enabled: false,
        },
      },
    };

    store = SpecUtil.mockReduxStore(state);
    actions = store.getActions();
  });

  describe('load', function () {
    it('requests load configuration', function (done) {
      let data = [];
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: [''] }),
        },
        get: {
          [automaticSourceControlControlConfigurationUrl]: Promise.resolve({ data }),
        },
      });
      store.dispatch(load()).then(() => {
        const [, { type: secondActionType, payload: secondActionPayload }] = actions;
        expect(secondActionType).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED');
        expect(secondActionPayload).toBe(data);
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED');
    });

    it('dispatches AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL action when there is not permissions', function (done) {
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: [] }),
        },
      });

      store.dispatch(load()).then(() => {
        const [, { type: secondActionType, payload: secondActionPayload }] = actions;
        expect(secondActionType).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL');
        expect(secondActionPayload).toBe(authErrorMessage);
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED');
    });
  });

  describe('update', function () {
    it('dispatches an AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED action', function () {
      mockAxiosCalls({
        put: {
          [automaticSourceControlControlConfigurationUrl]: Promise.resolve({}),
        },
      });
      store.dispatch(update()).then(() => {
        const [, { type }] = actions;
        expect(actions.length).toBe(2);
        expect(type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED');
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED');
    });

    it('dispatches an AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED action', function () {
      mockAxiosCalls({
        put: {
          [automaticSourceControlControlConfigurationUrl]: Promise.reject({ status: 403 }),
        },
      });
      store.dispatch(update()).then(() => {
        actions = store.getActions();
        const [, { type, payload }] = actions;
        expect(actions.length).toBe(2);
        expect(type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED');
        expect(payload).toBe('Error 403');
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED');
    });
  });
});
