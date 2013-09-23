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
    }).state('management.configuration.ldap.usermapping', {
      parent: 'management.configuration.ldap',
      controller: 'LdapUsermappingController',
      templateUrl: '../configuration-assets/components/ldap-usermapping.html?' + clmBuildTimestamp
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

  function resetDialog($dialog, reset) {
    return function() {
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
              reset();
            };
            scope.cancel = function() {
              dialog.close(true);
            };
          }
        ]
      }).open();
    };
  }

  function preventPageChange($scope) {
    //make sure user is aware they are about to lose changes
    function hander(event) {
      if ($scope.isDirty()) {
        event.preventDefault();
      }
    }
    $scope.$on('pageChangeStarted', hander);
    $scope.$on('ldapStateChangeStarted', hander);
  }

  // certain browsers will happily serve the request from their cache, 
  // thereby showing potentially outdated data to the end user.
  // use unique ?timestamp $http request parameter as a workaround 
  function weHeartIE() {
    return { 
      params: { 
        timestamp: new Date().getTime() 
      } 
    }; 
  }

  module.controller('LdapConfigurationController', [
    '$scope', '$state', '$dialog', 'LdapConfigurationStore', 'CLMLocations',
    function($scope, $state, $dialog, ldapStore, clmLocations) {
      function isDirty() {
        if ($scope.ldap) {
          return $scope.ldap.isDirty();
        }
        return false;
      }

      function setCurrentTab(tabname) {
        var ldapState = 'management.configuration.ldap', targetState = ldapState + '.' + tabname;

        if ($state.current.name !== targetState && $scope.ldap && $scope.ldap.id) {
          // if the current scope/state isDirty, ask the user if it's okay to discard the changes
          var event = $scope.$broadcast('ldapStateChangeStarted');
          if (!event.defaultPrevented) {
            $state.transitionTo(targetState, {}, false);
          } else {
            resetDialog($dialog, function () { 
              $state.transitionTo(targetState, {}, false); 
            })();
          }
        }
      }

      // used by nested scopes to calculate REST endpoint URL
      $scope.getConfigLdapUrl = function (resource) {
        return clmLocations.getLdapConfig() + '/' + $scope.ldap.id + '/' + resource;
      };

      preventPageChange($scope);

      $scope.$on('$stateChangeSuccess', function() {
        if ($state.current.name === 'management.configuration.ldap') {
          setCurrentTab('connection');
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
            setCurrentTab('connection');
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
      $scope.setCurrentTab = setCurrentTab;

      ldapStore.get().then(function(results) {
        $scope.ldap = results.length === 0 ? ldapStore.create() : results[0];
        setCurrentTab('connection');
      }, function() {
        $scope.$broadcast('showServerError', arguments);
      });
    }
  ]);
  
  module.controller('LdapConnectionController', [
    '$scope', '$dialog', '$http','CLMLocations',
    function($scope, $dialog, $http, clmLocations) {

      var origLdapConn = {
        serverId: $scope.ldap.id
      };

      preventPageChange($scope);

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

      $scope.reset = resetDialog($dialog, function() { $scope.ldapConn = angular.copy(origLdapConn); } );

      $scope.save = function() {
        $scope.saving = true;
        $http.put($scope.getConfigLdapUrl('connection'), $scope.ldapConn).success(function(data) {
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

      $scope.ldapConn = angular.copy(origLdapConn); // make sure the scope is clean while we query backend

      $http.get($scope.getConfigLdapUrl('connection'), weHeartIE()).success(function(data) {
        origLdapConn = data;
        $scope.ldapConn = angular.copy(origLdapConn);
      }).error(function(data, status) {
        $scope.$broadcast('showServerError', arguments);
      });
    }
  ]);

  module.controller('LdapUsermappingController', ['$scope', '$dialog', '$http', 
    function($scope, $dialog, $http) {
      var origLdapUserMapping = {
        serverId: $scope.ldap.id,
        userPasswordAttribute: null // to make tests happy, not needed otherwise
      };

      $scope.groupMappingTypes = ['NONE', 'STATIC', 'DYNAMIC'];

      $scope.useUserPasswordAttribute = false;
      $scope.$watch('useUserPasswordAttribute', function(newValue, oldValue) {
        if (!newValue) {
          $scope.ldapUserMapping.userPasswordAttribute = null;
        }
      });

      $scope.isDirty = function() {
        return !angular.equals(origLdapUserMapping, $scope.ldapUserMapping);
      };

      $scope.canSaveEdit = function() {
        return !$scope.ldapUserMappingEditor.$invalid && $scope.isDirty();
      };

      $scope.reset = resetDialog($dialog, function () { 
        $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
        $scope.useUserPasswordAttribute = $scope.ldapUserMapping.userPasswordAttribute != null;
      });

      preventPageChange($scope);

      $scope.save = function() {
        $scope.saving = true;
        $http.put($scope.getConfigLdapUrl('userMapping'), $scope.ldapUserMapping).success(function(data) {
          $scope.saving = false;
          origLdapUserMapping = data;
          $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
        }).error(function() {
          $scope.saving = false;
          $scope.$broadcast('showServerError', arguments);
        });
      };

      $scope.ldapUserMapping = angular.copy(origLdapUserMapping); // make sure the scope is clean while we query backend

      $scope.isGroupFieldRequired = function(groupMappingType) {
        return $scope.ldapUserMapping.groupMappingType === groupMappingType;
      };

      $http.get($scope.getConfigLdapUrl('userMapping'), weHeartIE()).success(function(data) {
        origLdapUserMapping = data;
        $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
        $scope.useUserPasswordAttribute = $scope.ldapUserMapping.userPasswordAttribute != null;
      }).error(function(data, status) {
        $scope.$broadcast('showServerError', arguments);
      });
    }
  ]);
}());
