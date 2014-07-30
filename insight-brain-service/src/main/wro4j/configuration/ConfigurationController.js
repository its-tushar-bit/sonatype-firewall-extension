/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp, AngularUtils */

(function() {
  'use strict';

  var module = angular.module('Configuration',
    ['ListEditor', 'ui.router', 'ManagementModule', 'ProductLicense', 'PermissionServiceModule'], ['$stateProvider', function($stateProvider) {
      $stateProvider.state('management.configuration', {
        parent: 'management',
        url: '/configuration',
        controller: 'ConfigurationController',
        templateUrl: '../configuration-assets/components/configuration-navigator.html?' + clmBuildTimestamp
      }).state('productlicense', {
        url: '/productlicense',
        controller: 'ProductLicenseController',
        templateUrl: '../configuration-assets/components/license.html?' + clmBuildTimestamp,
        data : {
          title : 'Product License'
        },
        resolve : {
          'hasAdminPermission' : ['PermissionService', function (PermissionService) {
            return PermissionService.isAuthorized(['ADMIN'], true);
          }]
        }
      }).state('proprietarycomponents', {
        url: '/proprietarycomponents',
        controller: 'ProprietaryConfigurationController',
        templateUrl: '../configuration-assets/components/proprietary.html?' + clmBuildTimestamp,
        data : {
          title : 'Proprietary Configuration'
        },
        resolve : {
          'hasAdminPermission' : ['PermissionService', function (PermissionService) {
            return PermissionService.isAuthorized(['ADMIN'], true);
          }]
        }
      });
    }
  ]);

  module.controller('ProprietaryConfigurationController', [
    '$scope', '$http', 'CLMLocations', 'Messages', 'hasAdminPermission', function($scope, $http, clmLocations, Messages, hasAdminPermission) {
      var PACKAGE_REGEXP = new RegExp('^[^ /.][^ /]*[^ /.]$');
      $scope.isRegex = false;
      $scope.isAuthorized = hasAdminPermission;

      $scope.validatePackage = function(value, isRegex) {
        return {
          invalidPrefix: !value || isRegex || PACKAGE_REGEXP.test(value),
          wildcards: !value || isRegex || value.indexOf('*') < 0
        };
      };

      $scope.doLoad = function() {
        if (hasAdminPermission) {
          $http.get(clmLocations.getProprietaryConfig()).success(function(data) {
            $scope.proprietary = data;
            $scope.reset();
          }).error(function() {
            $scope.loadError = Messages.getHttpErrorMessage(arguments);
          });
        }
      };

      $scope.save = function() {
        var proprietary = angular.extend({}, $scope.proprietary,
          { packages: angular.copy($scope.packages), regexes: angular.copy($scope.regexes) });

        $scope.saving = true;

        $http.put(clmLocations.getProprietaryConfig() + '/update', proprietary).success(function() {
          $scope.saving = false;
          $scope.proprietary = proprietary;
          $scope.reset();
        }).error(function() {
          $scope.saving = false;
          $scope.error = [AngularUtils.toAlert(Messages.getHttpErrorMessage(arguments))];
        });
      };

      $scope.reset = function() {
        $scope.packages = angular.copy($scope.proprietary.packages);
        $scope.regexes = angular.copy($scope.proprietary.regexes);
        $scope.error = null;
      };

      $scope.isDirty = function() {
        return $scope.packages && $scope.proprietary &&
          (!angular.equals($scope.packages, $scope.proprietary.packages) ||
            !angular.equals($scope.regexes, $scope.proprietary.regexes));
      };

      $scope.doLoad();

      $scope.$on('pageChangeStarted', function(event) {
        if ($scope.isDirty()) {
          event.preventDefault();
        }
      });
    }
  ]);
}());
