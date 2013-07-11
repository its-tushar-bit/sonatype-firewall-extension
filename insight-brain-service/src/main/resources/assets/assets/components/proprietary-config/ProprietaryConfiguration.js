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

    var module = angular.module('Configuration', ['ListEditor','ui.compat', 'ManagementModule'], ['$stateProvider', function ($stateProvider) {
      $stateProvider.state('management.configuration', {
        parent : 'management',
        url : '/configuration',
        controller : 'ConfigurationController',
        templateUrl : '../application-assets/components/configuration-navigator.html'
      }).state('management.configuration.proprietarypackages',{
            parent : 'management.configuration',
            url: '/proprietarypackages',
            controller: 'ProprietaryConfigurationController',
            templateUrl: '../application-assets/components/admin.html'
          })
    }]);

    module.controller('ConfigurationController',['$scope', '$state', 'commonCodeFactory', function ($scope, $state, commonCodeFactory) {
      $scope.$state = $state;

      $scope.configurationPanes = [
        {
          name: 'Proprietary',
          state: 'management/configuration/proprietarypackages',
          isEnabled: true
        }
      ];

      for (var i = 0; i < $scope.configurationPanes.length; i++) {
        var normalizedState = $scope.configurationPanes[i].state.replace('/', '.');
        if ($scope.$state.current.name.indexOf(normalizedState) !== -1) {
          $scope.$state.selectedPane = $scope.configurationPanes[i];
          break;
        }
      }

      $scope.$watch('$state.current.name', function() {
        if ($state.current.name === 'configuration') {
          $state.transitionTo('management.configuration.proprietarypackages');
        }
      });

      $scope.syncAlerts = [];
      var error = commonCodeFactory.getEncodedQueryString('errorMessage');
      if (error) {
        $scope.syncAlerts.push({ type: 'error', msg: decodeURIComponent(error) });
      }
    }]);

    module.controller('ProprietaryConfigurationController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, clmLocations) {
        var PACKAGE_REGEXP = new RegExp('^[^ /.][^ /]*[^ /.]$'); 

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
            return PACKAGE_REGEXP.test(value);
        };

        $scope.doLoad();
    }]);
}());
