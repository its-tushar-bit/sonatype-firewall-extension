/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */

(function() {
  'use strict';

  var module = angular.module('Configuration',
    ['ListEditor', 'ui.router', 'ManagementModule', 'ProductLicense'], ['$stateProvider', function($stateProvider) {
      $stateProvider.state('management.configuration', {
        parent: 'management',
        url: '/configuration',
        controller: 'ConfigurationController',
        templateUrl: '../configuration-assets/components/configuration-navigator.html?' + clmBuildTimestamp
      }).state('management.configuration.productlicense', {
        parent: 'management.configuration',
        url: '/productlicense',
        controller: 'ProductLicenseController',
        templateUrl: '../configuration-assets/components/license.html?' + clmBuildTimestamp
      }).state('management.configuration.proprietarycomponents', {
        parent: 'management.configuration',
        url: '/proprietarycomponents',
        controller: 'ProprietaryConfigurationController',
        templateUrl: '../configuration-assets/components/proprietary.html?' + clmBuildTimestamp
      });
    }
  ]);

  module.controller('ConfigurationController', [
    '$scope', '$state', 'commonCodeFactory', '$location', function($scope, $state, commonCodeFactory, $location) {
      $scope.$state = $state;
      $scope.$location = $location;

      $scope.configurationPanes = [
        {
          name: 'Product License',
          state: 'management/configuration/productlicense',
          isEnabled: true
        },
        {
          name: 'Proprietary Components',
          state: 'management/configuration/proprietarycomponents',
          isEnabled: true
        },
        {
          name: 'LDAP',
          state: 'management/configuration/ldap',
          isEnabled: true
        }
      ];

      for (var i = 0; i < $scope.configurationPanes.length; i++) {
        var normalizedState = $scope.configurationPanes[i].state.replace(/\//g, '.');
        if ($scope.$state.current.name.indexOf(normalizedState) !== -1) {
          $scope.$state.selectedPane = $scope.configurationPanes[i];
          break;
        }
      }

      $scope.$watch('$state.current.name', function() {
        if ($state.current.name === 'management.configuration') {
          $state.transitionTo('management.configuration.productlicense');
        }
      });

      $scope.syncAlerts = [];
      var error = commonCodeFactory.getEncodedQueryString('errorMessage');
      if (error) {
        $scope.syncAlerts.push({ type: 'error', msg: decodeURIComponent(error) });
      }
    }
  ]);

  module.controller('ProprietaryConfigurationController', [
    '$scope', '$http', 'CLMLocations', 'Messages', function($scope, $http, clmLocations, Messages) {
      var PACKAGE_REGEXP = new RegExp('^[^ /.][^ /]*[^ /.]$');
      $scope.isRegex = false;

      $scope.add = function() {
        if ($scope.isRegex === true) {
          if ($scope.validateRegex($scope.currentEntry)) {
            $scope.regexes.push($scope.currentEntry);
          }
        }
        else {
          if ($scope.validatePackage($scope.currentEntry)) {
            $scope.packages.push($scope.currentEntry);
          }
        }
        $scope.currentEntry = '';
        $scope.isRegex = false;
      };

      $scope.remove = function(index) {
        $scope.packages.splice(index, 1);
      };

      $scope.removeRegex = function(index) {
        $scope.regexes.splice(index, 1);
      };

      $scope.doLoad = function() {
        $http.get(clmLocations.getProprietaryConfig()).success(function(data) {
            $scope.proprietary = data;
            $scope.reset();
          }).error(function() {
            $scope.loadError = Messages.getHttpErrorMessage(arguments);
          });
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
          $scope.error =  Messages.getHttpErrorMessage(arguments);
        });
      };

      $scope.reset = function() {
        $scope.packages = angular.copy($scope.proprietary.packages);
        $scope.regexes = angular.copy($scope.proprietary.regexes);
      };

      $scope.isDirty = function() {
        return $scope.packages && $scope.proprietary &&
          (!angular.equals($scope.packages, $scope.proprietary.packages) ||
            !angular.equals($scope.regexes, $scope.proprietary.regexes));
      };

      $scope.validatePackage = function(value) {
        if($scope.packages.indexOf(value) !== -1) {
          $scope.error = 'Package already specified';
        }
        else if (value && !PACKAGE_REGEXP.test(value)) {
          $scope.error = 'Invalid package prefix, enter e.g. com.mycompany';
        }
        else if (value && value.indexOf('*') >= 0) {
          $scope.error = 'Wildcards are not allowed/required for packages';
        }
        else {
          $scope.error = null;
        }
        return $scope.error === null;
      };

      $scope.validateRegex = function(value) {
        if ($scope.regexes.indexOf(value) !== -1) {
          $scope.error = 'Regex already specified';
        }
        return $scope.error === null;
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
