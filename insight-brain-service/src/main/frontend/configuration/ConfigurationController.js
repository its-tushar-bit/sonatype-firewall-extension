/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp, AngularUtils */

(function() {
  'use strict';

  var module = angular.module('Configuration',
    ['ui.router', 'ProductLicense', 'PermissionServiceModule', 'AngularCommon', 'Validators'],
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
      }).state('proprietarycomponents', {
        url: '/proprietarycomponents',
        controller: 'ProprietaryConfigurationController',
        templateUrl: 'configuration/components/proprietary.html?' + clmBuildTimestamp,
        data : {
          title : 'Proprietary Configuration'
        },
        resolve : {
          'isAuthorized' : ['PermissionService', function (PermissionService) {
            return PermissionService.isAuthorized(['MANAGE_PROPRIETARY'], true);
          }]
        }
      });
    }
  ]);

  module.controller('ProprietaryConfigurationController', [
    '$scope', '$http', 'CLMLocations', 'Messages', 'isAuthorized', function($scope, $http, clmLocations, Messages, isAuthorized) {
      $scope.isAuthorized = isAuthorized;

      $scope.doLoad = function() {
        if (isAuthorized) {
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

  module.directive('proprietaryConfigEditor', ['validationHelper', function(validationHelper) {
    return {
      restrict: 'A',
      scope: {
        prefixes: '=',
        regexes: '='
      },
      templateUrl: 'config-editor',
      link: function($scope, element) {
        function resetComponent() {
          $scope.component = {
            prefix: '',
            regex: ''
          };
        }

        function rerunValidators() {
          validationHelper.revalidateChildren(element);
        }

        var PACKAGE_REGEXP = new RegExp('^[^ /.][^ /]*[^ /.]$');
        $scope.isRegex = false;
        resetComponent();

        $scope.validatePackage = function(value) {
          return {
            invalidPrefix: !value || PACKAGE_REGEXP.test(value),
            wildcards: !value || value.indexOf('*') < 0
          };
        };

        $scope.add = function($event, entry, group) {
          group.push(entry);
          resetComponent();

          // Use event object to reset calling form to pristine
          angular.element($event.currentTarget).controller('form').$setPristine();
        };

        $scope.remove = function(index, group) {
          group.splice(index, 1);

          //rerun validator for every input once entry is removed
          rerunValidators();
        };

        $scope.$watch('prefixes', rerunValidators, true);
        $scope.$watch('regexes', rerunValidators, true);
      }
    };
  }]);
}());
