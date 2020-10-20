/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map, pick } from 'ramda';
import axios from 'axios';

import { createReportEntries, createRawDataEntries } from './applicationReportService';
import { mappedPayloadParamActionCreator, noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import {
  getReportMetadataUrl,
  getReportBomUrl,
  getReportUnknownJsUrl,
  getExpandedCoverageEmbeddableUrl,
  getReportPolicyThreatsUrl,
  getReportDataUrl,
  getReportPartialMatchedUrl,
  getDependenciesUrl,
  getReportSecurityUrl,
  getReportLicenseUrl,
  getReportReevaluateUrl,
  redirectTo
} from '../util/CLMLocation';
import { Messages } from '../util/CommonServices';
import { stateGo } from '../reduxUiRouter/routerActions';

export const LOAD_REPORT_REQUESTED = 'LOAD_REPORT_REQUESTED';
export const LOAD_REPORT_FULFILLED = 'LOAD_REPORT_FULFILLED';
export const LOAD_REPORT_FAILED = 'LOAD_REPORT_FAILED';
export const LOAD_REPORT_UNNECESSARY = 'LOAD_REPORT_UNNECESSARY';
export const LOAD_REPORT_RAW_DATA_REQUESTED = 'LOAD_REPORT_RAW_DATA_REQUESTED';
export const LOAD_REPORT_RAW_DATA_FULFILLED = 'LOAD_REPORT_RAW_DATA_FULFILLED';
export const LOAD_REPORT_RAW_DATA_FAILED = 'LOAD_REPORT_RAW_DATA_FAILED';
export const LOAD_REPORT_RAW_DATA_UNNECESSARY = 'LOAD_REPORT_RAW_DATA_UNNECESSARY';
export const LOAD_COMMON_DATA_FULFILLED = 'LOAD_COMMON_DATA_FULFILLED';
export const LOAD_COMMON_DATA_FAILED = 'LOAD_COMMON_DATA_FAILED';
export const LOAD_COMMON_DATA_UNNECESSARY = 'LOAD_COMMON_DATA_UNNECESSARY';
export const LOAD_REPORT_ALL_DATA_REQUESTED = 'LOAD_REPORT_ALL_DATA_REQUESTED';
export const SET_AGGREGATE_REPORT_ENTRIES = 'SET_AGGREGATE_REPORT_ENTRIES';
export const SET_REPORT_PARAMETERS = 'SET_REPORT_PARAMETERS';
export const SELECT_COMPONENT = 'SELECT_COMPONENT';
export const SELECT_ROOT_ANCESTOR = 'SELECT_ROOT_ANCESTOR';
export const UNSELECT_ROOT_ANCESTOR = 'UNSELECT_ROOT_ANCESTOR';
export const REEVALUATE_REPORT_REQUESTED = 'REEVALUATE_REPORT_REQUESTED';
export const REEVALUATE_REPORT_FULFILLED = 'REEVALUATE_REPORT_FULFILLED';
export const REEVALUATE_REPORT_FAILED = 'REEVALUATE_REPORT_FAILED';
export const REEVALUATE_REPORT_CANCELLED = 'REEVALUATE_REPORT_CANCELLED';
export const GENERATE_VULNERABILITY_ENTRIES = 'GENERATE_VULNERABILITY_ENTRIES';
export const SET_SORTING_PARAMETERS = 'SET_SORTING_PARAMETERS';

// To be used for filters that are done by substring matching, as opposed to matching a discrete set of values
export const SET_SUBSTRING_FIELD_FILTER = 'SET_SUBSTRING_FIELD_FILTER';
export const SET_EXACT_VALUE_FILTER = 'SET_EXACT_VALUE_FILTER';
export const SET_RAW_DATA_SUBSTRING_FIELD_FILTER = 'SET_RAW_DATA_SUBSTRING_FIELD_FILTER';
export const SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER = 'SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER';
export const SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER = 'SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER';
export const SET_SORTING = 'SET_SORTING';
export const SET_SORTING_RAW_DATA = 'SET_SORTING_RAW_DATA';

export function setReportParameters(appId, scanId, isUnknownJs, embeddable, policyViolationId) {
  return {
    type: SET_REPORT_PARAMETERS,
    payload: { appId, scanId, isUnknownJs, embeddable, policyViolationId }
  };
}

export function setSortingParameters(key, sortFields, dir) {
  return {
    type: SET_SORTING_PARAMETERS,
    payload: { key, sortFields, dir }
  };
}

function fetchCommonData(forceClearMetadata = false) {
  return (dispatch, getState) => {
    const {bomData, unknownJsData, metadata, reportParameters} = getState().applicationReport;
    const {appId, scanId, isUnknownJs, embeddable} = reportParameters;

    if (forceClearMetadata || (!metadata || !bomData || (!unknownJsData && isUnknownJs))) {
      const promises = [
        axios.get(getReportBomUrl(appId, scanId)),
        axios.get(getReportMetadataUrl(appId, scanId))
      ];

      if (isUnknownJs) {
        promises.push(axios.get(getReportUnknownJsUrl(appId, scanId)));
      }

      return Promise.all(promises)
          .then((results) => {
            const bomResult = results[0].data || undefined;
            const metadataResult = results[1].data;
            const unknownJsResult = (isUnknownJs && results[2].data) || undefined;

            if (metadataResult.expandedCoverage) {
              // this is an Expanded Coverage report and should not be viewed on the Policy Centric app report
              // page. Redirect to the old report page, or if embeddable was requested, then to the iframe URL
              if (embeddable) {
                redirectTo(getExpandedCoverageEmbeddableUrl(appId, scanId));
              }
              else {
                dispatch(stateGo('report', {
                  publicId: appId,
                  scanId
                }));
              }

              return Promise.reject('XC Report');
            }
            else {
              return dispatch(loadCommonDataFulfilled({
                bomData: bomResult,
                metadata: metadataResult,
                unknownJsData: unknownJsResult
              }));
            }
          })
          .catch(error => {
            if (error !== 'XC Report') {
              dispatch(loadCommonDataFailed(error));
            }

            return Promise.reject(error);
          });
    }

    return Promise.resolve(dispatch(loadCommonDataUnnecessary()));
  };
}

function fetchReportData(forceReload = true) {
  return (dispatch, getState) => {
    const { bomData, unknownJsData, reportParameters, selectedReport } = getState().applicationReport;
    const { appId, scanId } = reportParameters;

    if (forceReload || !selectedReport) {
      const promises = [
        axios.get(getReportPolicyThreatsUrl(appId, scanId)),
        axios.get(getReportDataUrl(appId, scanId)),
        axios.get(getReportPartialMatchedUrl(appId, scanId)),
        axios.get(getDependenciesUrl(appId, scanId))
      ];

      return Promise.all(promises)
          .then((results) => {
            const policyResult = results[0].data || undefined;
            const dataResult = results[1].data;
            const partialMatches = results[2].data || undefined;
            const dependencies = results[3].data;

            const allEntries = createReportEntries(policyResult, bomData, unknownJsData, partialMatches,
                dependencies);
            const reportVersion = policyResult && policyResult.version || null;
            return dispatch(loadReportFulfilled({
              allEntries: allEntries.policies,
              isInnerSourceEnabled: allEntries.isInnerSourceEnabled,
              reportVersion,
              ...dataResult
            }));
          })
          .catch(error => {
            dispatch(loadReportFailed(error));
            return Promise.reject(error);
          });
    }
    else {
      return Promise.resolve(dispatch(loadReportUnnecessary()));
    }
  };
}

function fetchReportRawData(forceReload = true) {
  return (dispatch, getState) => {
    const {bomData, unknownJsData, reportParameters, reportRawData } = getState().applicationReport;
    const { appId, scanId } = reportParameters;

    if (forceReload || !reportRawData) {
      const promises = [
        axios.get(getReportSecurityUrl(appId, scanId)),
        axios.get(getReportLicenseUrl(appId, scanId))
      ];

      return Promise.all(promises)
          .then((results) => {
            const securityResult = results[0].data;
            const licenseResult = results[1].data;
            const allEntries = createRawDataEntries(securityResult, licenseResult, bomData, unknownJsData);
            return dispatch(loadReportRawDataFulfilled(allEntries));
          })
          .catch(error => {
            dispatch(loadReportRawDataFailed(error));
            return Promise.reject(error);
          });
    }
    else {
      return Promise.resolve(dispatch(loadReportRawDataUnnecessary()));
    }
  };
}

export function loadReport(forceClearMetadata = false) {
  return (dispatch) => {
    dispatch({
      type: LOAD_REPORT_REQUESTED
    });

    return dispatch(fetchCommonData(forceClearMetadata)).then(() => dispatch(fetchReportData()));
  };
}

function loadReportRawData() {
  return (dispatch) => {
    dispatch({
      type: LOAD_REPORT_RAW_DATA_REQUESTED
    });
    return dispatch(fetchCommonData()).then(() => dispatch(fetchReportRawData()));
  };
}

function loadReportAllData() {
  return (dispatch) => {
    dispatch({
      type: LOAD_REPORT_ALL_DATA_REQUESTED
    });
    return dispatch(fetchCommonData())
        .then(() => Promise.all(map(dispatch, [fetchReportRawData(false), fetchReportData(false)])))
        .then(() => dispatch(generateVulnerabilityEntries()));
  };
}

const httpErrorMessageActionCreator = type => mappedPayloadParamActionCreator(type, Messages.getHttpErrorMessage);

const loadCommonDataFulfilled = mappedPayloadParamActionCreator(LOAD_COMMON_DATA_FULFILLED,
    pick(['bomData', 'metadata', 'unknownJsData']));

const loadCommonDataFailed = httpErrorMessageActionCreator(LOAD_COMMON_DATA_FAILED);
const loadCommonDataUnnecessary = noPayloadActionCreator(LOAD_COMMON_DATA_UNNECESSARY);
const loadReportFulfilled = payloadParamActionCreator(LOAD_REPORT_FULFILLED);
const loadReportFailed = httpErrorMessageActionCreator(LOAD_REPORT_FAILED);
const loadReportUnnecessary = httpErrorMessageActionCreator(LOAD_REPORT_UNNECESSARY);
const loadReportRawDataFulfilled = payloadParamActionCreator(LOAD_REPORT_RAW_DATA_FULFILLED);
const loadReportRawDataFailed = httpErrorMessageActionCreator(LOAD_REPORT_RAW_DATA_FAILED);
const loadReportRawDataUnnecessary = httpErrorMessageActionCreator(LOAD_REPORT_RAW_DATA_UNNECESSARY);
const setSortingRawData = payloadParamActionCreator(SET_SORTING_RAW_DATA);
const generateVulnerabilityEntries = noPayloadActionCreator(GENERATE_VULNERABILITY_ENTRIES);
export const setSorting = payloadParamActionCreator(SET_SORTING);

export const setAggregateReportEntries = payloadParamActionCreator(SET_AGGREGATE_REPORT_ENTRIES);

export function setStringFieldFilter(fieldName, filterString) {
  return {
    type: SET_SUBSTRING_FIELD_FILTER,
    payload: { fieldName, filterString }
  };
}

function setRawDataStringFieldFilter(fieldName, filterString) {
  return {
    type: SET_RAW_DATA_SUBSTRING_FIELD_FILTER,
    payload: { fieldName, filterString }
  };
}

function setRawDataNumericMaxFilter(fieldName, filterValue) {
  return {
    type: SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER,
    payload: { fieldName, filterValue }
  };
}

function setRawDataNumericMinFilter(fieldName, filterValue) {
  return {
    type: SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER,
    payload: { fieldName, filterValue }
  };
}

export function setExactValueFilter(fieldName, allowedValues) {
  return {
    type: SET_EXACT_VALUE_FILTER,
    payload: { fieldName, allowedValues }
  };
}

const selectComponent = payloadParamActionCreator(SELECT_COMPONENT);
const selectRootAncestor = payloadParamActionCreator(SELECT_ROOT_ANCESTOR);
const unselectRootAncestor = noPayloadActionCreator(UNSELECT_ROOT_ANCESTOR);

export function reevaluateReport() {
  return (dispatch, getState) => {
    const { scanId, appId } = getState().applicationReport.reportParameters;

    dispatch({
      type: REEVALUATE_REPORT_REQUESTED
    });

    return axios.post(getReportReevaluateUrl(appId, scanId))
        .catch(error => {
          dispatch(reevaluateReportFailed(error));
          return Promise.reject(error);
        })
        .then(() => {
          dispatch(reevaluateReportFulfilled());
          return dispatch(loadReport(true));
        });
  };
}

const reevaluateReportFulfilled = noPayloadActionCreator(REEVALUATE_REPORT_FULFILLED);
const reevaluateReportFailed = httpErrorMessageActionCreator(REEVALUATE_REPORT_FAILED);
const reevaluateReportCancelled = noPayloadActionCreator(REEVALUATE_REPORT_CANCELLED);

export default function applicationReportActions() {
  return {
    setReportParameters,
    loadReport,
    loadReportRawData,
    loadReportAllData,
    reevaluateReport,
    reevaluateReportCancelled,
    setAggregateReportEntries,
    setStringFieldFilter,
    setExactValueFilter,
    setRawDataStringFieldFilter,
    setRawDataNumericMaxFilter,
    setRawDataNumericMinFilter,
    setSorting,
    setSortingRawData,
    selectComponent,
    selectRootAncestor,
    unselectRootAncestor,
    setSortingParameters
  };
}
