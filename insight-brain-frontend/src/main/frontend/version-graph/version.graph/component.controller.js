/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Brain, clmEndpoint*/

const NEXT_NO_VIOLATIONS = 'next-no-violations';
const NEXT_NON_FAILING = 'next-non-failing';

export default function ComponentController($scope, Coordinates, OwnerContext, errorMessage, Properties, $http,
                                            $injector) {
  function coordinatesChanged() {
    var coordinates = Coordinates.get() ? {coordinates: Coordinates.get(), appId: OwnerContext.ownerId} : null;

    $scope.errorMessage = null;

    if (!angular.equals($scope.coordinates, coordinates)) {
      $scope.componentDetailsList = null;
      $scope.loaded = false;
      $scope.coordinates = coordinates;

      if (coordinates && coordinates.appId && !Properties.isUnknown()) {
        $http.get(Brain[clmEndpoint.type].getComponentListUrl(OwnerContext.ownerType, OwnerContext.ownerId,
            Coordinates.getFormat(), Properties.getHash(), Properties.getMatchState(), Properties.getProprietary(),
            Coordinates.get(), Properties.getPathname(), Properties.getIdentificationSource(), OwnerContext.scanId))
            .then(function(response) {
              $scope.componentDetailsList = response.data.allVersions || response.data.list || response.data;
              for (var i = 0; i < $scope.componentDetailsList.length; i++) {
                $scope.componentDetailsList[i].proprietary = Coordinates.get().proprietary;
              }
              $scope.loaded = true;

              setRemediations(response.data);
            }, function(error) {
              $scope.setError(error);
            });
      }
    }
  }

  function setRemediations({ remediation }) {
    $scope.suggestedRemediations = new Map();

    if (remediation && remediation.versionChanges) {
      $.each(remediation.versionChanges, function(index, item) {
        $scope.suggestedRemediations.set(item.type,
            item.data.component.componentIdentifier);
      });
    }
  }

  $scope.setError = function(error) {
    $scope.errorMessage = errorMessage(error);
  };

  $scope.retryFn = function() {
    $scope.$broadcast('reload');
  };

  $scope.$on('reload', coordinatesChanged);

  $scope.$watch(function() {
    return Properties.isUnknown();
  }, function() {
    $scope.isUnknown = Properties.isUnknown();
  });

  $scope.$watch(function() {
    return Coordinates.get();
  }, coordinatesChanged);

  $scope.$watch(function() {
    return OwnerContext.ownerId;
  }, coordinatesChanged);

  $scope.showAddProprietary = function() {
    // 'proprietary.matchers.modal' is available only in the context of CIP (if clmEndpoint.canAddProprietary)
    var ProprietaryMatchersModal = $injector.get('proprietary.matchers.modal');
    ProprietaryMatchersModal.open(OwnerContext.ownerId, getPathNames());
  };

  $scope.canShowAddProprietary = function() {
    if (!clmEndpoint.canAddProprietary || Properties.getProprietary()) {
      return false;
    }
    // don't show if there are no pathNames
    return getPathNames().length !== 0;
  };

  function getPathNames() {
    // SelectedComponent is available only in the context of CIP (if clmEndpoint.canAddProprietary)
    var SelectedComponent = $injector.get('SelectedComponent');
    return SelectedComponent.get().pathnames.filter(isNotDependency);
  }

  $scope.markNextNoViolation = function() {
    changeSelectedVersionToSuggestedRemediation(NEXT_NO_VIOLATIONS);
  };

  $scope.nextNoViolationAvailableAndNotCurrent = function() {
    return isSuggestedRemediationOfTypeAvailableAndDifferentFromCurrent(NEXT_NO_VIOLATIONS);
  };

  $scope.markNextNoFail = function() {
    changeSelectedVersionToSuggestedRemediation(NEXT_NON_FAILING);
  };

  $scope.nextNoFailAvailableAndNotCurrent = function() {
    return isSuggestedRemediationOfTypeAvailableAndDifferentFromCurrent(NEXT_NON_FAILING);
  };

  $scope.getNoViolationVersion = function() {
    return getSuggestedVersion(NEXT_NO_VIOLATIONS);
  };

  $scope.getNoFailVersion = function() {
    return getSuggestedVersion(NEXT_NON_FAILING);
  };

  $scope.isApplicationOwnerContext = function() {
    if (OwnerContext.ownerType !== 'application') {
      return false;
    }

    if (!OwnerContext.ownerId) {
      return false;
    }
    return true;
  };

  $scope.isRecommendationsAvailable = function() {
    if ($scope.recommendationsSupported === false) {
      return false;
    }
    return true;
  };

  function changeSelectedVersionToSuggestedRemediation(type) {

    let version = $scope.suggestedRemediations.get(type);
    $.each($scope.componentDetailsList, function(index, item) {
      if (item.componentIdentifier.coordinates.version === version.coordinates.version) {
        Coordinates.setSelected(version.coordinates);
        Properties.setHash(item.hash);
        Insight.updateBars(index);
      }
    });
  }

  function isSuggestedRemediationOfTypeAvailableAndDifferentFromCurrent(type) {
    if (!$scope.suggestedRemediations) {
      return false;
    }
    return $scope.suggestedRemediations.get(type) &&
        $scope.suggestedRemediations.get(type).coordinates.version !== $scope.coordinates.coordinates.version;
  }

  function getSuggestedVersion(type) {
    if (!isSuggestedRemediationOfTypeAvailableAndDifferentFromCurrent(type)) {
      return null;
    }
    return $scope.suggestedRemediations.get(type).coordinates.version;
  }
}

function isNotDependency(pathName) {
  // doesn't start with "dependency:\"
  return !/^dependency:\//.test(pathName);
}

ComponentController.$inject = [
  '$scope', 'Coordinates', 'OwnerContext', 'ErrorMessage', 'Properties', '$http',
  '$injector'
];
