/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Brain, clmEndpoint */

export default function DetailsController($scope, $http, OwnerContext, Coordinates, Properties) {
  function coordinatesChanged() {
    var coordinates = Coordinates.getSelected()
      ? {
          coordinates: Coordinates.getSelected(),
          appId: OwnerContext.ownerId,
        }
      : null;

    if (!angular.equals(last, coordinates)) {
      $scope.componentDetails = null;
      $scope.highestPolicyThreat = null;
      last = coordinates;

      if (coordinates && coordinates.appId && !Properties.isUnknown()) {
        // only pass the hash if current version
        const hash = Coordinates.isOriginalVersion() ? Properties.getHash() : null;
        $http
          .get(
            Brain[clmEndpoint.type].getComponentUrl(
              OwnerContext.ownerType,
              OwnerContext.ownerId,
              Coordinates.getFormat(),
              hash,
              Properties.getMatchState(),
              Properties.getProprietary(),
              coordinates.coordinates,
              Properties.getPathname(),
              Coordinates.getIdentificationSource(),
              OwnerContext.scanId,
              Properties.getDependencyType()
            )
          )
          .then(
            function (response) {
              var data = response.data;
              if (data.matchState === 'unknown') {
                Properties.setMatchState('unknown');
              } else {
                $scope.componentDetails = data;
                $scope.componentDetails.proprietary = Coordinates.getSelected().proprietary;

                var i = 0;
                while (i < $scope.componentDetails.securityVulnerabilities.length) {
                  if ($scope.componentDetails.securityVulnerabilities[i].status === 'Not Applicable') {
                    $scope.componentDetails.securityVulnerabilities.splice(i, 1);
                  } else {
                    i++;
                  }
                }

                $scope.componentDetails.securityVulnerabilities.sort(function (a, b) {
                  if (a.severity === b.severity) {
                    return 0;
                  } else if (a.severity === null) {
                    return 1;
                  } else if (b.severity === null) {
                    return -1;
                  }
                  return b.severity - a.severity;
                });

                $scope.componentDetails.policyAlerts.sort(function (alertA, alertB) {
                  return alertB.trigger.threatLevel - alertA.trigger.threatLevel;
                });
                $scope.highestPolicyThreat = {
                  level:
                    $scope.componentDetails.policyAlerts.length > 0
                      ? $scope.componentDetails.policyAlerts[0].trigger.threatLevel
                      : null,
                  violatedPolicies: $scope.componentDetails.policyAlerts.length,
                };
              }
            },
            function (error) {
              $scope.setError(error);
            }
          );
      }
    }
  }

  var last = {};

  $scope.isManual = function () {
    return $scope.componentDetails && $scope.componentDetails.identificationSource === 'Manual';
  };

  $scope.canMigrate = function () {
    var coordinates = Coordinates.get(),
      selected = Coordinates.getSelected();

    return coordinates && selected && coordinates.version !== selected.version;
  };

  $scope.getMaximumSeverity = function () {
    if ($scope.componentDetails) {
      if ($scope.componentDetails.securityVulnerabilities.length === 0) {
        return 'NA';
      } else if ($scope.componentDetails.securityVulnerabilities[0].severity === null) {
        return 'Unscored';
      } else {
        return $scope.componentDetails.securityVulnerabilities[0].severity;
      }
    }
  };

  $scope.viewDetails = function () {
    $scope.$emit('viewDetails', Coordinates.getSelected().version);
  };

  $scope.markUpgrade = function () {
    $scope.$emit('markUpgrade', Coordinates.getSelected());
  };

  $scope.$on('reload', function () {
    last = {};
    coordinatesChanged();
  });

  $scope.$watch(function () {
    return Coordinates.getSelected();
  }, coordinatesChanged);

  $scope.$watch(function () {
    return OwnerContext.ownerId;
  }, coordinatesChanged);
}

DetailsController.$inject = ['$scope', '$http', 'OwnerContext', 'Coordinates', 'Properties'];
