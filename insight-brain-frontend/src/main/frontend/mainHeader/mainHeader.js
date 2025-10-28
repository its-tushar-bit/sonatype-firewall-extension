/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faUserAlt } from '@fortawesome/pro-regular-svg-icons';
import template from './mainHeader.html';
import {
  selectIsAutomationSupported,
  selectIsWebhooksSupported,
  selectIsCrowdIntegrationSupported,
  selectIsWebhookConfigurationEnabled,
  selectIsProductLicenseConfigurationEnabled,
  selectIsLdapConfigurationEnabled,
  selectIsEmailConfigurationEnabled,
  selectIsProxyConfigurationEnabled,
  selectIsSystemNoticeConfigurationEnabled,
  selectIsSuccessMetricsConfigurationEnabled,
  selectIsAutomaticApplicationConfigurationEnabled,
  selectIsAutomaticScmConfigurationEnabled,
  selectIsAdvancedSearchConfigurationEnabled,
  selectIsShowNotificationMenuEnabled,
  selectIsBaseUrlConfigurationEnabled,
  selectIsMonitoringSupported,
  selectTenantMode,
  selectIsSsoIdpManagedBySonatype,
  selectIsOrgsAndAppsEnabled,
  selectIsZscalerEnabled,
  selectIsSAMLEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectIsSbomManagerOnlyLicense,
  selectIsFirewallOnlyLicense,
  selectHasLifecycleLicense,
  selectHasAuditorLicense,
} from 'MainRoot/productFeatures/productLicenseSelectors';
import {
  selectIsStandaloneDeveloper,
  selectIsStandaloneFirewall,
  selectIsStandaloneSbomManager,
  selectCurrentRouteName,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { fetchUser, waitForLogin } from 'MainRoot/user/userSessionUtils';
import { stateRequiresAuthentication } from 'MainRoot/utility/services/routeStateUtilService';
import { getValidPermissions } from 'MainRoot/util/permissionService';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

/* global clmServerVersion */
const globalMajorMinorVersion = (clmServerVersion ? `${clmServerVersion}` : '').split('.').splice(0, 2).join('.');

function MainHeaderController($rootScope, $scope, $ngRedux) {
  var vm = this;
  vm.faUserAlt = faUserAlt;
  vm.permissions = {};
  vm.$onInit = doLoad;
  vm.hasAnyPermission = hasAnyPermission;
  vm.isLoggedIn = isLoggedIn;
  vm.login = login;
  vm.shouldShowLoginButton = false;
  vm.majorMinorVersion = globalMajorMinorVersion;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);

  function hasAnyPermission() {
    return !angular.equals({}, vm.permissions);
  }

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  function checkShowLoginButton() {
    stateRequiresAuthentication().then((stateRequiresAuth) => {
      vm.shouldShowLoginButton = !stateRequiresAuth && !isLoggedIn();
    });
  }

  function doLoad() {
    const validPermissions = [
      'CONFIGURE_SYSTEM',
      'MANAGE_PROPRIETARY',
      'VIEW_ROLES',
      'MANAGE_AUTOMATIC_APPLICATION_CREATION',
      'MANAGE_AUTOMATIC_SCM_CONFIGURATION',
    ];

    waitForLogin($ngRedux).then(function () {
      getValidPermissions(validPermissions).then(function (data) {
        const perms = {};
        angular.forEach(data, function (permission) {
          perms[permission] = true;
        });
        vm.permissions = perms;
      });
    });
    checkShowLoginButton();
  }

  function isLoggedIn() {
    return $rootScope.username;
  }

  function login() {
    fetchUser($ngRedux);
  }

  $rootScope.$on('$stateChangeSuccess', checkShowLoginButton);
}

export const mapStateToThis = (state) => ({
  isWebhooksSupported: selectIsWebhooksSupported(state),
  isSourceControlSupported: selectIsAutomationSupported(state),
  isCrowdIntegrationEnabled: selectIsCrowdIntegrationSupported(state),
  isWebhookConfigurationEnabled: selectIsWebhookConfigurationEnabled(state),
  isProductLicenseConfigurationEnabled: selectIsProductLicenseConfigurationEnabled(state),
  isLdapConfigurationEnabled: selectIsLdapConfigurationEnabled(state),
  isEmailConfigurationEnabled: selectIsEmailConfigurationEnabled(state),
  isProxyConfigurationEnabled: selectIsProxyConfigurationEnabled(state),
  isSystemNoticeConfigurationEnabled: selectIsSystemNoticeConfigurationEnabled(state),
  isSuccessMetricsConfigurationEnabled: selectIsSuccessMetricsConfigurationEnabled(state),
  isAutomaticApplicationConfigurationEnabled: selectIsAutomaticApplicationConfigurationEnabled(state),
  isAutomaticScmConfigurationEnabled: selectIsAutomaticScmConfigurationEnabled(state),
  isAdvancedSearchConfigurationEnabled: selectIsAdvancedSearchConfigurationEnabled(state),
  isShowNotificationMenuEnabled: selectIsShowNotificationMenuEnabled(state),
  isBaseUrlConfigurationEnabled: selectIsBaseUrlConfigurationEnabled(state),
  isSamlConfigurationEnabled: selectIsSAMLEnabled(state),
  isMonitoringSupported: selectIsMonitoringSupported(state),
  isSsoIdpManagedBySonatype: selectIsSsoIdpManagedBySonatype(state),
  isSingleTenant: selectTenantMode(state) !== 'multi-tenant',
  isSbomManagerOnlyLicense: selectIsSbomManagerOnlyLicense(state),
  isStandaloneDeveloper: selectIsStandaloneDeveloper(state),
  isStandaloneFirewall: selectIsStandaloneFirewall(state),
  isOrgsAndAppsEnabled: selectIsOrgsAndAppsEnabled(state),
  isFirewallOnlyLicense: selectIsFirewallOnlyLicense(state),
  isZscalerEnabled: selectIsZscalerEnabled(state),
  isStandaloneSbomManager: selectIsStandaloneSbomManager(state),
  hasLifecycleLicense: selectHasLifecycleLicense(state),
  hasRoutesResolved: !isNilOrEmpty(selectCurrentRouteName(state)),
  hasAuditorLicense: selectHasAuditorLicense(state),
});

MainHeaderController.$inject = ['$rootScope', '$scope', '$ngRedux'];

export default {
  controller: MainHeaderController,
  controllerAs: 'vm',
  template,
  bindings: {
    productEdition: '@',
  },
};
