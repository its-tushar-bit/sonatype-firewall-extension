/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { faUserAlt } from '@fortawesome/pro-regular-svg-icons';
import template from './mainHeader.html';

/* global clmServerVersion */
const globalMajorMinorVersion = (clmServerVersion ? `${clmServerVersion}` : '').split('.').splice(0, 2).join('.');
function MainHeaderController($rootScope, ProductFeatures, PermissionService, CurrentUser, routeStateUtilService) {
  var vm = this;
  vm.faUserAlt = faUserAlt;
  vm.permissions = {};
  vm.$onInit = doLoad;
  vm.hasAnyPermission = hasAnyPermission;
  vm.isLoggedIn = isLoggedIn;
  vm.isWebhooksSupported = undefined;
  vm.login = login;
  vm.shouldShowLoginButton = shouldShowLoginButton;
  vm.majorMinorVersion = globalMajorMinorVersion;
  vm.isSourceControlSupported = undefined;

  function hasAnyPermission() {
    return !angular.equals({}, vm.permissions);
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
      PermissionService.getValidPermissions(validPermissions).then(function (data) {
        const perms = {};
        angular.forEach(data, function (permission) {
          perms[permission] = true;
        });
        vm.permissions = perms;
      });

      ProductFeatures.load().then(function () {
        vm.isWebhooksSupported =
          ProductFeatures.isAvailable('webhooks-for-applications') ||
          ProductFeatures.isAvailable('webhooks-for-repositories');

        vm.isLabsDataInsightsEnabled = ProductFeatures.isAvailable('data-insights');

        vm.isSourceControlSupported = ProductFeatures.isAvailable('automation');
      });
    });
  }

  function isLoggedIn() {
    return $rootScope.username;
  }

  function login() {
    CurrentUser.fetch();
  }

  function shouldShowLoginButton() {
    return !routeStateUtilService.stateRequiresAuthentication() && !isLoggedIn();
  }
}

MainHeaderController.$inject = [
  '$rootScope',
  'ProductFeatures',
  'PermissionService',
  'CurrentUser',
  'routeStateUtilService',
];

export default {
  controller: MainHeaderController,
  controllerAs: 'vm',
  template,
  bindings: {
    productEdition: '@',
  },
};
