/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Brain */
(function () {
  'use strict';

  function ComponentUpdateController($scope, $rootScope, $http, $q, OwnerContext, hash) {
    var vm = this;

    vm.error = null;
    vm.doProcess = doProcess;
    vm.reevaluated = false;

    doProcess();

    function doProcess() {
      if (!vm.reevaluated) {
        reevaluate();
      }
      else {
        updateComponent();
      }
    }

    function updateComponent() {
      delete vm.error;

      // emit an event
      var promises = [];
      $rootScope.$broadcast('component.evaluation.updated', hash, promises);
      $q.all(promises).then(function () {
        $scope.$dismiss();
      }, function () {
        vm.error = arguments;
      });
    }

    function reevaluate() {
      delete vm.error;

      $http.post(Brain.getComponentReevaluationUrl(OwnerContext, hash)).success(function () {
        vm.reevaluated = true;
        updateComponent();
      }).error(function () {
        vm.error = arguments;
      });
    }
  }
  ComponentUpdateController.$inject = ['$scope', '$rootScope', '$http', '$q', 'OwnerContext', 'hash'];

  angular.module('audit').controller('component.update.controller', ComponentUpdateController);
}());
