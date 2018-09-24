/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Brain, clmEndpoint*/

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
            Coordinates.get(), Properties.getPathname())).then(function(response) {
          $scope.componentDetailsList = response.data.list || response.data;
          for (var i = 0; i < $scope.componentDetailsList.length; i++) {
            $scope.componentDetailsList[i].proprietary = Coordinates.get().proprietary;
          }
          $scope.loaded = true;
        }, function(error) {
          $scope.setError(error);
        });
      }
    }
  }

  $scope.setError = function(error) {
    $scope.errorMessage = errorMessage(error);
  };

  $scope.retryFn = function() {
    $scope.$broadcast('reload');
  };

  $scope.$on('reload', function() {
    $scope.coordinates = null;
    coordinatesChanged();
  });

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
}

function isNotDependency(pathName) {
  // doesn't start with "dependency:\"
  return !/^dependency:\//.test(pathName);
}

ComponentController.$inject = [
  '$scope', 'Coordinates', 'OwnerContext', 'ErrorMessage', 'Properties', '$http',
  '$injector'
];
