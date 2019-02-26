/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReportEntries, createRawDataEntries } from './applicationReportService';

export const LOAD_REPORT_REQUESTED = 'LOAD_REPORT_REQUESTED';
export const LOAD_REPORT_FULFILLED = 'LOAD_REPORT_FULFILLED';
export const LOAD_REPORT_FAILED = 'LOAD_REPORT_FAILED';
export const RELOAD_REPORT_REQUESTED = 'RELOAD_REPORT_REQUESTED';
export const LOAD_REPORT_RAW_DATA_REQUESTED = 'LOAD_REPORT_RAW_DATA_REQUESTED';
export const LOAD_REPORT_RAW_DATA_FULFILLED = 'LOAD_REPORT_RAW_DATA_FULFILLED';
export const LOAD_REPORT_RAW_DATA_FAILED = 'LOAD_REPORT_RAW_DATA_FAILED';
export const LOAD_COMMON_DATA_FULFILLED = 'LOAD_COMMON_DATA_FULFILLED';
export const LOAD_COMMON_DATA_FAILED = 'LOAD_COMMON_DATA_FAILED';
export const SET_AGGREGATE_REPORT_ENTRIES = 'SET_AGGREGATE_REPORT_ENTRIES';
export const SET_REPORT_PARAMETERS = 'SET_REPORT_PARAMETERS';
export const SELECT_COMPONENT = 'SELECT_COMPONENT';
export const REEVALUATE_REPORT_REQUESTED = 'REEVALUATE_REPORT_REQUESTED';
export const REEVALUATE_REPORT_FULFILLED = 'REEVALUATE_REPORT_FULFILLED';
export const REEVALUATE_REPORT_FAILED = 'REEVALUATE_REPORT_FAILED';
export const REEVALUATE_REPORT_CANCELLED = 'REEVALUATE_REPORT_CANCELLED';

// To be used for filters that are done by substring matching, as opposed to matchings a discreet set of values
export const SET_SUBSTRING_FIELD_FILTER = 'SET_SUBSTRING_FIELD_FILTER';
export const SET_EXACT_VALUE_FILTER = 'SET_EXACT_VALUE_FILTER';
export const SET_SORTING = 'SET_SORTING';

