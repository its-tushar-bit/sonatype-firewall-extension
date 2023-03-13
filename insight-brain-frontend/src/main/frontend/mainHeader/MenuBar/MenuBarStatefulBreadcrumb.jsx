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
  selectIsRepositories,
  selectCurrentRouteTitle,
  selectCurrentRouteName,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { useRouterState } from '../../react/RouterStateContext';
import {
  selectOwnersMap,
  selectDisplayedOrganization,
} from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';

const BREAD_CRUMB_CONTAINER_ID = 'menu-bar__bread-crumb-container';

const getBreadcrumb = (
  uiRouterState,
  ownersMap,
  displayedOrganization,
  isRepositories,
  isApplication,
  applicationPublicId,
  pageTitle,
  currentRouteName
) => {
  const breadcrumb = [];

  if (isRepositories) {
    breadcrumb.unshift({
      name: 'Repositories',
      href: uiRouterState.href('management.view.repositories'),
    });
  }

  if (currentRouteName.includes('management.edit')) {
    const id = isApplication ? ownersMap[applicationPublicId].publicId : displayedOrganization.id;
    const href = uiRouterState.href(
      currentRouteName,
      isApplication ? { applicationPublicId: id } : { organizationId: id }
    );
    breadcrumb.unshift({ name: pageTitle, href });
  }

  if (isApplication && ownersMap.hasOwnProperty(applicationPublicId)) {
    const displayedApplication = ownersMap[applicationPublicId];
    breadcrumb.unshift({
      name: displayedApplication.name,
      href: uiRouterState.href(`management.view.application`, { applicationPublicId: displayedApplication.publicId }),
    });
  }

  let currentOrganizationToAdd = displayedOrganization;
  breadcrumb.unshift({
    name: currentOrganizationToAdd.name,
    href: uiRouterState.href(`management.view.organization`, {
      organizationId: currentOrganizationToAdd.id,
    }),
  });

  // add all ancestors to breadcrumb
  while (currentOrganizationToAdd?.parentOrganizationId) {
    currentOrganizationToAdd = ownersMap[currentOrganizationToAdd.parentOrganizationId];
    if (!isNilOrEmpty(currentOrganizationToAdd)) {
      breadcrumb.unshift({
        name: currentOrganizationToAdd.name,
        href: uiRouterState.href(`management.view.organization`, {
          organizationId: currentOrganizationToAdd.id,
        }),
      });
    }
  }

  return breadcrumb;
};

const MenuBarStatefulBreadcrumb = () => {
  // Portal configuration
  const container = document.getElementById(BREAD_CRUMB_CONTAINER_ID);
  const containerRef = useRef(null);
  containerRef.current = containerRef.current || container;

  const uiRouterState = useRouterState();
  const ownersMap = useSelector(selectOwnersMap);
  const displayedOrganization = useSelector(selectDisplayedOrganization);
  const isApplication = useSelector(selectIsApplication);
  const isRepositories = useSelector(selectIsRepositories);
  const applicationPublicId = useSelector(selectApplicationId);
  const pageTitle = useSelector(selectCurrentRouteTitle);
  const routeName = useSelector(selectCurrentRouteName);

  if (isNilOrEmpty(ownersMap) || isNilOrEmpty(displayedOrganization)) {
    return null;
  }

  const breadcrumb = getBreadcrumb(
    uiRouterState,
    ownersMap,
    displayedOrganization,
    isRepositories,
    isApplication,
    applicationPublicId,
    pageTitle,
    routeName
  );

  const renderComponentInsidePortal = (componentToRender) =>
    containerRef.current && ReactDOM.createPortal(componentToRender, containerRef.current);

  return renderComponentInsidePortal(<NxStatefulBreadcrumb crumbs={breadcrumb} />);
};

export default MenuBarStatefulBreadcrumb;
