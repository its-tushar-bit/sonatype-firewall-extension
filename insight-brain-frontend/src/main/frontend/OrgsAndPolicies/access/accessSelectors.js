/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { includes, isEmpty, omit, prop, propEq, propOr } from 'ramda';
import { selectOrgsAndPoliciesSlice } from '../orgsAndPoliciesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsOrganization, selectRouterState } from 'MainRoot/reduxUiRouter/routerSelectors';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';
import { getRolesWithoutLocalMembers } from 'MainRoot/OrgsAndPolicies/utility/util';

export const selectAccessSlice = createSelector(selectOrgsAndPoliciesSlice, prop('access'));
export const selectAvailableRoles = createSelector(selectAccessSlice, prop('availableRoles'));
export const selectRole = createSelector(selectAccessSlice, prop('role'));
export const selectLoading = createSelector(selectAccessSlice, prop('loading'));
export const selectLoadError = createSelector(selectAccessSlice, prop('loadError'));
export const selectRolesSiblings = createSelector(selectAccessSlice, prop('siblings'));
export const selectFetchUsers = createSelector(selectAccessSlice, prop('fetchUsers'));
export const selectServerData = createSelector(selectAccessSlice, prop('serverData'));
export const selectMembersByRole = createSelector(selectServerData, propOr([], 'membersByRole'));
export const selectInheritedAccessOpen = createSelector(selectAccessSlice, prop('inheritedAccessOpen'));
export const selectExtendedMembersByRole = createSelector(selectMembersByRole, (membersByRole) => {
  const owners = membersByRole[0]?.membersByOwner.map((owner, index) => {
    const roles = membersByRole
      .filter((role) => {
        return role.membersByOwner[index].members?.length > 0;
      })
      .map((role) => {
        return {
          ...omit(['membersByOwner'], role),
          members: role.membersByOwner[index].members,
        };
      });
    return { ...omit(['members'], owner), roles, isInherited: index > 0 };
  });

  return owners;
});
export const selectIsGroupSearchEnabled = createSelector(selectServerData, prop('groupSearchEnabled'));
export const selectUnSortedAddedUsers = createSelector(selectAccessSlice, prop('addedUsers'));
export const selectRolesWithoutLocalMembersExist = createSelector(selectMembersByRole, (membersByRole) => {
  const roles = getRolesWithoutLocalMembers(membersByRole);
  return roles.length > 0;
});
export const selectRoleToEdit = createSelector(selectMembersByRole, selectRouterCurrentParams, (roles, { roleId }) =>
  find(propEq('roleId', roleId), roles)
);

export const selectOwnerType = createSelector(selectIsOrganization, selectRouterState, (isOrganization, { name }) => {
  if (includes('repository_container', name)) return 'all repository managers';
  if (includes('repository_manager', name)) return 'repository manager';
  if (includes('repository', name)) return 'repository';
  return isOrganization ? 'organization' : 'application';
});

export const selectGroupName = createSelector(selectAccessSlice, prop('groupName'));

export const selectValidationError = createSelector(selectAccessSlice, ({ role, addedUsers, isNew }) => {
  // When creating a role, need to validate that both NxFormSelect has a selected value
  // and NxTransferList has added items
  if (isNew) {
    return isNilOrEmpty(role) || isEmpty(addedUsers) ? GLOBAL_FORM_VALIDATION_ERROR : null;
  }
  // When editing a role, NxFormSelect is hidden and there are already NxTransferList items added
  return null;
});

export const selectNoRolesAvailableError = createSelector(
  createSelector(selectAccessSlice, prop('isNew')),
  selectAvailableRoles,
  (isNew, roles) => {
    return isNew && roles && roles.length === 0 ? 'no roles available' : null;
  }
);
