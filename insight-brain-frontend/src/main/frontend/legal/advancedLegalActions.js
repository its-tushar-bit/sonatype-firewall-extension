/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getComponentMultiLicensesLegalReviewerUrl,
  getLicenseLegalComponentByComponentIdentifierUrl,
  getLicenseLegalComponentUrl,
  getLicenseOverrideLegalReviewerUrl,
  getLicensesWithSyntheticFilterUrl,
  getOwnerHierarchyLegalReviewerUrl,
} from '../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { processOwnerHierarchy } from '../util/hierarchyUtil';

export const ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED = 'ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED';
export const ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED';
export const ADVANCED_LEGAL_LOAD_COMPONENT_FAILED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FAILED';

export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED';
export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED';
export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED';

export const ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED = 'ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED';
export const ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED = 'ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED';
export const ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FAILED = 'ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FAILED';

export const ADVANCED_LEGAL_SET_LICENSE_FORM_COMMENT = 'ADVANCED_LEGAL_SET_LICENSE_FORM_COMMENT';
export const ADVANCED_LEGAL_SET_LICENSE_FORM_STATUS = 'ADVANCED_LEGAL_SET_LICENSE_FORM_STATUS';
export const ADVANCED_LEGAL_SET_LICENSE_FORM_SCOPE = 'ADVANCED_LEGAL_SET_LICENSE_FORM_SCOPE';
export const ADVANCED_LEGAL_SET_LICENSE_FORM_LICENSE_IDS = 'ADVANCED_LEGAL_SET_LICENSE_FORM_LICENSE_IDS';
export const ADVANCED_LEGAL_SET_SHOW_UNSAVED_CHANGES_MODAL = 'ADVANCED_LEGAL_SET_SHOW_UNSAVED_CHANGES_MODAL';
export const ADVANCED_LEGAL_SET_LICENSE_FORM_RESET_FORM_FIELDS = 'ADVANCED_LEGAL_SET_LICENSE_FORM_RESET_FORM_FIELDS';

const loadComponentRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
const loadComponentFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
const loadComponentFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FAILED);

const loadAvailableScopesRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED);
const loadAvailableScopesFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
const loadAvailableScopesFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED);

const loadMultiLicensesRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED);
const loadMultiLicensesFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED);
const loadMultiLicensesFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FAILED);

export const setLicenseFormComment = payloadParamActionCreator(ADVANCED_LEGAL_SET_LICENSE_FORM_COMMENT);
export const setLicenseFormStatus = payloadParamActionCreator(ADVANCED_LEGAL_SET_LICENSE_FORM_STATUS);
export const setLicenseFormScope = payloadParamActionCreator(ADVANCED_LEGAL_SET_LICENSE_FORM_SCOPE);
export const setLicenseFormLicenseIds = payloadParamActionCreator(ADVANCED_LEGAL_SET_LICENSE_FORM_LICENSE_IDS);
export const setShowUnsavedChangesModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_UNSAVED_CHANGES_MODAL);
export const setLicenseFormResetFormFields = noPayloadActionCreator(ADVANCED_LEGAL_SET_LICENSE_FORM_RESET_FORM_FIELDS);

export function loadComponent(orgOrApp, ownerId, hash) {
  return (dispatch) => {
    dispatch(loadComponentRequested());

    return axios
      .get(getLicenseLegalComponentUrl(orgOrApp, ownerId, hash))
      .then(({ data }) => {
        const componentIdentifier = JSON.stringify(data.component.componentIdentifier);
        dispatch(loadMultiLicenses(orgOrApp, ownerId, hash, componentIdentifier));
        dispatch(loadComponentFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadComponentFailed(error));
      });
  };
}
export function loadComponentByComponentIdentifier(componentIdentifier, parameters) {
  return (dispatch) => {
    dispatch(loadComponentRequested());

    return axios
      .get(
        getLicenseLegalComponentByComponentIdentifierUrl(componentIdentifier, parameters?.orgOrApp, parameters?.ownerId)
      )
      .then(({ data }) => {
        const componentIdentifierFromData = JSON.stringify(data.component.componentIdentifier);
        if (parameters?.repositoryId) {
          dispatch(loadMultiLicensesByRepositoryId(componentIdentifierFromData, parameters.repositoryId));
        } else if (parameters?.orgOrApp && parameters?.ownerId) {
          dispatch(loadMultiLicenses(parameters.orgOrApp, parameters.ownerId, undefined, componentIdentifierFromData));
        }
        dispatch(loadComponentFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadComponentFailed(error));
      });
  };
}

export function loadAvailableScopes(ownerType, ownerId) {
  return (dispatch) => {
    dispatch(loadAvailableScopesRequested());

    return axios
      .get(getOwnerHierarchyLegalReviewerUrl(ownerType, ownerId))
      .then(({ data }) => {
        let payload = {
          values: processOwnerHierarchy(data),
        };
        dispatch(loadAvailableScopesFulfilled(payload));
      })
      .catch((error) => {
        dispatch(loadAvailableScopesFailed(error));
      });
  };
}

export function loadMultiLicenses(orgOrApp, ownerId, hash, componentIdentifier) {
  return (dispatch) => {
    dispatch(loadMultiLicensesRequested());
    const promises = [
      axios.get(getLicensesWithSyntheticFilterUrl()),
      axios.get(
        getComponentMultiLicensesLegalReviewerUrl({
          clientType: 'ci',
          ownerType: orgOrApp,
          ownerId,
          componentIdentifier,
        })
      ),
      axios.get(getLicenseOverrideLegalReviewerUrl(orgOrApp, ownerId, componentIdentifier)),
    ];

    return Promise.all(promises)
      .then((results) => {
        dispatch(loadMultiLicensesFulfilled(results));
      })
      .catch((error) => {
        dispatch(loadMultiLicensesFailed(error));
      });
  };
}

export function loadMultiLicensesByRepositoryId(componentIdentifier, repositoryId) {
  return (dispatch) => {
    dispatch(loadMultiLicensesRequested());
    const promises = [
      axios.get(getLicensesWithSyntheticFilterUrl()),
      axios.get(
        getComponentMultiLicensesLegalReviewerUrl({
          clientType: 'ci',
          ownerType: 'repository',
          ownerId: repositoryId,
          componentIdentifier,
        })
      ),
      axios.get(getLicenseOverrideLegalReviewerUrl('repository', repositoryId, componentIdentifier)),
    ];

    return Promise.all(promises)
      .then((results) => {
        dispatch(loadMultiLicensesFulfilled(results));
      })
      .catch((error) => {
        dispatch(loadMultiLicensesFailed(error));
      });
  };
}
