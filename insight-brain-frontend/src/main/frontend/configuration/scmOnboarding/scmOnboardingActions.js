/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {noPayloadActionCreator, payloadParamActionCreator} from '../../util/reduxUtil';
import axios from 'axios';
import {
  getCompositeSourceControlUrl,
  getScmOnboardingConfigUrl,
  getOrganizationsUrl,
  getScmRepositoriesUrl,
  getScmDefaultHostUrl
} from '../../util/CLMLocation';

export const SCM_ONBOARDING_LOAD_CONFIG_REQUESTED = 'SCM_ONBOARDING_LOAD_CONFIG_REQUESTED';
export const SCM_ONBOARDING_LOAD_CONFIG_FULFILLED = 'SCM_ONBOARDING_LOAD_CONFIG_FULFILLED';
export const SCM_ONBOARDING_LOAD_CONFIG_FAILED = 'SCM_ONBOARDING_CONFIG_LOAD_FAILED';

export const SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED = 'SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED';
export const SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED = 'SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED';
export const SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED = 'SCM_ONBOARDING_CONFIG_ORGS_FAILED';

export const SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED';
export const SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED';
export const SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED';

export const SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED = 'SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED';

export const SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_REQUESTED = 'SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_REQUESTED';
export const SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED = 'SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED';
export const SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FAILED = 'SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FAILED';
export const SCM_ONBOARDING_SET_CURRENT_HOST_URL = 'SCM_ONBOARDING_SET_CURRENT_HOST_URL';

export const SCM_ONBOARDING_SET_TARGET_ORGANIZATION = 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION';

export const SCM_ONBOARDING_LOAD_COMPOSITE_SCM_REQUESTED = 'SCM_ONBOARDING_LOAD_COMPOSITE_SCM_REQUESTED';
export const SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED = 'SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED';
export const SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FAILED = 'SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FAILED';

export function loadConfig() {
  return function(dispatch) {
    dispatch(loadConfigRequested());

    return axios.get(getScmOnboardingConfigUrl())
        .then(({ data }) => { dispatch(loadConfigFulfilled(data)); })
        .catch(error => { dispatch(loadConfigFailed(error)); });
  };
}

export function loadOrganizations(preselectedOrganizationId) {
  return function(dispatch) {
    dispatch(loadOrganizationsRequested(preselectedOrganizationId));

    return axios.get(getOrganizationsUrl())
        .then(({ data }) => { dispatch(loadOrganizationsFulfilled(data)); })
        .catch(error => { dispatch(loadOrganizationsFailed(error)); });
  };
}

export function loadRepositories(orgId, scmUrl) {
  return function(dispatch) {
    dispatch(loadRepositoriesRequested());

    return axios.get(getScmRepositoriesUrl(orgId, scmUrl))
        .then(({ data }) => { dispatch(loadRepositoriesFulfilled(data)); })
        .catch(error => { dispatch(loadRepositoriesFailed(error)); });
  };
}

export function loadCompositeSourceControl(ownerType, internalOwnerId) {
  return function(dispatch) {
    dispatch(loadCompositeSourceControlRequested());

    return axios.get(getCompositeSourceControlUrl(ownerType, internalOwnerId))
        .then(({ data }) => { dispatch(loadCompositeSourceControlFulfilled(data)); })
        .catch(error => { dispatch(loadCompositeSourceControlFailed(error)); });
  };
}

export function onRepositorySelectionChanged(repo) {
  return function(dispatch) {
    dispatch(repositorySelectionChanged(repo));
  };
}

export function importSelectedRepositories() {
  // TODO INT-3482
}

export function loadOrgHostUrl(orgId, provider) {
  return function(dispatch) {
    dispatch(loadOrgDefaultHostUrlRequested());

    return axios.get(getScmDefaultHostUrl(orgId, provider))
        .then(({ data }) => { dispatch(loadOrgDefaultHostUrlFulfilled(data)); })
        .catch(error => { dispatch(loadOrgDefaultHostUrlFailed(error)); });
  };
}

const loadConfigRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_CONFIG_REQUESTED);
const loadConfigFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_CONFIG_FULFILLED);
const loadConfigFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_CONFIG_FAILED);

const loadOrganizationsRequested = payloadParamActionCreator(SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED);
const loadOrganizationsFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED);
const loadOrganizationsFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED);

const loadRepositoriesRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED);
const loadRepositoriesFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED);
const loadRepositoriesFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED);
const repositorySelectionChanged = payloadParamActionCreator(SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED);

export const setSelectedOrganization = payloadParamActionCreator(SCM_ONBOARDING_SET_TARGET_ORGANIZATION);

const loadOrgDefaultHostUrlRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_REQUESTED);
const loadOrgDefaultHostUrlFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED);
const loadOrgDefaultHostUrlFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FAILED);

export const setCurrentHostUrl = payloadParamActionCreator(SCM_ONBOARDING_SET_CURRENT_HOST_URL);

const loadCompositeSourceControlRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_COMPOSITE_SCM_REQUESTED);
const loadCompositeSourceControlFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED);
const loadCompositeSourceControlFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FAILED);

export default function scmOnboarding() {
  return {
    setSelectedOrganization,
    setCurrentHostUrl,
    loadCompositeSourceControl,
    loadConfig,
    loadOrganizations,
    loadRepositories,
    onRepositorySelectionChanged,
    loadOrgHostUrl,
    importSelectedRepositories
  };
}
