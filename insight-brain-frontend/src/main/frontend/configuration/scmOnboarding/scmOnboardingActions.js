/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {noPayloadActionCreator, payloadParamActionCreator} from '../../util/reduxUtil';
import axios from 'axios';
import {getManifestScanConfigUrl, getOrganizationsUrl, getScmRepositoriesUrl} from '../../util/CLMLocation';

export const SCM_ONBOARDING_LOAD_CONFIG_REQUESTED = 'SCM_ONBOARDING_LOAD_CONFIG_REQUESTED';
export const SCM_ONBOARDING_LOAD_CONFIG_FULFILLED = 'SCM_ONBOARDING_LOAD_CONFIG_FULFILLED';
export const SCM_ONBOARDING_LOAD_CONFIG_FAILED = 'SCM_ONBOARDING_CONFIG_LOAD_FAILED';

export const SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED = 'SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED';
export const SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED = 'SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED';
export const SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED = 'SCM_ONBOARDING_CONFIG_ORGS_FAILED';

export const SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED';
export const SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED';
export const SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED';

export function loadConfig() {
  return function(dispatch) {
    dispatch(loadConfigRequested());

    return axios.get(getManifestScanConfigUrl())
        .then(({ data }) => { dispatch(loadConfigFulfilled(data)); })
        .catch(error => { dispatch(loadConfigFailed(error)); });
  };
}

export function loadOrganizations() {
  return function(dispatch) {
    dispatch(loadOrganizationsRequested());

    return axios.get(getOrganizationsUrl())
        .then(({ data }) => { dispatch(loadOrganizationsFulfilled(data)); })
        .catch(error => { dispatch(loadOrganizationsFailed(error)); });
  };
}

export function loadRepositories() {
  return function(dispatch) {
    dispatch(loadRepositoriesRequested());

    return axios.get(getScmRepositoriesUrl())
        .then(({ data }) => { dispatch(loadRepositoriesFulfilled(data)); })
        .catch(error => { dispatch(loadRepositoriesFailed(error)); });
  };
}

const loadConfigRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_CONFIG_REQUESTED);
const loadConfigFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_CONFIG_FULFILLED);
const loadConfigFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_CONFIG_FAILED);

const loadOrganizationsRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED);
const loadOrganizationsFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED);
const loadOrganizationsFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED);

const loadRepositoriesRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED);
const loadRepositoriesFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED);
const loadRepositoriesFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED);

export const SCM_ONBOARDING_SET_TARGET_ORGANIZATION = 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION';
export const setSelectedOrganization = payloadParamActionCreator(SCM_ONBOARDING_SET_TARGET_ORGANIZATION);
