/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { path } from 'ramda';
import template from './navigationContainer.html';
import { load as loadAdvancedSearchConfig } from '../configuration/advancedSearch/advancedSearchConfigActions';
import { loadConfiguration as loadSuccessMetricsConfig } from '../configuration/successMetricsConfiguration/successMetricsConfigurationActions';
import { load as loadProductLicense } from '../configuration/license/productLicenseActions';
import { actions as firewallOnboardingActions } from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';

import {
  selectIsAdvancedLegalPackSupported,
  selectIsFirewallSupportedForNavigationContainer,
  selectIsDashboardSupported,
  selectIsDashboardWaiversSupported,
  selectIsReportListSupported,
  selectIsApiPageSupported,
  selectIsShowVersionEnabled,
  selectIsDeveloperDashboardEnabled,
  selectIsOrgsAndAppsEnabled,
  selectIsSbomManagerEnabled,
  selectIsIntegratedEnterpriseReportingSupported,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectIsStandaloneFirewall,
  selectIsSbomManager,
  selectIsStandaloneDeveloper,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsSbomManagerOnlyLicense,
  selectIsFirewallOnlyLicense,
  selectLoadingProducts,
} from 'MainRoot/productFeatures/productLicenseSelectors';

/* global clmServerVersion */
function NavigationContainerController($rootScope, $state, $scope, CurrentUser, $ngRedux) {
  var vm = this;
  vm.$state = $state;
  vm.isDashboardAvailable = isDashboardAvailable;
  vm.isDashboardWaiversAvailable = isDashboardWaiversAvailable;
  vm.isReportsListAvailable = isReportsListAvailable;
  vm.isSuccessMetricsEnabled = false;
  vm.isAdvancedSearchEnabled = false;
  vm.$onInit = doLoad;
  vm.getReleaseVersion = getReleaseVersion;
  vm.isLoggedIn = isLoggedIn;
  vm.isLicensed = isLicensed;
  vm.isFirewallSupported = false;
  vm.isAdvancedLegalPackSupported = false;
  vm.isApiPageEnabled = false;
  vm.isDeveloperDashboardEnabled = false;
  vm.isOrgsAndAppsEnabled = false;
  vm.isSbomManagerEnabled = false;
  vm.isIntegratedEnterpriseReportingSupported = false;
  vm.isSbomManager = false;
  vm.isProductFeaturesLoading = false;
  vm.isSbomManagerOnlyLicense = false;
  vm.isProductsLoading = false;
  vm.isStandaloneDeveloper = false;
  vm.isStandaloneFirewall = false;
  vm.isFirewallOnlyLicense = false;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

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
    const { loadUnconfiguredRepoManagers } = firewallOnboardingActions;
    CurrentUser.waitForLogin().then(function () {
      $ngRedux.dispatch(loadAdvancedSearchConfig());
      $ngRedux.dispatch(loadSuccessMetricsConfig());
      $ngRedux.dispatch(loadProductLicense());
      $ngRedux.dispatch(loadUnconfiguredRepoManagers());
    });
  }

  function isLoggedIn() {
    return $rootScope.username;
  }

  function isLicensed() {
    return $rootScope.licensed;
  }

  function isDashboardAvailable() {
    return vm.isDashboardSupported;
  }

  function isDashboardWaiversAvailable() {
    return vm.isDashboardWaiversSupported;
  }

  function isReportsListAvailable() {
    return vm.isReportListSupported;
  }
}

function mapStateToThis(state) {
  return {
    isAdvancedSearchEnabled: path(['advancedSearchConfig', 'serverData', 'isEnabled'], state),
    isSuccessMetricsEnabled: path(['successMetricsConfiguration', 'serverData', 'enabled'], state),
    isFirewallSupported: selectIsFirewallSupportedForNavigationContainer(state),
    isAdvancedLegalPackSupported: selectIsAdvancedLegalPackSupported(state),
    isDashboardSupported: selectIsDashboardSupported(state),
    isDashboardWaiversSupported: selectIsDashboardWaiversSupported(state),
    isReportListSupported: selectIsReportListSupported(state),
    isApiPageEnabled: selectIsApiPageSupported(state),
    isShowVersionEnabled: selectIsShowVersionEnabled(state),
    isDeveloperDashboardEnabled: selectIsDeveloperDashboardEnabled(state),
    isOrgsAndAppsEnabled: selectIsOrgsAndAppsEnabled(state),
    isSbomManagerEnabled: selectIsSbomManagerEnabled(state),
    isIntegratedEnterpriseReportingSupported: selectIsIntegratedEnterpriseReportingSupported(state),
    isSbomManager: selectIsSbomManager(state),
    isProductFeaturesLoading: selectLoadingFeatures(state),
    isSbomManagerOnlyLicense: selectIsSbomManagerOnlyLicense(state),
    isProductsLoading: selectLoadingProducts(state),
    isStandaloneDeveloper: selectIsStandaloneDeveloper(state),
    isStandaloneFirewall: selectIsStandaloneFirewall(state),
    isFirewallOnlyLicense: selectIsFirewallOnlyLicense(state),
  };
}

NavigationContainerController.$inject = ['$rootScope', '$state', '$scope', 'CurrentUser', '$ngRedux'];

export default {
  controller: NavigationContainerController,
  controllerAs: 'vm',
  template,
  bindings: {
    productEdition: '@',
  },
};
