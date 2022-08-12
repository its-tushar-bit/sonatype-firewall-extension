/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { includes, isEmpty, prop, propEq, propOr } from 'ramda';
import { selectOrgsAndPoliciesSlice } from '../orgsAndPoliciesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsOrganization, selectRouterState } from 'MainRoot/reduxUiRouter/routerSelectors';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';

export const selectAccessSlice = createSelector(selectOrgsAndPoliciesSlice, prop('access'));
export const selectAvailableRoles = createSelector(selectAccessSlice, prop('availableRoles'));
export const selectRole = createSelector(selectAccessSlice, prop('role'));
export const selectRolesSiblings = createSelector(selectAccessSlice, prop('siblings'));
export const selectFetchUsers = createSelector(selectAccessSlice, prop('fetchUsers'));
export const selectServerData = createSelector(selectAccessSlice, prop('serverData'));
export const selectMembersByRole = createSelector(selectServerData, propOr([], 'membersByRole'));
export const selectIsGroupSearchEnabled = createSelector(selectServerData, prop('groupSearchEnabled'));
export const selectUnSortedAddedUsers = createSelector(selectAccessSlice, prop('addedUsers'));
export const selectRoleToEdit = createSelector(selectMembersByRole, selectRouterCurrentParams, (roles, { roleId }) =>
  find(propEq('roleId', roleId), roles)
);

export const selectOwnerType = createSelector(selectIsOrganization, selectRouterState, (isOrganization, { name }) => {
  if (includes('repositories', name)) return 'repository';
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
