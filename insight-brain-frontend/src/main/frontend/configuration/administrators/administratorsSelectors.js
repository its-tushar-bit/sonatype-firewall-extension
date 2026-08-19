/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop, propEq, propOr, find, reject, includes, compose } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { formatGroupUsers, sortByDisplayName } from 'MainRoot/util/formatGroupUsers';

export const selectAdministratorsSlice = prop('administratorsConfig');

export const selectIsLoading = createSelector(selectAdministratorsSlice, prop('loading'));
export const selectLoadError = createSelector(selectAdministratorsSlice, prop('loadError'));
export const selectServerData = createSelector(selectAdministratorsSlice, prop('serverData'));
export const selectFetchUsers = createSelector(selectAdministratorsSlice, prop('fetchUsers'));
export const selectSubmitMaskState = createSelector(selectAdministratorsSlice, prop('submitMaskState'));
export const selectSubmitError = createSelector(selectAdministratorsSlice, prop('submitError'));
export const selectAddedUsers = createSelector(
  selectAdministratorsSlice,
  compose(sortByDisplayName, formatGroupUsers, prop('addedUsers'))
);

export const selectMembersByRole = createSelector(selectServerData, propOr([], 'membersByRole'));
// this selector is to be used when the Associate Group field
export const selectIsGroupSearchEnabled = createSelector(selectServerData, prop('groupSearchEnabled'));

export const selectFetchUsersLoading = createSelector(selectFetchUsers, prop('loading'));
export const selectFetchUsersLoadingError = createSelector(selectFetchUsers, prop('loadError'));
export const selectFetchUsersPartialError = createSelector(selectFetchUsers, prop('partialError'));
export const selectFetchUsersData = createSelector(selectFetchUsers, prop('data'));

export const selectUsersNotAdded = createSelector(
  selectFetchUsersData,
  selectAdministratorsSlice,
  (data, administrators) => {
    const { addedUsers } = administrators;
    return compose(
      formatGroupUsers,
      reject((user) => includes(user, addedUsers))
    )(data);
  }
);

export const selectRoleToEdit = createSelector(selectMembersByRole, selectRouterCurrentParams, (roles, { roleId }) =>
  find(propEq('roleId', roleId), roles)
);
