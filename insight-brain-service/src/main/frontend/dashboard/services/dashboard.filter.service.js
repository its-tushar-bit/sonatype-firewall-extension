/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default
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

      // the uncategorized filter uses null but since it's an object key it gets turned into a string; we need to
      // fix that
      tagFilters: Object.keys(filter.categories).map(function(cat) {
        return cat === 'null' ? null : cat;
      }),
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
