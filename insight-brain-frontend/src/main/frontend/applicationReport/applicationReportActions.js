/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const LOAD_REPORT_REQUESTED = 'LOAD_REPORT_REQUESTED';
export const LOAD_REPORT_FULFILLED = 'LOAD_REPORT_FULFILLED';
export const LOAD_REPORT_FAILED = 'LOAD_REPORT_FAILED';

export default function applicationReportActions($http, $q, CLMLocations, Messages, applicationReportService) {

  function loadReport(applicationPublicId, scanId, isUnknownJs) {
    return dispatch => {
      dispatch({
        type: LOAD_REPORT_REQUESTED
      });

      const promises = [
        $http.get(CLMLocations.getReportMetadataUrl(applicationPublicId, scanId)),
        $http.get(CLMLocations.getReportPolicyThreatsUrl(applicationPublicId, scanId)),
        $http.get(CLMLocations.getReportBomUrl(applicationPublicId, scanId))
      ];

      if (isUnknownJs) {
        promises.push($http.get(CLMLocations.getReportUnknownJsUrl(applicationPublicId, scanId)));
      }

      return $q.all(promises)
          .then((results) => {
            const metadata = results[0].data;
            const policyResult = results[1].data;
            const bomResult = results[2].data;
            const unknownJsResult = isUnknownJs ? results[3].unknownJsResult : null;
            const reportData = applicationReportService.createReportData(policyResult, bomResult, unknownJsResult);
            dispatch(loadReportFulfilled({...metadata, ...reportData}));
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

  return {
    loadReport
  };
}

applicationReportActions.$inject = ['$http', '$q', 'CLMLocations', 'Messages', 'applicationReportService'];
