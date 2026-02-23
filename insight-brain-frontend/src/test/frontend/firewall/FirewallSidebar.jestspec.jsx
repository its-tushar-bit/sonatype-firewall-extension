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

  it('renders correctly when user is logged in', () => {
    renderComponent();
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(4);
    const dashboardLink = sidebarLinks[0];
    const repositoriesLink = sidebarLinks[1];
    const vulnSearchLink = sidebarLinks[2];
    const enterpriseReportingLink = sidebarLinks[3];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/firewall/dashboard');
    expect(repositoriesLink).toHaveTextContent('Repos and Policies');
    expect(repositoriesLink).toHaveAttribute('href', '#/firewall/management/view');
    expect(vulnSearchLink).toHaveTextContent('Vulnerability Lookup');
    expect(vulnSearchLink).toHaveAttribute('href', '#/firewall/vulnerabilities');
    expect(enterpriseReportingLink).toHaveTextContent('Enterprise Reporting');
    expect(enterpriseReportingLink).toHaveAttribute('href', '#/firewall/enterprise-reporting');
  });

  it('does not render the sidebar when the user is not logged in', () => {
    renderComponent({ isLoggedIn: false });
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('does not render the api link when isApiPageEnabled is false', () => {
    renderComponent({ isApiPageEnabled: false });
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(4);
    const dashboardLink = sidebarLinks[0];
    const repositoriesLink = sidebarLinks[1];
    const vulnSearchLink = sidebarLinks[2];
    const enterpriseReportingLink = sidebarLinks[3];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/firewall/dashboard');
    expect(repositoriesLink).toHaveTextContent('Repos and Policies');
    expect(repositoriesLink).toHaveAttribute('href', '#/firewall/management/view');
    expect(vulnSearchLink).toHaveTextContent('Vulnerability Lookup');
    expect(vulnSearchLink).toHaveAttribute('href', '#/firewall/vulnerabilities');
    expect(enterpriseReportingLink).toHaveTextContent('Enterprise Reporting');
    expect(enterpriseReportingLink).toHaveAttribute('href', '#/firewall/enterprise-reporting');
    expect(screen.queryByRole('link', { name: 'API' })).not.toBeInTheDocument();
  });

  it('does render the api link when isApiPageEnabled is true', () => {
    renderComponent({ isApiPageEnabled: true });
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(5);
    const dashboardLink = sidebarLinks[0];
    const repositoriesLink = sidebarLinks[1];
    const vulnSearchLink = sidebarLinks[2];
    const apiLink = sidebarLinks[3];
    const enterpriseReportingLink = sidebarLinks[4];
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '#/firewall/dashboard');
    expect(repositoriesLink).toHaveTextContent('Repos and Policies');
    expect(repositoriesLink).toHaveAttribute('href', '#/firewall/management/view');
    expect(vulnSearchLink).toHaveTextContent('Vulnerability Lookup');
    expect(vulnSearchLink).toHaveAttribute('href', '#/firewall/vulnerabilities');
    expect(apiLink).toHaveTextContent('API');
    expect(apiLink).toHaveAttribute('href', '#/firewall/api');
    expect(enterpriseReportingLink).toHaveTextContent('Enterprise Reporting');
    expect(enterpriseReportingLink).toHaveAttribute('href', '#/firewall/enterprise-reporting');
  });
});
