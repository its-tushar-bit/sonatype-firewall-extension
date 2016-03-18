/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function AccessTileController($scope, $http, CLMAppLocations, SameOwnerStateNavigationService, LocalRoleService,
                                EventNameConstant)
  {
    var vm = this;
    vm.ownerName = undefined;
    vm.membersByRole = undefined;
    vm.error = undefined;
    vm.rolesWithoutLocalMembersExist = undefined;
    vm.addAccess = addAccess;
    vm.editAccess = editAccess;
    vm.doLoad = doLoad;
    vm.filterRolesWithMembers = filterRolesWithMembers;

    vm.doLoad();

    $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);

    function doLoad() {
      $http.get(CLMAppLocations.getRoleMappingUrl()).then(function(results) {
        vm.membersByRole = results.data.membersByRole;
        vm.membersByRole.forEach(function(role) {
          role.membersByOwner.forEach(function(memberOwner, index) {
            memberOwner.inherited = index > 0;
            if (memberOwner.members.length > 0) {
              vm.membersByRole[0].membersByOwner[index].hasMembers = true;
            }
          });
        });

        vm.ownerName = vm.membersByRole[0].membersByOwner[0].ownerName;
        vm.rolesWithoutLocalMembersExist = LocalRoleService.getRolesWithoutLocalMembers(vm.membersByRole).length > 0;
      }, function(error) {
        vm.error = error;
      });

      delete vm.error;
    }

    function filterRolesWithMembers(index) {
      return function(role) {
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

  AccessTileController.$inject = [
    '$scope', '$http', 'CLMAppLocations', 'SameOwnerStateNavigationService', 'local.role.service', 'event.name.constant'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('AccessTileController', AccessTileController);
}(angular));
