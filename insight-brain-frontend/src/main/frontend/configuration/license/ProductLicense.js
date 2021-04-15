/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp */
import { identity } from 'ramda';

import { getDaysFromNow } from './../../util/jsUtil';
import template from './license.html';

export default {
  controller: ProductLicenseController,
  bindings: {
    isAuthorized: '<',
  },
  controllerAs: 'vm',
  template: template,
};

const mkLimit = (name, count) => ({ name, count });

function ProductLicenseController(
  $http,
  $scope,
  clmLocations,
  $timeout,
  $window,
  $cookies,
  Modal,
  Messages
) {
  const vm = this;

  Object.assign(vm, {
    summaryUrl: clmLocations.getLicenseSummaryUrl(),
    uploadUrl: clmLocations.getLicenseUploadUrl(),
    csrfTokenName: $http.defaults.xsrfHeaderName,
    csrfTokenValue: $cookies.get($http.defaults.xsrfCookieName),
    displayUserLimits: undefined,
    displayApplicationLimit: undefined,
    userLimits: undefined,
    formMask: undefined,
    loadError: undefined,
    submitError: undefined,

    $onInit() {
      if (vm.isAuthorized) {
        vm.loadError = null;

        $http.get(vm.summaryUrl).then(
          function ({ data }) {
            vm.license = Object.assign({}, data, {
              daysToExpiration: getDaysFromNow(data.expiryTimestamp),
            });

            vm.userLimits = [
              data.licensedUsersToDisplay &&
                mkLimit('Lifecycle', data.licensedUsersToDisplay),
              data.firewallUsersToDisplay &&
                mkLimit('Firewall', data.firewallUsersToDisplay),
            ].filter(identity);

            vm.displayUserLimits = vm.userLimits.length > 0;
            vm.displayApplicationLimit =
              data.applicationLimitToDisplay !== null;
          },
          function (errorResponse) {
            if (errorResponse.status !== 402) {
              vm.loadError = {
                status: errorResponse.status,
                data: errorResponse.data,
              };
            } else {
              vm.license = false;
            }
          }
        );
      }
    },

    postInstall() {
      // vm.license is still set as it was before the installation
      if (vm.license) {
        vm.reload();
      } else {
        $scope.$emit('licenseInstalled');
      }
    },

    reload() {
      $window.location.reload();
    },

    viewUninstallLicense() {
      Modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        templateUrl: 'license-uninstall-modal-template',
        controller: 'uninstall.license.controller as vm',
      });
    },

    onFileChanged() {
      Modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        templateUrl: 'eula-modal-template',
      }).result.then(
        function () {
          var form = new FormData();
          form.append('file', $('#license-input')[0].files[0]);

          vm.formMask
            .wrap($http.post(vm.uploadUrl, form))
            .then(vm.postInstall, function (error) {
              vm.submitError = Messages.getHttpErrorMessage(error);
            });
        },
        function () {
          $window.location.reload();
        }
      );
    },

    isLoaded() {
      return typeof vm.license !== 'undefined';
    },
  });
}

ProductLicenseController.$inject = [
  '$http',
  '$scope',
  'CLMLocations',
  '$timeout',
  '$window',
  '$cookies',
  'Modal',
  'Messages',
];
