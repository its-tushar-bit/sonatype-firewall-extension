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
  selectIsRepository,
  selectRepositoryId,
  selectIsRepositoryContainer,
  selectRepositoryContainerId,
} from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectOrgsAndPoliciesSlice = prop('orgsAndPolicies');
export const selectRootSlice = createSelector(selectOrgsAndPoliciesSlice, prop('root'));

export const selectSelectedOwner = createSelector(selectRootSlice, prop('selectedOwner'));
export const selectSelectedOwnerContact = createSelector(selectSelectedOwner, prop('contact'));
export const selectSelectedOwnerName = createSelector(selectSelectedOwner, prop('name'));
export const selectSelectedOwnerId = createSelector(selectSelectedOwner, prop('id'));
export const selectSelectedOwnerPublicId = createSelector(selectSelectedOwner, prop('publicId'));
export const selectSelectedOwnerParentId = createSelector(selectSelectedOwner, prop('parentOrganizationId'));
export const selectLoadError = createSelector(selectRootSlice, prop('loadError'));
export const selectLoading = createSelector(selectRootSlice, prop('loading'));

export const selectPoliciesByOwner = createSelector(selectRootSlice, prop('policiesByOwner'));

export const selectOwnerProperties = createSelector(
  selectRouterCurrentParams,
  selectIsRepositories,
  selectIsRepositoryManager,
  (params = {}, isRepositories, isRepositoryManager) => {
    const {
      applicationPublicId,
      organizationId,
      applicationId,
      repositoryContainerId,
      repositoryManagerId,
      repositoryId,
    } = params;

    if (repositoryContainerId || isRepositories) {
      return {
        ownerType: 'repository_container',
        ownerId: repositoryContainerId || 'REPOSITORY_CONTAINER_ID',
      };
    } else if (isRepositoryManager) {
      return {
        ownerType: 'repository_manager',
        ownerId: repositoryManagerId,
      };
    } else if (repositoryId) {
      return {
        ownerType: 'repository',
        ownerId: repositoryId,
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
  selectIsRepositoryContainer,
  selectIsRepositoryManager,
  selectIsRepository,
  selectOrganizationId,
  selectApplicationId,
  selectRepositoryContainerId,
  selectRepositoryManagerId,
  selectRepositoryId,
  (
    isOrganization,
    isApplication,
    isRepositoryContainer,
    isRepositoryManager,
    isRepository,
    orgId,
    appId,
    repositoryContainerId,
    repositoryManagerId,
    repositoryId
  ) => {
    if (isApplication) return appId;
    if (isOrganization) return orgId;
    if (isRepositoryContainer) return repositoryContainerId;
    if (isRepositoryManager) return repositoryManagerId;
    if (isRepository) return repositoryId;
    return null;
  }
);

export const selectSelectedOwnerTypeAndId = createSelector(selectSelectedOwner, (owner) => ({
  ownerType: has('publicId', owner) ? 'application' : 'organization',
  ownerId: owner?.id,
}));
