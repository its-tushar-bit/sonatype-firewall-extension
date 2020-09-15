/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {createReducerFromActionMap} from '../../util/reduxUtil';
import {
  SCM_ONBOARDING_LOAD_CONFIG_REQUESTED,
  SCM_ONBOARDING_LOAD_CONFIG_FULFILLED,
  SCM_ONBOARDING_LOAD_CONFIG_FAILED,

  SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED,
  SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED,
  SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION,

  SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED,
  SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED,
  SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED
} from './scmOnboardingActions';
import {Messages} from '../../util/CommonServices';

const initialState = {
  loadingConfig: true,
  isManifestScanFeatureEnabled: false,

  loadingOrganizations: false,
  organizations: [],
  selectedOrganization: null,

  loadingRepositories: false,
  repositories: [],

  defaultHostUrlState: {
    isPristine: true,
    value: ''
  }
};

function loadConfigRequested() {
  return {
    ...initialState,
    loadingConfig: true
  };
}

function loadConfigFulfilled(payload, state) {
  return {
    ...state,
    isManifestScanFeatureEnabled: payload.manifestScanFeatureEnabled,
    loadingConfig: false
  };
}

function loadConfigFailed(payload) {
  return {
    ...initialState,
    loadingConfig: false,
    error: payload.response && payload.response.status === 404 ? null : Messages.getHttpErrorMessage(payload)
  };
}

function loadOrganizationsRequested(payload, state) {
  return {
    ...state,
    loadingOrganizations: true
  };
}

function loadOrganizationsFulfilled(payload, state) {
  return {
    ...state,
    loadingOrganizations: false,
    organizations: payload
  };
}

function loadOrganizationsFailed(payload, state) {
  return {
    ...state,
    loadingOrganizations: false,
    error: payload.response && payload.response.status === 404 ? null : Messages.getHttpErrorMessage(payload)
  };
}

function setSelectedOrganization(payload, state) {
  return {
    ...state,
    selectedOrganization: payload
  };
}

function loadRepositoriesRequested(payload, state) {
  return {
    ...state,
    repositories: [],
    loadingRepositories: true
  };
}

function loadRepositoriesFulfilled(payload, state) {
  return {
    ...state,
    loadingRepositories: false,
    repositories: payload
  };
}

function loadRepositoriesFailed(payload, state) {
  return {
    ...state,
    loadingRepositories: false,
    error: payload.response && payload.response.status === 404 ? null : Messages.getHttpErrorMessage(payload)
  };
}

const reducerActionMap = {
  [SCM_ONBOARDING_LOAD_CONFIG_REQUESTED]: loadConfigRequested,
  [SCM_ONBOARDING_LOAD_CONFIG_FULFILLED]: loadConfigFulfilled,
  [SCM_ONBOARDING_LOAD_CONFIG_FAILED]: loadConfigFailed,

  [SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED]: loadOrganizationsRequested,
  [SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED]: loadOrganizationsFulfilled,
  [SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED]: loadOrganizationsFailed,

  [SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED]: loadRepositoriesRequested,
  [SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED]: loadRepositoriesFulfilled,
  [SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED]: loadRepositoriesFailed,

  [SCM_ONBOARDING_SET_TARGET_ORGANIZATION]: setSelectedOrganization
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
