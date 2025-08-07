/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';

import FirewallSidebar from 'MainRoot/firewall/FirewallSidebar';
import * as RouterStateContextModule from 'MainRoot/react/RouterStateContext';

describe('FirewallSidebar', () => {
  let renderComponent;
  beforeEach(() => {
    const defaultPreloadedState = {
      router: {
        currentState: {
          name: 'firewall.firewallPage',
        },
      },
    };

    const mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        switch (stateName) {
          case 'firewall.firewallPage':
            return '#/malware-defense/dashboard';
          case 'firewall.management.view':
            return '#/malware-defense/management/view';
          case 'firewall.api':
            return '#/malware-defense/api';
          default:
            return '/mocked-default-href';
        }
      }),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContextModule, 'useRouterState').mockImplementation(() => mockRouterState);

    const props = { isLoggedIn: true };
    renderComponent = (additionalProps, preloadedState) =>
      render(<FirewallSidebar {...props} {...additionalProps} />, {
        preloadedState: { ...defaultPreloadedState, ...preloadedState },
      });
  });

  it('renders correctly when user is logged in', () => {
    renderComponent();
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(2);
    const dashboardLink = sidebarLinks[0];
    const repositoriesLink = sidebarLinks[1];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/malware-defense/dashboard');
    expect(repositoriesLink).toHaveTextContent('Repos and Policies');
    expect(repositoriesLink).toHaveAttribute('href', '#/malware-defense/management/view');
  });

  it('does not render the sidebar when the user is not logged in', () => {
    renderComponent({ isLoggedIn: false });
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('does not render the api link when isApiPageEnabled is false', () => {
    renderComponent({ isApiPageEnabled: false });
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(2);
    const dashboardLink = sidebarLinks[0];
    const repositoriesLink = sidebarLinks[1];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/malware-defense/dashboard');
    expect(repositoriesLink).toHaveTextContent('Repos and Policies');
    expect(repositoriesLink).toHaveAttribute('href', '#/malware-defense/management/view');
    expect(screen.queryByRole('link', { name: 'API' })).not.toBeInTheDocument();
  });

  it('does render the api link when isApiPageEnabled is true', () => {
    renderComponent({ isApiPageEnabled: true });
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(3);
    const dashboardLink = sidebarLinks[0];
    const repositoriesLink = sidebarLinks[1];
    const apiLink = sidebarLinks[2];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/malware-defense/dashboard');
    expect(repositoriesLink).toHaveTextContent('Repos and Policies');
    expect(repositoriesLink).toHaveAttribute('href', '#/malware-defense/management/view');
    expect(apiLink).toHaveTextContent('API');
    expect(apiLink).toHaveAttribute('href', '#/malware-defense/api');
  });
});
