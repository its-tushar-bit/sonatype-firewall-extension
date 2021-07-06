/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { update } from '../../../../main/frontend/configuration/automaticSourceControlConfiguration/automaticSourceControlConfigurationActions';
import { getAutomaticSourceControlConfigurationUrl } from '../../../../main/frontend/util/CLMLocation';

describe('AutomaticSourceControlConfigurationActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const automaticSourceControlControlConfigurationUrl = getAutomaticSourceControlConfigurationUrl();
  let checkPermissionsSpy, load, store, state, actions;

  beforeEach(() => {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const module = require('inject-loader!../../../../main/frontend/configuration/automaticSourceControlConfiguration/automaticSourceControlConfigurationActions')(
      {
        '../../util/authorizationUtil': {
          checkPermissions: checkPermissionsSpy,
        },
      }
    );

    load = module.load;

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

  describe('load', () => {
    beforeEach(() => {
      checkPermissionsSpy.and.returnValue(Promise.resolve());
    });

    it('requests load configuration', (done) => {
      mockAxiosCalls({
        get: {
          [automaticSourceControlControlConfigurationUrl]: Promise.resolve({ data: [] }),
        },
      });

      store.dispatch(load()).then(() => {
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED' },
          { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED', payload: [] },
        ]);
        done();
      });
    });

    it('dispatches AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL action when there is not permissions', (done) => {
      checkPermissionsSpy.and.returnValue(Promise.reject('ASC config page authorization error'));

      store.dispatch(load()).then(() => {
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED' },
          { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL', payload: 'ASC config page authorization error' },
        ]);
        done();
      });
    });
  });

  describe('update', function () {
    it('dispatches an AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED action', (done) => {
      mockAxiosCalls({
        put: {
          [automaticSourceControlControlConfigurationUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(update()).then(() => {
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED' },
          { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED' },
        ]);
        done();
      });
    });

    it('dispatches an AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED action', (done) => {
      mockAxiosCalls({
        put: {
          [automaticSourceControlControlConfigurationUrl]: Promise.reject({ status: 403 }),
        },
      });

      store.dispatch(update()).then(() => {
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED' },
          { type: 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED', payload: 'Error 403' },
        ]);
        done();
      });
    });
  });
});
