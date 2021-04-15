/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function UninstallLicenseController($scope, $http, messages, clmLocations, $window) {
  const vm = this;

  Object.assign(vm, {
    submitError: undefined,
    formMask: undefined,

    uninstall() {
      vm.formMask
        .wrap($http['delete'](clmLocations.getLicenseUploadUrl()).then(() => $window.location.reload()))
        .catch((error) => {
          vm.submitError = messages.getHttpErrorMessage(error);
        });
    },
  });
}

UninstallLicenseController.$inject = ['$scope', '$http', 'Messages', 'CLMLocations', '$window'];
