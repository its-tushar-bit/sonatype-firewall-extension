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
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION,
} from './automaticApplicationsConfigurationActions';

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
  const { organizations, automaticApplicationsConfiguration } = payload;
  return {
    ...state,
    loading: false,
    ...clearedErrors,
    organizations,
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

function setParentOrganization(payload, state) {
  return checkIsDirty({
    ...state,
    formState: {
      ...state.formState,
      parentOrganizationId: payload,
    },
  });
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
  [AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION]: setParentOrganization,
  [AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED]: propSetConst('submitMaskState', false),
  [AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED]: updateFulfilled,
  [AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED]: updateFailed,
  [AUTOMATIC_APPLICATION_CONFIGURATION_RESET_FORM]: resetForm,
  [AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null),
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
