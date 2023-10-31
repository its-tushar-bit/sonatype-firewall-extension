/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { noPayloadActionCreator, payloadParamActionCreator } from 'MainRoot/util/reduxUtil';
import axios from 'axios';
import { debounce } from 'debounce';
import {
  getCompositeSourceControlUrl,
  getValidateScmConfigUrl,
  getScmOrganizationsUrl,
  getScmRepositoriesUrl,
  getScmDefaultHostUrl,
  getImportRepositoriesUrl,
} from 'MainRoot/util/CLMLocation';
import { valueFromHierarchy, tokenForOrg } from './utils/providers';
import { checkPermissions, checkFeatures } from 'MainRoot/util/authorizationUtil';

import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';

export const SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED = 'SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED';
export const SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED = 'SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED';
export const SCM_ONBOARDING_CHECK_PERMISSIONS_FAILED = 'SCM_ONBOARDING_CHECK_PERMISSIONS_FAILED';
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

export const SCM_ONBOARDING_SET_IS_NEW_ORGANIZATION_MODAL_VISIBLE =
  'SCM_ONBOARDING_SET_IS_NEW_ORGANIZATION_MODAL_VISIBLE';

export const SCM_ONBOARDING_SET_SORTING_PARAMETERS = 'SCM_ONBOARDING_SET_SORTING_PARAMETERS';

export const SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED = 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED';
export const SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED = 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED';
export const SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED = 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED';

export const SCM_ONBOARDING_SHOW_HOST_DIALOG = 'SCM_ONBOARDING_SHOW_HOST_DIALOG';

export const SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE = 'SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE';

const REQUIRED_PERMISSIONS = ['ADD_APPLICATION'];

const REQUIRED_FEATURES = ['automation'];

function checkScmOnboardingPermissions(orgId) {
  return function (dispatch) {
    dispatch(checkPermissionForScmOnboardingRequested());
    const isAutomationFeatureEnabledRequest = checkFeatures(REQUIRED_FEATURES);
    let isAuthorizedRequest;
    if (orgId) {
      isAuthorizedRequest = checkPermissions(REQUIRED_PERMISSIONS, 'organization', orgId);
    } else {
      isAuthorizedRequest = checkPermissions(REQUIRED_PERMISSIONS);
    }

    return Promise.all([isAutomationFeatureEnabledRequest, isAuthorizedRequest])
      .then(() => {
        dispatch(checkPermissionForScmOnboardingFulfilled());
      })
      .catch((error) => {
        dispatch(checkPermissionForScmOnboardingFailed(error));
        return Promise.reject(error);
      });
  };
}

function loadScmOnboardingInformation(orgId) {
  return function (dispatch) {
    dispatch(loadPageRequested(orgId));

    let organizations = axios.get(getScmOrganizationsUrl());
    let scm = orgId ? axios.get(getCompositeSourceControlUrl('organization', orgId)) : Promise.resolve(null);
    let hostUrl = scm.then((compositeSCResults) => {
      const provider =
        compositeSCResults !== null && compositeSCResults.data.provider !== null
          ? valueFromHierarchy(compositeSCResults.data.provider)
          : null;
      return provider !== null ? axios.get(getScmDefaultHostUrl(orgId, provider)) : Promise.resolve(null);
    });

    return Promise.all([organizations, scm, hostUrl, dispatch(ownerSideNavActions.load())])
      .then(([organizationsResults, compositeSourceControlResults, hostUrlResult]) => {
        dispatch(
          loadPageFulfilled({
            organizationsResults: organizationsResults.data,
            compositeSourceControlResults: compositeSourceControlResults ? compositeSourceControlResults.data : null,
            hostUrlResult: hostUrlResult ? hostUrlResult.data : null,
          })
        );
        const selectedOrganization = organizationsResults.data.find((org) => org.organization.id === orgId);
        if (selectedOrganization) dispatch(setSelectedOrganization(selectedOrganization));
        const hasToken =
          selectedOrganization &&
          (selectedOrganization.sourceControl.token.value !== null ||
            selectedOrganization.sourceControl.token.parentValue);
        if (orgId && hasToken && hostUrlResult && hostUrlResult.data.defaultHostUrl) {
          dispatch(loadRepositories(orgId, hostUrlResult.data.defaultHostUrl));
        }
      })
      .catch((error) => {
        dispatch(loadPageFailed(error));
      });
  };
}

