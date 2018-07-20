/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const LOAD_REPORT_REQUESTED = 'LOAD_REPORT_REQUESTED';
export const LOAD_REPORT_FULFILLED = 'LOAD_REPORT_FULFILLED';
export const LOAD_REPORT_FAILED = 'LOAD_REPORT_FAILED';

export default function applicationReportActions($http, $q, CLMLocations, Messages) {

  function loadReport(applicationPublicId, scanId) {
    return dispatch => {
      dispatch({
        type: LOAD_REPORT_REQUESTED
      });

      return $http.get(CLMLocations.getReportMetadataUrl(applicationPublicId, scanId))
          .then(({data}) => dispatch(loadReportFulfilled(data)))
          .catch(error => {
            dispatch(loadReportFailed(error));
            return $q.reject(error);
          });
    };
  }

  function loadReportFulfilled(reportMetadata) {
    return {
      type: LOAD_REPORT_FULFILLED,
      payload: reportMetadata
    };
  }

  function loadReportFailed(error) {
    return {
      type: LOAD_REPORT_FAILED,
      payload: Messages.getHttpErrorMessage(error)
    };
  }

  return {
    loadReport
  };
}

applicationReportActions.$inject = ['$http', '$q', 'CLMLocations', 'Messages'];
