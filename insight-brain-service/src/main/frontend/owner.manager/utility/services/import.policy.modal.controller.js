/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ImportPolicyModalController($rootScope, $scope, $q, $http, $window, $cookies, Messages,
                                       CLMAppLocations, formMaskDelay)
  {
    var vm = this,
        ieDeferred;

    vm.importFile = undefined;
    vm.csrfTokenName = $http.defaults.xsrfHeaderName;
    vm.csrfTokenValue = $cookies[$http.defaults.xsrfCookieName];
    vm.doSubmit = doSubmit;
    vm.error = undefined;
    vm.uploaded = uploaded;
    vm.ieImportPolicyUrl = ieImportPolicyUrl;
    
    function setError(message, retryFunction) {
      vm.retry = retryFunction ? retryFunction : vm.retry;

      if (message) {
        vm.error = message;
      }
      else {
        vm.error = 'Error uploading, please check the file.';
      }
    }

    function ieImportPolicyUrl() {
      if (vm.importFile) {
        return CLMAppLocations.getIeImportPolicyUrl();
      }
    }

    function doSubmit() {
      delete vm.error;
      var fileElement = angular.element('#importFile')[0];

      if ($window.FileReader) {
        var reader = new $window.FileReader();

        reader.onload = function() {

          formMaskDelay.wrap($scope, $http.put(CLMAppLocations.getImportPolicyUrl(), reader.result, {
            headers: {
              'Content-Type': 'application/json'
            }
          })).then(function() {
            $rootScope.$broadcast('policy.imported');
            $scope.$close();
          }, function(error) {
            setError(Messages.getHttpErrorMessage(error), doSubmit);
          });
        };

        reader.onerror = function() {
          setError(reader.error.message, doSubmit);
        };

        try {
          reader.readAsText(fileElement.files[0]);
        }
        catch (err) {
          // FF throws an exception in some instances
          setError(err.message, doSubmit);
        }
      }
      else {
        // IE9 case, trigger ng-upload
        ieDeferred = $q.defer();
        formMaskDelay.wrap($scope, ieDeferred.promise).then(function() {
          $rootScope.$broadcast('policy.imported');
          $scope.$close();
        }, function(error) {
          setError(Messages.getHttpErrorMessage(error), doSubmit);
        });
       $('form[name=importPolicy]').find('input[name=submitFile]').trigger('click');
      }
    }

    // Handler for ng-upload progress
    function uploaded(content, complete) {
      if (complete) {
        if (content.length === 0) {
          ieDeferred.resolve();
        }
        else {
          ieDeferred.reject(content);
        }
      }
    }
  }

  ImportPolicyModalController.$inject = [
    '$rootScope', '$scope', '$q', '$http', '$window', '$cookies', 'Messages', 'CLMAppLocations', 'FormMaskDelay'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('import.policy.modal.controller', ImportPolicyModalController);
}(angular));
