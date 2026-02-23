/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import EnterpriseReportingPage from 'MainRoot/firewall/enterpriseReporting/EnterpriseReportingPage';
import { getEnterpriseReportingDashboardsUrl, getIqVersion, getTelemetryStatusUrl } from 'MainRoot/util/CLMLocation';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

describe('EnterpriseReportingPage', () => {
  let axiosMock;
  let mockRouterState;

  const mockDashboards = [
    {
      dashboardId: 'malware-insights',
      title: 'Malware Insights',
      category: 'firewall',
      dashboardPath: 'firewall_malware::firewall_malware',
      description: 'Malware detection dashboard',
      features: ['Feature 1', 'Feature 2'],
      accessButtonText: 'View Dashboard',
      sinceIQVersion: '170',
      previewImageIcon: 'faShieldVirus',
    },
  ];

  const defaultPreloadedState = {
    firewallEnterpriseReporting: {
      dashboards: [],
      loading: false,
      loadError: null,
      iqVersion: null,
    },
    enterpriseReportingSupportInfo: {
      telemetryStatus: {
        advancedReportingEnabled: true,
      },
      loading: false,
      loadError: null,
    },
    router: {
      currentParams: {},
      currentState: { name: 'firewall.enterpriseReporting' },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    // Mock router state
    mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        if (stateName === 'firewall.enterpriseReporting') {
          return '#/firewall/enterprise-reporting';
        }
        if (stateName === 'firewall.enterpriseReportingDashboard') {
          return '#/firewall/enterprise-reporting/dashboard';
        }
        return '#/mocked-href';
      }),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);

    // Mock axios calls
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, {
      dashboardMetadata: mockDashboards,
    });
    axiosMock.onGet(getIqVersion()).reply(200, { version: '1.170.0' });
    axiosMock.onGet(getTelemetryStatusUrl()).reply(200, {
      advancedReportingEnabled: true,
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
    axiosMock.reset();
  });

  const renderComponent = (preloadedState) => {
    return render(<EnterpriseReportingPage />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });
  };

  it('should render page title', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: 'Enterprise Reporting' })).toBeInTheDocument();
  });

  it('should display loading state initially', () => {
    const loadingState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        ...defaultPreloadedState.firewallEnterpriseReporting,
        loading: true,
      },
    };
    renderComponent(loadingState);

    // Multiple NxLoadWrapper instances exist (dashboards + support info), so use getAllByText
    const loadingElements = screen.getAllByText('Loading…');
    expect(loadingElements.length).toBeGreaterThan(0);
  });

  it('should fetch dashboards and IQ version on mount', async () => {
    renderComponent();

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBeGreaterThanOrEqual(1);
    });

    const dashboardRequest = axiosMock.history.get.find((req) => req.url.includes('enterpriseReporting/dashboards'));

    expect(dashboardRequest).toBeDefined();
  });

  it('should display dashboard cards when data is loaded', async () => {
    const loadedState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        dashboards: mockDashboards,
        loading: false,
        loadError: null,
        iqVersion: '1.170.0',
      },
    };
    renderComponent(loadedState);

    // Verify the card is rendered
    await waitFor(() => {
      expect(screen.getByText('Malware Insights')).toBeInTheDocument();
    });
    expect(screen.getByText('Malware detection dashboard')).toBeInTheDocument();
  });

  it('should display error message when loading fails', async () => {
    // Override axios mock to return error
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(500, { message: 'Server Error' });

    const errorState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        ...defaultPreloadedState.firewallEnterpriseReporting,
        loading: false,
        loadError: 'Failed to load dashboards',
      },
    };
    renderComponent(errorState);

    // The NxLoadWrapper should show the error and retry button
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });
  });

  it('should call retry handler when retry button is clicked', async () => {
    const user = userEvent.setup();

    // Override axios mock to return error (will cause retry button to appear)
    axiosMock.reset();
    axiosMock.onGet(getTelemetryStatusUrl()).reply(200, { advancedReportingEnabled: true });
    axiosMock.onGet(getIqVersion()).reply(200, { version: '1.170.0' });
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(500, { message: 'Server Error' });

    const errorState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        ...defaultPreloadedState.firewallEnterpriseReporting,
        loading: false,
        loadError: 'Failed to load dashboards',
      },
    };
    renderComponent(errorState);

    // Wait for error state to render with retry button
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });

    const retryButton = screen.getByRole('button', { name: /retry/i });

    // Now change mock to return success for retry
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, { dashboardMetadata: mockDashboards });

    const initialRequestCount = axiosMock.history.get.filter((req) =>
      req.url.includes('enterpriseReporting/dashboards')
    ).length;

    // Click retry should trigger loadDashboards
    await user.click(retryButton);

    await waitFor(() => {
      const newRequestCount = axiosMock.history.get.filter((req) => req.url.includes('enterpriseReporting/dashboards'))
        .length;
      expect(newRequestCount).toBeGreaterThan(initialRequestCount);
    });
  });

  it('should filter and display only firewall category dashboards', async () => {
    const mixedDashboards = [
      ...mockDashboards,
      {
        dashboardId: 'lifecycle-dashboard',
        title: 'Lifecycle Dashboard',
        category: 'enterprise', // Different category - should be filtered out
        dashboardPath: 'lifecycle::lifecycle',
        description: 'Lifecycle dashboard',
        features: ['Feature X'],
        accessButtonText: 'View',
        sinceIQVersion: '170',
        previewImageIcon: 'faChartLine',
      },
    ];

    const loadedState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        dashboards: mixedDashboards,
        loading: false,
        loadError: null,
        iqVersion: '1.170.0',
      },
    };
    renderComponent(loadedState);

    // Should show firewall dashboard
    await waitFor(() => {
      expect(screen.getByText('Malware Insights')).toBeInTheDocument();
    });

    // Should NOT show non-firewall dashboard (filtered by selectDashboards selector)
    expect(screen.queryByText('Lifecycle Dashboard')).not.toBeInTheDocument();
  });

  it('should render telemetry status indicator when available', async () => {
    const loadedState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        dashboards: mockDashboards,
        loading: false,
        loadError: null,
        iqVersion: '1.170.0',
      },
      enterpriseReportingSupportInfo: {
        telemetryStatus: { advancedReportingEnabled: true },
        loading: false,
        loadError: null,
      },
    };
    renderComponent(loadedState);

    // Should show "On" status when advancedReportingEnabled is true
    await waitFor(() => {
      const statusElement = screen.getByRole('status');
      expect(statusElement).toHaveTextContent(/Advanced Reporting:/i);
      expect(statusElement).toHaveTextContent(/On/i);
    });
  });

  it('should show "Off" status when telemetry is disabled', async () => {
    // Override axios mock to return disabled telemetry
    axiosMock.onGet(getTelemetryStatusUrl()).reply(200, {
      advancedReportingEnabled: false,
    });

    const loadedState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        dashboards: mockDashboards,
        loading: false,
        loadError: null,
        iqVersion: '1.170.0',
      },
      enterpriseReportingSupportInfo: {
        telemetryStatus: { advancedReportingEnabled: false },
        loading: false,
        loadError: null,
      },
    };
    renderComponent(loadedState);

    // Should show "Off" status when advancedReportingEnabled is false
    await waitFor(() => {
      const statusElement = screen.getByRole('status');
      expect(statusElement).toHaveTextContent(/Advanced Reporting:/i);
      expect(statusElement).toHaveTextContent(/Off/i);
    });
  });

  it('should render contact card for support', async () => {
    const loadedState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        dashboards: mockDashboards,
        loading: false,
        loadError: null,
        iqVersion: '1.170.0',
      },
    };
    renderComponent(loadedState);

    // Should render the contact cards
    await waitFor(() => {
      expect(screen.getByText('Contact Us')).toBeInTheDocument();
    });
    expect(screen.getByText('Schedule a Discussion')).toBeInTheDocument();
    expect(screen.getByText('Suggest an Improvement')).toBeInTheDocument();
    expect(screen.getByText('Receive Technical Support')).toBeInTheDocument();
  });

  it('should handle empty dashboard list gracefully', () => {
    const emptyState = {
      ...defaultPreloadedState,
      firewallEnterpriseReporting: {
        dashboards: [],
        loading: false,
        loadError: null,
        iqVersion: '1.170.0',
      },
    };
    const { container } = renderComponent(emptyState);

    // Should render without crashing
    expect(container).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Enterprise Reporting' })).toBeInTheDocument();

    // Should not render any dashboard cards
    expect(screen.queryByText('Malware Insights')).not.toBeInTheDocument();
  });
});
