/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Brain */
(function() {
  'use strict';

  function ComponentUpdateOptionalController($scope, $rootScope, $http, $q, Messages, OwnerContext) {
    var vm = this;

    vm.error = null;
    vm.forceReevaluation = forceReevaluation;

    function forceReevaluation() {
      delete vm.error;
      $http.post(Brain.getRepositoryEvaluateUrl(OwnerContext)).then(function() {
        $scope.$close();
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    }
  }

  ComponentUpdateOptionalController.$inject = ['$scope', '$rootScope', '$http', '$q', 'Messages', 'OwnerContext'];

  angular.module('audit').controller('component.update.optional.controller', ComponentUpdateOptionalController);
}());
