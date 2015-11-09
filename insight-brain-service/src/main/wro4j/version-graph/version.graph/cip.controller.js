/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, clmEndpoint*/
(function() {
  'use strict';

  angular.module('version.graph').controller('CIPController', ['$scope', 'OwnerContext', 'Coordinates', function ($scope, OwnerContext, Coordinates) {
    $scope.canLoad = function () {
      return !$scope.selectApplication || OwnerContext.ownerId;
    };
    $scope.linkTarget = clmEndpoint.linkTarget;

    // Reset the selected coordinates when switching CIP tabs
    $scope.$on('$destroy', function () {
      Coordinates.setSelected(Coordinates.get());
    });
  }]);
}());
