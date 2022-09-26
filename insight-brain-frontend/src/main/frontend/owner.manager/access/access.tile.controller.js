/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsRepositories } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectRolesWithoutLocalMembersExist,
  selectExtendedMembersByRole,
  selectLoadError,
  selectLoading,
} from 'MainRoot/OrgsAndPolicies/access/accessSelectors';

import { actions as accessActions } from 'MainRoot/OrgsAndPolicies/access/accessSlice';

export default function AccessTileController($scope, SameOwnerStateNavigationService, EventNameConstant, $ngRedux) {
  var vm = this;

  const actions = pick(['loadRoles'], accessActions);
  vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);
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

  function doLoad() {
    vm.loadRoles();
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
  rolesWithoutLocalMembersExist: selectRolesWithoutLocalMembersExist(state),
  ownersWithRoles: angular.copy(selectExtendedMembersByRole(state)),
  loading: selectLoading(state),
  error: selectLoadError(state),
  isRepositories: selectIsRepositories(state),
});

AccessTileController.$inject = ['$scope', 'SameOwnerStateNavigationService', 'event.name.constant', '$ngRedux'];
