/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, AngularUtils */
(function () {
  'use strict';

  function ProprietaryConfigurationController($scope, $http, isAuthorized, Messages, clmLocations) {
    var vm = this;
    vm.isAuthorized = isAuthorized;
    vm.doLoad = doLoad;
    vm.save = save;
    vm.reset = reset;
    vm.proprietary = undefined;
    vm.loadError = undefined;
    vm.saving = undefined;
    vm.error = undefined;
    vm.isDirty = isDirty;

    function doLoad() {
      if (isAuthorized) {
        $http.get(clmLocations.getProprietaryConfig()).success(function(data) {
          vm.proprietary = data;
          reset();
        }).error(function() {
          vm.loadError = Messages.getHttpErrorMessage(arguments);
        });
      }
    }

    function save() {
      var proprietary = angular.extend({}, vm.proprietary,
          { packages: angular.copy(vm.packages), regexes: angular.copy(vm.regexes) });

      vm.saving = true;

      $http.put(clmLocations.getProprietaryConfig() + '/update', proprietary).success(function() {
        vm.saving = false;
        vm.proprietary = proprietary;
        reset();
      }).error(function() {
        vm.saving = false;
        vm.error = [AngularUtils.toAlert(Messages.getHttpErrorMessage(arguments))];
      });
    }

    function reset() {
      vm.packages = angular.copy(vm.proprietary.packages);
      vm.regexes = angular.copy(vm.proprietary.regexes);
      vm.error = null;
    }

    function isDirty() {
      return vm.packages && vm.proprietary &&
          (!angular.equals(vm.packages, vm.proprietary.packages) ||
          !angular.equals(vm.regexes, vm.proprietary.regexes));
    }

    doLoad();

    $scope.$on('pageChangeStarted', function(event) {
      if (isDirty()) {
        event.preventDefault();
      }
    });
  }

  ProprietaryConfigurationController.$inject = ['$scope', '$http', 'isAuthorized', 'Messages', 'CLMLocations'];

  angular.module('proprietary.configuration.module').controller('proprietary.configuration.controller',
      ProprietaryConfigurationController);
}());
