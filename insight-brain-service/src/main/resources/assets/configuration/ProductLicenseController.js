/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function() {
  'use strict';

  var module = angular.module('ProductLicense', ['AngularCommon', 'ngUpload', 'CLMLocation']);

  module.controller('ProductLicenseController', [
    '$http', '$scope', 'CLMLocations', '$timeout', '$window', 'Messages',
    function($http, $scope, clmLocations, $timeout, $window, Messages) {

      $scope.summaryUrl = clmLocations.getLicenseSummaryUrl();
      $scope.uploadUrl = clmLocations.getLicenseUploadUrl();

      $scope.doLoad = function() {
        $scope.error = null;
        $http.get($scope.summaryUrl).success(function(data) {
          $scope.license = data;
        }).error(function(data, status) {
              if (status !== 402) {
                $scope.error = {
                  status: status,
                  data: data
                };
              }
              else {
                $scope.license = false;
              }
            });
      };

      function showLicense() {
        $('#eulaModal').modal('hide');
        $('#licenseInstalledModal').modal('show');
        $timeout($scope.reload, 5000);
      }

      function showError(content) {
        $('#eulaModal').modal('hide');
        $scope.$broadcast('showError', content);
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
          $http.post($scope.uploadUrl, form, {
            headers : {
              'Content-Type' : undefined
            },
            transformRequest: angular.identity
          }).success(function (data) {
            showLicense();
          }).error(function () {
            showError(Messages.getHttpErrorMessage(arguments));
          });
        }
        else {
          $('input[type=submit]').trigger('click');
        }
      };

      $scope.installLicense = function(content, completed) {
        if (completed) {
          if (content.length === 0) {
            showLicense();
          }
          else {
            $scope.clearValue();
            $timeout(function() {
              showError(content);
            }, 0);
          }
        }
      };

      $scope.uninstallLicense = function() {
        $http['delete']($scope.uploadUrl).success(function(data) {
          $('#licenseUninstallConfirmationModal').modal('hide');
          $('#licenseUninstalledModal').modal('show');
          $timeout($scope.reload, 5000);
        }).error(function() {
              $scope.$broadcast('showServerError', arguments);
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
        link: function(scope, elem, attr, ctrl) {
          angular.element(elem).bind('change', function(event) {
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
          elem.attr('value', '');
        };
      }
    }
  });
}());
