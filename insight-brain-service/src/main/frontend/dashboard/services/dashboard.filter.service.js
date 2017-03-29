/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function dashboardFilterService($http, $q, CLMLocations, Messages) {

    function deleteSavedFilters(filters) {
      return $http.post(CLMLocations.getDashboardDeleteFiltersUrl(), filters).catch(function(error) {
        error = Messages.getHttpErrorMessage(error);
        if (angular.isArray(error)) {
          return $q.reject(error.map(function(err) {
            return 'Filter ' + err.name + ', ' + err.errorMessage;
          }));
        }
        else {
          return $q.reject([error]);
        }
      });
    }

    function filterToJson(filter) {
      return {
        organizationFilters: Object.keys(filter.organizations),
        applicationFilters: Object.keys(filter.applications),
        policyThreatCategoryFilters: Object.keys(filter.policyTypes),
        stageTypeFilters: Object.keys(filter.stages),
        tagFilters: Object.keys(filter.categories),
        policyViolationStates: Object.keys(filter.policyViolationStates),
        maxDaysOld: filter.age.maxDaysOld,
        minPolicyThreatLevel: filter.policyThreatLevels[0],
        maxPolicyThreatLevel: filter.policyThreatLevels[1]
      };
    }

    return {
      deleteSavedFilters: deleteSavedFilters,
      filterToJson: filterToJson
    };
  }

  dashboardFilterService.$inject = ['$http', '$q', 'CLMLocations', 'Messages'];

  angular //
      .module('dashboard.module') //
      .service('dashboard.filter.service', dashboardFilterService);

}());
