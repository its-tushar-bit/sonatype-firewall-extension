/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function() {
  'use strict';

  var module = angular.module('LdapConfiguration', [
    'CLMLocation', 'Hudson', 'ResourceModule', 'ui.compat', 'AngularCommon', 'CommonServices'
  ]);

  module.service('LdapConfigurationStore', [
    'CLMLocations', 'CLMResource',
    function(clmLocations, clmResource) {
      return clmResource.getStore({
        id: 'id',
        url: clmLocations.getLdapConfig(),
        template: {
          id: null,
          name: '',
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
        },
        params: {
          timestamp: new Date().getTime()
        }
      });
    }
  ]);

  module.controller('LdapConfigurationController', [
    '$scope', '$state', '$dialog', '$http', 'Messages', 'LdapConfigurationStore', 'CLMLocations',
    function($scope, $state, $dialog, $http, messages, ldapStore, clmLocations) {
      function isDirty() {
        if ($scope.ldap) {
          return $scope.ldap.isDirty();
        }
        return false;
      }

      //make sure user is aware they are about to lose changes
      $scope.$on('pageChangeStarted', function(event) {
        if (isDirty()) {
          event.preventDefault();
        }
      });

      $scope.canSaveEdit = function() {
        return isDirty() && !$scope.ldapEditor.$invalid;
      };

      $scope.testConnection = function() {
        $http.put(clmLocations.getLdapConfig() + '/test', $scope.ldap).success(function (result) {
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
        if ($scope.ldap) {
          if ($scope.ldap.isDirty()) {
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
                    $scope.ldap.$revert();
                  };
                  scope.cancel = function() {
                    dialog.close(true);
                  };
                }
              ]
            }).open();
          }
        }
      };

      $scope.save = function() {
        $scope.saving = true;
        $scope.ldap.$save().then(function(config) {
          $scope.saving = false;
          $scope.alerts.length = 0;
        }, function(error) {
          $scope.saving = false;
          $scope.alerts.push({
            type: 'error',
            msg: 'An error occurred while saving the configuration. (' + messages.getHttpErrorMessage(error) + ')'
          });
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
          $scope.$broadcast('showServerError', arguments)
        });
      };

      $scope.ldapProtocols = ['LDAP', 'LDAPS'];
      $scope.ldapMethods = ['NONE', 'SIMPLE', 'DIGESTMD5', 'CRAMMD5'];

      $scope.$state = $state;

      $scope.doLoad = function() {
        $scope.loadError = null;
        ldapStore.get().then(function(results) {
          $scope.ldap = results.length === 0 ? ldapStore.create() : results[0];
        }, function(errors) {
          $scope.loadError = angular.isArray(errors) ? errors[0] : errors;
        });
      };

      $scope.alerts = [];
      $scope.doLoad();
    }
  ]);
}());
