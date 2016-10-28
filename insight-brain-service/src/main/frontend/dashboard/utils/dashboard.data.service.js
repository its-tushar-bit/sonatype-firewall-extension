/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function dashboardDataService($http, $filter, CLMLocations, ComponentDisplayNameUtil) {

    function getNewestRisks(filter) {
      return getData(CLMLocations.getNewestRisksUrl(), filter).then(function(risks) {
        risks.forEach(function(risk) {
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
        return [risks];
      });
    }

    function getApplicationRisks(filter) {
      return getData(CLMLocations.getApplicationRisksUrl(), filter).then(function(data) {
        return [data];
      });
    }

    function getComponentRisks(filter) {
      var series = [];
      return getData(CLMLocations.getComponentRisksUrl(), filter).then(function(components) {
        components.forEach(function(component) {
          component.name = ComponentDisplayNameUtil.deriveComponentName(component);
          ['score', 'scoreCritical', 'scoreSevere', 'scoreModerate', 'scoreLow'].forEach(function(scoreField) {
            if (component[scoreField] && series.lastIndexOf(component[scoreField]) === -1) {
              series.push(component[scoreField]);
            }
          });
        });
        return [components, series];
      });
    }

    function getData(url, filter) {
      return $http.post(url, filter).then(function(response) {
        return response.data;
      });
    }

    return {
      getNewestRisks: getNewestRisks,
      getApplicationRisks: getApplicationRisks,
      getComponentRisks: getComponentRisks
    };
  }

  dashboardDataService.$inject = ['$http', '$filter', 'CLMLocations', 'ComponentDisplayNameUtil'];

  angular //
      .module('dashboard.utils') //
      .service('dashboard.data.service', dashboardDataService);

}());
