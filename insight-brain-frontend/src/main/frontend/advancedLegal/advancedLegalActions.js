/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getApplicationsUrl,
  getLicenseLegalApplicationReportUrl,
  getLicenseLegalComponentUrl,
  getOwnerHierarchyUrl
} from '../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { capitalize } from '../util/jsUtil';

export const ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED = 'ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED';
export const ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED = 'ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED';
export const ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED = 'ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED';

export const ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED = 'ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED';
export const ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED = 'ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED';
export const ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED = 'ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED';

export const ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED = 'ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED';
export const ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED';
export const ADVANCED_LEGAL_LOAD_COMPONENT_FAILED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FAILED';

export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED';
export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED';
export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED';

const loadApplicationsRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED);
const loadApplicationsFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED);
const loadApplicationsFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED);

const loadApplicationReportRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED);
const loadApplicationReportFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED);
const loadApplicationReportFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED);

const loadComponentRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
const loadComponentFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
const loadComponentFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FAILED);

const loadAvailableScopesRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED);
const loadAvailableScopesFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
const loadAvailableScopesFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED);

export function loadApplications() {
  return (dispatch) => {
    dispatch(loadApplicationsRequested());

    return axios.get(getApplicationsUrl())
        .then(({ data }) => {
          dispatch(loadApplicationsFulfilled(data));
        })
        .catch(error => {
          dispatch(loadApplicationsFailed(error));
        });
  };
}

export function loadApplicationReport(publicId) {
  return (dispatch) => {
    dispatch(loadApplicationReportRequested());

    return axios.get(getLicenseLegalApplicationReportUrl(publicId))
        .then(({ data }) => {
          dispatch(loadApplicationReportFulfilled(data));
        })
        .catch(error => {
          dispatch(loadApplicationReportFailed(error));
        });
  };
}

export function loadComponent(orgOrApp, ownerId, hash) {
  return (dispatch) => {
    dispatch(loadComponentRequested());

    return axios.get(getLicenseLegalComponentUrl(orgOrApp, ownerId, hash))
        .then(({ data }) => {
          dispatch(loadComponentFulfilled(data));
        })
        .catch(error => {
          dispatch(loadComponentFailed(error));
        });
  };
}

export function loadAvailableScopes(ownerType, ownerId) {
  return (dispatch) => {
    dispatch(loadAvailableScopesRequested());

    return axios.get(getOwnerHierarchyUrl(ownerType, ownerId))
        .then(({ data }) => {
          let payload = {
            values: processOwnerHierarchy(data)
          };
          dispatch(loadAvailableScopesFulfilled(payload));
        })
        .catch(error => {
          dispatch(loadAvailableScopesFailed(error));
        });
  };
}

/**
 * Flattens the Org/Apps hierarchy
 */
function processOwnerHierarchy(context) {
  // note that since the context data only includes the ancestors of the owner, `children` should
  // never have more than one element
  const processedChildren = context.children ? processOwnerHierarchy(context.children[0]) : [],
      { type, id, publicId, name } = context,
      label = capitalize(type);

  return processedChildren.concat({ type, id, publicId, name, label });
}
