/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef } from 'react';
import * as ReactDOM from 'react-dom';
import { NxStatefulBreadcrumb } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';

import {
  selectIsApplication,
  selectApplicationId,
  selectCurrentRouteTitle,
  selectCurrentRouteName,
  selectIsRepositoryContainer,
  selectIsRepository,
  selectRepositoryId,
  selectIsRepositoryManager,
  selectIsSbomManager,
  selectSbomVersionId,
  selectIsSbomManagerComponentDetails,
  selectSbomComponentHash,
  selectSbomVersionIdCdp,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { useRouterState } from '../../react/RouterStateContext';
import {
  selectOwnersMap,
  selectDisplayedOrganization,
} from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { getOwnerInfo } from 'MainRoot/OrgsAndPolicies/ownerSideNav/utils';
import { selectNoSbomManagerEnabledError } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectComponentDetails } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';

const BREAD_CRUMB_CONTAINER_ID = 'menu-bar__bread-crumb-container';

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
  isSbomManager,
  sbomVersionId,
  isSbomManagerCdp,
  sbomComponentHash,
  sbomManagerComponentDisplayName = ''
) => {
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
  }

  if (isSbomManager && sbomVersionId && !isSbomManagerCdp) {
    const href = uiRouterState.href(currentRouteName, applicationPublicId, sbomVersionId);
    breadcrumb.unshift({ name: sbomVersionId, href });
  }

  if ((isApplication && ownersMap.hasOwnProperty(applicationPublicId)) || (isSbomManager && sbomVersionId)) {
    const displayedApplication = ownersMap[applicationPublicId];
    breadcrumb.unshift({
      name: displayedApplication.name,
      href: uiRouterState.href(`${isSbomManager ? 'sbomManager.' : ''}management.view.application`, {
        applicationPublicId: displayedApplication.publicId,
      }),
    });
  }

  if (isRepository && ownersMap.hasOwnProperty(repositoryId)) {
    const displayedRepository = ownersMap[repositoryId];
    breadcrumb.unshift({
      name: displayedRepository.name,
      href: uiRouterState.href(`management.view.repository`, { repositoryId: repositoryId }),
    });
  }

  let currentOwner = displayedOrganization;

  while (!isNilOrEmpty(currentOwner)) {
    const [parentEntityIdKey, routeParams] = getOwnerInfo(currentOwner);
    breadcrumb.unshift({
      name: currentOwner.name,
      href: uiRouterState.href(
        `${isSbomManager ? 'sbomManager.' : ''}management.view.${currentOwner.type}`,
        routeParams
      ),
    });

    currentOwner = currentOwner[parentEntityIdKey] ? ownersMap[currentOwner[parentEntityIdKey]] : null;
  }

  return breadcrumb;
};

const MenuBarStatefulBreadcrumb = () => {
  // Portal configuration
  const container = document.getElementById(BREAD_CRUMB_CONTAINER_ID);
  const containerRef = useRef(null);
  containerRef.current = containerRef.current || container;

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
  const isSbomManager = useSelector(selectIsSbomManager);
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
    isSbomManager,
    sbomVersionId,
    isSbomManagerCdp,
    sbomComponentHash,
    sbomManagerComponentDisplayName
  );

  const renderComponentInsidePortal = (componentToRender) =>
    containerRef.current && ReactDOM.createPortal(componentToRender, containerRef.current);

  return renderComponentInsidePortal(<NxStatefulBreadcrumb crumbs={breadcrumb} />);
};

export default MenuBarStatefulBreadcrumb;
