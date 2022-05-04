/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function AccessTileController(
  $scope,
  RoleMappingService,
  CLMContextLocations,
  SameOwnerStateNavigationService,
  LocalRoleService,
  EventNameConstant,
  $ngRedux
) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);
  vm.isRepositories = CLMContextLocations.isRepositories();
  vm.membersByRole = undefined;
  vm.error = undefined;
  vm.rolesWithoutLocalMembersExist = undefined;
  vm.addAccess = addAccess;
  vm.editAccess = editAccess;
  vm.doLoad = doLoad;
  vm.filterRolesWithMembers = filterRolesWithMembers;

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, function () {
    doLoad(true);
  });

  function doLoad(reload) {
    RoleMappingService[reload ? 'refresh' : 'get']().then(
      function (roleMappings) {
        vm.membersByRole = roleMappings.membersByRole;
        vm.membersByRole.forEach(function (role) {
          role.membersByOwner.forEach(function (memberOwner, index) {
            memberOwner.inherited = index > 0;
            if (memberOwner.members.length > 0) {
              vm.membersByRole[0].membersByOwner[index].hasMembers = true;
            }
          });
        });

        if (vm.isRepositories) {
          vm.ownerName = vm.membersByRole[0].membersByOwner[0].ownerName;
        }
        vm.rolesWithoutLocalMembersExist = LocalRoleService.getRolesWithoutLocalMembers(vm.membersByRole).length > 0;
      },
      function (error) {
        vm.error = error;
      }
    );

    delete vm.error;
  }

  function filterRolesWithMembers(index) {
    return function (role) {
      return role.membersByOwner[index].members.length > 0;
    };
  }

  function editAccess(roleId, inherited) {
    if (!inherited) {
      SameOwnerStateNavigationService.goEdit('edit-access', { roleId: roleId });
    }
  }

  function addAccess() {
    if (vm.rolesWithoutLocalMembersExist) {
      SameOwnerStateNavigationService.goEdit('add-access');
    }
  }
}

export const mapStateToThis = (state) => ({
  ownerName: selectSelectedOwnerName(state),
});

AccessTileController.$inject = [
  '$scope',
  'role.mapping.service',
  'CLMContextLocations',
  'SameOwnerStateNavigationService',
  'local.role.service',
  'event.name.constant',
  '$ngRedux',
];
