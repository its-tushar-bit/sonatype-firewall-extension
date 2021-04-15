/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */

function buildActionMapById(actionTypes) {
  var actionMap = {};
  angular.forEach(actionTypes, function (actionType) {
    actionMap[actionType.id] = {
      actionSummary: actionType.summary,
    };
  });
  return actionMap;
}

// unclear why this is necessary
function addIfNotFound(actions, action) {
  if (!action) {
    return;
  }
  for (var i = 0; i < actions.length; i++) {
    if (actions[i].actionSummary === action.actionSummary) {
      //found a match, bail out
      return;
    }
  }
  actions.push(action);
}

// because we need to support reports generated from older servers, we must tweak the data so that it
// fits what the html expects
function processConstraint(constraint) {
  var processedConstraint = {
    constraintId: constraint.constraintId,
    constraintName: constraint.constraintName,
    constraintOperator: constraint.operatorName,
    conditions: [],
  };

  angular.forEach(constraint.conditionFacts, function (conditionFact) {
    processedConstraint.conditions.push({
      conditionType: conditionFact.conditionTypeId,
      conditionSummary: conditionFact.summary,
      conditionReason: conditionFact.reason,
    });
  });

  return processedConstraint;
}

export default function CIPolicyViolations(
  $http,
  $q,
  SelectedComponent,
  CLMLocations,
  $state
) {
  function doLegacyLoad(deferred) {
    $q.all([
      $http.get(CLM.path + 'rest/policy/actionType'),
      $http.get('policyalerts.json'),
    ]).then(
      function (result) {
        var actionMap = buildActionMapById(result[0].data),
          policyAlerts = result[1].data.aaData || [],
          processedPolicyAlerts = [];

        angular.forEach(policyAlerts, function (policyAlert) {
          var processedActions = [];
          angular.forEach(policyAlert.actions, function (action) {
            addIfNotFound(processedActions, actionMap[action.actionTypeId]);
          });

          angular.forEach(
            policyAlert.trigger.componentFacts,
            function (componentFact) {
              if (componentFact.hash === SelectedComponent.get().hash) {
                var processedConstraints = [];
                angular.forEach(
                  componentFact.constraintFacts,
                  function (constraintFact) {
                    processedConstraints.push(
                      processConstraint(constraintFact)
                    );
                  }
                );
                processedPolicyAlerts.push({
                  id: policyAlert.trigger.policyId,
                  name: policyAlert.trigger.policyName,
                  threatLevel: policyAlert.trigger.threatLevel,
                  groupId: componentFact.groupId,
                  artifactId: componentFact.artifactId,
                  version: componentFact.version,
                  hash: componentFact.hash,
                  constraints: processedConstraints,
                  actions: processedActions,
                  policyViolationId: policyAlert.trigger.policyViolationId,
                });
              }
            }
          );
        });
        deferred.resolve(processedPolicyAlerts);
      },
      function () {
        deferred.reject(arguments);
      }
    );
  }

  return {
    get: function () {
      var deferred = $q.defer();

      // for iframe reports just use policythreats.json relative to current url
      const url = $state.params.scanId
        ? CLMLocations.getReportPolicyThreatsUrl(
            $state.params.publicId,
            $state.params.scanId
          )
        : 'policythreats.json';

      $http.get(url).then(
        function (result) {
          var policyThreats = result.data.aaData || [];
          // if version isn't set we are dealing with old data, so revert to old request and massage data as
          // necessary
          if (!result.data.version) {
            doLegacyLoad(deferred);
          } else {
            var processedPolicyAlerts = [];

            angular.forEach(policyThreats, function (policyThreat) {
              if (policyThreat.hash === SelectedComponent.get().hash) {
                angular.forEach(
                  policyThreat.activeViolations,
                  function (activeViolation) {
                    var actions = [];
                    angular.forEach(activeViolation.actions, function (action) {
                      addIfNotFound(actions, action);
                    });

                    processedPolicyAlerts.push({
                      id: activeViolation.policyId,
                      name: activeViolation.policyName,
                      threatLevel: activeViolation.policyThreatLevel,
                      hash: policyThreat.hash,
                      constraints: activeViolation.constraints,
                      constraintFactsJson: activeViolation.constraintFactsJson,
                      actions: actions,
                      policyViolationId: activeViolation.policyViolationId,
                    });
                  }
                );
              }
            });

            deferred.resolve(processedPolicyAlerts);
          }
        },
        function () {
          deferred.reject(arguments);
        }
      );
      return deferred.promise;
    },
  };
}
CIPolicyViolations.$inject = [
  '$http',
  '$q',
  'SelectedComponent',
  'CLMLocations',
  '$state',
];
