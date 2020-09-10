/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map, pick } from 'ramda';

import { createReportEntries, createRawDataEntries } from './applicationReportService';
import { mappedPayloadParamActionCreator, noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';

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

// To be used for filters that are done by substring matching, as opposed to matching a discrete set of values
export const SET_SUBSTRING_FIELD_FILTER = 'SET_SUBSTRING_FIELD_FILTER';
export const SET_EXACT_VALUE_FILTER = 'SET_EXACT_VALUE_FILTER';
export const SET_RAW_DATA_SUBSTRING_FIELD_FILTER = 'SET_RAW_DATA_SUBSTRING_FIELD_FILTER';
export const SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER = 'SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER';
export const SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER = 'SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER';
export const SET_SORTING = 'SET_SORTING';
export const SET_SORTING_RAW_DATA = 'SET_SORTING_RAW_DATA';

export default function applicationReportActions($http, $q, $state, $window, CLMLocations, Messages) {

  function setReportParameters(appId, scanId, isUnknownJs, embeddable, policyViolationId) {
    return {
      type: SET_REPORT_PARAMETERS,
      payload: { appId, scanId, isUnknownJs, embeddable, policyViolationId }
    };
  }

  function fetchCommonData(forceClearMetadata = false) {
    return (dispatch, getState) => {
      const {bomData, unknownJsData, metadata, reportParameters} = getState().applicationReport;
      const {appId, scanId, isUnknownJs, embeddable} = reportParameters;

      if (forceClearMetadata || (!metadata || !bomData || (!unknownJsData && isUnknownJs))) {
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

              if (metadataResult.expandedCoverage) {
                // this is an Expanded Coverage report and should not be viewed on the Policy Centric app report
                // page. Redirect to the old report page, or if embeddable was requested, then to the iframe URL
                if (embeddable) {
                  $window.location = CLMLocations.getExpandedCoverageEmbeddableUrl(appId, scanId);
                }
                else {
                  $state.go('report', {
                    publicId: appId,
                    scanId
                  });
                }

                return $q.reject('XC Report');
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

              return $q.reject(error);
            });
      }

      return $q.resolve(dispatch(loadCommonDataUnnecessary()));
    };
  }

  function fetchReportData(forceReload = true) {
    return (dispatch, getState) => {
      const { bomData, unknownJsData, reportParameters, selectedReport } = getState().applicationReport;
      const { appId, scanId } = reportParameters;

      if (forceReload || !selectedReport) {
        const promises = {
          policyResult: $http.get(CLMLocations.getReportPolicyThreatsUrl(appId, scanId)),
          dataResult: $http.get(CLMLocations.getReportDataUrl(appId, scanId)),
          partialMatches: $http.get(CLMLocations.getReportPartialMatchedUrl(appId, scanId)),
          dependencies: $http.get(CLMLocations.getDependenciesUrl(appId, scanId))
        };

        return $q.all(promises)
            .then((results) => {
              const policyResult = results.policyResult.data || undefined;
              const dataResult = results.dataResult.data;
              const partialMatches = results.partialMatches.data || undefined;
              const dependencies = results.dependencies.data;

              const allEntries = createReportEntries(policyResult, bomData, unknownJsData, partialMatches,
                  dependencies);
              const reportVersion = policyResult && policyResult.version || null;
              return dispatch(loadReportFulfilled({ allEntries, reportVersion, ...dataResult }));
            })
            .catch(error => {
              dispatch(loadReportFailed(error));
              return $q.reject(error);
            });
      }
      else {
        return $q.resolve(dispatch(loadReportUnnecessary()));
      }
    };
  }

  function fetchReportRawData(forceReload = true) {
    return (dispatch, getState) => {
      const {bomData, unknownJsData, reportParameters, reportRawData } = getState().applicationReport;
      const { appId, scanId } = reportParameters;

      if (forceReload || !reportRawData) {
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
      }
      else {
        return $q.resolve(dispatch(loadReportRawDataUnnecessary()));
      }
    };
  }

  function loadReport(forceClearMetadata = false) {
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
          .then(() => $q.all(map(dispatch, [fetchReportRawData(false), fetchReportData(false)])))
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
  const setAggregateReportEntries = payloadParamActionCreator(SET_AGGREGATE_REPORT_ENTRIES);
  const setSorting = payloadParamActionCreator(SET_SORTING);
  const setSortingRawData = payloadParamActionCreator(SET_SORTING_RAW_DATA);
  const generateVulnerabilityEntries = noPayloadActionCreator(GENERATE_VULNERABILITY_ENTRIES);

  function setStringFieldFilter(fieldName, filterString) {
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

  function setExactValueFilter(fieldName, allowedValues) {
    return {
      type: SET_EXACT_VALUE_FILTER,
      payload: { fieldName, allowedValues }
    };
  }

  const selectComponent = payloadParamActionCreator(SELECT_COMPONENT);
  const selectRootAncestor = payloadParamActionCreator(SELECT_ROOT_ANCESTOR);
  const unselectRootAncestor = noPayloadActionCreator(UNSELECT_ROOT_ANCESTOR);

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
            return dispatch(loadReport(true));
          });
    };
  }

  const reevaluateReportFulfilled = noPayloadActionCreator(REEVALUATE_REPORT_FULFILLED);
  const reevaluateReportFailed = httpErrorMessageActionCreator(REEVALUATE_REPORT_FAILED);
  const reevaluateReportCancelled = noPayloadActionCreator(REEVALUATE_REPORT_CANCELLED);

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
    unselectRootAncestor
  };
}

applicationReportActions.$inject = ['$http', '$q', '$state', '$window', 'CLMLocations', 'Messages'];
