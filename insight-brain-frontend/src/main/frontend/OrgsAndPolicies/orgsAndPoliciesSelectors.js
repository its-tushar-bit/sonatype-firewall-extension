/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, has } from 'ramda';
import {
  selectRouterCurrentParams,
  selectApplicationId,
  selectIsOrganization,
  selectIsApplication,
  selectOrganizationId,
  selectIsRepositories,
  selectIsRepositoryManager,
  selectRepositoryManagerId,
} from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectOrgsAndPoliciesSlice = prop('orgsAndPolicies');
export const selectRootSlice = createSelector(selectOrgsAndPoliciesSlice, prop('root'));

export const selectSelectedOwner = createSelector(selectRootSlice, prop('selectedOwner'));
export const selectSelectedOwnerContact = createSelector(selectSelectedOwner, prop('contact'));
export const selectSelectedOwnerName = createSelector(
  selectSelectedOwner,
  selectIsRepositories,
  (selectedOwner, isRepositories) => {
    return isRepositories ? 'All Repositories' : selectedOwner.name;
  }
);
export const selectSelectedOwnerId = createSelector(selectSelectedOwner, prop('id'));
export const selectSelectedOwnerParentId = createSelector(selectSelectedOwner, prop('parentOrganizationId'));
export const selectLoadError = createSelector(selectRootSlice, prop('loadError'));

export const selectPoliciesByOwner = createSelector(selectRootSlice, prop('policiesByOwner'));

export const selectOwnerProperties = createSelector(
  selectRouterCurrentParams,
  selectIsRepositories,
  selectIsRepositoryManager,
  (
    { applicationPublicId, organizationId, applicationId, repositoryContainerId, repositoryManagerId },
    isRepositories,
    isReposityManager
  ) => {
    if (repositoryContainerId || isRepositories) {
      return {
        ownerType: 'repository_container',
        ownerId: repositoryContainerId || 'REPOSITORY_CONTAINER_ID',
      };
    } else if (isReposityManager) {
      return {
        ownerType: 'repository_manager',
        ownerId: repositoryManagerId,
      };
    } else {
      return {
        ownerType: organizationId ? 'organization' : 'application',
        ownerId: organizationId ?? applicationId ?? applicationPublicId,
      };
    }
  }
);

export const selectEntityId = createSelector(
  selectIsOrganization,
  selectIsApplication,
  selectIsRepositoryManager,
  selectOrganizationId,
  selectApplicationId,
  selectRepositoryManagerId,
  (isOrganization, isApplication, isRepositoryManager, orgId, appId, selectRepositoryManagerId) =>
    isApplication ? appId : isOrganization ? orgId : isRepositoryManager ? selectRepositoryManagerId : 'global'
);

export const selectSelectedOwnerTypeAndId = createSelector(selectSelectedOwner, (owner) => ({
  ownerType: has('publicId', owner) ? 'application' : 'organization',
  ownerId: owner?.id,
}));
