/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Mock the authorizationUtil module before importing actions
jest.mock('../../../../main/frontend/util/authorizationUtil', () => ({
  checkPermissions: jest.fn(),
}));

import '../../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { checkPermissions } from '../../../../main/frontend/util/authorizationUtil';
import {
  load,
  update,
} from '../../../../main/frontend/configuration/automaticSourceControlConfiguration/automaticSourceControlConfigurationActions';
import {
  getAutomaticApplicationsConfigurationUrl,
  getAutomaticSourceControlConfigurationUrl,
  getCompositeSourceControlUrl,
  getOrganizationsUrl,
} from '../../../../main/frontend/util/CLMLocation';

describe('AutomaticSourceControlConfigurationActions', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  const automaticSourceControlControlConfigurationUrl = getAutomaticSourceControlConfigurationUrl(),
    automaticApplicationsConfigurationUrl = getAutomaticApplicationsConfigurationUrl(),
    organizationsUrl = getOrganizationsUrl(),
    compositeSourceControlUrl = getCompositeSourceControlUrl('organization', 1);

  let store, state, actions;

  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
    checkPermissions.mockClear();

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
      checkPermissions.mockReturnValue(Promise.resolve());
    });

    it('requests load configuration without automatic applications', (done) => {
      axiosMock.onGet(automaticSourceControlControlConfigurationUrl).reply(200, []);
      axiosMock.onGet(automaticApplicationsConfigurationUrl).reply(200, []);
      axiosMock.onGet(organizationsUrl).reply(200, []);

      store.dispatch(load()).then(() => {
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED');
        expect(actions[1].payload).toEqual({
          automaticSourceControlConfiguration: [],
          automaticApplicationsConfiguration: [],
          organizations: [],
        });
        done();
      });
    });

    it('requests load configuration with automatic applications', (done) => {
      const automaticApplicationsConfiguration = { enabled: true, parentOrganizationId: '1' };
      axiosMock.onGet(automaticSourceControlControlConfigurationUrl).reply(200, []);
      axiosMock.onGet(automaticApplicationsConfigurationUrl).reply(200, automaticApplicationsConfiguration);
      axiosMock.onGet(organizationsUrl).reply(200, []);
      axiosMock.onGet(compositeSourceControlUrl).reply(200, []);

      store.dispatch(load()).then(() => {
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED');
        expect(actions[1].payload).toEqual({
          automaticSourceControlConfiguration: [],
          automaticApplicationsConfiguration: automaticApplicationsConfiguration,
          organizations: [],
          compositeSourceControl: [],
        });
        done();
      });
    });

    it('dispatches AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL action when there is not permissions', (done) => {
      checkPermissions.mockImplementation(() => Promise.reject('ASC config page authorization error'));

      store.dispatch(load()).then(() => {
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL');
        expect(actions[1].payload).toBe('ASC config page authorization error');
        done();
      });
    });
  });

  describe('update', function () {
    it('dispatches an AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED action', (done) => {
      axiosMock.onPut(automaticSourceControlControlConfigurationUrl).reply(200, {});

      store.dispatch(update()).then(() => {
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED');
        done();
      });
    });

    it('dispatches an AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED action', (done) => {
      axiosMock.onPut(automaticSourceControlControlConfigurationUrl).reply(403, { status: 403 });

      store.dispatch(update());

      setTimeout(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED');
        expect(actions[1].payload).toBe('Error');
        done();
      }, 100);
    });
  });
});
