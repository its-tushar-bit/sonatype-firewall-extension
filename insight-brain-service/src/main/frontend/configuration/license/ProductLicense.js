/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp */
import template from './license.html';

export default {
  controller: ProductLicenseController,
  bindings: {
    isAuthorized: '<'
  },
  controllerAs: 'vm',
  template: template
};

const getDaysToExpiration = expiryTimestamp => Math.floor((expiryTimestamp - Date.now()) / (1000 * 60 * 60 * 24));

function ProductLicenseController($http, $scope, clmLocations, $timeout, $window, $cookies, Modal, Messages) {
  const vm = this;

  Object.assign(vm, {
    summaryUrl: clmLocations.getLicenseSummaryUrl(),
    uploadUrl: clmLocations.getLicenseUploadUrl(),
    csrfTokenName: $http.defaults.xsrfHeaderName,
    csrfTokenValue: $cookies.get($http.defaults.xsrfCookieName),
    displayUserLimits: undefined,
    displayApplicationLimit: undefined,
    displayFirewallLimit: undefined,
    formMask: undefined,
    loadError: undefined,
    submitError: undefined,

    $onInit() {
      if (vm.isAuthorized) {
        vm.loadError = null;

        $http.get(vm.summaryUrl).then(function({data}) {
          vm.license = Object.assign({}, data, {
            daysToExpiration: getDaysToExpiration(data.expiryTimestamp)
          });

          vm.displayFirewallLimit = data.firewallLicensedUsers !== null;
          vm.displayUserLimits = data.licensedUsersToDisplay !== null || vm.displayFirewallLimit;
          vm.displayApplicationLimit = data.applicationLimitToDisplay !== null;
        }, function(errorResponse) {
          if (errorResponse.status !== 402) {
            vm.loadError = {
              status: errorResponse.status,
              data: errorResponse.data
            };
          }
          else {
            vm.license = false;
          }
        });
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
        controller: 'uninstall.license.controller as vm'
      });
    },

    onFileChanged() {
      Modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        templateUrl: 'eula-modal-template'
      }).result.then(function() {
        if ($window.FormData) {
          var form = new FormData();
          form.append('file', $('#license-input')[0].files[0]);

          vm.formMask.wrap($http.post(vm.uploadUrl, form)).then(function() {
            vm.license = undefined;
            vm.submitError = undefined;
            vm.reload();
          }, function(error) {
            vm.submitError = Messages.getHttpErrorMessage(error);
          });
        }
        else {
          $timeout(function() {
            $('#license-form').submit();
          });
        }
      }, function() {
        $window.location.reload();
      });
    },

    uploadCompleted(content) {
      if (angular.isString(content) && content) {
        if ($scope.clearValue) {
          $scope.clearValue();
        }
        $timeout(function() {
          vm.submitError = content;
        }, 0);
      }
      else {
        vm.formMask.showSuccessMaskBriefly().then(() => vm.reload());
      }
    },

    isLoaded() {
      return typeof vm.license !== 'undefined';
    }
  });
}

ProductLicenseController.$inject = [
  '$http', '$scope', 'CLMLocations', '$timeout', '$window', '$cookies', 'Modal', 'Messages'
];
