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
          case 'developer.priorities':
            return '#/developer/priorities';
          case 'developer.advancedSearch':
            return '#/developer/advancedSearch';
          case 'developer.api':
            return '#/developer/api';
          default:
            return '/mocked-default-href';
        }
      }),
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
    const dashboardLink = sidebarLinks[0];
    const prioritiesLink = sidebarLinks[1];
    const searchLink = sidebarLinks[2];
    const integrationsHelpLink = sidebarLinks[3];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/developer/dashboard');
    expect(prioritiesLink).toHaveTextContent('Priorities');
    expect(prioritiesLink).toHaveAttribute('href', '#/developer/priorities');
    expect(searchLink).toHaveTextContent('Advanced Search');
    expect(searchLink).toHaveAttribute('href', '#/developer/advancedSearch');
    expect(integrationsHelpLink).toHaveTextContent('Integrations Help');
    expect(integrationsHelpLink).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nxiq/doc/iq-server-integrations'
    );
  });

  it('does not render advanced search link when isAdvancedSearchEnabled is false', () => {
    renderComponent({ isAdvancedSearchEnabled: false });
    const sidebarLinks = screen.getAllByRole('link');
    const dashboardLink = sidebarLinks[0];
    const prioritiesLink = sidebarLinks[1];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/developer/dashboard');
    expect(prioritiesLink).toHaveTextContent('Priorities');
    expect(prioritiesLink).toHaveAttribute('href', '#/developer/priorities');
    expect(screen.queryByRole('link', { name: 'Advanced Search' })).not.toBeInTheDocument();
  });

  it('does not render any links when the user is not logged in', () => {
    renderComponent({ isLoggedIn: false });
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('does not render the api link when isApiPageEnabled is false', () => {
    renderComponent({ isApiPageEnabled: false });
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(4);
    const dashboardLink = sidebarLinks[0];
    const prioritiesLink = sidebarLinks[1];
    const searchLink = sidebarLinks[2];
    const integrationsHelpLink = sidebarLinks[3];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/developer/dashboard');
    expect(prioritiesLink).toHaveTextContent('Priorities');
    expect(prioritiesLink).toHaveAttribute('href', '#/developer/priorities');
    expect(searchLink).toHaveTextContent('Advanced Search');
    expect(searchLink).toHaveAttribute('href', '#/developer/advancedSearch');
    expect(integrationsHelpLink).toHaveTextContent('Integrations Help');
    expect(integrationsHelpLink).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nxiq/doc/iq-server-integrations'
    );
    expect(screen.queryByRole('link', { name: 'API' })).not.toBeInTheDocument();
  });

  it('does render the api link when isApiPageEnabled is true', () => {
    renderComponent({ isApiPageEnabled: true });
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(5);
    const dashboardLink = sidebarLinks[0];
    const prioritiesLink = sidebarLinks[1];
    const searchLink = sidebarLinks[2];
    const integrationsHelpLink = sidebarLinks[3];
    const apiLink = sidebarLinks[4];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/developer/dashboard');
    expect(prioritiesLink).toHaveTextContent('Priorities');
    expect(prioritiesLink).toHaveAttribute('href', '#/developer/priorities');
    expect(searchLink).toHaveTextContent('Advanced Search');
    expect(searchLink).toHaveAttribute('href', '#/developer/advancedSearch');
    expect(integrationsHelpLink).toHaveTextContent('Integrations Help');
    expect(integrationsHelpLink).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nxiq/doc/iq-server-integrations'
    );
    expect(apiLink).toHaveTextContent('API');
    expect(apiLink).toHaveAttribute('href', '#/developer/api');
  });
});
