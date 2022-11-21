/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop, split, contains, curryN, propOr, propEq } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectRouterSlice = prop('router');
export const selectRouterCurrentParams = createSelector(selectRouterSlice, prop('currentParams'));
export const selectRouterState = createSelector(selectRouterSlice, prop('currentState'));

export const selectCurrentRouteName = createSelector(selectRouterState, prop('name'));

export const selectRouterPrevState = createSelector(selectRouterSlice, prop('prevState'));

export const selectRouterPrevParams = createSelector(selectRouterSlice, prop('prevParams'));

export const selectPreviousRouteName = createSelector(selectRouterPrevState, prop('name'));

const includesNamePart = curryN(2, (part, str) => contains(part, split('.', str)));
const nameIncludesOrganization = includesNamePart('organization');
const nameIncludesApplication = includesNamePart('application');
const nameIncludesRepositories = includesNamePart('repositories');
const nameIncludesFirewall = includesNamePart('firewall');

export const selectIsOrganization = createSelector(selectCurrentRouteName, nameIncludesOrganization);
export const selectIsApplication = createSelector(selectCurrentRouteName, nameIncludesApplication);
export const selectIsRepositories = createSelector(selectCurrentRouteName, nameIncludesRepositories);
export const selectIsFirewall = createSelector(selectCurrentRouteName, nameIncludesFirewall);
export const selectIsPrevFirewall = createSelector(selectPreviousRouteName, nameIncludesFirewall);

export const selectOrganizationId = createSelector(selectRouterCurrentParams, propOr('', 'organizationId'));
export const selectApplicationId = createSelector(selectRouterCurrentParams, propOr('', 'applicationPublicId'));
export const selectRepositoryId = createSelector(selectRouterCurrentParams, propOr('', 'repositoryId'));
export const selectHash = createSelector(selectRouterCurrentParams, propOr('', 'componentHash'));
export const selectRepositoryPolicyId = createSelector(selectRouterCurrentParams, propOr('', 'repositoryPolicyId'));
export const selectPrevRepositoryPolicyId = createSelector(selectRouterPrevParams, propOr('', 'repositoryPolicyId'));

export const selectIsRootOrganization = createSelector(
  selectRouterCurrentParams,
  propEq('organizationId', 'ROOT_ORGANIZATION_ID')
);

export const selectRouteParamsFromSecurityTab = createSelector(selectRouterCurrentParams, (routerParams) => {
  const isRepositoryComponent = !!(routerParams.repositoryId && routerParams.componentHash);
  if (isRepositoryComponent) {
    return {
      ownerId: routerParams.repositoryId,
      hash: routerParams.componentHash,
      isRepositoryComponent,
    };
  } else {
    return {
      ownerId: routerParams.publicId,
      hash: routerParams.hash,
      isRepositoryComponent,
    };
  }
});

export const selectOwnerInfo = createSelector(
  selectIsOrganization,
  selectIsApplication,
  selectIsRepositories,
  selectOrganizationId,
  selectApplicationId,
  (isOrganization, isApplication, isRepositories, organizationId, applicationId) => {
    const ownerId = isApplication ? applicationId : isOrganization ? organizationId : 'global';
    const ownerType = isApplication
      ? 'application'
      : isOrganization
      ? 'organization'
      : isRepositories
      ? 'repository_container'
      : 'global';

    if (isRepositories) {
      return { ownerType };
    } else if (ownerId === '_new_') {
      return { ownerType: 'global', ownerId: 'global' };
    } else {
      return { ownerType, ownerId };
    }
  }
);
