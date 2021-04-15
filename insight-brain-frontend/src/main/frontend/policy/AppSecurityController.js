/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import commonServicesModule from '../util/CommonServices';
import angularCommonModule from '../util/AngularCommon';
import CLMContextLocationModule from '../util/CLMContextLocation';

var groupings = [
  {
    type: 'GROUP',
    header: 'GROUPS',
    icon: 'group',
  },
  {
    type: 'USER',
    header: 'USERS',
    icon: 'user',
  },
];
var showGroupings = function (grouping, mappings) {
  if (!mappings) {
    return false;
  }
  for (var i = 0; i < mappings.length; i++) {
    var mapping = mappings[i];
    // Mapping can either have an array of members in a members property or be an array of members itself
    if (mapping.members) {
      for (var j = 0; j < mapping.members.length; j++) {
        var member = mapping.members[j];
        if (member.type === grouping.type) {
          return true;
        }
      }
    } else {
      if (mapping.type === grouping.type) {
        return true;
      }
    }
  }
  return false;
};

/**
 * Controller for the overall Administration roles table
 */
function AppSecurityController($scope, $http, clmAppLocations, isAuthorized) {
  $scope.groupings = groupings;
  $scope.showGrouping = showGroupings;
  $scope.isAuthorized = isAuthorized;

  $scope.doLoad = function () {
    if (isAuthorized) {
      $scope.error = null;

      $http.get(clmAppLocations.getRoleMappingUrl()).then(
        function (response) {
          var data = response.data;
          $scope.context = {
            roles: data.membersByRole,
            groupSearchEnabled: data.groupSearchEnabled,
          };
        },
        function (error) {
          $scope.error = error;
        }
      );
    }
  };

  $scope.$on('roleSaveComplete', function (event, roleId, newMembers) {
    for (var i = 0; i < $scope.context.roles.length; i++) {
      if ($scope.context.roles[i].roleId === roleId) {
        $scope.context.roles[i].membersByOwner[0].members = newMembers.slice();
        break;
      }
    }
    event.preventDefault();
  });

  $scope.doLoad();
}

AppSecurityController.$inject = ['$scope', '$http', 'CLMContextLocations', 'isAuthorized'];

/**
 * Controller for the editor component of each row of the Administration Roles table.  This essentially
 * manages a role-management directive along with some submissions buttons
 */
function AppSecurityEditorController($scope, $http, Dialog, clmAppLocations, Messages) {
  $scope.alerts = [];
  $scope.$watch('role', function (newVal) {
    if (newVal) {
      $scope.originalMembers = angular.copy(newVal.membersByOwner[0].members);
    }
  });

  $scope.$on('pageChangeStarted', function (e) {
    if ($scope.isDirty()) {
      e.preventDefault();
    }
  });

  $scope.cancel = function () {
    if ($scope.isDirty()) {
      Dialog.open({
        title: 'Unsaved Changes',
        body: 'This role may contain unsaved changes, continuing will discard them.',
        buttons: [
          {
            name: 'Continue',
            type: 'primary',
            click: function () {
              $scope.hide();
            },
          },
          {
            name: 'Cancel',
            type: 'cancel',
          },
        ],
      });
    } else {
      $scope.hide();
    }
  };

  $scope.save = function () {
    var roleId = $scope.role.roleId,
      currentMembers;

    if ($scope.isDirty()) {
      currentMembers = $scope.getCurrentMembersToSave();

      return $http.put(clmAppLocations.getRoleMappingUrl(roleId), currentMembers).then(
        function () {
          $scope.$emit('roleSaveComplete', roleId, currentMembers);
          $scope.hide();
        },
        function (error) {
          $scope.alerts.push({
            type: 'error',
            msg: Messages.getHttpErrorMessage(error),
          });
        }
      );
    } else {
      $scope.hide();
    }
  };

  //isDirty bound from role.membership.controller
}

AppSecurityEditorController.$inject = ['$scope', '$http', 'Dialog', 'CLMContextLocations', 'Messages'];

function AppSecurityEditorDirective() {
  return {
    scope: {
      hide: '&',
      role: '<',
      groupSearchEnabled: '<',
    },
    controller: 'AppSecurityEditorController',
    templateUrl: 'appSecurityEditor',
  };
}

export default angular //
  .module(
    'ApplicationSecurityModule', //
    [commonServicesModule.name, angularCommonModule.name, CLMContextLocationModule.name, 'role.membership.module']
  )
  .controller('AppSecurityController', AppSecurityController) //
  .controller('AppSecurityEditorController', AppSecurityEditorController) //
  .directive('appSecurityEditor', AppSecurityEditorDirective);
