/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import {
  translateViolationsSortFields,
  translateComponentsSortFields,
  translateApplicationsSortFields
} from './sortFieldsUtils';

const MAX_RESULTS = 100;

export default
function dashboardDataService($http, $filter, CLMLocations, createRequest) {

  var latestResultCounts = {
        newestRisk: undefined,
        applicationRisk: undefined,
        componentRisk: undefined
      },
      lastAppliedFilter;

  function getNewestRisks(filters, sortFields) {
    const request = createRequest(filters, MAX_RESULTS, translateViolationsSortFields(sortFields));
    return getData(CLMLocations.getNewestRisksUrl(), request).then(function(resultsWrapper) {
      resetCountsIfFilterChanged(filters);
      latestResultCounts.newestRisk = resultsWrapper.numResults;
      return [resultsWrapper.dashboardResults];
    });
  }

  function getApplicationRisks(filters, sortFields) {
    var scoreFields = ['totalRisk', 'criticalRisk', 'severeRisk', 'moderateRisk', 'lowRisk'];

    const request = createRequest(filters, MAX_RESULTS, translateApplicationsSortFields(sortFields));
    return getData(CLMLocations.getApplicationRisksUrl(), request).then(function(resultsWrapper) {
      var series = {};
      resetCountsIfFilterChanged(filters);
      latestResultCounts.applicationRisk = resultsWrapper.numResults;
      resultsWrapper.dashboardResults.forEach(function(application) {
        scoreFields.forEach(function(scoreField) {
          if (application.totalApplicationRisk[scoreField]) {
            series[application.totalApplicationRisk[scoreField]] = true;
          }
        });
      });

      return [resultsWrapper.dashboardResults, Object.keys(series).map(function(x) {
        return parseInt(x, 10);
      })];
    });
  }

  function getComponentRisks(filters, sortFields) {
    var series = [];

    const request = createRequest(filters, MAX_RESULTS, translateComponentsSortFields(sortFields));
    return getData(CLMLocations.getComponentRisksUrl(), request).then(function(resultsWrapper) {
      resetCountsIfFilterChanged(filters);
      latestResultCounts.componentRisk = resultsWrapper.numResults;
      resultsWrapper.dashboardResults.forEach(function(component) {
        ['score', 'scoreCritical', 'scoreSevere', 'scoreModerate', 'scoreLow'].forEach(function(scoreField) {
          if (component[scoreField] && series.lastIndexOf(component[scoreField]) === -1) {
            series.push(component[scoreField]);
          }
        });
      });
      return [resultsWrapper.dashboardResults, series];
    });
  }

  function resetCountsIfFilterChanged(filter) {
    if (!angular.equals(lastAppliedFilter, filter)) {
      lastAppliedFilter = filter;
      latestResultCounts.newestRisk = undefined;
      latestResultCounts.componentRisk = undefined;
      latestResultCounts.applicationRisk = undefined;
    }
  }

  function getData(url, filter) {
    return $http.post(url, filter).then(function(response) {
      return response.data;
    });
  }

  return {
    getNewestRisks: getNewestRisks,
    getApplicationRisks: getApplicationRisks,
    getComponentRisks: getComponentRisks,
    latestResultCounts: latestResultCounts,
    MAX_RESULTS
  };
}

dashboardDataService.$inject = [
  '$http', '$filter', 'CLMLocations', 'createDashboardDataRequestPayload'
];
