/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  faAnalytics,
  faBug,
  faFileAlt,
  faFireSmoke,
  faSearch,
  faSitemap,
  faTachometerAltFast,
  faUserAlt
} from '@fortawesome/pro-regular-svg-icons';
import {load as loadAdvancedSearchConfig} from '../configuration/advancedSearch/advancedSearchConfigActions';
import {loadStatus as loadFirewallStatus} from '../firewall/firewallActions';
import template from './mainHeader.html';
import {path} from 'ramda';

/* global angular, clmServerVersion, clmBuildTimestamp */
function MainHeaderController($rootScope, $state, $scope, ProductFeatures, PermissionService, CurrentUser,
                              systemConfigurationPropertyService, routeStateUtilService, $ngRedux) {
  var vm = this;

  Object.assign(vm, {faTachometerAltFast, faFileAlt, faSitemap, faAnalytics, faBug, faSearch, faUserAlt, faFireSmoke});
  vm.$state = $state;
  vm.isDashboardAvailable = ProductFeatures.isDashboardAvailable;
  vm.isReportsListAvailable = ProductFeatures.isReportsListAvailable;
  vm.isSuccessMetricsEnabled = false;
  vm.isAdvancedSearchEnabled = false;
  vm.permissions = {};
  vm.$onInit = doLoad;
  vm.getReleaseVersion = getReleaseVersion;
  vm.hasAnyPermission = hasAnyPermission;
  vm.isLoggedIn = isLoggedIn;
  vm.isLicensed = isLicensed;
  vm.isWebhooksSupported = undefined;
  vm.login = login;
  vm.shouldShowLoginButton = shouldShowLoginButton;
  vm.isFirewallSupported = false;
  vm.isFirewallEnabled = false;

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
      'MANAGE_AUTOMATIC_APPLICATION_CREATION', 'MANAGE_AUTOMATIC_SCM_CONFIGURATION'];

    CurrentUser.waitForLogin().then(function() {
      PermissionService.getValidPermissions(validPermissions).then(function(data) {
        angular.forEach(data, function(permission) {
          vm.permissions[permission] = true;
        });
      });

      systemConfigurationPropertyService.isSuccessMetricsEnabled().then(function(data) {
        vm.isSuccessMetricsEnabled = data;
      });

      const unsubscribe = $ngRedux.connect(mapStateToThis)(vm);
      $scope.$on('$destroy', unsubscribe);
      $ngRedux.dispatch(loadAdvancedSearchConfig());
      $ngRedux.dispatch(loadFirewallStatus());

      ProductFeatures.load().then(function() {
        vm.isWebhooksSupported = ProductFeatures.isAvailable('webhooks-for-applications') ||
            ProductFeatures.isAvailable('webhooks-for-repositories');

        vm.isFirewallSupported = ProductFeatures.isAvailable('firewall') &&
            ProductFeatures.isAvailable('release-integrity');
      });

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

  function login() {
    CurrentUser.fetch();
  }

  function shouldShowLoginButton() {
    return !routeStateUtilService.stateRequiresAuthentication() && !isLoggedIn();
  }
}

function mapStateToThis(state) {
  return {
    isAdvancedSearchEnabled: path(['advancedSearchConfig', 'serverData', 'isEnabled'], state),
    isFirewallEnabled: path(['firewall', 'configurationState', 'isEnabled'], state)
  };
}

MainHeaderController.$inject = [
  '$rootScope', '$state', '$scope', 'ProductFeatures', 'PermissionService', 'CurrentUser',
  'systemConfigurationPropertyService', 'routeStateUtilService', '$ngRedux'
];

export default {
  controller: MainHeaderController,
  controllerAs: 'vm',
  template,
  bindings: {
    productEdition: '@'
  }
};
