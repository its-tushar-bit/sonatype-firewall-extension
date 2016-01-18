/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
(function() {
  'use strict';
  
  function ViewWaiverController($scope, $http, OwnerContext, SelectedComponent, messages) {
    function handleHttpError() {
      $scope.appError = messages.getHttpErrorMessage(arguments);
    }

    function doLoad() {
      $scope.waiversLoading = true;
      // get the waivers from the server
      $http.get(CLM.path + 'rest/policyWaiver/' + OwnerContext.ownerType + '/' + OwnerContext.ownerId + '/component/' +
            SelectedComponent.get().hash).success(function(data) {
        $scope.waiversLoading = false;
        $scope.waivers = [];
        angular.forEach(data.waiversByOwner, function(waiversByOwner) {
          angular.forEach(waiversByOwner.waivers, function(waiver) {
            waiver.type = waiversByOwner.ownerType;
            waiver.ownerName = waiversByOwner.ownerName;
            $scope.waivers.push(waiver);
          });
        });
      }).error(handleHttpError);
    }

    doLoad();

    $scope.remove = function(waiver) {
      $scope.confirmDelete = waiver;
      $scope.appError = null;
    };

    $scope.removeWaiver = function() {
      var waiver = $scope.confirmDelete;
      $scope.confirmDelete = null;
      $scope.appError = null;
      $http['delete'](CLM.path + 'rest/policyWaiver/' + waiver.type + '/' + waiver.ownerId + '/' +
              waiver.id).success(function() {
        $scope.$emit('component.data.changed', waiver.hash);
        $scope.waivers.splice($scope.waivers.indexOf(waiver), 1);
      }).error(handleHttpError);
    };
  }
  ViewWaiverController.$inject = ['$scope', '$http', 'OwnerContext', 'SelectedComponent', 'Messages'];

  angular.module('cip.policy.violations').controller('ViewWaiverController', ViewWaiverController);
}());
