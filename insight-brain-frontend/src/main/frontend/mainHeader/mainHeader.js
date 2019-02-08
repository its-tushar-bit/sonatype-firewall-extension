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
  vm.getReleaseVersion = getReleaseVersion;
  vm.hasAnyPermission = hasAnyPermission;
  vm.isLoggedIn = isLoggedIn;
  vm.isLicensed = isLicensed;
  vm.isWebhooksSupported = undefined;

  function getReleaseVersion() {
    const serverVersionWithoutBuildNumber = clmServerVersion.substring(0, clmServerVersion.indexOf('-'));
    const serverVersionParts = serverVersionWithoutBuildNumber.split('.');
    // remove major version if present
    if (serverVersionParts.length === 3) {
      serverVersionParts.shift();
    }
    const [minorVersion, pointVersion] = serverVersionParts;
    let result = minorVersion;
    if (pointVersion !== '0') {
      result += '.';
      result += pointVersion;
    }
    return result;
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

    ProductFeatures.load().then(function() {
      vm.isWebhooksSupported = ProductFeatures.isAvailable('webhooks-for-applications') ||
          ProductFeatures.isAvailable('webhooks-for-repositories');
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

export default {
  controller: MainHeaderController,
  controllerAs: 'vm',
  templateUrl: 'mainHeader/mainHeader.html?' + clmBuildTimestamp,
  bindings: {
    productEdition: '@'
  }
};
