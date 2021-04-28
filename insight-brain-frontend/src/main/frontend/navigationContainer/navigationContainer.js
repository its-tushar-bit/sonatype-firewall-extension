/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { load as loadAdvancedSearchConfig } from '../configuration/advancedSearch/advancedSearchConfigActions';
import { loadStatus as loadFirewallStatus } from '../firewall/firewallActions';
import template from './navigationContainer.html';
import { path } from 'ramda';

/* global angular, clmServerVersion, clmBuildTimestamp */
function NavigationContainerController(
  $rootScope,
  $state,
  $scope,
  CurrentUser,
  ProductFeatures,
  systemConfigurationPropertyService,
  $ngRedux
) {
  var vm = this;
  vm.$state = $state;
  vm.isDashboardAvailable = ProductFeatures.isDashboardAvailable;
  vm.isReportsListAvailable = ProductFeatures.isReportsListAvailable;
  vm.isSuccessMetricsEnabled = false;
  vm.isAdvancedSearchEnabled = false;
  vm.$onInit = doLoad;
  vm.getReleaseVersion = getReleaseVersion;
  vm.isLoggedIn = isLoggedIn;
  vm.isLicensed = isLicensed;
  vm.isFirewallSupported = false;
  vm.isFirewallEnabled = false;
  vm.isAdvancedLegalPackSupported = false;

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

  function doLoad() {
    CurrentUser.waitForLogin().then(function () {
      systemConfigurationPropertyService.isSuccessMetricsEnabled().then(function (data) {
        vm.isSuccessMetricsEnabled = data;
      });

      const unsubscribe = $ngRedux.connect(mapStateToThis)(vm);
      $scope.$on('$destroy', unsubscribe);

      $ngRedux.dispatch(loadAdvancedSearchConfig());
      $ngRedux.dispatch(loadFirewallStatus());

      ProductFeatures.load().then(function () {
        vm.isFirewallSupported =
          ProductFeatures.isAvailable('firewall') && ProductFeatures.isAvailable('release-integrity');

        vm.isAdvancedLegalPackSupported = ProductFeatures.isAvailable('advanced-legal-pack');
      });
    });
  }

  $scope.$on('successMetricsConfigurationUpdated', function (event, newValue) {
    vm.isSuccessMetricsEnabled = newValue;
  });

  function isLoggedIn() {
    return $rootScope.username;
  }

  function isLicensed() {
    return $rootScope.licensed;
  }
}

function mapStateToThis(state) {
  return {
    isAdvancedSearchEnabled: path(['advancedSearchConfig', 'serverData', 'isEnabled'], state),
    isFirewallEnabled: path(['firewall', 'statusState', 'isEnabled'], state),
  };
}

NavigationContainerController.$inject = [
  '$rootScope',
  '$state',
  '$scope',
  'CurrentUser',
  'ProductFeatures',
  'systemConfigurationPropertyService',
  '$ngRedux',
];

export default {
  controller: NavigationContainerController,
  controllerAs: 'vm',
  template,
  bindings: {
    productEdition: '@',
  },
};
