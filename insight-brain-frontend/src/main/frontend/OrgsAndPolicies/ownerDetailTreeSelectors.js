/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import { getRolesWithLocalMembers, getRolesWithoutLocalMembers } from './utility/util';

export const selectOwnerDetailTreeSlice = createSelector(selectOrgsAndPoliciesSlice, prop('ownerDetailTree'));

export const selectLoading = createSelector(selectOwnerDetailTreeSlice, prop('loading'));
export const selectOwnerDetails = createSelector(selectOwnerDetailTreeSlice, (ownerDetailTreeSlice) => {
  const details = ownerDetailTreeSlice.ownerDetails;
  const roles = getRolesWithLocalMembers(details?.roles?.membersByRole);
  return { ...details, roles };
});

export const selectRolesWithoutLocalMembersExist = createSelector(
  selectOwnerDetailTreeSlice,
  (ownerDetailTreeSlice) => {
    const details = ownerDetailTreeSlice.ownerDetails;
    const roles = getRolesWithoutLocalMembers(details?.roles?.membersByRole);
    return roles.length > 0;
  }
);

export const selectLoadError = createSelector(selectOwnerDetailTreeSlice, prop('loadError'));
