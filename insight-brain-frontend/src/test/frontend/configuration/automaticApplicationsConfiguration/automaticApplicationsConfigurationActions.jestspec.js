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
import '../../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { checkPermissions } from '../../../../main/frontend/util/authorizationUtil';
import {
  AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED,
  AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED,
  AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED,
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED,
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED,
  load,
  update,
  setParentOrganization,
} from '../../../../main/frontend/configuration/automaticApplicationsConfiguration/automaticApplicationsConfigurationActions';
import {
  getAutomaticApplicationsConfigurationUrl,
  getCompositeSourceControlUrl,
  getOrganizationsUrl,
} from '../../../../main/frontend/util/CLMLocation';

describe('AutomaticApplicationConfigurationActions', function () {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });
  const OrganizationsUrl = getOrganizationsUrl();
  const AutomaticApplicationsConfigurationUrl = getAutomaticApplicationsConfigurationUrl();
  const CompositeSourceControlUrl = getCompositeSourceControlUrl('organization', '1');
  beforeEach(() => {
    jest.clearAllMocks();
    checkPermissions.mockClear();
  });

  describe('load', function () {
    let store;

    beforeEach(() => {
      checkPermissions.mockReturnValue(Promise.resolve());
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
      checkPermissions.mockImplementation(() => Promise.reject(errorMsg));

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
      axiosMock.onGet(OrganizationsUrl).reply(404, errorMsg);

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
      axiosMock.onGet(AutomaticApplicationsConfigurationUrl).reply(404, errorMsg);

      store.dispatch(load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED');
        expect(actions[1].payload).toBe('Error 404');
        done();
      });
    });

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED on fetch compositeSourceControl error', function (done) {
      const errorMsg = 'error fetching compositeSourceControl';
      const automaticApplicationsConfiguration = { enabled: true, parentOrganizationId: '1' };
      const organizations = [{ id: '1', name: 'test' }];
      axiosMock.onGet(OrganizationsUrl).reply(200, organizations);
      axiosMock.onGet(AutomaticApplicationsConfigurationUrl).reply(200, automaticApplicationsConfiguration);
      axiosMock.onGet(CompositeSourceControlUrl).reply(404, errorMsg);

      store.dispatch(load());

      setTimeout(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED');
        expect(actions[1].payload).toBe(errorMsg);
        done();
      }, 100);
    });

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED on success', function (done) {
      const organizations = [{ id: '1', name: 'test' }];
      const automaticApplicationsConfiguration = { enabled: true };
      axiosMock.onGet(OrganizationsUrl).reply(200, organizations);
      axiosMock.onGet(AutomaticApplicationsConfigurationUrl).reply(200, automaticApplicationsConfiguration);

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

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED on success with parentOrganizationId', function (done) {
      const organizations = [{ id: '1', name: 'test' }];
      const compositeSourceControl = { scmProvider: 'provider' };
      const automaticApplicationsConfiguration = { enabled: true, parentOrganizationId: '1' };
      axiosMock.onGet(OrganizationsUrl).reply(200, organizations);
      axiosMock.onGet(AutomaticApplicationsConfigurationUrl).reply(200, automaticApplicationsConfiguration);
      axiosMock.onGet(CompositeSourceControlUrl).reply(200, compositeSourceControl);

      store.dispatch(load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED');
        expect(actions[1]).toEqual({
          type: 'AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED',
          payload: { organizations, automaticApplicationsConfiguration, compositeSourceControl },
        });
        done();
      });
    });
  });
  describe('update', function () {
    let store;

    beforeEach(() => {
      checkPermissions.mockReturnValue(Promise.resolve());
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
      axiosMock.onPut(AutomaticApplicationsConfigurationUrl).reply(500, errorMsg);

      store.dispatch(update()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED');
        expect(actions[1].payload).toBe(errorMsg);
        done();
      });
    });
    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED on success', function (done) {
      axiosMock.onPut(AutomaticApplicationsConfigurationUrl).reply(200);

      store.dispatch(update()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED');
        done();
      });
    });
    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE on success', function (done) {
      jest.useFakeTimers();
      axiosMock.onPut(AutomaticApplicationsConfigurationUrl).reply(200);

      store.dispatch(update()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED');
        expect(actions[2].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE');
        done();
      });
    });
  });

  describe('setParent', function () {
    let store;

    beforeEach(() => {
      checkPermissions.mockReturnValue(Promise.resolve());
      store = SpecUtil.mockReduxStore();
    });

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED', function (done) {
      store.dispatch(setParentOrganization('1')).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED,
          payload: '1',
        });
        done();
      });
    });

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED on permissions error', function (done) {
      const errorMsg = 'authorization error';
      checkPermissions.mockImplementation(() => Promise.reject(errorMsg));

      store.dispatch(setParentOrganization('1')).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED,
          payload: errorMsg,
        });
        done();
      });
    });

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED on fetch compositeSourceControl error', function (done) {
      const errorMsg = 'error fetching compositeSourceControl';
      axiosMock.onGet(CompositeSourceControlUrl).reply(404, errorMsg);

      store.dispatch(setParentOrganization('1'));

      setTimeout(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED');
        expect(actions[1].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED');
        expect(actions[1].payload).toBe(errorMsg);
        done();
      }, 100);
    });

    it('dispatches AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FULFILLED on success', function (done) {
      const compositeSourceControl = { scmProvider: 'provider' };
      axiosMock.onGet(CompositeSourceControlUrl).reply(200, compositeSourceControl);

      store.dispatch(setParentOrganization('1'));

      setTimeout(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED');
        expect(actions[1]).toEqual({
          type: 'AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FULFILLED',
          payload: { compositeSourceControl },
        });
        done();
      }, 100);
    });
  });
});
