/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { isEmpty, prop, propOr, ascend, sort } from 'ramda';
import { selectOrgsAndPoliciesSlice, selectSelectedOwner, selectSelectedOwnerId } from '../orgsAndPoliciesSelectors';
import { flatEntries } from './utils';

export const selectOwnerSideNavSlice = createSelector(selectOrgsAndPoliciesSlice, prop('ownerSideNav'));

export const selectLoadError = createSelector(selectOwnerSideNavSlice, prop('loadError'));
export const selectLoading = createSelector(selectOwnerSideNavSlice, prop('loading'));

export const selectDisplayedOrganization = createSelector(selectOwnerSideNavSlice, prop('displayedOrganization'));
export const selectDisplayedOrganizationId = createSelector(selectDisplayedOrganization, prop('id'));
export const selectOwnersMap = createSelector(selectOwnerSideNavSlice, prop('ownersMap'));
export const selectOwnersFlattenEntries = createSelector(selectOwnersMap, flatEntries);
export const selectRepoManagerOwnersEntries = createSelector(
  selectOwnersFlattenEntries,
  propOr([], 'repositoryManagers')
);
export const selectRepoManagerOwnersEntriesSorted = createSelector(
  selectRepoManagerOwnersEntries,
  sort(ascend(prop('name')))
);
export const selectTopParentOrganizationId = createSelector(selectOwnerSideNavSlice, prop('topParentOrganizationId'));
export const selectTopParentOrganization = createSelector(
  selectTopParentOrganizationId,
  selectOwnersMap,
  (id, ownersMap) => ownersMap[id] || {}
);
export const selectShowRepositories = createSelector(selectOwnerSideNavSlice, prop('showRepositories'));

export const selectOwnerById = createSelector(
  selectOwnersMap,
  selectTopParentOrganizationId,
  (_, organizationId, needsSyntheticRoot) => ({ organizationId, needsSyntheticRoot }),
  (organizations, topParentOrganizationId, { organizationId, needsSyntheticRoot }) => {
    if (!organizations || !organizationId) return {};
    if (needsSyntheticRoot && !organizations['ROOT_ORGANIZATION_ID']) {
      return {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
        organizationIds: [topParentOrganizationId],
        synthetic: true,
      };
    }
    return organizations[organizationId];
  }
);

export const selectChildApplicationsByOrgId = createSelector(
  selectOwnersMap,
  (_, organizationId) => organizationId,
  (organizations, organizationId) => {
    if (!organizations || !organizationId) return [];
    if (!organizations[organizationId]) return [];
    return organizations[organizationId].applicationIds || [];
  }
);

export const selectAllDescendantsByParentId = createSelector(
  selectOwnersMap,
  (_, organizationId) => organizationId,
  (ownersMap, parentId) => {
    return findDescendantsByParentId(ownersMap, parentId);
  }
);

export const selectAllDescendants = createSelector(selectOwnersMap, selectSelectedOwnerId, (ownersMap, parentId) => {
  return findDescendantsByParentId(ownersMap, parentId);
});

function findDescendantsByParentId(ownersMap, parentId) {
  if (!ownersMap || !parentId) return {};
  if (!ownersMap[parentId]) return {};

  const applicationIds = [];
  const organizationIds = [];
  const repositoryManagerIds = [];
  const repositoryIds = [];
  let repositoryContainerId = null;
  const parentOrganizations = [ownersMap[parentId].id];

  while (!isEmpty(parentOrganizations)) {
    const parent = ownersMap[parentOrganizations.shift()];

    if (!isNilOrEmpty(parent.applicationIds)) {
      applicationIds.push(...parent.applicationIds);
    }
    if (!isNilOrEmpty(parent.organizationIds)) {
      parentOrganizations.push(...parent.organizationIds);
      organizationIds.push(...parent.organizationIds);
    }
    if (!isNilOrEmpty(parent.repositoryContainerId)) {
      parentOrganizations.push(parent.repositoryContainerId);
      repositoryContainerId = parent.repositoryContainerId;
    }
    if (!isNilOrEmpty(parent.repositoryManagerIds)) {
      parentOrganizations.push(...parent.repositoryManagerIds);
      repositoryManagerIds.push(...parent.repositoryManagerIds);
    }
    if (!isNilOrEmpty(parent.repositoryIds)) {
      repositoryIds.push(...parent.repositoryIds);
    }
  }

  return { applicationIds, organizationIds, repositoryContainerId, repositoryManagerIds, repositoryIds };
}

export const selectIsDisplayedOrganizationSynthetic = createSelector(selectDisplayedOrganization, prop('synthetic'));

export const selectTotalDescendantsCount = createSelector(
  selectSelectedOwner,
  selectOwnersMap,
  (currentOrganization, organizations) => {
    if (!organizations || !currentOrganization?.id) {
      return 0;
    }

    const derivedOrg = organizations[currentOrganization.id];
    return derivedOrg ? derivedOrg.subOrgs + derivedOrg.totalApps : 0;
  }
);

export const selectPrevStateOwnerName = createSelector(
  selectOwnersMap,
  selectTopParentOrganization,
  (_, prevOwnerId) => prevOwnerId,
  (ownersMap, topParentOrganization, prevOwnerId) => {
    return ownersMap[prevOwnerId] ? ownersMap[prevOwnerId].name : topParentOrganization?.name;
  }
);

/**
 * This selector identifies if the displayed organization is the top org that the user has permission,
 * it could be a synthetic org or a full org
 */
export const selectIsOrganizationTopOfHierarchyForUser = createSelector(
  selectOwnersMap,
  selectDisplayedOrganization,
  (ownersMap, displayedOrganization) => {
    const orgHasNoParentOrg = !displayedOrganization?.parentOrganizationId;
    const parentOrgIsNotInOwnersMap = !ownersMap?.[displayedOrganization?.parentOrganizationId];

    return orgHasNoParentOrg || parentOrgIsNotInOwnersMap;
  }
);
