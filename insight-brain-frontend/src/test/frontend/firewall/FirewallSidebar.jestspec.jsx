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
            return '#/firewall/dashboard';
          case 'firewall.management.view':
            return '#/firewall/management/view';
          case 'firewall.api':
            return '#/firewall/api';
          case 'firewall.vulnerabilitySearch':
            return '#/firewall/vulnerabilities';
          case 'firewall.enterpriseReporting':
            return '#/firewall/enterprise-reporting';
          case 'firewall.waivers':
            return '#/firewall/waivers';
          default:
            return '/mocked-default-href';
        }
      }),
    };
    jest.spyOn(RouterStateContextModule, 'useRouterState').mockImplementation(() => mockRouterState);

    const props = { isLoggedIn: true, isFirewallEnterpriseReportingEnabled: true };
    renderComponent = (additionalProps, preloadedState) =>
      render(<FirewallSidebar {...props} {...additionalProps} />, {
        preloadedState: { ...defaultPreloadedState, ...preloadedState },
      });
  });

  it('renders the Waivers nav link', () => {
    renderComponent();
    const waiversLink = screen.getByRole('link', { name: /waivers/i });
    expect(waiversLink).toBeInTheDocument();
    expect(waiversLink).toHaveAttribute('href', '#/firewall/waivers');
  });

  it('renders correctly when user is logged in with all standard links', () => {
    renderComponent();
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(5);
    const dashboardLink = sidebarLinks[0];
    const repositoriesLink = sidebarLinks[1];
    const vulnSearchLink = sidebarLinks[2];
    const waiversLink = sidebarLinks[3];
    const enterpriseReportingLink = sidebarLinks[4];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/firewall/dashboard');
    expect(repositoriesLink).toHaveTextContent('Repos and Policies');
    expect(repositoriesLink).toHaveAttribute('href', '#/firewall/management/view');
    expect(vulnSearchLink).toHaveTextContent('Vulnerability Lookup');
    expect(vulnSearchLink).toHaveAttribute('href', '#/firewall/vulnerabilities');
    expect(waiversLink).toHaveTextContent('Waivers');
    expect(waiversLink).toHaveAttribute('href', '#/firewall/waivers');
    expect(enterpriseReportingLink).toHaveTextContent('Enterprise Reporting');
    expect(enterpriseReportingLink).toHaveAttribute('href', '#/firewall/enterprise-reporting');
  });

  it('does not render the sidebar when the user is not logged in', () => {
    renderComponent({ isLoggedIn: false });
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('marks Waivers link as selected when on a waivers state', () => {
    renderComponent({ currentState: { name: 'firewall.waivers.components.requested' } });
    const waiversLink = screen.getByRole('link', { name: /waivers/i });
    expect(waiversLink).toHaveClass('selected');
  });

  it('does not render the api link when isApiPageEnabled is false', () => {
    renderComponent({ isApiPageEnabled: false });
    expect(screen.queryByRole('link', { name: 'API' })).not.toBeInTheDocument();
  });

  it('renders the API link when isApiPageEnabled is true', () => {
    renderComponent({ isApiPageEnabled: true });
    expect(screen.getByRole('link', { name: /api/i })).toBeInTheDocument();
  });
});
