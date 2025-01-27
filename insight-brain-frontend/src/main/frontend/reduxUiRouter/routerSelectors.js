/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop, path, propOr, propEq } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

import { isSbomManagerComponentDetails, nameStartsWithSbomManager } from 'MainRoot/sbomManager/sbomManagerUtil';

export const selectRouterSlice = prop('router');
export const selectRouterCurrentParams = createSelector(selectRouterSlice, prop('currentParams'));
export const selectRouterState = createSelector(selectRouterSlice, prop('currentState'));
export const selectRouterStateUrl = createSelector(selectRouterState, prop('url'));

export const selectCurrentRouteName = createSelector(selectRouterState, prop('name'));
export const selectCurrentRouteTitle = createSelector(selectRouterState, path(['data', 'title']));

export const selectRouterPrevState = createSelector(selectRouterSlice, prop('prevState'));

export const selectRouterPrevParams = createSelector(selectRouterSlice, prop('prevParams'));

export const selectPreviousRouteName = createSelector(selectRouterPrevState, prop('name'));

const includesNamePart = (part) => (stringToSearch = '') => stringToSearch.includes(part);

const includesNamePartSeparateByDot = (part) => (stringToSearch = '') => stringToSearch.split('.').includes(part);
const nameIncludesOrganization = includesNamePart('organization');
const nameIncludesTransitiveViolations = includesNamePart('transitiveViolations');
const nameIncludesApplication = includesNamePart('application');
const nameIncludesRepositories = includesNamePart('repositories');
const nameIncludesRepository = includesNamePartSeparateByDot('repository');
const nameIncludesFirewall = includesNamePart('firewall');
const nameStartsWithDeveloper = (stringToSearch = '') => stringToSearch.startsWith('developer');
export const nameStartsWithFirewall = (stringToSearch = '') => stringToSearch.startsWith('firewall');
const nameIncludesRepositoryContainer = includesNamePart('repository_container');
const nameIncludesRepositoryManager = includesNamePart('repository_manager');
const nameIncludesCategory = includesNamePart('category');
const nameIncludesPolicy = includesNamePart('policy');
const nameIncludesLegacyViolations = includesNamePart('legacyViolations');
const nameIncludesMonitoring = includesNamePart('monitoring');
const nameIncludesProprietary = includesNamePart('proprietary');
const nameIncludesLabel = includesNamePart('label');
const nameIncludesLicenseThreatGroup = includesNamePart('licenseThreatGroup');
const nameIncludesSourceControl = includesNamePart('source-control');
const nameIncludesOnboarding = includesNamePart('onboarding');
const nameIncludesAccess = includesNamePart('access');
const nameIncludesPrioritiesPageContainer = includesNamePart('WithinPrioritiesPageContainer');
const nameIncludesPrioritiesPage = includesNamePart('prioritiesPage');
const nameIncludesWaivers = includesNamePart('waivers');

export const selectIsOrganization = createSelector(selectCurrentRouteName, nameIncludesOrganization);
export const selectIsTransitiveViolations = createSelector(selectCurrentRouteName, nameIncludesTransitiveViolations);
export const selectIsApplication = createSelector(selectCurrentRouteName, nameIncludesApplication);
export const selectIsRepositories = createSelector(selectCurrentRouteName, nameIncludesRepositories);
export const selectIsRepository = createSelector(selectCurrentRouteName, nameIncludesRepository);
export const selectIsFirewall = createSelector(selectCurrentRouteName, nameIncludesFirewall);
export const selectIsSbomManager = createSelector(selectCurrentRouteName, nameStartsWithSbomManager);
export const selectIsSbomManagerComponentDetails = createSelector(
  selectCurrentRouteName,
  isSbomManagerComponentDetails
);
export const selectIsRepositoryContainer = createSelector(selectCurrentRouteName, nameIncludesRepositoryContainer);
export const selectIsRepositoryManager = createSelector(selectCurrentRouteName, nameIncludesRepositoryManager);
export const selectIsCategory = createSelector(selectRouterStateUrl, nameIncludesCategory);
export const selectIsLegacyViolation = createSelector(selectRouterStateUrl, nameIncludesLegacyViolations);
export const selectIsPolicy = createSelector(selectRouterStateUrl, nameIncludesPolicy);
export const selectIsMonitoring = createSelector(selectRouterStateUrl, nameIncludesMonitoring);
export const selectIsProprietary = createSelector(selectRouterStateUrl, nameIncludesProprietary);
export const selectIsLabel = createSelector(selectRouterStateUrl, nameIncludesLabel);
export const selectIsLicenseThreatGroup = createSelector(selectRouterStateUrl, nameIncludesLicenseThreatGroup);
export const selectIsSourceControl = createSelector(selectRouterStateUrl, nameIncludesSourceControl);
export const selectIsScmOnboarding = createSelector(selectRouterStateUrl, nameIncludesOnboarding);
export const selectIsAccess = createSelector(selectRouterStateUrl, nameIncludesAccess);
export const selectIsPrevFirewall = createSelector(selectPreviousRouteName, nameIncludesFirewall);
export const selectIsWaivers = createSelector(selectRouterStateUrl, nameIncludesWaivers);
export const selectIsPrioritiesPageContainer = createSelector(selectCurrentRouteName, (routeName) => {
  return nameIncludesPrioritiesPageContainer(routeName) || nameIncludesPrioritiesPage(routeName);
});
export const selectPrioritiesPageContainerName = createSelector(
  selectIsPrioritiesPageContainer,
  selectCurrentRouteName,
  (isPrioritiesPageContainer, currentRouteName) => {
    if (isPrioritiesPageContainer) {
      return currentRouteName.split('.')[0];
    }
  }
);
export const selectPrioritiesPageName = createSelector(
  selectIsPrioritiesPageContainer,
  selectCurrentRouteName,
  (isPrioritiesPageContainer, currentRouteName) => {
    if (isPrioritiesPageContainer) {
      const prioritiesPageContainerName = currentRouteName.split('.')[0];
      if (prioritiesPageContainerName === 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard') {
        return 'prioritiesPageFromDashboard';
      } else if (prioritiesPageContainerName === 'componentDetailsPageWithinPrioritiesPageContainerFromReports') {
        return 'prioritiesPageFromReports';
      } else if (prioritiesPageContainerName === 'componentDetailsPageWithinPrioritiesPageContainerFromIntegrations') {
        return 'prioritiesPageFromIntegrations';
      }
    }
    return '';
  }
);
export const selectIsDeveloper = createSelector(selectCurrentRouteName, nameStartsWithDeveloper);
export const selectIsStandaloneDeveloper = createSelector(
  selectIsDeveloper,
  selectIsPrioritiesPageContainer,
  (isDeveloper, isPrioritiesPageContainer) => isDeveloper || isPrioritiesPageContainer
);
export const selectIsDependencyTreePageFromPrioritiesPage = createSelector(selectCurrentRouteName, (currentRouteName) =>
  [
    'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.dependencyTree',
    'componentDetailsPageWithinPrioritiesPageContainerFromReports.dependencyTree',
    'componentDetailsPageWithinPrioritiesPageContainerFromIntegrations.dependencyTree',
  ].includes(currentRouteName)
);

