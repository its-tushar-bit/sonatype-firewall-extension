/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Brain, clmEndpoint*/
(function() {
  'use strict';

  function ComponentController($scope, Coordinates, OwnerContext, errorMessage, Properties, $http) {
    function coordinatesChanged() {
      var coordinates = Coordinates.get() ? { coordinates : Coordinates.get(), appId : OwnerContext.ownerId } : null;

      $scope.errorMessage = null;

      if (!angular.equals($scope.coordinates, coordinates)) {
        $scope.componentDetailsList = null;
        $scope.loaded = false;
        $scope.coordinates = coordinates;

        if (coordinates && coordinates.appId && !Properties.isUnknown()) {
          $http.get(Brain[clmEndpoint.type].getComponentListUrl(OwnerContext.ownerType, OwnerContext.ownerId, Coordinates.getFormat(), Properties.getHash(), Properties.getMatchState(), Properties.getProprietary(), Coordinates.get(), Properties.getPathname())).success(function (data) {
            $scope.componentDetailsList = data.list ? data.list : data;
            for (var i = 0; i < $scope.componentDetailsList.length; i++) {
              $scope.componentDetailsList[i].proprietary = Coordinates.get().proprietary;
            }
            $scope.loaded = true;
          }).error(function () {
            $scope.setError(arguments);
          });
        }
      }
    }

    $scope.setError = function (error) {
      $scope.errorMessage = errorMessage(error);
    };

    $scope.retryFn = function () {
      $scope.$broadcast('reload');
    };

    $scope.$on('reload', function () {
      $scope.coordinates = null;
      coordinatesChanged();
    });

    $scope.$watch(function () {
      return Properties.isUnknown();
    }, function () {
      $scope.isUnknown = Properties.isUnknown();
    });

    $scope.$watch(function () {
      return Coordinates.get();
    }, coordinatesChanged);

    $scope.$watch(function () {
      return OwnerContext.ownerId;
    }, coordinatesChanged);
  }
  ComponentController.$inject = ['$scope', 'Coordinates', 'OwnerContext', 'ErrorMessage', 'Properties', '$http'];

  angular.module('version.graph').controller('ComponentController', ComponentController);
}());
