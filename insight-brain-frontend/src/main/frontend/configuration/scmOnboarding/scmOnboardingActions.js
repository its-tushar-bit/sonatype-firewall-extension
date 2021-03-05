/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {noPayloadActionCreator, payloadParamActionCreator} from '../../util/reduxUtil';
import axios from 'axios';
import {debounce} from 'debounce';
import {
  getCompositeSourceControlUrl,
  getValidateScmConfigUrl,
  getScmOnboardingConfigUrl,
  getScmOrganizationsUrl,
  getScmRepositoriesUrl,
  getScmDefaultHostUrl,
  getImportRepositoriesUrl,
  getOrganizationsUrl
} from '../../util/CLMLocation';

export const SCM_ONBOARDING_LOAD_CONFIG_FULFILLED = 'SCM_ONBOARDING_LOAD_CONFIG_FULFILLED';
export const SCM_ONBOARDING_LOAD_CONFIG_FAILED = 'SCM_ONBOARDING_CONFIG_LOAD_FAILED';

export const SCM_ONBOARDING_LOAD_PAGE_REQUESTED = 'SCM_ONBOARDING_LOAD_PAGE_REQUESTED';
export const SCM_ONBOARDING_LOAD_PAGE_FULFILLED = 'SCM_ONBOARDING_LOAD_PAGE_FULFILLED';
export const SCM_ONBOARDING_LOAD_PAGE_FAILED = 'SCM_ONBOARDING_LOAD_PAGE_FAILED';

export const SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED';
export const SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED';
export const SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED';

export const SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED = 'SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED';

export const SCM_ONBOARDING_SET_CURRENT_HOST_URL = 'SCM_ONBOARDING_SET_CURRENT_HOST_URL';

export const SCM_ONBOARDING_IS_GIT_HOST_NEEDED = 'SCM_ONBOARDING_IS_GIT_HOST_NEEDED';

export const SCM_ONBOARDING_IMPORT_REPOS_REQUESTED = 'SCM_ONBOARDING_IMPORT_REPOS_REQUESTED';
export const SCM_ONBOARDING_IMPORT_REPOS_FULFILLED = 'SCM_ONBOARDING_IMPORT_REPOS_FULFILLED';
export const SCM_ONBOARDING_IMPORT_REPOS_FAILED = 'SCM_ONBOARDING_IMPORT_REPOS_FAILED';

export const SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED = 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED';
export const SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED = 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED';
export const SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED = 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED';

export const SCM_ONBOARDING_ADD_ORGANIZATION_FULFILLED = 'SCM_ONBOARDING_ADD_ORGANIZATION_FULFILLED';
export const SCM_ONBOARDING_ADD_ORGANIZATION_FAILED = 'SCM_ONBOARDING_ADD_ORGANIZATION_FAILED';
export const SCM_ONBOARDING_SET_IS_NEW_ORGANIZATION_MODAL_VISIBLE =
  'SCM_ONBOARDING_SET_IS_NEW_ORGANIZATION_MODAL_VISIBLE';

export const SCM_ONBOARDING_SET_SORTING_PARAMETERS = 'SCM_ONBOARDING_SET_SORTING_PARAMETERS';

export const SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED = 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED';
export const SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED = 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED';
export const SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED = 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED';

export const SCM_ONBOARDING_SHOW_HOST_DIALOG = 'SCM_ONBOARDING_SHOW_HOST_DIALOG';

export const SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE = 'SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE';

