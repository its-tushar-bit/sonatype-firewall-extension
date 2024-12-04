/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';

import SbomManagerSidebar from 'MainRoot/sbomManager/sidebar/SbomManagerSidebar';
import * as RouterStateContextModule from 'MainRoot/react/RouterStateContext';

describe('SbomManagerSidebar', () => {
  let renderComponent;
  beforeEach(() => {
    const defaultPreloadedState = {
      router: {
        currentState: {
          name: 'sbomManager.dashboard',
        },
      },
    };

    const mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        switch (stateName) {
          case 'sbomManager.dashboard':
            return '#/sbomManager/dashboard';
          case 'sbomManager.applications':
            return '#/sbomManager/applications';
          case 'sbomManager.management.view':
            return '#/sbomManager/management/view';
          case 'sbomManager.management.tree':
            return '#/sbomManager/management/tree';
          case 'sbomManager.advancedSearch':
            return '#/sbomManager/advancedSearch';
          default:
            return '/mocked-default-href';
        }
      }),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContextModule, 'useRouterState').mockImplementation(() => mockRouterState);

    const props = { isLoggedIn: true, isSbomManagerEnabled: true };
    renderComponent = (additionalProps, preloadedState) =>
      render(<SbomManagerSidebar {...props} {...additionalProps} />, {
        preloadedState: { ...defaultPreloadedState, ...preloadedState },
      });
  });

  it('renders correctly when user is logged in and sbomManager is enabled', () => {
    renderComponent();
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(5);
    const mainLink = sidebarLinks[0];
    const dashboardLink = sidebarLinks[1];
    const applicationsLink = sidebarLinks[2];
    const orgsLink = sidebarLinks[3];
    const searchLink = sidebarLinks[4];
    expect(mainLink).toHaveAttribute('href', '#/sbomManager/dashboard');
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/sbomManager/dashboard');
    expect(applicationsLink).toHaveTextContent('Applications');
    expect(applicationsLink).toHaveAttribute('href', '#/sbomManager/applications');
    expect(orgsLink).toHaveTextContent('Organizations');
    expect(orgsLink).toHaveAttribute('href', '#/sbomManager/management/view');
    expect(searchLink).toHaveTextContent('Advanced Search');
    expect(searchLink).toHaveAttribute('href', '#/sbomManager/advancedSearch');
  });

  it('does not render the sidebar when the user is not logged in', () => {
    renderComponent({ isLoggedIn: false });
    const sidebarLinks = screen.getAllByRole('link');
    const mainLink = sidebarLinks[0];
    expect(sidebarLinks.length).toBe(1);
    expect(mainLink).toHaveAttribute('href', '#/sbomManager/dashboard');
  });

  it('does not render the sidebar when isSbomManagerEnabled is disabled', () => {
    renderComponent({ isSbomManagerEnabled: false });
    const sidebarLinks = screen.getAllByRole('link');
    const mainLink = sidebarLinks[0];
    expect(sidebarLinks.length).toBe(1);
    expect(mainLink).toHaveAttribute('href', '#/sbomManager/dashboard');
  });
});
