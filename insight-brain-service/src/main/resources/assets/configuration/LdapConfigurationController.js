/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function() {
  'use strict';

  function showAlert(alerts, alert){
    alerts.length = 0;
    alerts.push(alert);
  }

  var module = angular.module('LdapConfiguration',
  ['CLMLocation', 'Hudson', 'ResourceModule', 'ui.router', 'AngularCommon', 'CommonServices', 'Configuration'],
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

  function resetDialog($modal, discardFn, discardLabel) {
    if (!discardLabel) {
      discardLabel = "Discard";
    }
    return function() {
      $modal.open({
        backdrop: 'static',
        template: '<div class="modal-header"><h3>Unsaved Changes</h3></div>' +
            '<div class="modal-body">There are unsaved changes, continuing will discard them.</div>' +
            '<div class="modal-footer"><button class="btn" ng-click="$close()">Cancel</button>' +
            '<button class="btn btn-danger" ng-click="discardChanges()">' + discardLabel + '</button></div>',
        controller: [
          '$scope', function(modalScope) {
            modalScope.discardChanges = function() {
              discardFn();
              modalScope.$close(true);
            };
          }
        ]
      });
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

  /**
   * Executes PUT request passing provided requestData object to the specified requestUrl.
   * Updates $scope.alerts according to response from the server.
   */
  function testRequest($scope, $http, resourceUrl, requestData) {
    $scope.testInProgress = true;
    $http.put(resourceUrl, requestData).success(function(result) {
      $scope.testInProgress = false;
      $scope.alerts.length = 0; // clear old alerts
      if (result.status === 'OK') {
        showAlert($scope.alerts, {
          type: 'success',
          msg: 'Success!'
        });
      } else {
        showAlert($scope.alerts,{
          type: 'error',
          msg: result.message
        });
      }
    }).error(function(data, status) {
      var msg = data;
      $scope.testInProgress = false;
      $scope.alerts.length = 0; // clear old alerts
      if (status === 0) {
        msg = 'Unable to reach CLM server';
      }
      showAlert($scope.alerts,{
        type: 'error',
        msg: msg
      });
    });
  }
  
  module.controller('LdapConfigurationController', [
    '$scope', '$state', '$modal', 'Dialog', 'LdapConfigurationStore', 'CLMLocations',
    function($scope, $state, $modal, Dialog, ldapStore, clmLocations) {
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
            resetDialog($modal, function () { 
              $state.transitionTo(targetState, {}, false); 
            }, 'Continue')();
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
        Dialog.open({
          title : 'Delete Configuration',
          body : 'Are you sure you want to delete this LDAP configuration?',
          buttons : [{
            name : 'Cancel'
          },{
            name : 'Delete',
            type : 'danger',
            click : $scope.deleteConfiguration
          }]
        });
      };

      $scope.deleteConfiguration = function () {
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

      $scope.doLoad = function () {
        $scope.loadError = null;

        ldapStore.get().then(function(results) {
          $scope.ldap = results.length === 0 ? ldapStore.create() : results[0];
          setCurrentTab('connection');
        }, function(error) {
          $scope.loadError = error;
        });
      };
      $scope.doLoad();
    }
  ]);
  
  module.controller('LdapConnectionController', [
    '$scope', '$modal', '$http',
    function($scope, $modal, $http) {

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

      $scope.testInProgress = false;
      $scope.testConnection = function() {
        testRequest($scope, $http, $scope.getConfigLdapUrl('testConnection'), $scope.ldapConn);
      };

      $scope.reset = resetDialog($modal, function() {
          $scope.ldapConn = angular.copy(origLdapConn);
          $scope.alerts.length = 0;
        }
      );

      $scope.save = function() {
        $scope.saving = true;
        $http.put($scope.getConfigLdapUrl('connection'), $scope.ldapConn).success(function(data) {
          $scope.saving = false;
          origLdapConn = data;
          $scope.ldapConn = angular.copy(origLdapConn);
          showAlert($scope.alerts, {type:'success', msg: 'Configuration saved.'});
        }).error(function() {
          $scope.saving = false;
          $scope.$broadcast('showServerError', arguments);
        });
      };

      $scope.ldapProtocols = ['LDAP', 'LDAPS'];
      $scope.ldapMethods = ['NONE', 'SIMPLE', 'DIGESTMD5', 'CRAMMD5'];

      $scope.alerts = [];

      $scope.ldapConn = angular.copy(origLdapConn); // make sure the scope is clean while we query backend

      $scope.$watch('ldapConn.protocol', function(newProtocol) {
        if (newProtocol === 'LDAP' && (!$scope.ldapConn.port || $scope.ldapConn.port === 636)) {
          $scope.ldapConn.port = 389;
        } else if (newProtocol === 'LDAPS' && (!$scope.ldapConn.port || $scope.ldapConn.port === 389)) {
          $scope.ldapConn.port = 636;
        }
      });

      $http.get($scope.getConfigLdapUrl('connection'), weHeartIE()).success(function(data) {
        origLdapConn = data;
        $scope.ldapConn = angular.copy(origLdapConn);
      }).error(function() {
        $scope.$broadcast('showServerError', arguments);
      });
    }
  ]);

  module.controller('LdapUsermappingController', ['$scope', '$modal', '$http', 
    function($scope, $modal, $http) {
      $scope.alerts = [];

      var origLdapUserMapping = {
        serverId: $scope.ldap.id,
        userPasswordAttribute: null // to make tests happy, not needed otherwise
      };

      $scope.groupMappingTypes = ['NONE', 'STATIC', 'DYNAMIC'];

      $scope.isDirty = function() {
        return !angular.equals(origLdapUserMapping, $scope.ldapUserMapping);
      };

      $scope.canSaveEdit = function() {
        return !$scope.ldapUserMappingEditor.$invalid && $scope.isDirty();
      };

      $scope.reset = resetDialog($modal, function () { 
        $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
        $scope.alerts.length = 0;
      });

      preventPageChange($scope);

      $scope.save = function() {
        $scope.saving = true;
        $http.put($scope.getConfigLdapUrl('userMapping'), $scope.ldapUserMapping).success(function(data) {
          $scope.saving = false;
          origLdapUserMapping = data;
          $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
          showAlert($scope.alerts,{type:'success', msg: 'Configuration saved.'});
        }).error(function() {
          $scope.saving = false;
          $scope.$broadcast('showServerError', arguments);
        });
      };

      $scope.testInProgress = false;

      $scope.checkUserMapping = function() {
        $scope.testInProgress = true;
        $modal.open({
          backdrop: 'static',
          scope: $scope,
          templateUrl: '../configuration-assets/components/ldap-checkusermapping.html?' + clmBuildTimestamp,
          controller: 'LdapCheckUserMappingController',
          windowClass: 'modal modal-ldap',
          resolve: {
            users: function($q, $http) {
                var deferred = $q.defer();
                $http.put($scope.getConfigLdapUrl('testUserMapping'), $scope.ldapUserMapping).success(function (users) {
                    deferred.resolve(users);
                }).error(function(data, status, headers, config) {
                    $scope.testInProgress = false;
                    showAlert($scope.alerts, {type: 'error', msg: data});
                    deferred.reject({ data: data, status: status, headers: headers, config: config });
                  });
                return deferred.promise;
            }
          }
        }).result.then(function() {
          $scope.testInProgress = false;
        }, function() {
          $scope.testInProgress = false;
        });
      };

      $scope.checkLogin = function() {
        $scope.testInProgress = true;
        $modal.open({
          backdrop: 'static',
          scope: $scope,
          templateUrl: '../configuration-assets/components/ldap-checklogin.html?' + clmBuildTimestamp,
          controller: 'LdapCheckLoginController',
        }).result.then(function() {
          $scope.testInProgress = false;
        }, function() {
          $scope.testInProgress = false;
        });
      };

      $scope.ldapUserMapping = angular.copy(origLdapUserMapping); // make sure the scope is clean while we query backend

      $scope.isGroupFieldRequired = function(groupMappingType) {
        return $scope.ldapUserMapping.groupMappingType === groupMappingType;
      };

      $http.get($scope.getConfigLdapUrl('userMapping'), weHeartIE()).success(function(data) {
        origLdapUserMapping = data;
        $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
      }).error(function() {
        $scope.$broadcast('showServerError', arguments);
      });
    }
  ]);

  module.controller('LdapCheckUserMappingController', function($scope, users) {
    $scope.users = users;
    $scope.infoText = 'Scroll through the table and verify that the values in each column are in the correct format. ' +
      'If they are not, click "Close" and revise your LDAP field mappings.';
  });

  module.controller('LdapCheckLoginController', function($scope, $http) {
    $scope.alerts = [];
    $scope.testInProgress = false;
    $scope.ldapCredentials = {};
    $scope.testLogin = function() {
      var request = {
        userMapping: $scope.ldapUserMapping,
        username: $scope.ldapCredentials.username,
        password: $scope.ldapCredentials.password
      };
      testRequest($scope, $http, $scope.getConfigLdapUrl('testLogin'), request);
    };
  });

}());
