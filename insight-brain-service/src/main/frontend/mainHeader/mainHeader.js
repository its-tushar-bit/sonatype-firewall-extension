/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmServerVersion, clmBuildTimestamp */
function MainHeaderController($state, $scope, ProductFeatures, PermissionService,
                              systemConfigurationPropertyService)
{
  var vm = this;

  vm.$state = $state;
  vm.isDashboardLicensed = ProductFeatures.isDashboardLicensed;
  vm.isSuccessMetricsEnabled = false;
  vm.permissions = {};
  vm.$onInit = doLoad;
  vm.getServerVersion = getServerVersion;
  vm.hasAnyPermission = hasAnyPermission;

  function getServerVersion() {
    return clmServerVersion;
  }

  function hasAnyPermission() {
    return !angular.equals({}, vm.permissions);
  }

  function doLoad() {
    PermissionService.getValidPermissions(['CONFIGURE_SYSTEM', 'MANAGE_PROPRIETARY', 'VIEW_ROLES']).then(
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
}

MainHeaderController.$inject = [
  '$state', '$scope', 'ProductFeatures', 'PermissionService', 'systemConfigurationPropertyService'
];

angular.module('mainHeader').component('mainHeader', {
  controller: MainHeaderController,
  controllerAs: 'vm',
  templateUrl: 'mainHeader/mainHeader.html?' + clmBuildTimestamp,
  bindings: {
    productEdition: '@'
  }
});