export function loadPage(orgId) {
  function isRoutedFromAndToScmOnboarding(getState) {
    return (
      getState().router.currentState.name === 'scmOnboardingOrg' &&
      getState().router.prevState.name === 'scmOnboardingOrg'
    );
  }

  return function (dispatch, getState) {
    // retain state if navigating using router within the same page
    if (isRoutedFromAndToScmOnboarding(getState)) {
      // update form with values from router
      const selectedOrganization = getState().scmOnboarding.formState.organizations.find(
        (org) => org.organization.id === orgId
      );
      // When an org is added through the form it does not exists in the current list so the list has to be requested again
      if (selectedOrganization) {
        return dispatch(setSelectedOrganization(selectedOrganization));
      }
    }

    return dispatch(checkScmOnboardingPermissions(orgId))
      .then(() => dispatch(loadScmOnboardingInformation(orgId)))
      .catch((error) => Promise.reject(error));
  };
}

const validateScmHostUrlDebounceTimeout = 300;

const validateScmHostUrlDebounce = debounce((dispatch, scmProvider, scmHostUrl) => {
  dispatch(validateScmHostUrlRequested());

  return axios
    .get(getValidateScmConfigUrl(scmProvider, scmHostUrl))
    .then(({ data }) => {
      dispatch(validateScmHostUrlFulfilled(data));
    })
    .catch((error) => {
      dispatch(validateScmHostUrlFailed(error));
    });
}, validateScmHostUrlDebounceTimeout);

export function setSelectedOrganization(selectedOrg) {
  return function (dispatch, getState) {
    const state = getState().scmOnboarding;
    const isScmTokenOverridden = state.configState.isScmTokenOverridden;
    const previousOrg = state.formState.selectedOrganization;
    const orgId = selectedOrg.organization.id;
    const isSelectedTokenOverridden = selectedOrg.sourceControl.token.value != null;
    const isProviderOverridden = selectedOrg.sourceControl.provider.value != null;
    const provider = valueFromHierarchy(selectedOrg.sourceControl.provider);
    const hasToken = !!tokenForOrg(selectedOrg);
    dispatch(setTargetOrganizationRequested());
    if (!hasToken) {
      // no token, so can't query for anything
      dispatch(
        setTargetOrganizationFulfilled({
          selectedOrganization: selectedOrg,
          defaultHostUrl: null,
        })
      );
    } else if (isScmTokenOverridden || isProviderOverridden || isSelectedTokenOverridden || !previousOrg) {
      // newly selected org has a custom token, or previous one did, so requery for host URL, possibly reload repos
      return axios
        .get(getScmDefaultHostUrl(orgId, provider))
        .then(({ data }) => {
          dispatch(
            setTargetOrganizationFulfilled({
              selectedOrganization: selectedOrg,
              defaultHostUrl: data.defaultHostUrl,
            })
          );
          if (data.defaultHostUrl) {
            dispatch(loadRepositories(orgId, data.defaultHostUrl));
          }
          dispatch(ownerSideNavActions.setDisplayedOrganization(selectedOrg.organization));
          dispatch(rootActions.loadSelectedOwner());
        })
        .catch((error) => {
          dispatch(setTargetOrganizationFailed(error));
        });
    } else {
      // can use existing host URL
      dispatch(
        setTargetOrganizationFulfilled({
          selectedOrganization: selectedOrg,
          defaultHostUrl: state.formState.defaultHostUrl,
        })
      );
    }
    dispatch(ownerSideNavActions.setDisplayedOrganization(selectedOrg.organization));
    dispatch(rootActions.loadSelectedOwner());
  };
}

export function validateScmHostUrl(scmProvider, scmHostUrl) {
  return (dispatch) => validateScmHostUrlDebounce(dispatch, scmProvider, scmHostUrl);
}