export const selectIsStandaloneFirewall = createSelector(selectCurrentRouteName, nameStartsWithFirewall);

// we can access to component details page from application report but also from firewall or repository results view,
// so this is used to find out if the route is a firewall route or repository route
export const selectIsFirewallOrRepository = createSelector(
  selectIsFirewall,
  selectIsRepository,
  (isFirewall, isRepository) => isFirewall || isRepository
);
// for some pages, state treatment is the same regardless if the route contains /repository, /repository_container
// or /repositories
export const selectIsRepositoriesRelated = createSelector(
  selectIsRepository,
  selectIsRepositories,
  selectIsRepositoryContainer,
  selectIsRepositoryManager,
  (isRepository, isRepositories, isRepositoryContainer, isRepositoryManager) =>
    isRepository || isRepositories || isRepositoryContainer || isRepositoryManager
);

export const selectOrganizationId = createSelector(selectRouterCurrentParams, propOr('', 'organizationId'));
export const selectApplicationId = createSelector(selectRouterCurrentParams, propOr('', 'applicationPublicId'));
export const selectRepositoryId = createSelector(selectRouterCurrentParams, propOr('', 'repositoryId'));
export const selectSbomVersionId = createSelector(selectRouterCurrentParams, propOr('', 'versionId'));
export const selectSbomVersionIdCdp = createSelector(selectRouterCurrentParams, propOr('', 'sbomVersion'));
export const selectSbomComponentHash = createSelector(selectRouterCurrentParams, propOr('', 'componentHash'));
export const selectRepositoryManagerId = createSelector(selectRouterCurrentParams, propOr('', 'repositoryManagerId'));
export const selectRepositoryContainerId = createSelector(
  selectRouterCurrentParams,
  propOr('REPOSITORY_CONTAINER_ID', 'repositoryContainerId')
);
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
  selectIsRepositoryManager,
  selectIsRepository,
  selectOrganizationId,
  selectApplicationId,
  selectRepositoryManagerId,
  selectRepositoryId,
  (
    isOrganization,
    isApplication,
    isRepositories,
    isRepositoryManager,
    isRepository,
    organizationId,
    applicationId,
    repositoryManagerId,
    repositoryId
  ) => {
    const ownerId = isApplication
      ? applicationId
      : isOrganization
      ? organizationId
      : isRepositoryManager
      ? repositoryManagerId
      : isRepository
      ? repositoryId
      : 'global';
    const ownerType = isApplication
      ? 'application'
      : isOrganization
      ? 'organization'
      : isRepositoryManager
      ? 'repository_manager'
      : isRepositories
      ? 'repository_container'
      : isRepositoryManager
      ? 'repository_manager'
      : isRepository
      ? 'repository'
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
  (currentState) =>
    currentState.name === 'management.view' ||
    currentState.name === 'sbomManager.management.view' ||
    currentState.name === 'firewall.management.view'
);

export const selectIncludesManagementView = createSelector(
  selectRouterState,
  (currentState) => currentState.name !== 'management.view' && currentState.name.includes('management.view')
);

export const selectPrevStateIsAppOwnerManagementView = createSelector(selectRouterPrevState, (prevState) =>
  prevState.name?.includes('management.view.application')
);

export const selectPrevStateIsFirewallDashboard = createSelector(selectRouterPrevState, (prevState) =>
  prevState.name?.includes('firewall.firewallPage')
);

export const selectPrevStateIsRepositoryManagerView = createSelector(selectRouterPrevState, (prevState) =>
  prevState.name?.includes('management.view.repository_manager')
);

export const selectHideBackButtonParam = createSelector(selectRouterCurrentParams, propOr(false, 'hideBackButton'));
