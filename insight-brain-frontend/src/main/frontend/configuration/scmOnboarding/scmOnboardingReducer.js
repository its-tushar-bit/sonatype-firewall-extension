/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {createReducerFromActionMap} from '../../util/reduxUtil';
import {
  SCM_ONBOARDING_LOAD_COMPOSITE_SCM_REQUESTED,
  SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FAILED,
  SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED,
  SCM_ONBOARDING_LOAD_CONFIG_FAILED,
  SCM_ONBOARDING_LOAD_CONFIG_FULFILLED,
  SCM_ONBOARDING_LOAD_CONFIG_REQUESTED,
  SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FAILED,
  SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED,
  SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_REQUESTED,
  SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED,
  SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED,
  SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED,
  SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED,
  SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED,
  SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION
} from './scmOnboardingActions';
import {Messages} from '../../util/CommonServices';

const initialState = {
  loadingConfig: true,
  loadingScmConfig: true,
  isScmOnboardingFeatureEnabled: false,
  scmTokenConfigured: false,
  scmProvider: '',

  loadingOrganizations: false,
  organizations: [],
  selectedOrganization: null,
  preselectedOrganizationId: null,

  loadingRepositories: false,
  repositories: [],
  selectedRepositoryCount: 0,
  importedRepositoryCount: 0,

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

function loadCompositeSourceControlRequested(payload, state) {
  return {
    ...state,
    loadingScmConfig: true
  };
}

function loadConfigFulfilled(payload, state) {
  return {
    ...state,
    isScmOnboardingFeatureEnabled: payload.scmOnboardingFeatureEnabled,
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
    loadingOrganizations: true,
    preselectedOrganizationId: payload
  };
}

function loadOrganizationsFulfilled(payload, state) {
  return {
    ...state,
    loadingOrganizations: false,
    organizations: payload,
    selectedOrganization: payload.find(org => org.id === state.preselectedOrganizationId) || state.selectedOrganization
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
    repositories: payload,
    selectedRepositoryCount: 0,
    // todo gather correct values INT-3479
    importedRepositoryCount: 0
  };
}

function loadRepositoriesFailed(payload, state) {
  return {
    ...state,
    loadingRepositories: false,
    error: payload.response && payload.response.status === 404 ? null : Messages.getHttpErrorMessage(payload)
  };
}

function loadOrgDefaultHostUrlRequested(payload, state) {
  return {
    ...state,
    defaultHostUrlState: {
      isPristine: true,
      value: ''
    }
  };
}

function loadOrgDefaultHostUrlFulfilled(payload, state) {
  return {
    ...state,
    defaultHostUrlState: {
      isPristine: false,
      value: payload.defaultHostUrl
    }
  };
}

function loadOrgDefaultHostUrlFailed(payload, state) {
  return {
    ...state
  };
}

function loadCompositeSourceControlFulfilled(payload, state) {
  return {
    ...state,
    scmProvider: payload.provider, // TODO entry point for INT-3695
    scmTokenConfigured: !!payload.token.value || !!payload.token.parentValue
  };
}

function loadCompositeSourceControlFailed(payload, state) {
  return {
    ...state,
    compositeSourceControlError: payload
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

  [SCM_ONBOARDING_SET_TARGET_ORGANIZATION]: setSelectedOrganization,

  [SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_REQUESTED]: loadOrgDefaultHostUrlRequested,
  [SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED]: loadOrgDefaultHostUrlFulfilled,
  [SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FAILED]: loadOrgDefaultHostUrlFailed,

  [SCM_ONBOARDING_LOAD_COMPOSITE_SCM_REQUESTED]: loadCompositeSourceControlRequested,
  [SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED]: loadCompositeSourceControlFulfilled,
  [SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FAILED]: loadCompositeSourceControlFailed
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
