/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function AccessEditorController(
  $rootScope,
  $scope,
  $stateParams,
  Messages,
  LocalRoleService,
  SameOwnerStateNavigationService,
  DeleteModalService,
  RoleMappingService
) {
  var ownerType,
    isNavigatingAfterRemove,
    isNavigatingAfterSave,
    vm = this;

  vm.accessEditorMask = undefined;
  vm.availableRoles = undefined;
  vm.submitError = undefined;
  vm.isNew = !$stateParams.roleId;
  vm.isValid = isValid;
  vm.removeRole = removeRole;
  vm.save = save;
  vm.doLoad = doLoad;
  vm.role = undefined;
  vm.originalMembers = undefined;

  vm.doLoad();

  $scope.$on('pageChangeStarted', function (event) {
    if (!isNavigatingAfterRemove && !isNavigatingAfterSave && isDirty()) {
      event.preventDefault();
    }
  });

  function doLoad() {
    delete vm.loadError;

    vm.originalMembers = [];

    RoleMappingService.get().then(
      function (response) {
        var roleMappings = angular.copy(response); // copied as we modify objects
        if (!vm.isNew) {
          roleMappings.membersByRole.some(function (role) {
            if ($stateParams.roleId === role.roleId) {
              vm.role = {
                roleId: role.roleId,
                roleName: role.roleName,
                roleDescription: role.roleDescription,
              };
              ownerType = role.membersByOwner[0].ownerType;
              vm.originalMembers = role.membersByOwner[0].members;
              return true;
            }
          });
          if (!vm.role) {
            vm.loadError =
              'Could not find a role with ID: ' + $stateParams.roleId + '.';
            return;
          }
        }
        vm.groupSearchEnabled = roleMappings.groupSearchEnabled;
        vm.availableRoles = LocalRoleService.getRolesWithoutLocalMembers(
          roleMappings.membersByRole
        );
      },
      function (error) {
        vm.loadError = Messages.getHttpErrorMessage(error);
      }
    );
  }

  function isDirty() {
    if (vm.isNew) {
      return vm.role || vm.getCurrentMembers().length > 0;
    } else {
      //binding for role.membership.controller's isDirty function
      return vm.isMembershipDirty();
    }
  }

  function isValid() {
    if (vm.isNew) {
      return vm.role && vm.getCurrentMembers().length > 0;
    } else {
      return isDirty();
    }
  }

  function removeRole(customMessage) {
    customMessage = customMessage ? ' ' + customMessage : '';
    var message =
      'You are about to remove the ' +
      vm.role.roleName +
      ' role from ' +
      (ownerType === 'repository_container'
        ? 'all repositories'
        : 'this ' + ownerType) +
      '.';
    DeleteModalService.deleteCustom(
      'Remove Role',
      message + customMessage,
      'Removing',
      function () {
        return RoleMappingService.put(vm.role.roleId, []);
      }
    ).then(function () {
      isNavigatingAfterRemove = true;
      vm.availableRoles.push(vm.role);
      $rootScope.$broadcast('resource.data.modified');
      SameOwnerStateNavigationService.goEdit('add-access');
    });
  }

  function save() {
    var madePristine = false,
      currentMembers = vm.getCurrentMembersToSave();

    if (isValid()) {
      delete vm.submitError;

      if (currentMembers.length === 0) {
        vm.removeRole(
          'Next time, consider using the "Remove Role" button; it will save you some clicks!'
        );
      } else {
        vm.accessEditorMask
          .wrap(RoleMappingService.put(vm.role.roleId, currentMembers))
          .then(
            function () {
              if (vm.isNew) {
                $rootScope.$broadcast('resource.data.modified');
                vm.availableRoles.some(function (role, index) {
                  if (role.roleId === vm.role.roleId) {
                    vm.availableRoles.splice(index, 1);
                    return true;
                  }
                });

                if (vm.availableRoles.length === 0) {
                  vm.isNew = false;
                  makeEditorPristine();
                  isNavigatingAfterSave = true;
                  SameOwnerStateNavigationService.goEdit('edit-access', {
                    roleId: vm.role.roleId,
                  });
                } else {
                  vm.originalMembers = [];
                  delete vm.role;
                }
              }

              makeEditorPristine();
            },
            function (error) {
              vm.submitError = Messages.getHttpErrorMessage(error);
            }
          );
      }
    }

    function makeEditorPristine() {
      if (!madePristine) {
        madePristine = true;

        if (!vm.isNew) {
          vm.originalMembers = currentMembers;
        } else {
          vm.rolePicker.$setPristine();
        }

        //tell child component to clean itself up too
        $scope.$broadcast('role.membership.makeEditorPristine');
      }
    }
  }
}

AccessEditorController.$inject = [
  '$rootScope',
  '$scope',
  '$stateParams',
  'Messages',
  'local.role.service',
  'SameOwnerStateNavigationService',
  'DeleteModalService',
  'role.mapping.service',
];
