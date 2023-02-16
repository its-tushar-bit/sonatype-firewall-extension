/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop, propOr, propEq } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectRouterSlice = prop('router');
export const selectRouterCurrentParams = createSelector(selectRouterSlice, prop('currentParams'));
export const selectRouterState = createSelector(selectRouterSlice, prop('currentState'));

export const selectCurrentRouteName = createSelector(selectRouterState, prop('name'));

export const selectRouterPrevState = createSelector(selectRouterSlice, prop('prevState'));

export const selectRouterPrevParams = createSelector(selectRouterSlice, prop('prevParams'));

export const selectPreviousRouteName = createSelector(selectRouterPrevState, prop('name'));

const includesNamePart = (part) => (stringToSearch = '') => stringToSearch.includes(part);
const nameIncludesOrganization = includesNamePart('organization');
const nameIncludesApplication = includesNamePart('application');
const nameIncludesRepositories = includesNamePart('repositories');
const nameIncludesRepository = includesNamePart('repository');
const nameIncludesFirewall = includesNamePart('firewall');

export const selectIsOrganization = createSelector(selectCurrentRouteName, nameIncludesOrganization);
export const selectIsApplication = createSelector(selectCurrentRouteName, nameIncludesApplication);
export const selectIsRepositories = createSelector(selectCurrentRouteName, nameIncludesRepositories);
export const selectIsRepository = createSelector(selectCurrentRouteName, nameIncludesRepository);
export const selectIsFirewall = createSelector(selectCurrentRouteName, nameIncludesFirewall);
export const selectIsPrevFirewall = createSelector(selectPreviousRouteName, nameIncludesFirewall);
// we can access to component details page from application report but also from firewall or repository results view,
// so this is used to find out if the route is a firewall route or repository route
export const selectIsFirewallOrRepository = createSelector(
  selectIsFirewall,
  selectIsRepository,
  (isFirewall, isRepository) => isFirewall || isRepository
);

export const selectOrganizationId = createSelector(selectRouterCurrentParams, propOr('', 'organizationId'));
export const selectApplicationId = createSelector(selectRouterCurrentParams, propOr('', 'applicationPublicId'));
export const selectRepositoryId = createSelector(selectRouterCurrentParams, propOr('', 'repositoryId'));
export const selectViolationId = createSelector(selectRouterCurrentParams, propOr('', 'violationId'));
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

export const selectIsManagementViewRouterState = createSelector(
  selectRouterState,
  (currentState) => currentState.name === 'management.view'
);

export const selectIncludesManagementView = createSelector(
  selectRouterState,
  (currentState) => currentState.name !== 'management.view' && currentState.name.includes('management.view')
);

export const selectPrevStateIsAppOwnerManagementView = createSelector(selectRouterPrevState, (prevState) =>
  prevState.name?.includes('management.view.application')
);
