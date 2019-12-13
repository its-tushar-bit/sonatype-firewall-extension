/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
// unclear why this is necessary
function addIfNotFound(actions, action) {
  if (!action) { return; }
  for (var i = 0; i < actions.length; i++) {
    if (actions[i].actionSummary === action.actionSummary) {
      //found a match, bail out
      return;
    }
  }
  actions.push(action);
}

export default function RepositoryPolicyViolations($http, $q, SelectedComponent, OwnerContext) {
  return {
    get: function () {
      var deferred = $q.defer();
      $http.get(CLM.path + 'rest/repositories/' + OwnerContext.ownerId + '/report/policyThreat/' +
              encodeURIComponent(SelectedComponent.get().pathname).replace(/%2F/gi, '/')).then(function (response) {
        var policyThreat = response.data,
            processedPolicyAlerts = [];

        angular.forEach(policyThreat.activePolicyViolations, function(activeViolation) {
          var actions = [];
          angular.forEach(activeViolation.actions, function(action) {
            addIfNotFound(actions, action);
          });

          processedPolicyAlerts.push({
            id: activeViolation.policyId,
            name: activeViolation.policyName,
            threatLevel: activeViolation.policyThreatLevel,
            hash: policyThreat.hash,
            constraints: activeViolation.constraints,
            actions: actions,
            blocksUnquarantine: activeViolation.blocksUnquarantine,
            constraintFactsJson: activeViolation.constraintFactsJson,
            isRequestWaiverDisabled: true
          });
        });
        deferred.resolve(processedPolicyAlerts);
      }, function () {
        deferred.reject(arguments);
      });
      return deferred.promise;
    }
  };
}
RepositoryPolicyViolations.$inject = ['$http', '$q', 'SelectedComponent', 'OwnerContext'];
