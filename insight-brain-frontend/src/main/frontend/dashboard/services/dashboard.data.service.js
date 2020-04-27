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

export const MAX_RESULTS = 100;

export default
function dashboardDataService($http, $filter, CLMLocations, createRequest, ClassyBrew) {

  function getNewestRisks(filters, sortFields) {
    const request = createRequest(filters, MAX_RESULTS, translateViolationsSortFields(sortFields));
    return getData(CLMLocations.getNewestRisksUrl(), request).then(function({dashboardResults, numResults}) {
      return {results: dashboardResults, numResults};
    });
  }

  function getApplicationRisks(filters, sortFields) {
    const request = createRequest(filters, MAX_RESULTS, translateApplicationsSortFields(sortFields));
    return getData(CLMLocations.getApplicationRisksUrl(), request).then(function({dashboardResults, numResults}) {
      const series = generateApplicationsSeries(dashboardResults);
      return {results: dashboardResults, numResults, classyBrew: ClassyBrew.create(series)};
    });
  }

  function generateApplicationsSeries(applications) {
    const scoreFields = ['totalRisk', 'criticalRisk', 'severeRisk', 'moderateRisk', 'lowRisk'];
    const series = {};
    applications.forEach(function(application) {
      scoreFields.forEach(function(scoreField) {
        if (application.totalApplicationRisk[scoreField]) {
          series[application.totalApplicationRisk[scoreField]] = true;
        }
      });
    });

    return Object.keys(series).map(function(x) {
      return parseInt(x, 10);
    });
  }

  function getComponentRisks(filters, sortFields) {
    const request = createRequest(filters, MAX_RESULTS, translateComponentsSortFields(sortFields));
    return getData(CLMLocations.getComponentRisksUrl(), request).then(function({dashboardResults, numResults}) {
      const series = generateComponentsSeries(dashboardResults);
      return {results: dashboardResults, numResults, classyBrew: ClassyBrew.create(series)};
    });
  }

  function generateComponentsSeries(components) {
    const series = [];
    components.forEach(function(component) {
      ['score', 'scoreCritical', 'scoreSevere', 'scoreModerate', 'scoreLow'].forEach(function(scoreField) {
        if (component[scoreField] && series.lastIndexOf(component[scoreField]) === -1) {
          series.push(component[scoreField]);
        }
      });
    });
    return series;
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
    MAX_RESULTS
  };
}

dashboardDataService.$inject = [
  '$http', '$filter', 'CLMLocations', 'createDashboardDataRequestPayload', 'ClassyBrew'
];
