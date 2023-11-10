/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import MenuBarStatefulBreadcrumb from 'MainRoot/mainHeader/MenuBar/MenuBarStatefulBreadcrumb';
import { render, screen } from 'TestRoot/SpecUtil';
import { getOwnersMap } from 'TestRoot/OrgsAndPolicies/ownerSideNav/nLevelMockData';

export const setupMenuBarBreadcrumbsPortalContainer = () => {
  if (!global.document.getElementById('menu-bar__bread-crumb-container')) {
    const breadCrumbContainer = global.document.createElement('div');
    breadCrumbContainer.setAttribute('id', 'menu-bar__bread-crumb-container');
    const body = global.document.querySelector('body');
    body.appendChild(breadCrumbContainer);
  }
};

describe('MenuBarStatefulBreadcrumb', () => {
  setupMenuBarBreadcrumbsPortalContainer();

  let state;
  let renderComponent;
  const applicationPublicId = 'application publicId 2 at organization 2';
  const organizationsDepth = 2;
  const ownersMap = getOwnersMap(organizationsDepth);
  const displayedApplication = ownersMap[applicationPublicId];
  const displayedOrganization = ownersMap[displayedApplication.organizationId];

  beforeEach(() => {
    state = {
      router: {
        currentParams: { applicationPublicId },
        currentState: { name: 'management.view.application' },
      },
      orgsAndPolicies: {
        ownerSideNav: {
          ownersMap,
          displayedOrganization,
        },
      },
    };

    renderComponent = (preloadedState = state) => {
      return render(<MenuBarStatefulBreadcrumb />, { preloadedState });
    };
  });

  it('renders bread crumb', async () => {
    renderComponent();

    const expectedBreadCrumbs = [
      'ROOT_ORGANIZATION_NAME',
      'organization name 1',
      'organization name 2',
      'application name 2 at organization 2',
    ];

    expectedBreadCrumbs.forEach((breadCrumbName) => {
      expect(screen.getByText(breadCrumbName)).toBeVisible();
    });
  });

  it('renders bread crumbs for all repositories page', async () => {
    const stateWithRouterInRepoContainerPage = {
      ...state,
      router: {
        currentState: { name: 'management.view.repository_container' },
        currentParams: { repositoryContainerId: 'REPOSITORY_CONTAINER_ID' },
      },
    };

    renderComponent(stateWithRouterInRepoContainerPage);

    const expectedBreadCrumbs = ['ROOT_ORGANIZATION_NAME', 'All Repositories'];

    expectedBreadCrumbs.forEach((breadCrumbName) => {
      expect(screen.getByText(breadCrumbName)).toBeVisible();
    });
  });
});
