/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmServerVersion, clmBuildTimestamp */
function MainHeaderController($rootScope, $state, $scope, ProductFeatures, PermissionService,
                              systemConfigurationPropertyService) {
  var vm = this;

  vm.$state = $state;
  vm.isDashboardLicensed = ProductFeatures.isDashboardLicensed;
  vm.isSuccessMetricsEnabled = false;
  vm.permissions = {};
  vm.$onInit = doLoad;
  vm.getServerVersion = getServerVersion;
  vm.hasAnyPermission = hasAnyPermission;
  vm.isLoggedIn = isLoggedIn;
  vm.isLicensed = isLicensed;

  function getServerVersion() {
    return clmServerVersion;
  }

  function hasAnyPermission() {
    return !angular.equals({}, vm.permissions);
  }

  function doLoad() {
    const validPermissions = ['CONFIGURE_SYSTEM', 'MANAGE_PROPRIETARY', 'VIEW_ROLES',
      'MANAGE_AUTOMATIC_APPLICATION_CREATION'];
    PermissionService.getValidPermissions(validPermissions).then(
        function(data) {
          angular.forEach(data, function(permission) {
            vm.permissions[permission] = true;
          });
        });
    systemConfigurationPropertyService.isSuccessMetricsEnabled().then(function(data) {
      vm.isSuccessMetricsEnabled = data;
    });
  }

  $scope.$on('successMetricsConfigurationUpdated', function(event, newValue) {
    vm.isSuccessMetricsEnabled = newValue;
  });

  function isLoggedIn() {
    return $rootScope.username;
  }

  function isLicensed() {
    return $rootScope.licensed;
  }
}

MainHeaderController.$inject = [
  '$rootScope', '$state', '$scope', 'ProductFeatures', 'PermissionService', 'systemConfigurationPropertyService'
];

angular.module('mainHeader').component('mainHeader', {
  controller: MainHeaderController,
  controllerAs: 'vm',
  templateUrl: 'mainHeader/mainHeader.html?' + clmBuildTimestamp,
  bindings: {
    productEdition: '@'
  }
});
