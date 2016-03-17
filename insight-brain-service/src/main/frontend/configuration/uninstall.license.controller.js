/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function UninstallLicenseController($scope, $http, messages, clmLocations) {
    var vm = this;

    vm.uninstall = uninstall;
    vm.error = undefined;

    function uninstall() {
      $http['delete'](clmLocations.getLicenseUploadUrl()).then(function() {
        $scope.$close();
      }, function(error) {
        vm.error = messages.getHttpErrorMessage(error);
      });
    }
  }

  UninstallLicenseController.$inject = ['$scope', '$http', 'Messages', 'CLMLocations'];

  angular.module('ProductLicense').controller('uninstall.license.controller', UninstallLicenseController);
}());
