/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function() {
  'use strict';

  var module = angular.module('LdapConfiguration', //
  ['CLMLocation', 'Hudson', 'ResourceModule', 'ui.compat', 'AngularCommon', 'CommonServices', 'Configuration'], //
  ['$stateProvider', function($stateProvider) {
    $stateProvider.state('management.configuration.ldap', {
      parent: 'management.configuration',
      url: '/ldap',
      controller: 'LdapConfigurationController',
      templateUrl: '../configuration-assets/components/ldap.html?' + clmBuildTimestamp
    }).state('management.configuration.ldap.connection', {
      parent: 'management.configuration.ldap',
      controller: 'LdapConnectionController',
      templateUrl: '../configuration-assets/components/ldap-connection.html?' + clmBuildTimestamp
    });
  }]);

  module.service('LdapConfigurationStore', [
    'CLMLocations', 'CLMResource', 
    function(clmLocations, clmResource) {
      return clmResource.getStore({
        id: 'id',
        url: clmLocations.getLdapConfig(),
        template: {
          id: null,
          name: ''
        },
        params: {
          timestamp: new Date().getTime()
        }
      });
    }
  ]);

  module.controller('LdapConfigurationController', [
    '$scope', '$state', 'LdapConfigurationStore', 
    function($scope, $state, ldapStore) {
      function isDirty() {
        if ($scope.ldap) {
          return $scope.ldap.isDirty();
        }
        return false;
      }

      function revealConnectionTab() {
        if ($scope.ldap && $scope.ldap.id) {
          $state.transitionTo('management.configuration.ldap.connection', {}, false);
        }
      }

      //make sure user is aware they are about to lose changes
      $scope.$on('pageChangeStarted', function(event) {
        if (isDirty()) {
          event.preventDefault();
        }
      });

      $scope.$on('$stateChangeSuccess', function() {
        if ($state.current.name === 'management.configuration.ldap') {
          revealConnectionTab();
        }
      });

      $scope.isDirty = isDirty;
      $scope.canSaveEdit = function() {
        return isDirty() && !$scope.ldapEditor.$invalid;
      };

      $scope.save = function() {
        $scope.saving = true;
        $scope.ldap.$save().then(function(config) {
          $scope.saving = false;
          if ($state.current.name === 'management.configuration.ldap') {
            revealConnectionTab();
          }
        }, function() {
          $scope.saving = false;
          $scope.$broadcast('showServerError', arguments);
        });
      };

      $scope.confirmDeleteConfiguration = function(config) {
        $('#deleteConfigurationModal').modal('show');
      };

      $scope.deleteConfiguration = function() {
        $('#deleteConfigurationModal').modal('hide');
        $scope.ldap.$delete().then(function() {
          $scope.ldap = null;
          $state.transitionTo('management.configuration');
        }, function() {
          $scope.$broadcast('showServerError', arguments);
        });
      };

      $scope.isCurrentTab = function(tabName) {
        return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
      };

      ldapStore.get().then(function(results) {
        $scope.ldap = results.length === 0 ? ldapStore.create() : results[0];
        if ($scope.ldap.id) {
          revealConnectionTab();
        }
      }, function() {
        $scope.$broadcast('showServerError', arguments);
      });
    }
  ]);

  module.controller('LdapConnectionController', [
    '$scope', '$dialog', '$http','CLMLocations',
    function($scope, $dialog, $http, clmLocations) {

      var origLdapConn = {
        serverId: $scope.ldap.id,
        protocol: 'LDAP',
        hostname: '',
        port: 389,
        searchBase: '',
        authenticationMethod: 'NONE',
        saslRealm: '',
        systemUsername: '',
        systemPassword: '',
        connectionTimeout: 30,
        retryDelay: 300
      };

      //make sure user is aware they are about to lose changes
      $scope.$on('pageChangeStarted', function(event) {
        if ($scope.isDirty()) {
          event.preventDefault();
        }
      });

      $scope.isDirty = function() {
        return !angular.equals(origLdapConn, $scope.ldapConn);
      };

      $scope.canSaveEdit = function() {
        return !$scope.ldapConnectionEditor.$invalid && $scope.isDirty();
      };

      $scope.testConnection = function() {
        $http.put(clmLocations.getLdapConfig() + '/test', $scope.ldapConn).success(function (result) {
            $scope.alerts.length = 0; // clear old alerts
            if (result.status === 'OK') {
                $scope.alerts.push({
                    type: 'success',
                    msg: 'Success!'
                });
            } else {
                $scope.alerts.push({
                    type: 'error',
                    msg: result.message
                });
            }
        }).error(function(data, status) {
            var msg = data;
            $scope.alerts.length = 0; // clear old alerts
            if (status === 0) {
                msg = 'Unable to reach CLM server';
            }
            $scope.alerts.push({
                type: 'error',
                msg: msg
            });
        });
      };

      $scope.closeAlert = function(index) {
        $scope.alerts.splice(index, 1);
      };

      $scope.reset = function() {
        if ($scope.ldapConnectionEditor.$dirty) {
          $dialog.dialog({
            backdrop: true,
            backdropClick: false,
            backdropFade: true,
            dialogFade: true,
            template: '<div class="modal-header"><h3>Unsaved Changes</h3></div>' +
                '<div class="modal-body">There are unsaved changes, continuing will discard them.</div>' +
                '<div class="modal-footer"><button class="btn" ng-click="cancel()">Cancel</button>' +
                '<button class="btn btn-danger" ng-click="discard()">Discard</button></div>',
            controller: [
              '$scope', 'dialog', function(scope, dialog) {
                scope.discard = function() {
                  dialog.close(true);
                  $scope.ldapConn = angular.copy(origLdapConn);
                };
                scope.cancel = function() {
                  dialog.close(true);
                };
              }
            ]
          }).open();
        }
      };

      $scope.save = function() {
        $scope.saving = true;
        $http.put(clmLocations.getLdapConfig() + '/' + $scope.ldap.id + '/connection', $scope.ldapConn).success(function(data) {
          $scope.saving = false;
          origLdapConn = data;
          $scope.ldapConn = angular.copy(origLdapConn);
        }).error(function() {
          $scope.saving = false;
          $scope.$broadcast('showServerError', arguments);
        });
      };

      $scope.ldapProtocols = ['LDAP', 'LDAPS'];
      $scope.ldapMethods = ['NONE', 'SIMPLE', 'DIGESTMD5', 'CRAMMD5'];

      $scope.alerts = [];

      $scope.ldapConn = angular.copy(origLdapConn); // make sure the scope is clean when first entered

      $http.get(clmLocations.getLdapConfig() + '/' + $scope.ldap.id + '/connection').success(function(data) {
        origLdapConn = data;
        $scope.ldapConn = angular.copy(origLdapConn);
      }).error(function(data, status) {
        if (status !== 404) {
          $scope.$broadcast('showServerError', arguments);
        }
      });
    }
  ]);


}());
