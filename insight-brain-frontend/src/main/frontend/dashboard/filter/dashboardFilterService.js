/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { setToArray } from '../../util/jsUtil';

export function filterToJson(filter) {
  return {
    organizationFilters: setToArray(filter.organizations),
    applicationFilters: setToArray(filter.applications),
    policyThreatCategoryFilters: setToArray(filter.policyTypes),
    stageTypeFilters: setToArray(filter.stages),
    tagFilters: setToArray(filter.categories),
    policyViolationStates: setToArray(filter.policyViolationStates),
    maxDaysOld: filter.maxDaysOld,
    minPolicyThreatLevel: filter.policyThreatLevels[0],
    maxPolicyThreatLevel: filter.policyThreatLevels[1]
  };
}

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

  return {
    deleteSavedFilters,
    filterToJson
  };
}

dashboardFilterService.$inject = ['$http', '$q', 'CLMLocations', 'Messages'];
