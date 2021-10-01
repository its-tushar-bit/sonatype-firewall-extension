/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';

import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';

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
} from './automaticApplicationsConfigurationActions';
import { valueFromHierarchy } from '../scmOnboarding/utils/providers';

export const initialState = Object.freeze({
  loading: true,
  loadError: null,
  updateError: null,
  submitMaskState: null,
  isDirty: false,
  organizations: [],
  formState: {
    enabled: false,
    parentOrganizationId: '',
  },
  serverData: {},
});

function checkIsDirty(state) {
  const { serverData, formState } = state;

  const isDirty = Object.keys(serverData).some((prop) => serverData[prop] !== formState[prop]);

  return { ...state, isDirty };
}

const clearedErrors = pick(['loadError', 'updateError'], initialState);

function loadFulfilled(payload, state) {
  const { organizations, automaticApplicationsConfiguration, compositeSourceControl } = payload;
  return {
    ...state,
    loading: false,
    ...clearedErrors,
    organizations,
    scmProvider: compositeSourceControl != null ? valueFromHierarchy(compositeSourceControl.provider) : null,
    formState: { ...automaticApplicationsConfiguration },
    serverData: { ...automaticApplicationsConfiguration },
  };
}

function loadFailed(payload, state) {
  return {
    ...state,
    loading: false,
    loadError: payload,
  };
}

function toggleAutomaticApplicationEnabled(_, state) {
  const {
    formState,
    serverData: { parentOrganizationId: initialOrg },
  } = state;
  const { parentOrganizationId: changedOrg } = formState;
  const enabled = !formState.enabled;
  return checkIsDirty({
    ...state,
    formState: {
      ...formState,
      parentOrganizationId: enabled ? changedOrg : initialOrg,
      enabled,
    },
  });
}

function setParentOrganizationRequested(payload, state) {
  return checkIsDirty({
    ...state,
    formState: {
      ...state.formState,
      parentOrganizationId: payload,
    },
  });
}

function setParentOrganizationFulfilled(payload, state) {
  const { compositeSourceControl } = payload;
  return {
    ...state,
    scmProvider: compositeSourceControl != null ? valueFromHierarchy(compositeSourceControl.provider) : null,
  };
}

function setParentOrganizationFailed(payload, state) {
  return {
    ...state,
    loading: false,
    loadError: payload,
  };
}

const updateFulfilled = (_, state) => {
  return {
    ...state,
    submitMaskState: true,
    isDirty: false,
    ...clearedErrors,
    serverData: { ...state.formState },
  };
};

function updateFailed(payload, state) {
  return {
    ...state,
    updateError: payload,
    submitMaskState: null,
  };
}

function resetForm(_, state) {
  return {
    ...state,
    isDirty: false,
    updateError: null,
    formState: { ...state.serverData },
  };
}

const reducerActionMap = {
  [AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED]: () => initialState,
  [AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED]: loadFulfilled,
  [AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED]: loadFailed,
  [AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED]: toggleAutomaticApplicationEnabled,
  [AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED]: setParentOrganizationRequested,
  [AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FULFILLED]: setParentOrganizationFulfilled,
  [AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED]: setParentOrganizationFailed,
  [AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED]: propSetConst('submitMaskState', false),
  [AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED]: updateFulfilled,
  [AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED]: updateFailed,
  [AUTOMATIC_APPLICATION_CONFIGURATION_RESET_FORM]: resetForm,
  [AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null),
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
