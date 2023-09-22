/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';
import {
  selectIsOrganizationTopOfHierarchyForUser,
  selectShowRepositories,
} from './ownerSideNav/ownerSideNavSelectors';
import { selectIsRepositoriesRelated } from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectOwnersTreeSlice = createSelector(selectOrgsAndPoliciesSlice, prop('ownersTree'));
export const selectSearchTerm = createSelector(selectOwnersTreeSlice, prop('searchTerm'));
export const selectFilteredOwners = createSelector(selectOwnersTreeSlice, prop('filteredOwners'));

export const selectOwnersTreeNodesStatus = createSelector(
  selectOwnersTreeSlice,
  ({ nodesStatus, filteredNodesStatus, searchTerm }) => {
    return searchTerm ? filteredNodesStatus : nodesStatus;
  }
);

export const selectOwnersTreeNodesInitialStatus = createSelector(
  selectOwnersTreeSlice,
  ({ initialFilteredStatus, initialStatus, searchTerm }) => {
    return searchTerm ? initialFilteredStatus : initialStatus;
  }
);

export const selectIsOwnerNodeExpanded = createSelector(
  selectOwnersTreeNodesStatus,
  selectOwnersTreeNodesInitialStatus,
  (_, ownerId) => ownerId,
  (ownersStatus, initialStatus, ownerId) => {
    if (!ownersStatus || !ownerId) return undefined;
    return ownersStatus[ownerId] ?? initialStatus;
  }
);

export const selectShouldDisplayRepositories = createSelector(
  selectShowRepositories,
  selectIsOrganizationTopOfHierarchyForUser,
  selectIsRepositoriesRelated,
  (showRepositories, isOrganizationTopOfHierarchyForUser, isRepositoriesRelated) =>
    showRepositories && (isOrganizationTopOfHierarchyForUser || isRepositoriesRelated)
);
