/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ImportPolicyModalController($rootScope, $scope, $q, $http, $window, $cookies, Messages, CLMAppLocations)
  {
    var vm = this,
        ieDeferred;

    vm.importFile = undefined;
    vm.csrfTokenName = $http.defaults.xsrfHeaderName;
    vm.csrfTokenValue = $cookies.get($http.defaults.xsrfCookieName);
    vm.doSubmit = doSubmit;
    vm.error = undefined;
    vm.uploaded = uploaded;
    vm.importPolicyMask = undefined;
    vm.importPolicyUrl = importPolicyUrl;

    $scope.$on('pageChangeAccepted', function () {
      $scope.$dismiss();
    });

    function setError(message, retryFunction) {
      vm.retry = retryFunction ? retryFunction : vm.retry;

      if (message) {
        vm.error = message;
      }
      else {
        vm.error = 'Error uploading, please check the file.';
      }
    }

    function importPolicyUrl() {
      if (vm.importFile) {
        return CLMAppLocations.getImportPolicyUrl();
      }
    }

    function doSubmit() {
      delete vm.error;
      var form = $('form[name=importPolicy]');

      if ($window.FormData) {
        var formData = new FormData(form[0]);
        vm.importPolicyMask.wrap($http.post(CLMAppLocations.getImportPolicyUrl(), formData)).then(function() {
          $rootScope.$broadcast('policy.imported');
          $scope.$close();
        }, function(error) {
          setError(Messages.getHttpErrorMessage(error), doSubmit);
        });
      }
      else {
        // IE9 case, trigger ng-upload
        ieDeferred = $q.defer();
        vm.importPolicyMask.wrap(ieDeferred.promise).then(function() {
          $rootScope.$broadcast('policy.imported');
          $scope.$close();
        }, function(error) {
          setError(Messages.getHttpErrorMessage(error), doSubmit);
        });
        form.submit();
      }
    }

    // Handler for ng-upload progress
    function uploaded(content) {
      if (angular.isString(content)) {
        ieDeferred.reject(content);
      }
      else {
        ieDeferred.resolve();
      }
    }
  }

  ImportPolicyModalController.$inject = [
    '$rootScope', '$scope', '$q', '$http', '$window', '$cookies', 'Messages', 'CLMAppLocations'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('import.policy.modal.controller', ImportPolicyModalController);
}(angular));
