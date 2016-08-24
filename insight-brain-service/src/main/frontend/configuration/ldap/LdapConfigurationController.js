/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';

  function showAlert(alerts, alert) {
    alerts.length = 0;
    alerts.push(alert);
  }

  function resetDialog($modal, discardFn, discardLabel) {
    if (!discardLabel) {
      discardLabel = 'Discard';
    }
    return function() {
      $modal.open({
        backdrop: 'static',
        windowClass: 'clm-modal',
        template: '<div id="ldap-unsaved-changes"><div class="clm-modal-header"><h2>Unsaved Changes</h2></div>' +
            '<div class="clm-modal-body">There are unsaved changes, continuing will discard them.</div>' +
            '<div class="clm-modal-footer">' +
              '<button class="btn btn-primary" ng-click="discardChanges()">' + discardLabel + '</button>' +
              '<button class="btn btn-link btn-cancel" ng-click="$close()">Cancel</button>' +
            '</div></div>',
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
        msg = 'Unable to reach IQ Server';
      }
      showAlert($scope.alerts,{
        type: 'error',
        msg: msg
      });
    });
  }

  var module = angular.module('ldap.module');

  module.controller('LdapConfigurationController', [
    '$scope', '$state', '$modal', 'Dialog', 'LdapConfigurationStore', 'CLMLocations', 'ErrorDialog', 'isAuthorized',
    function($scope, $state, $modal, Dialog, ldapStore, clmLocations, ErrorDialog, isAuthorized) {
      function isDirty() {
        if ($scope.ldapNameForm && $scope.ldapNameForm.$visible) {
          return true;
        } else if ($scope.ldap) {
          return $scope.ldap.isDirty();
        }
        return false;
      }

      function setCurrentTab(tabname) {
        $state.go('edit-ldap.' + tabname, { ldapId: $state.params.ldapId });
      }

      $scope.isAuthorized = isAuthorized;

      $scope.$on('pageChangeStarted', function(event) {
        if ($scope.isDirty()) {
          event.preventDefault();
        }
      });

      $scope.isDirty = isDirty;
      $scope.canSaveEdit = function() {
        return $scope.ldapEditor && !$scope.ldapEditor.$invalid && ($scope.ldapNameForm && $scope.ldapNameForm.$visible || $scope.ldap && $scope.ldap.name);
      };

      $scope.save = function() {
        $scope.ldapNameForm.$save();

        if (!isDirty() || $scope.ldapNameForm.$invalid) {
          return;
        }
        $scope.saving = true;
        $scope.ldap.$save().then(function(ldapServer) {
          $scope.saving = false;
          if ($state.current.name === 'edit-ldap' || $state.current.name === 'create-ldap') {
            $state.go('edit-ldap.connection', {ldapId: ldapServer.id});
          }
        }, function() {
          $scope.saving = false;
          ErrorDialog.open(arguments[0]);
        });
      };

      $scope.confirmDeleteConfiguration = function() {
        Dialog.open({
          title : 'Delete Configuration',
          body : 'Are you sure you want to delete this LDAP configuration?',
          id : 'delete-ldap-confirmation',
          buttons : [{
            name : 'Delete',
            type : 'primary',
            click : $scope.deleteConfiguration
          }, {
            name : 'Cancel',
            type: 'cancel'
          }]
        });
      };

      $scope.deleteConfiguration = function () {
        $scope.ldap.$delete().then(function() {
          $scope.ldap = null;
          $state.transitionTo('ldap-servers');
        }, function() {
          ErrorDialog.open(arguments[0]);
        });
      };

      $scope.setCurrentTab = setCurrentTab;

      $scope.doLoad = function () {
        if (isAuthorized) {
          $scope.loadError = null;

          if ($state.params.ldapId) {
            ldapStore.getById($state.params.ldapId).then(function(ldapServer) {
              $scope.ldap = ldapServer.$clone();
              setCurrentTab('connection');
            }, function(error) {
              $scope.loadError = error;
            });
          }
          else {
            ldapStore.get().then(function() {
              $scope.ldap = ldapStore.create();
            }, function(error) {
              $scope.loadError = error;
            });
          }
        }
      };
      $scope.doLoad();
    }
  ]);

  module.controller('LdapConnectionController', [
    '$scope', '$modal', '$http', 'CLMLocations', 'ErrorDialog',
    function($scope, $modal, $http, CLMLocations, ErrorDialog) {
      $scope.ldapProtocols = ['LDAP', 'LDAPS'];
      $scope.ldapMethods = ['NONE', 'SIMPLE', 'DIGESTMD5', 'CRAMMD5'];
      $scope.alerts = [];
      delete $scope.ldapConn;

      var origLdapConn = {
        serverId: $scope.ldap.id
      };

      $scope.$on('pageChangeStarted', function(event) {
        if ($scope.isDirty()) {
          event.preventDefault();
        }
      });

      $scope.isDirty = function() {
        return $scope.ldapConn && !angular.equals(origLdapConn, $scope.ldapConn);
      };

      $scope.canSaveEdit = function() {
        return !$scope.ldapConnectionEditor.$invalid && $scope.isDirty();
      };

      $scope.testInProgress = false;
      $scope.testConnection = function() {
        testRequest($scope, $http, CLMLocations.getLdapConnectionTest(), $scope.ldapConn);
      };

      $scope.reset = resetDialog($modal, function() {
          $scope.ldapConn = angular.copy(origLdapConn);
          $scope.alerts.length = 0;
          $scope.ldapConnectionEditor.$setPristine();
        }
      );

      $scope.save = function() {
        $scope.saving = true;
        $http.put(CLMLocations.getLdapConnectionConfig(), $scope.ldapConn).success(function(data) {
          $scope.saving = false;
          origLdapConn = data;
          $scope.ldapConn = angular.copy(origLdapConn);
          showAlert($scope.alerts, {type:'success', msg: 'Configuration saved.'});
        }).error(function() {
          $scope.saving = false;
          ErrorDialog.open(arguments);
        });
      };

      $scope.$watch('ldapConn.protocol', function(newProtocol) {
        if (newProtocol === 'LDAP' && (!$scope.ldapConn.port || $scope.ldapConn.port === 636)) {
          $scope.ldapConn.port = 389;
        } else if (newProtocol === 'LDAPS' && (!$scope.ldapConn.port || $scope.ldapConn.port === 389)) {
          $scope.ldapConn.port = 636;
        }
      });

      $http.get(CLMLocations.getLdapConnectionConfig()).success(function(data) {
        origLdapConn = data;
        $scope.ldapConn = angular.copy(origLdapConn);
      }).error(function() {
        ErrorDialog.open(arguments);
      });
    }
  ]);

  module.controller('LdapUsermappingController', ['$scope', '$modal', '$http', 'CLMLocations', 'ErrorDialog', '$q',
    function($scope, $modal, $http, CLMLocations, ErrorDialog, $q) {
      $scope.alerts = [];
      delete $scope.ldapUserMapping;// make sure the scope is clean while we query backend

      var origLdapUserMapping = {
        serverId: $scope.ldap.id
      };

      $scope.groupMappingTypes = ['NONE', 'STATIC', 'DYNAMIC'];

      $scope.isDirty = function() {
        return $scope.ldapUserMapping && !angular.equals(origLdapUserMapping, $scope.ldapUserMapping);
      };

      $scope.canSaveEdit = function() {
        return !$scope.ldapUserMappingEditor.$invalid && $scope.isDirty();
      };

      $scope.reset = resetDialog($modal, function () {
        $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
        $scope.alerts.length = 0;
        $scope.ldapUserMappingEditor.$setPristine();
      });

      $scope.$on('pageChangeStarted', function(event) {
        if ($scope.isDirty()) {
          event.preventDefault();
        }
      });

      $scope.save = function() {
        $scope.saving = true;
        $http.put(CLMLocations.getLdapUserMappingConfig(), $scope.ldapUserMapping).success(function(data) {
          $scope.saving = false;
          origLdapUserMapping = data;
          $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
          showAlert($scope.alerts,{type:'success', msg: 'Configuration saved.'});
        }).error(function() {
          $scope.saving = false;
          ErrorDialog.open(arguments);
        });
      };

      $scope.testInProgress = false;

      $scope.checkUserMapping = function() {
        $scope.testInProgress = true;
        $modal.open({
          backdrop: 'static',
          scope: $scope,
          templateUrl: 'configuration/components/ldap-checkusermapping.html?' + clmBuildTimestamp,
          controller: 'LdapCheckUserMappingController',
          windowClass: 'modal modal-ldap',
          resolve: {
            users: function() {
              var deferred = $q.defer();
              $http.put(CLMLocations.getLdapUserMappingTest(), $scope.ldapUserMapping).success(function (users) {
                // Add property that holds the count of fields that are populated
                users.forEach(function(user) {
                  user.fieldCount = 0;
                  user.membership = user.membership.join(', ');
                  ['username', 'realName', 'email', 'membership'].forEach(function(field) {
                    if (user[field]) {
                      user.fieldCount++;
                    }
                  });
                });
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
          templateUrl: 'configuration/components/ldap-checklogin.html?' + clmBuildTimestamp,
          controller: 'LdapCheckLoginController'
        }).result.then(function() {
          $scope.testInProgress = false;
        }, function() {
          $scope.testInProgress = false;
        });
      };

      $scope.isGroupFieldRequired = function(groupMappingType) {
        return $scope.ldapUserMapping && $scope.ldapUserMapping.groupMappingType === groupMappingType;
      };

      $http.get(CLMLocations.getLdapUserMappingConfig()).success(function(data) {
        origLdapUserMapping = data;
        $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
      }).error(function() {
        ErrorDialog.open(arguments);
      });
    }
  ]);

  module.controller('LdapCheckUserMappingController', ['$scope', 'users', function($scope, users) {
    $scope.users = users;
    $scope.infoText = 'Scroll through the table and verify that the values in each column are in the correct format. ' +
      'If they are not, click "Close" and revise your LDAP field mappings.';
  }]);

  module.controller('LdapCheckLoginController', ['$scope', '$http', 'CLMLocations', function($scope, $http, CLMLocations) {
    $scope.alerts = [];
    $scope.testInProgress = false;
    $scope.ldapCredentials = {};
    $scope.testLogin = function() {
      var request = {
        userMapping: $scope.ldapUserMapping,
        username: $scope.ldapCredentials.username,
        password: $scope.ldapCredentials.password
      };
      testRequest($scope, $http, CLMLocations.getLdapLoginTest(), request);
    };
  }]);

}());
