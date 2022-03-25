/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faUserAlt } from '@fortawesome/pro-regular-svg-icons';
import template from './mainHeader.html';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import {
  selectIsSourceControlSupported,
  selectIsDataInsightsSupported,
  selectIsWebhooksSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

/* global clmServerVersion */
const globalMajorMinorVersion = (clmServerVersion ? `${clmServerVersion}` : '').split('.').splice(0, 2).join('.');
function MainHeaderController($rootScope, $scope, PermissionService, CurrentUser, routeStateUtilService, $ngRedux) {
  var vm = this;
  vm.faUserAlt = faUserAlt;
  vm.permissions = {};
  vm.$onInit = doLoad;
  vm.hasAnyPermission = hasAnyPermission;
  vm.isLoggedIn = isLoggedIn;
  vm.isWebhooksSupported = undefined;
  vm.login = login;
  vm.shouldShowLoginButton = false;
  vm.majorMinorVersion = globalMajorMinorVersion;
  vm.isSourceControlSupported = undefined;

  function hasAnyPermission() {
    return !angular.equals({}, vm.permissions);
  }

  function checkShowLoginButton() {
    routeStateUtilService.stateRequiresAuthentication().then((stateRequiresAuthentication) => {
      vm.shouldShowLoginButton = !stateRequiresAuthentication && !isLoggedIn();
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

    CurrentUser.waitForLogin().then(function () {
      const unsubscribe = $ngRedux.connect(mapStateToThis)(vm);
      $scope.$on('$destroy', unsubscribe);

      PermissionService.getValidPermissions(validPermissions).then(function (data) {
        const perms = {};
        angular.forEach(data, function (permission) {
          perms[permission] = true;
        });
        vm.permissions = perms;
      });

      $ngRedux.dispatch(actions.fetchProductFeaturesIfNeeded());
    });

    checkShowLoginButton();
  }

  function isLoggedIn() {
    return $rootScope.username;
  }

  function login() {
    CurrentUser.fetch();
  }

  $rootScope.$on('$stateChangeSuccess', checkShowLoginButton);
}

export const mapStateToThis = (state) => ({
  isWebhooksSupported: selectIsWebhooksSupported(state),
  isLabsDataInsightsEnabled: selectIsDataInsightsSupported(state),
  isSourceControlSupported: selectIsSourceControlSupported(state),
});

MainHeaderController.$inject = [
  '$rootScope',
  '$scope',
  'PermissionService',
  'CurrentUser',
  'routeStateUtilService',
  '$ngRedux',
];

export default {
  controller: MainHeaderController,
  controllerAs: 'vm',
  template,
  bindings: {
    productEdition: '@',
  },
};
