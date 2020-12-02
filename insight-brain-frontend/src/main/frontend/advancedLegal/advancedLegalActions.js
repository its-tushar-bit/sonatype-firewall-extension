/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getApplicationsUrl,
  getLicenseLegalApplicationReportUrl,
  getLicenseLegalComponentUrl
} from '../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';

export const ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED = 'ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED';
export const ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED = 'ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED';
export const ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED = 'ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED';

export const ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED = 'ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED';
export const ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED = 'ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED';
export const ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED = 'ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED';

export const ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED = 'ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED';
export const ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED';
export const ADVANCED_LEGAL_LOAD_COMPONENT_FAILED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FAILED';

const loadApplicationsRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED);
const loadApplicationsFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED);
const loadApplicationsFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED);

const loadApplicationReportRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED);
const loadApplicationReportFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED);
const loadApplicationReportFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED);

const loadComponentRequested = noPayloadActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
const loadComponentFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
const loadComponentFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FAILED);

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
