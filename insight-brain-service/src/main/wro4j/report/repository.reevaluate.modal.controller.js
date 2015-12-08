/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function() {
  'use strict';

  function RepositoryReEvaluateModalController($scope, $http, $stateParams, CLMLocations, Messages) {
    var vm = this;
    vm.error = undefined;
    vm.reEvaluatePolicy = reEvaluatePolicy;

    function reEvaluatePolicy() {
      delete vm.error;
      vm.submitActive = true;
      $http.post(CLMLocations.getRepositoryEvaluateUrl($stateParams.repositoryId)).success(function() {
        $scope.$close();
      }).error(function(error) {
        vm.submitActive = false;
        vm.error = Messages.getHttpErrorMessage(error);
      });
    }
  }

  RepositoryReEvaluateModalController.$inject = ['$scope', '$http', '$stateParams', 'CLMLocations', 'Messages'];

  angular.module('Report').controller('repository.reevaluate.modal.controller', RepositoryReEvaluateModalController);
}());
