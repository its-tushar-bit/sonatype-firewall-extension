/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular*/
(function() {
  'use strict';

  angular.module('version.graph').controller('ApplicationController', ['$scope', 'Applications', 'OwnerContext', function ($scope, Applications, OwnerContext) {
    $scope.doLoad = function () {
      Applications.get().then(function (data) {
        $scope.applications = data;
      }, function (error) {
        $scope.setError(error);
      });
    };

    $scope.applications = null;
    $scope.selectedApplication = OwnerContext.ownerId;

    $scope.$on('reload', function () {
      $scope.doLoad();
    });

    // Monitor user's change to dropdown
    $scope.$watch('selectedApplication', function (app) {
      OwnerContext.setApplicationId(app);
    });

    $scope.doLoad();
  }]);

}());
