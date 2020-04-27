/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import checkUserMappingTemplate from '../components/ldap-checkusermapping.html';
import checkLoginTemplate from '../components/ldap-checklogin.html';

/* global angular, clmBuildTimestamp */
function showAlert(alerts, alert) {
  alerts.length = 0;
  alerts.push(alert);
}

function resetDialog(Modal, discardFn, discardLabel) {
  if (!discardLabel) {
    discardLabel = 'Discard';
  }
  return function() {
    Modal.open({
      backdrop: 'static',
      template: '<div id="ldap-unsaved-changes"><div class="iq-modal-header"><h2>Unsaved Changes</h2></div>' +
          '<div class="iq-modal-content">There are unsaved changes, continuing will discard them.</div>' +
          '<div class="iq-modal-footer">' +
            '<button class="iq-btn iq-btn--primary" ng-click="discardChanges()">' + discardLabel + '</button>' +
            '<button class="iq-btn" ng-click="$close()">Cancel</button>' +
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
  $http.put(resourceUrl, requestData).then(function(response) {
    var result = response.data;
    $scope.testInProgress = false;
    $scope.alerts.length = 0; // clear old alerts
    if (result.status === 'OK') {
      showAlert($scope.alerts, {
        type: 'success',
        msg: 'Success!'
      });
    }
    else {
      showAlert($scope.alerts, {
        type: 'error',
        msg: result.message
      });
    }
  }, function(errorResponse) {
    var msg = errorResponse.data;
    $scope.testInProgress = false;
    $scope.alerts.length = 0; // clear old alerts
    if (errorResponse.status === 0) {
      msg = 'Unable to reach IQ Server';
    }
    showAlert($scope.alerts, {
      type: 'error',
      msg: msg
    });
  });
}

export function LdapConfigurationController($scope, $state, Dialog, ldapStore, ErrorDialog, isAuthorized) {
  function isDirty() {
    if ($scope.ldapNameForm && $scope.ldapNameForm.$visible) {
      return true;
    }
    else if ($scope.ldap) {
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
    return $scope.ldapEditor && !$scope.ldapEditor.$invalid &&
        ($scope.ldapNameForm && $scope.ldapNameForm.$visible || $scope.ldap && $scope.ldap.name);
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

  $scope.cancel = function() {
    $state.go('ldap-servers');
  };

  $scope.confirmDeleteConfiguration = function() {
    Dialog.open({
      title: 'Delete Configuration',
      body: 'Are you sure you want to delete this LDAP configuration? \
            <p>This will delete all data associated with this LDAP configuration, \
            including all data associated with the LDAP users in this configuration. \
            <p>This action cannot be undone.',
      id: 'delete-ldap-confirmation',
      buttons: [{
        name: 'Delete',
        type: 'primary',
        click: $scope.deleteConfiguration
      }, {
        name: 'Cancel',
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

LdapConfigurationController.$inject = [
  '$scope', '$state', 'Dialog', 'LdapConfigurationStore', 'ErrorDialog', 'isAuthorized'
];

export function LdapConnectionController($scope, Modal, $http, CLMContextLocations, ErrorDialog) {
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
    testRequest($scope, $http, CLMContextLocations.getLdapConnectionTest(), $scope.ldapConn);
  };

  $scope.reset = resetDialog(Modal, function() {
    $scope.ldapConn = angular.copy(origLdapConn);
    $scope.alerts.length = 0;
    $scope.ldapConnectionEditor.$setPristine();
  });

  $scope.save = function() {
    $scope.saving = true;
    $http.put(CLMContextLocations.getLdapConnectionConfig(), $scope.ldapConn).then(function(response) {
      $scope.saving = false;
      origLdapConn = response.data;
      $scope.ldapConn = angular.copy(origLdapConn);
      showAlert($scope.alerts, {type: 'success', msg: 'Configuration saved.'});
    }, function(error) {
      $scope.saving = false;
      ErrorDialog.open(error);
    });
  };

  $scope.$watch('ldapConn.protocol', function(newProtocol) {
    if (newProtocol === 'LDAP' && (!$scope.ldapConn.port || $scope.ldapConn.port === 636)) {
      $scope.ldapConn.port = 389;
    }
    else if (newProtocol === 'LDAPS' && (!$scope.ldapConn.port || $scope.ldapConn.port === 389)) {
      $scope.ldapConn.port = 636;
    }
  });

  $http.get(CLMContextLocations.getLdapConnectionConfig()).then(function(response) {
    origLdapConn = response.data;
    $scope.ldapConn = angular.copy(origLdapConn);
  }, function(error) {
    ErrorDialog.open(error);
  });
}

LdapConnectionController.$inject = ['$scope', 'Modal', '$http', 'CLMContextLocations', 'ErrorDialog'];

export function LdapUsermappingController($scope, Modal, $http, CLMContextLocations, ErrorDialog, $q) {
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

  $scope.reset = resetDialog(Modal, function () {
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
    $http.put(CLMContextLocations.getLdapUserMappingConfig(), $scope.ldapUserMapping).then(function(response) {
      $scope.saving = false;
      origLdapUserMapping = response.data;
      $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
      showAlert($scope.alerts, {type: 'success', msg: 'Configuration saved.'});
    }, function(error) {
      $scope.saving = false;
      ErrorDialog.open(error);
    });
  };

  $scope.testInProgress = false;

  $scope.checkUserMapping = function() {
    $scope.testInProgress = true;
    Modal.open({
      backdrop: 'static',
      scope: $scope,
      template: checkUserMappingTemplate,
      controller: 'LdapCheckUserMappingController',
      resolve: {
        users: function() {
          var deferred = $q.defer();
          $http.put(CLMContextLocations.getLdapUserMappingTest(), $scope.ldapUserMapping).then(function (response) {
            var users = response.data;
            // Add property that holds the count of fields that are populated
            users.forEach(function(user) {
              user.fieldCount = 0;
              user.membership = (user.membership || []).join(', ');
              ['username', 'realName', 'email', 'membership'].forEach(function(field) {
                if (user[field]) {
                  user.fieldCount++;
                }
              });
            });
            deferred.resolve(users);
          }, function(errorResponse) {
            $scope.testInProgress = false;
            showAlert($scope.alerts, {type: 'error', msg: errorResponse.data});
            deferred.reject(errorResponse);
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
    Modal.open({
      backdrop: 'static',
      scope: $scope,
      template: checkLoginTemplate,
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

  $http.get(CLMContextLocations.getLdapUserMappingConfig()).then(function(response) {
    origLdapUserMapping = response.data;

    // non-required properties must be initialized to empty string (not null or undefined) so that
    // pristine-ness styles work correctly in regards to whitespace
    ['userBaseDN', 'userFilter', 'userPasswordAttribute', 'groupBaseDN'].forEach(function(nonRequiredProp) {
      origLdapUserMapping[nonRequiredProp] = origLdapUserMapping[nonRequiredProp] || '';
    });

    $scope.ldapUserMapping = angular.copy(origLdapUserMapping);
  }, function(error) {
    ErrorDialog.open(error);
  });
}

LdapUsermappingController.$inject = ['$scope', 'Modal', '$http', 'CLMContextLocations', 'ErrorDialog', '$q'];

export function LdapCheckUserMappingController($scope, users) {
  $scope.users = users;
  $scope.infoText = 'Scroll through the table and verify that the values in each column are in the correct format. ' +
    'If they are not, click "Close" and revise your LDAP field mappings.';
}

LdapCheckUserMappingController.$inject = ['$scope', 'users'];

export function LdapCheckLoginController($scope, $http, CLMContextLocations) {
  $scope.alerts = [];
  $scope.testInProgress = false;
  $scope.ldapCredentials = {};
  $scope.testLogin = function() {
    var request = {
      userMapping: $scope.ldapUserMapping,
      username: $scope.ldapCredentials.username,
      password: $scope.ldapCredentials.password
    };
    testRequest($scope, $http, CLMContextLocations.getLdapLoginTest(), request);
  };
}

LdapCheckLoginController.$inject = ['$scope', '$http', 'CLMContextLocations'];