export function loadRepositories(orgId, scmUrl) {
  return function (dispatch) {
    if (!scmUrl) {
      return;
    }
    dispatch(loadRepositoriesRequested());

    return axios
      .get(getScmRepositoriesUrl(orgId, scmUrl))
      .then(({ data }) => {
        dispatch(loadRepositoriesFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadRepositoriesFailed(error));
      });
  };
}

export function onRepositorySelectionChanged(repo) {
  return function (dispatch) {
    dispatch(repositorySelectionChanged(repo));
  };
}

export function importSelectedRepositories(orgId, totalRepoCount, prevImportedCount, selectedRepositories) {
  return function (dispatch) {
    dispatch(importSelectedRepositoriesRequested());

    let postBody = {
      totalRepoCount: totalRepoCount,
      prevImportedCount: prevImportedCount,
      scmRepositories: selectedRepositories,
    };

    return axios
      .post(getImportRepositoriesUrl(orgId), postBody)
      .then(({ data }) => {
        dispatch(importSelectedRepositoriesFulfilled(data));
      })
      .catch((error) => {
        dispatch(importSelectedRepositoriesFailed(error));
      });
  };
}

export function setSortingParameters(key, sortFields, dir) {
  return {
    type: SCM_ONBOARDING_SET_SORTING_PARAMETERS,
    payload: { key: key, sortFields: sortFields, dir: dir },
  };
}

export function setShowHostDialog(isShow) {
  return {
    type: SCM_ONBOARDING_SHOW_HOST_DIALOG,
    payload: isShow,
  };
}

export function setIsGitHostNeeded(isNeeded) {
  return {
    type: SCM_ONBOARDING_IS_GIT_HOST_NEEDED,
    payload: isNeeded,
  };
}

const checkPermissionForScmOnboardingRequested = noPayloadActionCreator(SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED);
const checkPermissionForScmOnboardingFulfilled = noPayloadActionCreator(SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED);
const checkPermissionForScmOnboardingFailed = payloadParamActionCreator(SCM_ONBOARDING_CHECK_PERMISSIONS_FAILED);

const loadPageRequested = payloadParamActionCreator(SCM_ONBOARDING_LOAD_PAGE_REQUESTED);
const loadPageFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_PAGE_FULFILLED);
const loadPageFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_PAGE_FAILED);

const loadRepositoriesRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED);
const loadRepositoriesFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED);
const loadRepositoriesFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED);
const repositorySelectionChanged = payloadParamActionCreator(SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED);

export const setTargetOrganizationRequested = noPayloadActionCreator(SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED);
export const setTargetOrganizationFulfilled = payloadParamActionCreator(
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED
);
export const setTargetOrganizationFailed = payloadParamActionCreator(SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED);

export const setIsNewOrganizationModalVisible = payloadParamActionCreator(
  SCM_ONBOARDING_SET_IS_NEW_ORGANIZATION_MODAL_VISIBLE
);

export const setCurrentHostUrl = payloadParamActionCreator(SCM_ONBOARDING_SET_CURRENT_HOST_URL);

const importSelectedRepositoriesRequested = noPayloadActionCreator(SCM_ONBOARDING_IMPORT_REPOS_REQUESTED);
const importSelectedRepositoriesFulfilled = payloadParamActionCreator(SCM_ONBOARDING_IMPORT_REPOS_FULFILLED);
const importSelectedRepositoriesFailed = payloadParamActionCreator(SCM_ONBOARDING_IMPORT_REPOS_FAILED);

const validateScmHostUrlRequested = noPayloadActionCreator(SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED);
const validateScmHostUrlFulfilled = payloadParamActionCreator(SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED);
const validateScmHostUrlFailed = payloadParamActionCreator(SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED);

export const setIsImportStatusDialogVisible = payloadParamActionCreator(SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE);

export default function scmOnboarding() {
  return {
    setSelectedOrganization,
    setCurrentHostUrl,
    validateScmHostUrl,
    loadPage,
    loadRepositories,
    onRepositorySelectionChanged,
    importSelectedRepositories,
    setSortingParameters,
    setShowHostDialog,
    setIsGitHostNeeded,
    setIsImportStatusDialogVisible,
  };
}
