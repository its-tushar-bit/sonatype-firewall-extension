/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function AccessEditorController($rootScope, $scope, $stateParams, $http, CLMAppLocations, Messages, LocalRoleService,
                                  SameOwnerStateNavigationService, DeleteModalService)
  {
    var originalMembers,
        ownerType,
        isNavigatingAfterRemove,
        ldapRealm,
        vm = this;

    vm.accessEditor = undefined;
    vm.accessEditorMask = undefined;
    vm.accessEditorSearch = undefined;
    vm.accessEditorSearchMask = undefined;
    vm.searchInProgress = false;
    vm.loadError = undefined;
    vm.query = undefined;
    vm.role = undefined;
    vm.availableRoles = undefined;
    vm.submitError = undefined;
    vm.searchError = undefined;
    vm.members = undefined;
    vm.newGroupName = undefined;
    vm.addGroup = addGroup;
    vm.doLoad = doLoad;
    vm.groupExists = groupExists;
    vm.isNew = !$stateParams.roleId;
    vm.isValid = isValid;
    vm.removeRole = removeRole;
    vm.save = save;
    vm.search = search;
    vm.getIconName = getIconName;
    vm.getTooltip = getTooltip;

    vm.doLoad();

    $scope.$on('pageChangeStarted', function(event) {
      if (!isNavigatingAfterRemove && isDirty()) {
        event.preventDefault();
      }
    });

    function doLoad() {
      delete vm.loadError;
      vm.members = [];

      $http.get(CLMAppLocations.getRoleMappingUrl()).then(function(result) {
        if (!vm.isNew) {
          result.data.membersByRole.some(function(role) {
            if ($stateParams.roleId === role.roleId) {
              vm.role = {
                roleId: role.roleId,
                roleName: role.roleName,
                roleDescription: role.roleDescription
              };
              ownerType = role.membersByOwner[0].ownerType;
              vm.members = role.membersByOwner[0].members;
              originalMembers = angular.copy(vm.members);
              vm.members.forEach(function(user) {
                user.picked = true;
              });
              return true;
            }
          });
          if (!vm.role) {
            vm.loadError = 'Could not find a role with ID: ' + $stateParams.roleId + '.';
            return;
          }
        }
        vm.groupSearchEnabled = result.data.groupSearchEnabled;
        vm.availableRoles = LocalRoleService.getRolesWithoutLocalMembers(result.data.membersByRole);
        ldapRealm = result.data.ldapRealm;
      }, function(error) {
        vm.loadError = Messages.getHttpErrorMessage(error);
      });
    }

    function addGroup() {
      var group = {
        displayName: vm.newGroupName,
        email: null,
        internalName: vm.newGroupName,
        realm: ldapRealm,
        type: 'GROUP'
      };
      updatePickedUsers([group]);

      vm.newGroupName = undefined;
      vm.accessEditorAddGroup.$setPristine();
    }

    function isDirty(){
      function internalName(user) {
        return user.internalName;
      }

      if (vm.isNew) {
        return vm.role || currentlyPicked().length > 0;
      }
      else {
        return !angular.equals(currentlyPicked().map(internalName).sort(), originalMembers.map(internalName).sort());
      }
    }

    function isValid() {
      if (vm.isNew) {
        return vm.role && currentlyPicked().length > 0;
      }
      else {
        return isDirty();
      }
    }

    function removeRole(customMessage) {
      customMessage = customMessage ? ' ' + customMessage : '';
      DeleteModalService.deleteCustom('Remove Role', 'You are about to remove the ' + vm.role.roleName + ' role from ' +
          (ownerType === 'repository_container' ? 'all repositories' : 'this ' + ownerType) + '.' + customMessage,
          'Removing',
          function() {
            return $http.put(CLMAppLocations.getRoleMappingUrl(vm.role.roleId), []);
          }
      ).then(function() {
        isNavigatingAfterRemove = true;
        vm.availableRoles.push(vm.role);
        $rootScope.$broadcast('resource.data.modified');
        SameOwnerStateNavigationService.goEdit('add-access');
      });
    }

    function save() {
      var madePristine = false;

      if (isValid()) {
        delete vm.submitError;

        if (currentlyPicked().length === 0) {
          vm.removeRole('Next time, consider using the "Remove Role" button; it will save you some clicks!');
        }
        else {
          vm.accessEditorMask.wrap($http.put(CLMAppLocations.getRoleMappingUrl(vm.role.roleId),
              currentlyPicked())).then(function() {

            if (vm.isNew) {
              $rootScope.$broadcast('resource.data.modified');
              vm.availableRoles.some(function(role, index) {
                if (role.roleId === vm.role.roleId) {
                  vm.availableRoles.splice(index, 1);
                  return true;
                }
              });

              if (vm.availableRoles.length === 0) {
                vm.isNew = false;
                makeEditorPristine();
                SameOwnerStateNavigationService.goEdit('edit-access', {roleId: vm.role.roleId});
              }
              else {
                vm.members = [];
                delete vm.role;
              }
            }

            makeEditorPristine();
          }, function(error) {
            vm.submitError = Messages.getHttpErrorMessage(error);
          });
        }
      }

      function makeEditorPristine() {
        if (!madePristine) {
          madePristine = true;

          if (!vm.isNew) {
            originalMembers = currentlyPicked();
          }

          delete vm.newGroupName;
          delete vm.query;
          delete vm.searchError;
          vm.accessEditor.$setPristine();
          vm.accessEditorSearch.$setPristine();
          vm.members.forEach(function(user) {
            user.checked = false;
          });
        }
      }
    }

    function search() {
      if (vm.query) {
        delete vm.searchError;
        var pickedUsers = currentlyPicked();
        vm.searchInProgress= true;

        vm.accessEditorSearchMask.wrap($http.get(CLMAppLocations.getFindUsersUrl(), {
          params: {
            q: vm.query
          }
        })).then(function(result) {
          vm.searchInProgress = false;
          vm.members = result.data.members;
          updatePickedUsers(pickedUsers);
        }, function(error) {
          vm.searchInProgress = false;
          vm.searchError = Messages.getHttpErrorMessage(error);
        });
      }
    }

    function currentlyPicked() {
      return vm.members.filter(function(user) {
        return user.picked;
      });
    }

    function updatePickedUsers(pickedUsers) {
      pickedUsers.forEach(function(pickedUser) {
        var replaced = vm.members.some(function(user, index) {
          if (user.internalName === pickedUser.internalName) {
            vm.members[index] = pickedUser;
            return true;
          }
        });
        if (!replaced) {
          vm.members.push(pickedUser);
        }
      });
    }

    function groupExists(groupName) {
      return vm.members.some(function(member) {
        return member.internalName === groupName;
      });
    }

    function getIconName(item) {
      return item.type === 'USER' ? 'fa-user' : 'fa-group';
    }

    function getTooltip(item) {
      return item.realm ? item.realm + (item.email ? '\n' + item.email : '') : null;
    }
  }

  AccessEditorController.$inject = [
    '$rootScope', '$scope', '$stateParams', '$http', 'CLMAppLocations', 'Messages', 'local.role.service',
    'SameOwnerStateNavigationService', 'DeleteModalService'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('access.editor.controller', AccessEditorController);
}(angular));