export function loadPage(orgId) {
  return function(dispatch) {
    dispatch(loadPageRequested(orgId));

    let config = axios.get(getScmOnboardingConfigUrl());
    let organizations = axios.get(getScmOrganizationsUrl());
    let scm = orgId ? axios.get(getCompositeSourceControlUrl('organization', orgId)) : Promise.resolve(null);
    let hostUrl = scm.then(compositeSCResults => {
      return compositeSCResults !== null && compositeSCResults.data.provider !== null
        ? axios.get(getScmDefaultHostUrl(orgId, compositeSCResults.data.provider))
        : Promise.resolve(null);
    });

    return Promise.all([config, organizations, scm, hostUrl])
        .then(([configResults, organizationsResults, compositeSourceControlResults, hostUrlResult]) => {
          dispatch(loadPageFulfilled({
            configResults: configResults.data,
            organizationsResults: organizationsResults.data,
            compositeSourceControlResults: compositeSourceControlResults ? compositeSourceControlResults.data : null,
            hostUrlResult: hostUrlResult ? hostUrlResult.data : null
          }));
          if (orgId && hostUrlResult && hostUrlResult.data.defaultHostUrl) {
            dispatch(loadRepositories(orgId, hostUrlResult.data.defaultHostUrl));
          }
        })
        .catch(error => {
          dispatch(loadPageFailed(error));
        });
  };
}

/*
 this should be only be used to determine whether to render menu items, not to determine if the page
 itself should load
 */
export function loadConfig() {
  return function(dispatch) {
    return axios.get(getScmOnboardingConfigUrl())
        .then(({ data }) => { dispatch(loadConfigFulfilled(data)); })
        .catch(error => { dispatch(loadConfigFailed(error)); });
  };
}

const validateScmHostUrlDebounceTimeout = 300;

const validateScmHostUrlDebounce = debounce((dispatch, scmProvider, scmHostUrl) => {
  dispatch(validateScmHostUrlRequested());

  return axios.get(getValidateScmConfigUrl(scmProvider, scmHostUrl))
      .then(({ data }) => { dispatch(validateScmHostUrlFulfilled(data)); })
      .catch(error => { dispatch(validateScmHostUrlFailed(error)); });
}, validateScmHostUrlDebounceTimeout);

export function setSelectedOrganization(selectedOrg) {
  return function(dispatch, getState) {
    const state = getState().scmOnboarding;
    const isScmTokenOverridden = state.configState.isScmTokenOverridden;
    const previousOrg = state.formState.selectedOrganization;
    const orgId = selectedOrg.organization.id;
    const isSelectedTokenOverridden = selectedOrg.sourceControl.token.value != null;
    dispatch(setTargetOrganizationRequested());
    if (isScmTokenOverridden || isSelectedTokenOverridden || !previousOrg) {
      // newly selected org has a custom token, or previous one did, so requery for host URL, possibly reload repos
      return axios.get(getScmDefaultHostUrl(orgId, selectedOrg.sourceControl.provider))
          .then(({ data }) => {
            dispatch(setTargetOrganizationFulfilled({
              selectedOrganization: selectedOrg,
              defaultHostUrl: data.defaultHostUrl
            }));
            if (data.defaultHostUrl) {
              dispatch(loadRepositories(orgId, data.defaultHostUrl));
            }
          })
          .catch(error => {
            dispatch(setTargetOrganizationFailed(error));
          });
    }
    else {
      // can use existing host URL
      dispatch(setTargetOrganizationFulfilled({
        selectedOrganization: selectedOrg,
        defaultHostUrl: state.formState.defaultHostUrl
      }));
    }
  };
}

export function addOrganization(organizationName) {
  return function(dispatch, getState) {
    return axios.post(getOrganizationsUrl(), {name: organizationName})
        .then(({data}) => {
          // Note INT-4477: provider is set here in preparation for work to be done in the epic to support multiple SCMs
          const newOrganization = {
            sourceControl: {
              token: {value: null},
              provider: getState().scmOnboarding.configState.scmProvider},
            organization: data
          };
          dispatch(addOrganizationFulfilled(newOrganization));
          dispatch(setSelectedOrganization(newOrganization));
        })
        .catch(error => dispatch(addOrganizationFailed(error)));
  };
}

export function validateScmHostUrl(scmProvider, scmHostUrl) {
  return (dispatch) => validateScmHostUrlDebounce(dispatch, scmProvider, scmHostUrl);
}

export function loadRepositories(orgId, scmUrl) {
  return function(dispatch) {
    if (!scmUrl) {
      return;
    }
    dispatch(loadRepositoriesRequested());

    return axios.get(getScmRepositoriesUrl(orgId, scmUrl))
        .then(({ data }) => { dispatch(loadRepositoriesFulfilled(data)); })
        .catch(error => { dispatch(loadRepositoriesFailed(error)); });
  };
}

