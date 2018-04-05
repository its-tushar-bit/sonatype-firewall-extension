/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './gettingStarted.html';
import {VISITED_ACTION} from './gettingStartedUsageTelemetryService';

export default {
  controller: GettingStartedController,
  controllerAs: 'vm',
  template: template
};

function GettingStartedController($q, $rootScope, $http, PermissionService, CLMLocations, CurrentUser,
                                  gettingStartedUsageTelemetryService) {
  const vm = this;

  Object.assign(vm, {
    validPermissions: undefined,
    license: undefined,
    error: undefined,
    shouldDisplayChangePassword: undefined,
    shouldDisplayHdsUnreachable: undefined,
    isDefaultUser: undefined,

    $onInit() {
      // if license was just installed, page will be reloaded. Until it is - show loading indicator.
      if (!$rootScope.licensed) {
        return;
      }

      firePageVisitedEvent();
      vm.error = undefined;

      loadDataForAllUsers().then(results => {
        vm.validPermissions = results[0];
        vm.shouldDisplayHdsUnreachable = results[1].data === 'false';
        return isAdmin() ? loadDataForAdminUsers() : null;
      }).then(results => {
        if (results) {
          vm.license = results[0].data;
          vm.shouldDisplayChangePassword = results[1].data !== 'true';
          vm.isDefaultUser = results[2].username === 'admin';
        }
      }).catch(error => {
        vm.error = error;
      });
    },

    isLoading() {
      return !vm.isDataLoaded() && vm.error === undefined;
    },

    isDataLoaded() {
      return vm.validPermissions !== undefined && !(isAdmin() && vm.license === undefined);
    },

    isAuthorizedToViewSystemSetup() {
      return vm.validPermissions.length > 0;
    }
  });

  function isAdmin() {
    return vm.validPermissions.indexOf('CONFIGURE_SYSTEM') >= 0;
  }

  function loadDataForAllUsers() {
    const promises = [
      PermissionService.getValidPermissions(['CONFIGURE_SYSTEM', 'ADD_APPLICATION']),
      $http.get(CLMLocations.getIsHdsReachable())
    ];
    return $q.all(promises);
  }

  function loadDataForAdminUsers() {
    const promises = [
      $http.get(CLMLocations.getLicenseSummaryUrl()),
      $http.get(CLMLocations.getIsAdminDefaultPasswordChanged()),
      CurrentUser
    ];
    return $q.all(promises);
  }

  function firePageVisitedEvent() {
    gettingStartedUsageTelemetryService.submitData(VISITED_ACTION);
  }
}

GettingStartedController.$inject = [
  '$q', '$rootScope', '$http', 'PermissionService', 'CLMLocations', 'CurrentUser', 'gettingStartedUsageTelemetryService'
];
