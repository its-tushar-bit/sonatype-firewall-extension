/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/*global angular */

(function () {
    'use strict';

    function getMessage(data, status, headersFn, config) {
        if (status === 0) {
            return 'Error: Unable to contact server';
        } else {
            return 'Error: ' + status + ' ' + data;
        }
    }

    var module = angular.module('Configuration', ['ListEditor','ui.compat', 'ManagementModule', 'ProductLicense'], ['$stateProvider', function ($stateProvider) {
      $stateProvider.state('management.configuration', {
        parent : 'management',
        url : '/configuration',
        controller : 'ConfigurationController',
        templateUrl : '../configuration-assets/components/configuration-navigator.html'
      }).state('management.configuration.productlicense',{
        parent : 'management.configuration',
        url: '/productlicense',
        controller: 'ProductLicenseController',
        templateUrl: '../configuration-assets/components/license.html'
      }).state('management.configuration.proprietarypackages',{
        parent : 'management.configuration',
        url: '/proprietarypackages',
        controller: 'ProprietaryConfigurationController',
        templateUrl: '../configuration-assets/components/proprietary.html'
      });
    }]);

    module.controller('ConfigurationController',['$scope', '$state', 'commonCodeFactory', '$location', function ($scope, $state, commonCodeFactory, $location) {
      $scope.$state = $state;
      $scope.$location = $location;

      $scope.configurationPanes = [
        {
          name: 'Product License',
          state: 'management/configuration/productlicense',
          isEnabled: true
        },{
          name: 'Proprietary Packages',
          state: 'management/configuration/proprietarypackages',
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
    }]);

    module.controller('ProprietaryConfigurationController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, clmLocations) {
        var PACKAGE_REGEXP = new RegExp('^[^ /.][^ /]*([^ /.*]|[^ /.*]\\*)$'); 

        $scope.doLoad = function () {
            $http.get(clmLocations.getProprietaryConfig(), { params : { "ts" : new Date().getTime() } }).success(function (data) {
                $scope.proprietary = data;
                $scope.reset();
            }).error(function () {
                $scope.loadError = getMessage.apply(null, arguments);
            });
        };

        $scope.save = function () {
            var proprietary = angular.extend({}, $scope.proprietary, { packages : angular.copy($scope.packages) });

            $scope.saving = true;

            $http.put(clmLocations.getProprietaryConfig(), proprietary).success(function () {
                $scope.saving = false;
                $scope.proprietary = proprietary;
            }).error(function (data, status, headersFn, config) {
                $scope.saving = false;
                $scope.error = getMessage.apply(null, arguments);
            });
        };

        $scope.reset = function () {
            $scope.packages = angular.copy($scope.proprietary.packages);
        };

        $scope.setEditorError = function (error) {
            $scope.error = error;
        };

        $scope.validatePackage = function (value) {
            return PACKAGE_REGEXP.test(value) ? null : 'Invalid package prefix, enter e.g. com.mycompany';
        };

        $scope.doLoad();
    }]);
}());