export default function applicationReportActions($http, $q, CLMLocations, Messages) {

  function setReportParameters(appId, scanId, isUnknownJs) {
    return {
      type: SET_REPORT_PARAMETERS,
      payload: { appId, scanId, isUnknownJs }
    };
  }

  function fetchCommonData() {
    return (dispatch, getState) => {
      const {bomData, unknownJsData, metadata, reportParameters} = getState().applicationReport;
      const {appId, scanId, isUnknownJs} = reportParameters;

      if (!metadata || !bomData || (!unknownJsData && isUnknownJs)) {
        const promises = {
          bomResult: $http.get(CLMLocations.getReportBomUrl(appId, scanId)),
          metadata: $http.get(CLMLocations.getReportMetadataUrl(appId, scanId))
        };

        if (isUnknownJs) {
          promises.unknownJsResult = $http.get(CLMLocations.getReportUnknownJsUrl(appId, scanId));
        }

        return $q.all(promises)
            .then((results) => {
              const bomResult = results.bomResult.data || undefined;
              const metadataResult = results.metadata.data;
              const unknownJsResult = (isUnknownJs && results.unknownJsResult.data) || undefined;
              return dispatch(loadCommonDataFulfilled({
                bomData: bomResult,
                metadata: metadataResult,
                unknownJsData: unknownJsResult
              }));
            })
            .catch(error => {
              dispatch(loadCommonDataFailed(error));
              return $q.reject(error);
            });
      }

      return $q.resolve();
    };
  }

  function fetchReportData() {
    return (dispatch, getState) => {
      const { bomData, unknownJsData, reportParameters } = getState().applicationReport;
      const { appId, scanId } = reportParameters;
      const promises = {
        policyResult: $http.get(CLMLocations.getReportPolicyThreatsUrl(appId, scanId)),
        dataResult: $http.get(CLMLocations.getReportDataUrl(appId, scanId)),
        partialMatches: $http.get(CLMLocations.getReportPartialMatchedUrl(appId, scanId))
      };

      return $q.all(promises)
          .then((results) => {
            const policyResult = results.policyResult.data || undefined;
            const dataResult = results.dataResult.data;
            const partialMatches = results.partialMatches.data || undefined;

            const allEntries = createReportEntries(policyResult, bomData, unknownJsData, partialMatches);
            const reportVersion = policyResult && policyResult.version || null;
            return dispatch(loadReportFulfilled({ allEntries, reportVersion, ...dataResult }));
          })
          .catch(error => {
            dispatch(loadReportFailed(error));
            return $q.reject(error);
          });
    };
  }

  function fetchReportRawData() {
    return (dispatch, getState) => {
      const {bomData, unknownJsData, reportParameters} = getState().applicationReport;
      const { appId, scanId } = reportParameters;
      const promises = {
        securityResult: $http.get(CLMLocations.getReportSecurityUrl(appId, scanId)),
        licenseResult: $http.get(CLMLocations.getReportLicenseUrl(appId, scanId))
      };

      return $q.all(promises)
          .then((results) => {
            const securityResult = results.securityResult.data;
            const licenseResult = results.licenseResult.data;
            const allEntries = createRawDataEntries(securityResult, licenseResult, bomData, unknownJsData);
            return dispatch(loadReportRawDataFulfilled(allEntries));
          })
          .catch(error => {
            dispatch(loadReportRawDataFailed(error));
            return $q.reject(error);
          });
    };
  }

  function loadReport() {
    return (dispatch) => {
      dispatch({
        type: LOAD_REPORT_REQUESTED
      });

      return dispatch(fetchCommonData()).then(() => dispatch(fetchReportData()));
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

  function reloadReport() {
    return (dispatch) => {
      dispatch({
        type: RELOAD_REPORT_REQUESTED
      });
      return dispatch(fetchCommonData()).then(() => dispatch(fetchReportData()));
    };
  }

  function loadCommonDataFulfilled({ bomData, metadata, unknownJsData }) {
    return {
      type: LOAD_COMMON_DATA_FULFILLED,
      payload: { bomData, metadata, unknownJsData }
    };
  }

  function loadCommonDataFailed(error) {
    return {
      type: LOAD_COMMON_DATA_FAILED,
      payload: Messages.getHttpErrorMessage(error)
    };
  }

  function loadReportFulfilled(payload) {
    return {
      type: LOAD_REPORT_FULFILLED,
      payload
    };
  }

  function loadReportFailed(error) {
    return {
      type: LOAD_REPORT_FAILED,
      payload: Messages.getHttpErrorMessage(error)
    };
  }

  function loadReportRawDataFulfilled(payload) {
    return {
      type: LOAD_REPORT_RAW_DATA_FULFILLED,
      payload
    };
  }

  function loadReportRawDataFailed(error) {
    return {
      type: LOAD_REPORT_RAW_DATA_FAILED,
      payload: Messages.getHttpErrorMessage(error)
    };
  }

  /**
   * @param isAggregated a boolean for whether or not to aggregate
   */
  function setAggregateReportEntries(isAggregated) {
    return {
      type: SET_AGGREGATE_REPORT_ENTRIES,
      payload: isAggregated
    };
  }

  function setSorting(sortFields) {
    return {
      type: SET_SORTING,
      payload: sortFields
    };
  }

  function setStringFieldFilter(fieldName, filterString) {
    return {
      type: SET_SUBSTRING_FIELD_FILTER,
      payload: { fieldName, filterString }
    };
  }

  function setExactValueFilter(fieldName, allowedValues) {
    return {
      type: SET_EXACT_VALUE_FILTER,
      payload: { fieldName, allowedValues }
    };
  }

  function selectComponent(index) {
    return {
      type: SELECT_COMPONENT,
      payload: index
    };
  }

  function reevaluateReport() {
    return (dispatch, getState) => {
      const { scanId, appId } = getState().applicationReport.reportParameters;

      dispatch({
        type: REEVALUATE_REPORT_REQUESTED
      });

      return $http.post(CLMLocations.getReportReevaluateUrl(appId, scanId))
          .catch(error => {
            dispatch(reevaluateReportFailed(error));
            return $q.reject(error);
          })
          .then(() => {
            dispatch(reevaluateReportFulfilled());
            return dispatch(loadReport());
          });
    };
  }

  function reevaluateReportFulfilled() {
    return {
      type: REEVALUATE_REPORT_FULFILLED
    };
  }

  function reevaluateReportFailed(error) {
    return {
      type: REEVALUATE_REPORT_FAILED,
      payload: Messages.getHttpErrorMessage(error)
    };
  }

  function reevaluateReportCancelled() {
    return {
      type: REEVALUATE_REPORT_CANCELLED
    };
  }

  return {
    setReportParameters,
    loadReport,
    loadReportRawData,
    reloadReport,
    reevaluateReport,
    reevaluateReportCancelled,
    setAggregateReportEntries,
    setStringFieldFilter,
    setExactValueFilter,
    setSorting,
    selectComponent
  };
}

applicationReportActions.$inject = ['$http', '$q', 'CLMLocations', 'Messages'];
