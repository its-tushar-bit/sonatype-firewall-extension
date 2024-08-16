/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';

import SonatypeDeveloperSidebar from 'MainRoot/development/SonatypeDeveloperSidebar';
import * as RouterStateContextModule from 'MainRoot/react/RouterStateContext';

describe('SonatypeDeveloperSidebar', () => {
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
          case 'developer.dashboard':
            return '#/developer/dashboard';
          case 'developer.reports':
            return '#/developer/reports';
          case 'developer.advancedSearch':
            return '#/developer/advancedSearch';
          default:
            return '/mocked-default-href';
        }
      }),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContextModule, 'useRouterState').mockImplementation(() => mockRouterState);

    const props = { isLoggedIn: true, isAdvancedSearchEnabled: true };
    renderComponent = (additionalProps, preloadedState) =>
      render(<SonatypeDeveloperSidebar {...props} {...additionalProps} />, {
        preloadedState: { ...defaultPreloadedState, ...preloadedState },
      });
  });

  it('renders correctly when user is logged in ', () => {
    renderComponent();
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(4);
    const mainLink = sidebarLinks[0];
    const dashboardLink = sidebarLinks[1];
    const reportsLink = sidebarLinks[2];
    const searchLink = sidebarLinks[3];
    expect(mainLink).toHaveAttribute('href', '#/developer/dashboard');
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/developer/dashboard');
    expect(reportsLink).toHaveTextContent('Reports');
    expect(reportsLink).toHaveAttribute('href', '#/developer/reports');
    expect(searchLink).toHaveTextContent('Advanced Search');
    expect(searchLink).toHaveAttribute('href', '#/developer/advancedSearch');
  });

  it('does not render advanced search link when isAdvancedSearchEnabled is false', () => {
    renderComponent({ isAdvancedSearchEnabled: false });
    const sidebarLinks = screen.getAllByRole('link');
    const mainLink = sidebarLinks[0];
    const dashboardLink = sidebarLinks[1];
    const reportsLink = sidebarLinks[2];
    expect(mainLink).toHaveAttribute('href', '#/developer/dashboard');
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/developer/dashboard');
    expect(reportsLink).toHaveTextContent('Reports');
    expect(reportsLink).toHaveAttribute('href', '#/developer/reports');
    expect(screen.queryByRole('link', { name: 'Advanced Search' })).not.toBeInTheDocument();
  });

  it('does not render the sidebar when the user is not logged in', () => {
    renderComponent({ isLoggedIn: false });
    const sidebarLinks = screen.getAllByRole('link');
    const mainLink = sidebarLinks[0];
    expect(sidebarLinks.length).toBe(1);
    expect(mainLink).toHaveAttribute('href', '#/developer/dashboard');
  });
});
