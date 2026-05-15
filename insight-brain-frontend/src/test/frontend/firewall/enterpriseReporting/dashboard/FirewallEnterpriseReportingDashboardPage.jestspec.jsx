/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, within } from 'TestRoot/SpecUtil';
import FirewallEnterpriseReportingDashboardPage from 'MainRoot/firewall/enterpriseReporting/dashboard/FirewallEnterpriseReportingDashboardPage';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as useLookerDashboard from 'MainRoot/react/useLookerDashboard';
import { actions } from 'MainRoot/firewall/enterpriseReporting/firewallEnterpriseReportingSlice';
import { initialState as filterInitialState } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';
import { getEnterpriseReportingDashboardsUrl, getEnterpriseReportingBaseUrl } from 'MainRoot/util/CLMLocation';

jest.mock('MainRoot/react/useLookerDashboard', () => jest.fn());

describe('FirewallEnterpriseReportingDashboardPage', () => {
  let axiosMock;
  let mockRouterState;

  const defaultPreloadedState = {
    firewallEnterpriseReporting: {
      dashboards: [
        {
          dashboardId: 'malware-threat-landscape',
          title: 'Malware Threat Landscape',
          category: 'firewall',
          dashboardPath: 'firewall_malware::malware_threat_landscape',
          description: 'Malware threat landscape dashboard',
          features: ['Feature 1'],
          accessButtonText: 'View Dashboard',
          sinceIQVersion: '170',
          previewImageIcon: 'faShieldVirus',
          priorityOrder: 100,
        },
      ],
      loading: false,
      loadError: null,
      baseUrl: 'https://looker.example.com',
      selectedDashboard: {
        dashboardId: 'malware-threat-landscape',
        dashboardPath: 'firewall_malware::malware_threat_landscape',
        category: 'firewall',
      },
    },
    enterpriseReportingFilter: filterInitialState,
    enterpriseReportingSupportInfo: {
      telemetryStatus: { advancedReportingEnabled: true },
      loading: false,
      loadError: null,
    },
    router: {
      currentParams: { id: 'malware-threat-landscape' },
      currentState: { name: 'firewall.enterpriseReportingDashboard' },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, { dashboardMetadata: [] });
    axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(200, 'https://looker.example.com');

    useLookerDashboard.default.mockReturnValue({ loadingDashboard: false, iframeError: false });

    mockRouterState = {
      href: jest.fn().mockImplementation((stateName, params) => {
        if (stateName === 'firewall.enterpriseReportingDashboard' && params?.id) {
          return `#/firewall/enterprise-reporting/${params.id}`;
        }
        return '#/mocked-href';
      }),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const renderComponent = (preloadedState) =>
    render(<FirewallEnterpriseReportingDashboardPage />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });

  it('renders the dashboard page', () => {
    renderComponent();
    expect(screen.getByRole('main')).toBeInTheDocument();
  });

  it('renders the dashboard name in the page title', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: 'Malware Threat Landscape' })).toBeInTheDocument();
  });

  it('renders the Save / Apply Filters button', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: 'Save / Apply Filters' })).toBeInTheDocument();
  });

  it('does not render the filter button bar when selectedDashboard is null', () => {
    const stateWithNullDashboard = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        ...defaultPreloadedState.firewallEnterpriseReporting,
        dashboards: [],
        selectedDashboard: null,
      },
    };
    renderComponent(stateWithNullDashboard);
    expect(screen.queryByRole('button', { name: 'Save / Apply Filters' })).not.toBeInTheDocument();
  });

  it('opens the filter panel when Save / Apply Filters is clicked', async () => {
    const user = userEvent.setup();
    const { store } = renderComponent();
    await user.click(screen.getByRole('button', { name: 'Save / Apply Filters' }));
    expect(store.getState().enterpriseReportingFilter.isOpen).toBe(true);
  });

  it('does not render the filter button bar when selectedDashboard category is not firewall', () => {
    const stateWithEnterpriseCategory = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        ...defaultPreloadedState.firewallEnterpriseReporting,
        dashboards: [],
        selectedDashboard: { dashboardId: 'some-dashboard', category: 'enterprise' },
      },
    };
    renderComponent(stateWithEnterpriseCategory);
    expect(screen.queryByRole('button', { name: 'Save / Apply Filters' })).not.toBeInTheDocument();
  });

  it('renders the current filter tag showing Sonatype Default when no filter is applied', () => {
    renderComponent();
    expect(screen.getByText('Sonatype Default')).toBeInTheDocument();
  });

  it('renders the current filter tag showing the applied filter name when a filter is applied', () => {
    const stateWithFilter = {
      ...defaultPreloadedState,
      enterpriseReportingFilter: {
        ...defaultPreloadedState.enterpriseReportingFilter,
        appliedFilterName: 'My Custom Filter',
        filterState: 'clean',
      },
    };
    renderComponent(stateWithFilter);
    expect(screen.getByText('My Custom Filter')).toBeInTheDocument();
  });

  it('renders a dirty indicator (*) in the filter tag when filter state is changed', () => {
    const dirtyState = {
      ...defaultPreloadedState,
      enterpriseReportingFilter: {
        ...defaultPreloadedState.enterpriseReportingFilter,
        appliedFilterName: 'My Filter',
        filterState: 'changed',
      },
    };
    renderComponent(dirtyState);
    expect(screen.getByText('*My Filter', { exact: false })).toBeInTheDocument();
  });

  it('renders a loading state when loading is true', () => {
    const loadingState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        ...defaultPreloadedState.firewallEnterpriseReporting,
        loading: true,
      },
    };
    renderComponent(loadingState);
    expect(screen.getAllByText('Loading…').length).toBeGreaterThan(0);
  });

  it('renders the navigation bar with a single dashboard as a non-interactive span', () => {
    // defaultPreloadedState has exactly one dashboard — active dashboard renders as span, not link
    renderComponent();
    const nav = screen.getByRole('navigation');
    expect(within(nav).getByText('Malware Threat Landscape')).toBeInTheDocument();
    expect(within(nav).queryByRole('link', { name: 'Malware Threat Landscape' })).not.toBeInTheDocument();
  });

  it('renders the navigation bar with firewall dashboards', () => {
    const stateWithMultipleDashboards = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        ...defaultPreloadedState.firewallEnterpriseReporting,
        dashboards: [
          ...defaultPreloadedState.firewallEnterpriseReporting.dashboards,
          {
            dashboardId: 'firewall-malware',
            title: 'Firewall Malware',
            category: 'firewall',
            dashboardPath: 'firewall_malware::firewall_malware',
            description: 'Firewall malware dashboard',
            features: ['Feature 2'],
            accessButtonText: 'View Dashboard',
            sinceIQVersion: '170',
            previewImageIcon: 'faShieldVirus',
            priorityOrder: 200,
          },
        ],
      },
    };
    renderComponent(stateWithMultipleDashboards);
    expect(screen.getByText('Firewall Dashboards:')).toBeInTheDocument();
    // Active dashboard renders as span (not link) inside the nav
    const nav = screen.getByRole('navigation');
    // Active dashboard renders as span (not link) inside the nav
    expect(within(nav).getByText('Malware Threat Landscape')).toBeInTheDocument();
    expect(within(nav).queryByRole('link', { name: 'Malware Threat Landscape' })).not.toBeInTheDocument();
    // Inactive dashboard renders as link
    const inactiveLink = within(nav).getByRole('link', { name: 'Firewall Malware' });
    expect(inactiveLink).toHaveAttribute('href', '#/firewall/enterprise-reporting/firewall-malware');
    // Lower priorityOrder (100) appears first (left), higher (200) appears second (right)
    const navItems = within(nav).getAllByRole('listitem');
    expect(navItems[0]).toHaveTextContent('Malware Threat Landscape');
    expect(navItems[1]).toHaveTextContent('Firewall Malware');
  });

  it('renders fallback title when selectedDashboardName is absent', () => {
    const stateWithoutName = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        ...defaultPreloadedState.firewallEnterpriseReporting,
        dashboards: [],
        selectedDashboardName: null,
      },
    };
    renderComponent(stateWithoutName);
    expect(screen.getByRole('heading', { name: 'Loading Dashboard...' })).toBeInTheDocument();
  });

  it('renders an error retry button when there is a load error and no baseUrl', () => {
    // Prevent the loadDashboardDetail thunk from overwriting the preloaded error state
    jest.spyOn(actions, 'loadDashboardDetail').mockReturnValue({ type: 'noop' });
    const errorState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        ...defaultPreloadedState.firewallEnterpriseReporting,
        loading: false,
        loadError: 'Failed to load dashboards',
        baseUrl: null,
        selectedDashboard: null,
      },
    };
    renderComponent(errorState);
    expect(screen.getAllByRole('button', { name: /retry/i }).length).toBeGreaterThan(0);
  });
});
