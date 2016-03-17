/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp */
(function() {
  'use strict';

  var module = angular.module('ProductLicense',
      ['ui.router', 'AngularCommon', 'ngUpload', 'ngCookies', 'CLMLocation'],
      ['$stateProvider', function($stateProvider) {
        $stateProvider.state('productlicense', {
          url: '/productlicense',
          controller: 'ProductLicenseController',
          templateUrl: 'configuration/components/license.html?' + clmBuildTimestamp,
          data : {
            title : 'Product License'
          },
          resolve : {
            'isAuthorized' : ['PermissionService', function (PermissionService) {
              return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
            }]
          }
        });
      }
      ]);

  module.controller('ProductLicenseController', [
    '$http', '$scope', 'CLMLocations', '$timeout', '$window', '$cookies', '$modal', 'Messages', 'ErrorDialog', 'isAuthorized',
    function($http, $scope, clmLocations, $timeout, $window, $cookies, $modal, Messages, ErrorDialog, isAuthorized) {

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
        $modal.open({
          animation: false,
          backdrop: 'static',
          keyboard: false,
          windowClass: 'clm-modal',
          templateUrl: 'license-installed-modal-template'
        }).result.then($scope.reload);

        $timeout($scope.reload, 5000);
      }

      function showUninstalled() {
        $modal.open({
          animation: false,
          backdrop: 'static',
          keyboard: false,
          windowClass: 'clm-modal',
          templateUrl: 'license-uninstalled-modal-template'
        }).result.then($scope.reload);
        $timeout($scope.reload, 5000);
      }

      function showError(content) {
        ErrorDialog.open(content);
      }

      $scope.reload = function() {
        $window.location.reload();
      };

      $scope.viewUninstallLicense = function() {
        $modal.open({
          animation: false,
          backdrop: 'static',
          keyboard: false,
          windowClass: 'clm-modal',
          templateUrl: 'license-uninstall-modal-template',
          controller: 'uninstall.license.controller as vm'
        }).result.then(function () {
          showUninstalled();
        });
      };

      $scope.onFileChanged = function() {
        $modal.open({
          animation: false,
          backdrop: 'static',
          keyboard: false,
          windowClass: 'clm-modal',
          templateUrl: 'eula-modal-template'
        }).result.then(function () {
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
        }, function () {
          $window.location.reload();
        });
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
