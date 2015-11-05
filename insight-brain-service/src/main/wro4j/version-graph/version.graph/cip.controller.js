/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, clmEndpoint*/
(function() {
  'use strict';

  angular.module('version.graph').controller('CIPController', ['$scope', 'OwnerContext', function ($scope, OwnerContext) {
    $scope.canLoad = function () {
      return !$scope.selectApplication || OwnerContext.ownerId;
    };
    $scope.linkTarget = clmEndpoint.linkTarget;
  }]);
}());
