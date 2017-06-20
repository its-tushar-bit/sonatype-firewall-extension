/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
export default
function dashboardDataService($http, $filter, CLMLocations, ComponentDisplayNameUtil) {

  var latestResultCounts = {
        newestRisk: undefined,
        applicationRisk: undefined,
        componentRisk: undefined
      },
      lastAppliedFilter;

  function getNewestRisks(filter) {
    return getData(CLMLocations.getNewestRisksUrl(), filter).then(function(resultsWrapper) {
      resetCountsIfFilterChanged(filter);
      latestResultCounts.newestRisk = resultsWrapper.numResults;
      resultsWrapper.dashboardResults.forEach(function(risk) {
        risk.gavName = ComponentDisplayNameUtil.deriveComponentName(risk);
        // to aid sortability:
        // - copy the times from each stage to a property on the row
        // - provide a single sortable string for the component name
        if (risk.stageDetails) {
          risk.stageDetails.forEach(function(stageDetail) {
            var propName = $filter('removeDashes')(stageDetail.stageTypeId) + 'Time';
            risk[propName] = stageDetail.time > 0 ? stageDetail.time : null;
          });
        }
      });
      return [resultsWrapper.dashboardResults];
    });
  }

  function getApplicationRisks(filter) {
    var scoreFields = ['totalRisk', 'criticalRisk', 'severeRisk', 'moderateRisk', 'lowRisk'];

    return getData(CLMLocations.getApplicationRisksUrl(), filter).then(function(resultsWrapper) {
      var series = {};
      resetCountsIfFilterChanged(filter);
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

  function getComponentRisks(filter) {
    var series = [];
    return getData(CLMLocations.getComponentRisksUrl(), filter).then(function(resultsWrapper) {
      resetCountsIfFilterChanged(filter);
      latestResultCounts.componentRisk = resultsWrapper.numResults;
      resultsWrapper.dashboardResults.forEach(function(component) {
        component.name = ComponentDisplayNameUtil.deriveComponentName(component);
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
    latestResultCounts: latestResultCounts
  };
}

dashboardDataService.$inject = ['$http', '$filter', 'CLMLocations', 'ComponentDisplayNameUtil'];
