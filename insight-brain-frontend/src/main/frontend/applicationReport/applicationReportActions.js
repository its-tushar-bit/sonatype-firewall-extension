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

// To be used for filters that are done by substring matching, as opposed to matchings a discreet set of values
export const SET_SUBSTRING_FIELD_FILTER = 'SET_SUBSTRING_FIELD_FILTER';
export const SET_SORTING = 'SET_SORTING';

export default function applicationReportActions($http, $q, CLMLocations, Messages) {

  function loadReport(applicationPublicId, scanId, isUnknownJs) {
    return dispatch => {
      dispatch({
        type: LOAD_REPORT_REQUESTED
      });

      const promises = [
        $http.get(CLMLocations.getReportMetadataUrl(applicationPublicId, scanId)),
        $http.get(CLMLocations.getReportPolicyThreatsUrl(applicationPublicId, scanId)),
        $http.get(CLMLocations.getReportBomUrl(applicationPublicId, scanId)),
        $http.get(CLMLocations.getReportDataUrl(applicationPublicId, scanId))
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
            const unknownJsResult = isUnknownJs ? results[4].unknownJsResult : undefined;
            const allEntries = createReportEntries(policyResult, bomResult, unknownJsResult);
            dispatch(loadReportFulfilled({ ...metadata, allEntries, ...dataResult }));
          })
          .catch(error => {
            dispatch(loadReportFailed(error));
            return $q.reject(error);
          });
    };
  }

  function loadReportFulfilled(report) {
    return {
      type: LOAD_REPORT_FULFILLED,
      payload: report
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

  function selectComponent(index) {
    return {
      type: SELECT_COMPONENT,
      payload: index
    };
  }

  return {
    loadReport,
    setAggregateReportEntries,
    setStringFieldFilter,
    setSorting,
    selectComponent
  };
}

applicationReportActions.$inject = ['$http', '$q', 'CLMLocations', 'Messages'];
