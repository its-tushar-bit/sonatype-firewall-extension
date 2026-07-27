/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxStatefulBreadcrumb } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';

import {
  selectIsApplication,
  selectApplicationId,
  selectCurrentRouteTitle,
  selectCurrentRouteName,
  selectIsRepositoryContainer,
  selectIsVirtualRepositoryContainer,
  selectIsRepository,
  selectRepositoryId,
  selectIsRepositoryManager,
  selectSbomVersionId,
  selectIsSbomManagerComponentDetails,
  selectSbomComponentHash,
  selectSbomVersionIdCdp,
  selectRoutePrefix,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { useRouterState } from '../../react/RouterStateContext';
import {
  selectOwnersMap,
  selectDisplayedOrganization,
  selectIsVirtualRepositoryManager,
} from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { getOwnerInfo } from 'MainRoot/OrgsAndPolicies/ownerSideNav/utils';
import { selectNoSbomManagerEnabledError } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectComponentDetails } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';

const getBreadcrumb = (
  uiRouterState,
  ownersMap,
  displayedOrganization,
  isApplication,
  isRepository,
  applicationPublicId,
  repositoryId,
  pageTitle,
  currentRouteName,
  routePrefix,
  sbomVersionId,
  isSbomManagerCdp,
  sbomComponentHash,
  sbomManagerComponentDisplayName = '',
  isVirtualRepositoryManager = false,
  isVirtualRepositoryContainer = false
) => {
  const isSbomManager = routePrefix === 'sbomManager.';
  const breadcrumb = [];

  if (isSbomManagerCdp) {
    // component cdp link
    const sbomCdpHref = uiRouterState.href(currentRouteName, applicationPublicId, sbomVersionId, sbomComponentHash);
    breadcrumb.unshift({ name: sbomManagerComponentDisplayName, href: sbomCdpHref });

    // sbom version link
    const sbomVersionHref = uiRouterState.href('sbomManager.management.view.bom', {
      applicationPublicId,
      versionId: sbomVersionId,
    });
    breadcrumb.unshift({ name: sbomVersionId, href: sbomVersionHref });
  }

  if (currentRouteName.includes('management.edit')) {
    const id = isApplication ? ownersMap[applicationPublicId]?.publicId : displayedOrganization.id;
    const href = uiRouterState.href(
      currentRouteName,
      isApplication ? { applicationPublicId: id } : { organizationId: id }
    );
    breadcrumb.unshift({ name: pageTitle, href });

    if (currentRouteName.endsWith('.manage-github-apps')) {
      const sourceControlRoute = currentRouteName.replace('.manage-github-apps', '.edit-source-control');
      const sourceControlHref = uiRouterState.href(
        sourceControlRoute,
        isApplication ? { applicationPublicId: id } : { organizationId: id }
      );
      breadcrumb.unshift({ name: 'Source Control Configuration', href: sourceControlHref });
    }
  }

  if (isSbomManager && sbomVersionId && !isSbomManagerCdp) {
    const href = uiRouterState.href(currentRouteName, applicationPublicId, sbomVersionId);
    breadcrumb.unshift({ name: sbomVersionId, href });
  }

  if ((isApplication && ownersMap.hasOwnProperty(applicationPublicId)) || (isSbomManager && sbomVersionId)) {
    const displayedApplication = ownersMap[applicationPublicId];
    breadcrumb.unshift({
      name: displayedApplication.name,
      href: uiRouterState.href(`${routePrefix}management.view.application`, {
        applicationPublicId: displayedApplication.publicId,
      }),
    });
  }

  if (isRepository && ownersMap.hasOwnProperty(repositoryId)) {
    const displayedRepository = ownersMap[repositoryId];
    breadcrumb.unshift({
      name: `${displayedRepository.name} (${displayedRepository.format} : ${displayedRepository.repositoryType})`,
      href: uiRouterState.href(`${routePrefix}management.view.repository`, { repositoryId: repositoryId }),
    });
  }

  let currentOwner = displayedOrganization;

  while (!isNilOrEmpty(currentOwner)) {
    const [parentEntityIdKey, routeParams] = getOwnerInfo(currentOwner);
    const isRepoContainerCrumb = currentOwner.type === 'repository_container';
    const isRepoManagerCrumb = currentOwner.type === 'repository_manager';
    const isVirtualManagerCrumb = isRepoManagerCrumb && currentOwner.managerType === 'virtual';
    const showContainerAsVirtual = isRepoContainerCrumb && (isVirtualRepositoryManager || isVirtualRepositoryContainer);

    let crumbType = currentOwner.type;
    let crumbName = currentOwner.name;
    if (showContainerAsVirtual) {
      crumbType = 'virtual_repository_container';
      crumbName = 'Virtual Repository Managers';
    } else if (isVirtualManagerCrumb) {
      crumbType = 'virtual_repository_manager';
    }

    breadcrumb.unshift({
      name: crumbName,
      href: uiRouterState.href(`${routePrefix}management.view.${crumbType}`, routeParams),
    });

    currentOwner = currentOwner[parentEntityIdKey] ? ownersMap[currentOwner[parentEntityIdKey]] : null;
  }

  return breadcrumb;
};

const MenuBarStatefulBreadcrumb = () => {
  const uiRouterState = useRouterState();

  const routeName = useSelector(selectCurrentRouteName);
  const ownersMap = useSelector(selectOwnersMap);
  const displayedOrganization = useSelector(selectDisplayedOrganization);
  const isApplication = useSelector(selectIsApplication);
  const isRepositoryContainer = useSelector(selectIsRepositoryContainer);
  const isRepositoryManager = useSelector(selectIsRepositoryManager);
  const isRepository = useSelector(selectIsRepository) && !isRepositoryContainer && !isRepositoryManager;
  const applicationPublicId = useSelector(selectApplicationId);
  const repositoryId = useSelector(selectRepositoryId);
  const pageTitle = useSelector(selectCurrentRouteTitle);
  const routePrefix = useSelector(selectRoutePrefix);
  const isVirtualRepositoryManager = useSelector(selectIsVirtualRepositoryManager);
  const isVirtualRepositoryContainer = useSelector(selectIsVirtualRepositoryContainer);
  const isSbomManagerCdp = useSelector(selectIsSbomManagerComponentDetails);
  const sbomVersionId = isSbomManagerCdp ? useSelector(selectSbomVersionIdCdp) : useSelector(selectSbomVersionId);
  const sbomManagerComponentDisplayName = useSelector(selectComponentDetails)?.displayName;
  const sbomComponentHash = useSelector(selectSbomComponentHash);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);

  if (isNilOrEmpty(ownersMap) || isNilOrEmpty(displayedOrganization) || noSbomManagerEnabledError) {
    return null;
  }

  const breadcrumb = getBreadcrumb(
    uiRouterState,
    ownersMap,
    displayedOrganization,
    isApplication,
    isRepository,
    applicationPublicId,
    repositoryId,
    pageTitle,
    routeName,
    routePrefix,
    sbomVersionId,
    isSbomManagerCdp,
    sbomComponentHash,
    sbomManagerComponentDisplayName,
    isVirtualRepositoryManager,
    isVirtualRepositoryContainer
  );

  return <NxStatefulBreadcrumb crumbs={breadcrumb} />;
};

export default MenuBarStatefulBreadcrumb;
