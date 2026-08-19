/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { find, propEq } from 'ramda';
import { createReducerFromActionMap, propSetConst } from '../../../util/reduxUtil';
import { combineValidators, validateDoubleWhitespace, validateNonEmpty } from '../../../util/validationUtil';
import {
  ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED,
  ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED,
  ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED,
  ADD_SUCCESS_METRICS_REPORT_SET_INCLUDE_LATEST_DATA,
  ADD_SUCCESS_METRICS_REPORT_SET_IS_ALL_APPLICATIONS,
  ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS,
  ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_MASK_STATE_DONE,
  ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED,
} from './addSuccessMetricsReportActions';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

export const initialState = {
  loading: true,
  organizations: [],
  applications: [],
  loadError: null,
  submitError: null,
  reportName: initUserInput(''),
  includeLatestData: false,
  isAllApplications: true,
  selectedOrgsAndApps: {
    organizations: new Set(),
    applications: new Set(),
  },
};

function loadOrgsAndAppsFulfilled(payload, state) {
  return {
    ...state,
    ...payload,
    loading: false,
  };
}

function loadOrgsAndAppsFailed(payload, state) {
  return {
    ...state,
    loadError: payload,
  };
}

function setOrgsAndApps(payload, state) {
  const { selectedOrganizations: organizations, selectedApplications: applications } = { ...payload };
  return {
    ...state,
    selectedOrgsAndApps: {
      organizations,
      applications,
    },
  };
}

function setReportName({ value: payload, reports }, state) {
  const duplicationValidator = (value) => {
    const isDuplicate = propEq('name', value.toLowerCase());
    const mappedReports = reports.map(({ name, ...otherProps }) => ({ ...otherProps, name: name.toLowerCase() }));
    return find(isDuplicate, mappedReports) ? 'Name is already in use' : null;
  };
  const validators = combineValidators([duplicationValidator, validateNonEmpty, validateDoubleWhitespace]);
  const reportName = userInput(validators, payload);
  return {
    ...state,
    reportName,
  };
}

function setIncludeLatestData(payload, state) {
  return {
    ...state,
    includeLatestData: payload,
  };
}

function setIsAllApplications(payload, state) {
  return {
    ...state,
    isAllApplications: payload,
  };
}

function submitFailed(payload, state) {
  return {
    ...state,
    submitError: payload,
    submitMaskState: null,
  };
}

const reducerActionMap = {
  [ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED]: () => initialState,
  [ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED]: loadOrgsAndAppsFulfilled,
  [ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED]: loadOrgsAndAppsFailed,
  [ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS]: setOrgsAndApps,
  [ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME]: setReportName,
  [ADD_SUCCESS_METRICS_REPORT_SET_INCLUDE_LATEST_DATA]: setIncludeLatestData,
  [ADD_SUCCESS_METRICS_REPORT_SET_IS_ALL_APPLICATIONS]: setIsAllApplications,
  [ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED]: propSetConst('submitMaskState', false),
  [ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED]: propSetConst('submitMaskState', true),
  [ADD_SUCCESS_METRICS_REPORT_SUBMIT_MASK_STATE_DONE]: propSetConst('submitMaskState', null),
  [ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED]: submitFailed,
};

export default createReducerFromActionMap(reducerActionMap, initialState);
