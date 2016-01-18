/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $ */
(function() {
  'use strict';

  var module = angular.module('ProductLicense',
      ['AngularCommon', 'ngUpload', 'ngCookies', 'CLMLocation']);

  module.controller('ProductLicenseController', [
    '$http', '$scope', 'CLMLocations', '$timeout', '$window', '$cookies', 'Messages', 'ErrorDialog', 'isAuthorized',
    function($http, $scope, clmLocations, $timeout, $window, $cookies, Messages, ErrorDialog, isAuthorized) {

      $scope.summaryUrl = clmLocations.getLicenseSummaryUrl();
      $scope.uploadUrl = clmLocations.getLicenseUploadUrl();
      $scope.isAuthorized = isAuthorized;
      $scope.csrfTokenName = $http.defaults.xsrfHeaderName;
      $scope.csrfTokenValue = $cookies.get($http.defaults.xsrfCookieName);

      $scope.doLoad = function() {
        if (isAuthorized) {
          $scope.error = null;
          $http.get($scope.summaryUrl).success(function(data) {
            $scope.license = data;
          }).error(function(data, status) {
            if (status !== 402) {
              $scope.error = {
                status: status,
                data: data
              };
            } else {
              $scope.license = false;
            }
          });
        }
      };

      function showLicense() {
        $('#eulaModal').modal('hide');
        $('#licenseInstalledModal').modal('show');
        $timeout($scope.reload, 5000);
      }

      function showError(content) {
        $('#eulaModal').modal('hide');
        ErrorDialog.open(content);
      }

      $scope.reload = function() {
        $window.location.reload();
      };

      $scope.viewUninstallLicense = function() {
        $('#licenseUninstallConfirmationModal').modal('show');
      };

      $scope.onFileChanged = function() {
        $('#eulaModal').modal('show');
      };

      $scope.eulaDeclined = function() {
        $window.location.reload();
      };

      $scope.eulaAccepted = function() {
        if ($window.FormData) {
          var form = new FormData();
          form.append('file', $('#license-input')[0].files[0]);
          $http.post($scope.uploadUrl, form).success(function() {
            showLicense();
          }).error(function () {
            showError(Messages.getHttpErrorMessage(arguments));
          });
        }
        else {
          $('#license-form').submit();
        }
      };

      $scope.uploadCompleted = function(content) {
        if (angular.isString(content) && content) {
          if ($scope.clearValue) {
            $scope.clearValue();
          }
          $timeout(function() {
            showError(content);
          }, 0);
        }
        else {
          showLicense();
        }
      };

      $scope.uninstallLicense = function() {
        $http['delete']($scope.uploadUrl).success(function() {
          $('#licenseUninstallConfirmationModal').modal('hide');
          $('#licenseUninstalledModal').modal('show');
          $timeout($scope.reload, 5000);
        }).error(function() {
          ErrorDialog.open(arguments);
        });
      };

      $scope.isLoaded = function() {
        return typeof $scope.license !== 'undefined';
      };

      $scope.doLoad();

    }
  ]);

  module.directive('onFileChange', [
    function() {
      return {
        restrict: 'A',
        scope: false,
        link: function(scope, elem, attr) {
          angular.element(elem).bind('change', function() {
            if (attr.onFileChange) {
              scope.$apply(attr.onFileChange);
            }
          });
        }
      };
    }
  ]);

  module.directive('manualFileClear', function() {
    return {
      restrict: 'A',
      link: function(scope, elem) {
        scope.clearValue = function() {
          elem.wrap('<form>').closest('form').get(0).reset();
          elem.unwrap();
        };
      }
    };
  });
}());
