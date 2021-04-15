/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function ImportPolicyModalController(
  $rootScope,
  $scope,
  $q,
  $http,
  $window,
  $cookies,
  Messages,
  CLMContextLocations,
  PolicyHierarchyStore
) {
  var vm = this;

  vm.importFile = undefined;
  vm.csrfTokenName = $http.defaults.xsrfHeaderName;
  vm.csrfTokenValue = $cookies.get($http.defaults.xsrfCookieName);
  vm.doSubmit = doSubmit;
  vm.error = undefined;
  vm.importPolicyMask = undefined;
  vm.importPolicyUrl = importPolicyUrl;

  $scope.$on('pageChangeAccepted', function () {
    $scope.$dismiss();
  });

  function setError(message, retryFunction) {
    vm.retry = retryFunction ? retryFunction : vm.retry;

    if (message) {
      vm.error = message;
    } else {
      vm.error = 'Error uploading, please check the file.';
    }
  }

  function importPolicyUrl() {
    if (vm.importFile) {
      return CLMContextLocations.getImportPolicyUrl();
    }
  }

  function doSubmit(evt) {
    evt.preventDefault();

    delete vm.error;
    var form = $('form[name=importPolicy]');

    var formData = new FormData(form[0]);
    vm.importPolicyMask
      .wrap($http.post(CLMContextLocations.getImportPolicyUrl(), formData))
      .then(
        function () {
          PolicyHierarchyStore.refresh();
          $rootScope.$broadcast('policy.imported');
          $scope.$close();
        },
        function (error) {
          setError(Messages.getHttpErrorMessage(error), doSubmit);
        }
      );
  }
}

ImportPolicyModalController.$inject = [
  '$rootScope',
  '$scope',
  '$q',
  '$http',
  '$window',
  '$cookies',
  'Messages',
  'CLMContextLocations',
  'PolicyHierarchyStore',
];
