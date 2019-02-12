/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReportEntries } from './applicationReportService';

export const LOAD_REPORT_REQUESTED = 'LOAD_REPORT_REQUESTED';
export const LOAD_REPORT_FULFILLED = 'LOAD_REPORT_FULFILLED';
export const LOAD_REPORT_FAILED = 'LOAD_REPORT_FAILED';
export const SET_AGGREGATE_REPORT_ENTRIES = 'SET_AGGREGATE_REPORT_ENTRIES';
export const SELECT_COMPONENT = 'SELECT_COMPONENT';
export const REEVALUATE_REPORT_REQUESTED = 'REEVALUATE_REPORT_REQUESTED';
export const REEVALUATE_REPORT_FULFILLED = 'REEVALUATE_REPORT_FULFILLED';
export const REEVALUATE_REPORT_FAILED = 'REEVALUATE_REPORT_FAILED';
export const REEVALUATE_REPORT_CANCELLED = 'REEVALUATE_REPORT_CANCELLED';

// To be used for filters that are done by substring matching, as opposed to matchings a discreet set of values
export const SET_SUBSTRING_FIELD_FILTER = 'SET_SUBSTRING_FIELD_FILTER';
export const SET_EXACT_VALUE_FILTER = 'SET_EXACT_VALUE_FILTER';
export const SET_SORTING = 'SET_SORTING';
export const RESET_REPORT_VIEW_SETTINGS = 'RESET_REPORT_VIEW_SETTINGS';

export default function applicationReportActions($http, $q, CLMLocations, Messages) {

  function fetchReportData(applicationPublicId, scanId, isUnknownJs) {
    const promises = [
      $http.get(CLMLocations.getReportMetadataUrl(applicationPublicId, scanId)),
      $http.get(CLMLocations.getReportPolicyThreatsUrl(applicationPublicId, scanId)),
      $http.get(CLMLocations.getReportBomUrl(applicationPublicId, scanId)),
      $http.get(CLMLocations.getReportDataUrl(applicationPublicId, scanId)),
      $http.get(CLMLocations.getReportPartialMatchedUrl(applicationPublicId, scanId))
    ];

    if (isUnknownJs) {
      promises.push($http.get(CLMLocations.getReportUnknownJsUrl(applicationPublicId, scanId)));
    }

    return $q.all(promises)
        .then((results) => {
          const metadata = results[0].data;
          const policyResult = results[1].data || undefined;
          const bomResult = results[2].data || undefined;
          const dataResult = results[3].data;
          const partialMatches = results[4].data || undefined;
          const unknownJsResult = isUnknownJs && results[5].data || undefined;
          const allEntries = createReportEntries(policyResult, bomResult, unknownJsResult, partialMatches);
          return { ...metadata, allEntries, ...dataResult, scanId };
        });
  }

  function loadReport(applicationPublicId, scanId, isUnknownJs) {
    return dispatch => {
      dispatch({
        type: LOAD_REPORT_REQUESTED
      });

      return fetchReportData(applicationPublicId, scanId, isUnknownJs)
          .then((results) => {
            dispatch(loadReportFulfilled(results, isUnknownJs));
          })
          .catch(error => {
            dispatch(loadReportFailed(error));
            return $q.reject(error);
          });
    };
  }

  function reloadReport() {
    return (dispatch, getState) => {
      const isUnknownJs = getState().applicationReport.isUnknownJs;
      const {application, scanId} = getState().applicationReport.selectedReport;
      const applicationPublicId = application.publicId;

      return fetchReportData(applicationPublicId, scanId, isUnknownJs)
          .then((results) => {
            dispatch(loadReportFulfilled(results, isUnknownJs));
          })
          .catch(error => {
            dispatch(loadReportFailed(error));
            return $q.reject(error);
          });
    };
  }

  function loadReportFulfilled(report, isUnknownJs) {
    return {
      type: LOAD_REPORT_FULFILLED,
      payload: { report, isUnknownJs }
    };
  }

  function loadReportFailed(error) {
    return {
      type: LOAD_REPORT_FAILED,
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
      const { selectedReport, isUnknownJs } = getState().applicationReport,
          { application, scanId } = selectedReport,
          applicationPublicId = application.publicId;

      dispatch({
        type: REEVALUATE_REPORT_REQUESTED
      });

      return $http.post(CLMLocations.getReportReevaluateUrl(applicationPublicId, scanId))
          .catch(error => {
            dispatch(reevaluateReportFailed(error));
            return $q.reject(error);
          })
          .then(() => {
            dispatch(reevaluateReportFulfilled());
            return dispatch(loadReport(applicationPublicId, scanId, isUnknownJs));
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

  /**
   * Reset all filter, sorting, and aggregation settings to the default
   */
  function resetReportViewSettings() {
    return {
      type: RESET_REPORT_VIEW_SETTINGS
    };
  }

  return {
    loadReport,
    reloadReport,
    reevaluateReport,
    reevaluateReportCancelled,
    setAggregateReportEntries,
    setStringFieldFilter,
    setExactValueFilter,
    setSorting,
    selectComponent,
    resetReportViewSettings
  };
}

applicationReportActions.$inject = ['$http', '$q', 'CLMLocations', 'Messages'];
