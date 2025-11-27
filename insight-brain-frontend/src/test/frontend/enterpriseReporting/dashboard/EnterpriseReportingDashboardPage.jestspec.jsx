/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor, within } from 'TestRoot/SpecUtil';
import {
  getEnterpriseReportingGenerateEmbedTokensUrl,
  getEnterpriseReportingAcquireEmbedSessionUrl,
  getEnterpriseReportingBaseUrl,
  getEnterpriseReportingDashboardsUrl,
  getProductFeaturesUrl,
} from 'MainRoot/util/CLMLocation';
import EnterpriseReportingDashboardPage from 'MainRoot/enterpriseReporting/dashboard/EnterpriseReportingDashboardPage';
import { actions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import { screen } from '@testing-library/dom';
import userEvent from '@testing-library/user-event';
import { LookerEmbedSDK } from '@looker/embed-sdk';
import { mockData } from '../enterpriseReportingMockData';

describe('EnterpriseReportingDashboardPage', () => {
  let axiosMock, renderPage;

  const mockLookerBaseUrl = 'https://sonatypeinstance.looker.com';
  const mockDashboards = mockData;

  renderPage = () =>
    render(<EnterpriseReportingDashboardPage />, {
      preloadedState: {
        enterpriseReportingDashboard: {
          selectedDashboard: {
            dashboardId: 'rolling-recap',
            dashboardPath: 'dashboards/rolling_recap::rolling_recap',
            category: 'dataInsight',
          },
          dashboardTabs: [],
        },
      },
    });

  beforeAll(() => {
    global.CLM_SERVER_VERSION = '1.188.0-SNAPSHOT';
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    const mockRouterContext = {
      href: jest.fn().mockImplementation((stateName, stateParam) => {
        if (stateName === 'enterpriseReportingDashboardGroup') {
          return `/enterpriseReportingDashboard/${stateParam?.groupId}/${stateParam?.id}`;
        } else {
          return `/${stateName}/${stateParam?.id}`;
        }
      }),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue(mockRouterContext);
  });

  beforeEach(() => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-enterprise-reporting']);
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, mockDashboards);
    axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(200, mockLookerBaseUrl);
    axiosMock.onGet(getEnterpriseReportingAcquireEmbedSessionUrl('rolling-recap')).reply(200, {
      authentication_token: 'authentication_token',
      authentication_token_ttl: 600,
      api_token: 'api_token',
      api_token_ttl: 600,
      navigation_token: 'navigation_token',
      navigation_token_ttl: 600,
      session_reference_token: 'session_reference_token',
      session_reference_token_ttl: 1800,
    });
    axiosMock.onPut(getEnterpriseReportingGenerateEmbedTokensUrl()).reply(200, {
      api_token: 'api_token',
      api_token_ttl: 600,
      navigation_token: 'navigation_token',
      navigation_token_ttl: 600,
      session_reference_token: 'session_reference_token',
      session_reference_token_ttl: 1800,
    });

    const chain = {
      appendTo: jest.fn().mockReturnThis(),
      withParams: jest.fn().mockReturnThis(),
      withDynamicIFrameHeight: jest.fn().mockReturnThis(),
      build: jest.fn().mockReturnThis(),
      connect: jest.fn(),
    };
    jest.spyOn(LookerEmbedSDK, 'createDashboardWithId').mockReturnValue(chain);
  });

  it('shows loading before iframe is loaded and the renders the iframe', async () => {
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ id: 'rolling-recap' });
    renderPage();

    expect(screen.getAllByRole('status').length).toBe(2); //one for iframe, one for telemetry status

    await waitFor(() => {
      expect(screen.getByRole('enterprise-reporting-dashboard')).toBeInTheDocument();
      expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });
  });

  it('shows error when there are API errors', async () => {
    axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(401, 'Unauthorized');

    renderPage();

    expect(screen.getAllByRole('status').length).toBe(2);
    const errorMessage = await screen.findByText('An error occurred loading data. Unauthorized');
    expect(errorMessage).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows error when there license does not have integrated-enterprise-reporting', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);

    renderPage();

    expect(screen.getAllByRole('status').length).toBe(2);
    const errorMessage = await screen.findByText(
      'An error occurred loading data. Enterprise Reporting feature not supported'
    );
    await waitFor(() => {
      expect(errorMessage).toBeVisible();
    });
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('calls setSelectDashboard according to the dashboardId from the url', async () => {
    const selectedDashboardSpy = jest.spyOn(actions, 'setSelectedDashboard');
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ id: 'rolling-recap' });

    renderPage();
    await waitFor(() => {
      expect(selectedDashboardSpy).toHaveBeenCalledWith(mockData.dashboardMetadata[5]);
    });
  });

  describe('Dashboard Navigation Links', () => {
    let hrefSpy;

    const preloadedState = {
      enterpriseReportingDashboard: {
        dashboardsData: mockDashboards,
        loading: false,
        loadError: null,
        selectedDashboard: { dashboardId: 'success-metrics', dashboardPath: 'success-metrics::success_metrics' },
        dashboardTabs: [],
      },
    };

    beforeEach(() => {
      hrefSpy = jest.fn('href').mockImplementation((stateName, stateParam) => {
        if (stateName === 'enterpriseReportingDashboardGroup') {
          return `/enterpriseReportingDashboard/${stateParam.groupId}/${stateParam.id}`;
        } else {
          return `/${stateName}/${stateParam.id}`;
        }
      });
      const routerContextMock = { href: hrefSpy, get: jest.fn(), includes: jest.fn() };
      jest.spyOn(routerContext, 'useRouterState').mockReturnValue(routerContextMock);
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        id: 'rolling-recap',
      });
    });

    const renderComponent = () => render(<EnterpriseReportingDashboardPage />, { preloadedState });

    it('renders a navigation link for dashboard when not on its page ', async () => {
      renderComponent();
      const nav = screen.getByRole('navigation');

      const linkItemTests = (link, title, id, isGroup, groupId) => {
        expect(link).toHaveTextContent(title);
        if (isGroup) {
          expect(hrefSpy).toHaveBeenCalledWith('enterpriseReportingDashboardGroup', {
            id: id,
            groupId: groupId,
          });
        } else {
          expect(link).toHaveAttribute('href', `/enterpriseReportingDashboard/${id}`);
          expect(hrefSpy).toHaveBeenCalledWith('enterpriseReportingDashboard', {
            id: id,
          });
        }
      };

      const linkItems = await within(nav).findAllByRole('link');
      expect(linkItems.length).toBe(4);
      linkItemTests(linkItems[0], 'Success Metrics', 'success-metrics', false, null);
      linkItemTests(linkItems[1], 'Security Risk', 'security_risk_breakdown', true, 'security-risk');
      linkItemTests(linkItems[2], 'AI Group', 'ai-models', true, 'ai-group');
      linkItemTests(linkItems[3], 'Component End-of-Life', 'component-eol', false);
    });

    it("renders text instead of navigation link when on dashboard's page", async () => {
      renderComponent();
      const nav = screen.getByRole('navigation');

      //2 instances - the H1 title & span in navigation bar
      const rollingRecap = screen.getAllByText('Rolling Recap Dashboard')[0];
      expect(rollingRecap).toBeInTheDocument();
      expect(rollingRecap.tagName).toBe('SPAN');

      const rollingRecapLink = within(nav).queryByRole('link', { name: 'Rolling Recap Dashboard' });
      expect(rollingRecapLink).not.toBeInTheDocument();
    });

    it('separates dashboards into Enterprise and Data Insights categories', async () => {
      renderComponent();
      const nav = screen.getByRole('navigation');
      expect(nav).toBeInTheDocument();

      const enterprise = within(nav).getByRole('heading', { name: 'Enterprise Dashboards:' });
      const dataInsights = within(nav).getByRole('heading', { name: 'Data Insights:' });
      expect(enterprise).toBeInTheDocument();
      expect(dataInsights).toBeInTheDocument();

      const enterpriseList = await within(enterprise.nextElementSibling).findAllByRole('link');
      expect(enterpriseList.length).toBe(2);
      expect(enterpriseList[0]).toHaveTextContent('Success Metrics');

      const dataInsightsList = await within(dataInsights.nextElementSibling).findAllByRole('link');
      expect(dataInsightsList.length).toBe(2);
      expect(dataInsightsList[0]).toHaveTextContent('AI Group');
    });

    it('if a group dashboard exists, it is rendered instead of dashboards with matching groupId', async () => {
      renderComponent();

      expect(await screen.findByRole('link', { name: 'Security Risk' })).toBeInTheDocument();
      expect(screen.queryByRole('link', { name: 'Security Risk Trends' })).not.toBeInTheDocument();
      expect(screen.queryByRole('link', { name: 'Security Risk Breakdown' })).not.toBeInTheDocument();
    });

    it('renders a link if it has a groupId defined, but no matching group dashboard', async () => {
      const dashboardsData = {
        dashboardMetadata: [
          { ...mockData.dashboardMetadata[0], groupId: 'not-matching' },
          mockData.dashboardMetadata[5],
        ],
        dashboardGroupMetadata: [],
      };
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, dashboardsData);
      renderComponent();
      expect(await screen.findByRole('link', { name: 'Success Metrics' })).toBeInTheDocument();
    });

    describe('disabled', () => {
      describe('dashboard link', () => {
        beforeEach(() => {
          global.CLM_SERVER_VERSION = '1.182.0-SNAPSHOT';
          const updatedDashboardData = [
            {
              dashboardId: 'success-metrics',
              category: 'enterprise',
              description: 'Explore your vulnerability discovery and remediation patterns',
              features: ['Follow remediation activity', 'Analyze violation pattners', 'Explore apps an components'],
              accessButtonText: 'View Success Metrics',
              previewImageIcon: 'faCalendarCheck',
              priority: -20,
              sinceIQVersion: '184',
              spotlight: true,
              title: 'Success Metrics',
            },
            {
              dashboardId: 'rolling-recap',
              category: 'dataInsight',
              dashboardPath: 'dashboards/rolling_recap::rolling_recap',
              description: 'Unlock trends by comparing your usage with the rest of the industry, over the past year.',
              features: ['Analyze performance', 'Compare scans', "View experts' ratings"],
              accessButtonText: 'View Rolling Recap',
              previewImageIcon: 'faCalendarCheck',
              priorityOrder: 500,
              sinceIQVersion: '',
              spotlight: false,
              title: 'Rolling Recap Dashboard',
            },
          ];
          axiosMock
            .onGet(getEnterpriseReportingDashboardsUrl())
            .reply(200, { dashboardMetadata: updatedDashboardData, dashboardGroupMetadata: [] });
        });

        it('disables links when sinceIQVersion is greater than serverVersion', async () => {
          renderPage();

          const successMetricsLink = await screen.findByRole('link', { name: 'Success Metrics' });
          expect(successMetricsLink).toHaveTextContent('Success Metrics');
          expect(successMetricsLink).toHaveAttribute('aria-disabled', 'true');
        });

        it('renders a tooltip on hover', async () => {
          const user = userEvent.setup();
          renderPage();

          const successMetricsLink = await screen.findByRole('link', { name: 'Success Metrics' });
          expect(successMetricsLink).toHaveAttribute('aria-disabled', 'true');

          await user.hover(successMetricsLink);
          const tooltip = await screen.findByRole('tooltip');
          expect(tooltip).toHaveTextContent('Upgrade to IQ version 184 to access this insight');
        });
      });

      describe('group link', () => {
        const disabledDashboard = {
          dashboardMetadata: [
            { ...mockData.dashboardMetadata[1], sinceIQVersion: '199' },
            { ...mockData.dashboardMetadata[2], sinceIQVersion: '200' },
          ],
          dashboardGroupMetadata: [{ ...mockData.dashboardGroupMetadata[0], sinceIQVersion: '' }],
        };
        const enabledDashboard = {
          dashboardMetadata: [
            { ...mockData.dashboardMetadata[1], sinceIQVersion: '' },
            { ...mockData.dashboardMetadata[2], sinceIQVersion: '200' },
          ],
          dashboardGroupMetadata: [{ ...mockData.dashboardGroupMetadata[0], sinceIQVersion: '' }],
        };

        beforeEach(() => {
          global.CLM_SERVER_VERSION = '1.188.0-SNAPSHOT';
        });

        it('does not disable the group dashboard link if one its children is not disabled', async () => {
          axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, enabledDashboard);
          renderPage();

          const securityRiskLink = await screen.findByRole('link', { name: 'Security Risk' });
          expect(securityRiskLink).not.toHaveAttribute('aria-disabled', 'true');
        });

        it('disabled the group dashboard link if both its children are disabled', async () => {
          axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, disabledDashboard);
          renderPage();

          const securityRiskLink = await screen.findByRole('link', { name: 'Security Risk' });
          expect(securityRiskLink).toHaveAttribute('aria-disabled', 'true');
        });

        it('renders a tooltip with newest version for upgrade', async () => {
          const user = userEvent.setup();
          axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, disabledDashboard);
          renderPage();

          const securityRiskLink = await screen.findByRole('link', { name: 'Security Risk' });
          expect(securityRiskLink).toHaveAttribute('aria-disabled', 'true');

          await user.hover(securityRiskLink);
          const tooltip = await screen.findByRole('tooltip');
          expect(tooltip).toHaveTextContent('Upgrade to IQ version 200 to access this insight');
        });
      });
    });
  });

  describe('tabbing', () => {
    const preloadedState = {
      enterpriseReportingDashboard: {
        dashboardsData: mockData,
        loading: false,
        loadError: null,
        selectedDashboard: {
          dashboardId: 'security_risk_breakdown',
          dashboardPath: 'security_risk_breakdown::security_risk_breakdown',
        },
        dashboardTabs: [],
        activeDashboardTab: 0,
      },
    };
    let currentParams = { groupId: 'security-risk', id: 'security_risk_breakdown' };

    beforeEach(() => {
      global.CLM_SERVER_VERSION = '1.192.0-SNAPSHOT';
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(currentParams);
    });

    const renderGroupPage = () => render(<EnterpriseReportingDashboardPage />, { preloadedState });

    it('renders tabs and selects tab based on id prop in router params', async () => {
      renderGroupPage();

      const tabs = await screen.findAllByRole('tab');
      expect(tabs.length).toBe(2);
      expect(tabs[0]).toHaveAttribute('aria-selected', 'false');
      expect(tabs[0]).toHaveTextContent('Security Trends');
      expect(tabs[1]).toHaveAttribute('aria-selected', 'true');
      expect(tabs[1]).toHaveTextContent('Security Breakdown');
    });

    it('calls setSelectedDashboard when the active tab dashboard meets sinceIQVersion requirements', async () => {
      const selectedDashboardSpy = jest.spyOn(actions, 'setSelectedDashboard');
      const activeDashboardTabSpy = jest.spyOn(actions, 'setActiveDashboardTab');
      renderGroupPage();

      await waitFor(() => {
        expect(selectedDashboardSpy).toHaveBeenCalledWith(mockData.dashboardMetadata[2]);
        expect(activeDashboardTabSpy).toHaveBeenCalledWith(1);
      });
    });

    it('renders an error alert and does not call setSelectedDashboard when active tab dashboard does not meet sinceIQVersion requirement', async () => {
      const selectedDashboardSpy = jest.spyOn(actions, 'setSelectedDashboard');
      const activeDashboardTabSpy = jest.spyOn(actions, 'setActiveDashboardTab');
      currentParams = { groupId: 'security-risk', id: 'security_risk_trends' };
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(currentParams);
      renderGroupPage();

      await waitFor(() => {
        expect(activeDashboardTabSpy).toHaveBeenCalledWith(0);
        const alert = screen.getAllByRole('alert'); //one for dashboard, one for enterpriseReportingSupportInfo
        expect(alert[0]).toHaveTextContent(
          /You're using a version of Lifecycle that does not support this dashboard. To unlock this feature, update to version 199 or later/i
        );
        expect(selectedDashboardSpy).not.toHaveBeenCalled();
      });
    });
  });
});
