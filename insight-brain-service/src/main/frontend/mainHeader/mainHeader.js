/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmServerVersion, clmBuildTimestamp */
(function() {
  'use strict';

  function MainHeaderController($rootScope, $state, ProductFeatures, PermissionService) {
    var vm = this;

    vm.productEdition = $rootScope.productEdition;
    vm.$state = $state;
    vm.isDashboardLicensed = ProductFeatures.isDashboardLicensed;
    vm.permissions = {};
    vm.$onInit = loadPermissions;
    vm.getServerVersion = getServerVersion;
    vm.hasAnyPermission = hasAnyPermission;

    function getServerVersion() {
      return clmServerVersion;
    }

    function hasAnyPermission() {
      return !angular.equals({}, vm.permissions);
    }

    function loadPermissions() {
      PermissionService.getValidPermissions([
        'CONFIGURE_SYSTEM', 'MANAGE_PROPRIETARY', 'VIEW_ROLES'
      ]).then(function(permissions) {
        angular.forEach(permissions, function(permission) {
          vm.permissions[permission] = true;
        });
      });
    }
  }

  MainHeaderController.$inject = ['$rootScope', '$state', 'ProductFeatures', 'PermissionService'];

  angular.module('mainHeader').component('mainHeader', {
    controller: MainHeaderController,
    controllerAs: 'vm',
    templateUrl : 'mainHeader/mainHeader.html?' + clmBuildTimestamp
  });

}());
