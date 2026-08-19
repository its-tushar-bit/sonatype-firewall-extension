/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED,
  AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED,
  AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED,
  AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED,
  AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED,
  AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED,
  AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE,
  AUTOMATIC_APPLICATION_CONFIGURATION_RESET_FORM,
  AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED,
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED,
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FULFILLED,
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED,
} from '../../../../main/frontend/configuration/automaticApplicationsConfiguration/automaticApplicationsConfigurationActions';
import reducer from '../../../../main/frontend/configuration/automaticApplicationsConfiguration/automaticApplicationsConfigurationReducer';

describe('AutomaticApplicationConfigurationReducer', function () {
  const organizations = [{ id: '1', name: 'test' }];
  const automaticApplicationsConfiguration = { enabled: true, parentOrganizationId: '3' };
  let initialState;

  beforeEach(() => {
    const dummyAction = { type: 'DUMMY_ACTION' };
    initialState = reducer(undefined, dummyAction);
  });

  describe('initial state', () => {
    it('has default field values', function () {
      expect(initialState.loading).toBe(true);
      expect(initialState.loadError).toBeNull();
      expect(initialState.updateError).toBeNull();
      expect(initialState.submitMaskState).toBeNull();
      expect(initialState.isDirty).toBe(false);
      expect(initialState.organizations).toEqual([]);
      expect(initialState.formState.enabled).toBe(false);
      expect(initialState.formState.parentOrganizationId).toBe('');
    });
  });

  describe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED', function () {
    it('returns initial state', function () {
      const action = { type: AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED };
      const newState = reducer(undefined, action);

      expect(newState).toEqual(initialState);
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED', function () {
    let action;
    beforeEach(() => {
      action = {
        type: AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED,
        payload: { organizations, automaticApplicationsConfiguration },
      };
    });

    it('clears any error', function () {
      const oldState = { ...initialState, loadError: 'error', updateError: 'error' };
      const newState = reducer(oldState, action);

      expect(newState.loadError).toBeNull();
      expect(newState.updateError).toBeNull();
    });
    it('updates formState', function () {
      const newState = reducer(initialState, action);

      expect(newState.organizations).toBe(organizations);
      expect(newState.formState.enabled).toBe(true);
      expect(newState.formState.parentOrganizationId).toBe('3');
    });
    it('updates serverData', function () {
      const newState = reducer(initialState, action);

      expect(newState.serverData.enabled).toBe(true);
      expect(newState.serverData.parentOrganizationId).toBe('3');
    });
    it('updates scmProvider', function () {
      const newState = reducer(initialState, {
        ...action,
        payload: { ...action.payload, compositeSourceControl: { provider: { value: 'provider' } } },
      });

      expect(newState.scmProvider).toBe('provider');
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED', function () {
    let action, newState, errorMsg;
    beforeEach(() => {
      errorMsg = 'error on load';
      action = { type: AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED, payload: errorMsg };
      newState = reducer(initialState, action);
    });

    it('sets loading', function () {
      expect(newState.loading).toBe(false);
    });

    it('sets loadError', function () {
      expect(newState.loadError).toBe(errorMsg);
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED', function () {
    it('sets submitMaskState', function () {
      const action = { type: AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED };
      const newState = reducer(initialState, action);

      expect(newState.submitMaskState).toBe(false);
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED', function () {
    let newState;
    beforeEach(() => {
      const oldState = {
        ...initialState,
        loadError: 'error',
        updateError: 'error',
        isDirty: true,
        submitMaskState: false,
        organizations,
        formState: { ...automaticApplicationsConfiguration },
      };
      newState = reducer(oldState, { type: AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED });
    });
    it('sets submitMaskState', function () {
      expect(newState.submitMaskState).toBe(true);
    });
    it('sets isDirty', function () {
      expect(newState.isDirty).toBe(false);
    });
    it('clears errors', function () {
      expect(newState.loadError).toBeNull();
      expect(newState.updateError).toBeNull();
    });
    it('sets serverData', function () {
      expect(newState.serverData).toEqual(automaticApplicationsConfiguration);
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED', function () {
    let newState, action, errorMsg;
    beforeEach(() => {
      errorMsg = 'error on update';
      action = { type: AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED, payload: errorMsg };
      const oldState = { ...initialState, submitMaskState: true };
      newState = reducer(oldState, action);
    });
    it('sets submitMaskState', function () {
      expect(newState.submitMaskState).toBe(null);
    });
    it('sets updateError', function () {
      expect(newState.updateError).toBe(errorMsg);
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE', function () {
    it('sets submitMaskState', function () {
      const action = { type: AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE };
      const oldState = { ...initialState, submitMaskState: true };
      const newState = reducer(oldState, action);

      expect(newState.submitMaskState).toBeNull();
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_RESET_FORM', function () {
    let newState, action;
    beforeEach(() => {
      action = { type: AUTOMATIC_APPLICATION_CONFIGURATION_RESET_FORM };
      const oldState = {
        ...initialState,
        updateError: 'error',
        isDirty: true,
        organizations,
        serverData: { ...automaticApplicationsConfiguration },
      };
      newState = reducer(oldState, action);
    });
    it('sets isDirty', function () {
      expect(newState.isDirty).toBe(false);
    });
    it('sets updateError', function () {
      expect(newState.updateError).toBeNull();
    });
    it('sets formState', function () {
      expect(newState.formState).toEqual({ ...automaticApplicationsConfiguration });
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED', function () {
    let newState, action;
    beforeEach(() => {
      action = { type: AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED };
      const oldState = {
        ...initialState,
        serverData: { ...automaticApplicationsConfiguration },
      };
      newState = reducer(oldState, action);
    });
    it('sets isDirty', function () {
      expect(newState.isDirty).toBe(true);
    });
    it('sets formState.enabled', function () {
      expect(newState.formState.enabled).toBe(true);
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED', function () {
    let newState, action;
    beforeEach(() => {
      action = { type: AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED, payload: '3' };
      const oldState = {
        ...initialState,
        serverData: { ...automaticApplicationsConfiguration },
      };
      newState = reducer(oldState, action);
    });
    it('sets isDirty', function () {
      expect(newState.isDirty).toBe(true);
    });
    it('updates parentOrganizationId', function () {
      expect(newState.formState.parentOrganizationId).toBe(action.payload);
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FULFILLED', function () {
    let newState, action;
    beforeEach(() => {
      action = {
        type: AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FULFILLED,
        payload: { compositeSourceControl: { provider: { value: 'provider' } } },
      };
      const oldState = {
        ...initialState,
      };
      newState = reducer(oldState, action);
    });
    it('updates scmProvider', function () {
      expect(newState.scmProvider).toBe('provider');
    });
  });
  describe('AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED', function () {
    let action, newState, errorMsg;
    beforeEach(() => {
      errorMsg = 'error on load';
      action = { type: AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED, payload: errorMsg };
      newState = reducer(initialState, action);
    });

    it('sets loading', function () {
      expect(newState.loading).toBe(false);
    });

    it('sets loadError', function () {
      expect(newState.loadError).toBe(errorMsg);
    });
  });
  describe('when toggle is initially enabled and parent org is preselected, changing the parent org and clicking on toggle', function () {
    let newState;

    const INITIAL_PARENT_ID = automaticApplicationsConfiguration.parentOrganizationId;
    const NEW_PARENT_ID = '2';

    const toggleAction = { type: AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED };
    const toggleNewParentSelectionAction = {
      type: AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED,
      payload: NEW_PARENT_ID,
    };

    const oldState = {
      ...initialState,
      formState: { ...automaticApplicationsConfiguration },
      serverData: { ...automaticApplicationsConfiguration },
    };

    it('disables the toggle and reverts parent org to initial state ', function () {
      newState = reducer(oldState, toggleNewParentSelectionAction);
      expect(newState.formState.parentOrganizationId).toBe(NEW_PARENT_ID);

      newState = reducer(oldState, toggleAction);
      expect(newState.formState.parentOrganizationId).toBe(INITIAL_PARENT_ID);
    });
  });
});