export function onRepositorySelectionChanged(repo) {
  return function(dispatch) {
    dispatch(repositorySelectionChanged(repo));
  };
}

export function importSelectedRepositories(orgId, totalRepoCount, prevImportedCount, selectedRepositories) {
  return function(dispatch) {
    dispatch(importSelectedRepositoriesRequested());

    let postBody = {
      totalRepoCount: totalRepoCount,
      prevImportedCount: prevImportedCount,
      scmRepositories: selectedRepositories
    };

    return axios.post(getImportRepositoriesUrl(orgId), postBody)
        .then(({ data }) => { dispatch(importSelectedRepositoriesFulfilled(data)); })
        .catch(error => { dispatch(importSelectedRepositoriesFailed(error)); });
  };
}

export function setSortingParameters(key, sortFields, dir) {
  return {
    type: SCM_ONBOARDING_SET_SORTING_PARAMETERS,
    payload: { key: key, sortFields: sortFields, dir: dir }
  };
}

export function setShowHostDialog(isShow) {
  return {
    type: SCM_ONBOARDING_SHOW_HOST_DIALOG,
    payload: isShow
  };
}

export function setIsGitHostNeeded(isNeeded) {
  return {
    type: SCM_ONBOARDING_IS_GIT_HOST_NEEDED,
    payload: isNeeded
  };
}

const loadConfigFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_CONFIG_FULFILLED);
const loadConfigFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_CONFIG_FAILED);

const loadPageRequested = payloadParamActionCreator(SCM_ONBOARDING_LOAD_PAGE_REQUESTED);
const loadPageFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_PAGE_FULFILLED);
const loadPageFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_PAGE_FAILED);

const loadRepositoriesRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED);
const loadRepositoriesFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED);
const loadRepositoriesFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED);
const repositorySelectionChanged = payloadParamActionCreator(SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED);

export const setTargetOrganizationRequested = noPayloadActionCreator(
    SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED);
export const setTargetOrganizationFulfilled = payloadParamActionCreator(
    SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED);
export const setTargetOrganizationFailed = payloadParamActionCreator(SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED);

export const setIsNewOrganizationModalVisible = payloadParamActionCreator(
    SCM_ONBOARDING_SET_IS_NEW_ORGANIZATION_MODAL_VISIBLE);

export const setCurrentHostUrl = payloadParamActionCreator(SCM_ONBOARDING_SET_CURRENT_HOST_URL);

const addOrganizationFulfilled = payloadParamActionCreator(SCM_ONBOARDING_ADD_ORGANIZATION_FULFILLED);
const addOrganizationFailed = payloadParamActionCreator(SCM_ONBOARDING_ADD_ORGANIZATION_FAILED);

const importSelectedRepositoriesRequested = noPayloadActionCreator(SCM_ONBOARDING_IMPORT_REPOS_REQUESTED);
const importSelectedRepositoriesFulfilled = payloadParamActionCreator(SCM_ONBOARDING_IMPORT_REPOS_FULFILLED);
const importSelectedRepositoriesFailed = payloadParamActionCreator(SCM_ONBOARDING_IMPORT_REPOS_FAILED);

const validateScmHostUrlRequested = noPayloadActionCreator(SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED);
const validateScmHostUrlFulfilled = payloadParamActionCreator(
    SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED);
const validateScmHostUrlFailed = payloadParamActionCreator(SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED);

export const setIsImportStatusDialogVisible = payloadParamActionCreator(SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE);

export default function scmOnboarding() {
  return {
    setSelectedOrganization,
    addOrganization,
    setCurrentHostUrl,
    validateScmHostUrl,
    loadConfig,
    loadPage,
    loadRepositories,
    onRepositorySelectionChanged,
    importSelectedRepositories,
    setSortingParameters,
    setShowHostDialog,
    setIsGitHostNeeded,
    setIsImportStatusDialogVisible
  };
}
