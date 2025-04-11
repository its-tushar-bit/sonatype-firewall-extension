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
import { initialState } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import EnterpriseReportingDashboardPage from 'MainRoot/enterpriseReporting/dashboard/EnterpriseReportingDashboardPage';
import { actions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import { screen } from '@testing-library/dom';
import userEvent from '@testing-library/user-event';
import { LookerEmbedSDK } from '@looker/embed-sdk';

describe('EnterpriseReportingDashboardPage', () => {
  let axiosMock, renderPage;

  const mockLookerBaseUrl = 'https://sonatypeinstance.looker.com';
  const dashboardMetadata = [
    {
      dashboardId: 'rolling-recap',
      title: 'Rolling Recap Dashboard',
      category: 'dataInsight',
      description: 'Unlock trends by comparing your usage with the rest of the industry, over the past year.',
      features: ['Analyze app performance', 'Compare initial & latest scans', 'View security experts’ rating'],
      accessButtonText: 'View Rolling Recap',
      previewImage: '',
      previewImageIcon: '',
      priority: 1,
      spotlight: false,
    },
    {
      dashboardId: 'ai-consumption',
      title: 'Machine Learning AI',
      category: 'dataInsight',
      description: 'Observe Machine Learning (ML) components and integrations within your software.',
      features: ['Sort components by AI type', 'Monitor AI within your apps', 'Isolate exact locations of AI'],
      accessButtonText: 'View ML/AI',
      previewImage: '',
      previewImageIcon: '',
      priority: 2,
      spotlight: true,
    },
    {
      dashboardId: 'success-metrics',
      title: 'Success Metrics',
      category: 'enterprise',
      description: 'Explore your vulnerability discovery and remediation patterns',
      features: ['Follow remediation activity', 'Analyze violation pattners', 'Explore apps an components'],
      accessButtonText: 'View Success Metrics',
      previewImage: '',
      previewImageIcon: '',
      priority: -20,
      spotlight: true,
    },
  ];

  renderPage = () =>
    render(<EnterpriseReportingDashboardPage />, {
      preloadedState: {
        enterpriseReportingDashboard: {
          selectedDashboard: { dashboardId: 'rolling-recap', dashboardPath: 'dashboardPath' },
        },
      },
    });

  beforeAll(() => {
    window.clmServerVersion = '1.188.0-SNAPSHOT';
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-enterprise-reporting']);
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, { dashboardMetadata: dashboardMetadata });
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
    jest.spyOn(LookerEmbedSDK, 'createDashboardWithId').mockReturnValue({
      appendTo: jest.fn().mockReturnValue({ build: jest.fn().mockReturnValue({ connect: jest.fn() }) }),
    });
  });

  it('shows loading before iframe is loaded and the renders the iframe', async () => {
    renderPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(await screen.findByRole('enterprise-reporting-dashboard')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });
  });

  it('shows error when there are API errors', async () => {
    axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(401, 'Unauthorized');

    renderPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
    const errorMessage = await screen.findByText('An error occurred loading data. Unauthorized');
    expect(errorMessage).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows error when there license does not have integrated-enterprise-reporting', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);

    renderPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
    const errorMessage = await screen.findByText(
      'An error occurred loading data. Enterprise Reporting feature not supported'
    );
    expect(errorMessage).toBeVisible();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('calls setSelectDashboard according to the dashboardId from the url', async () => {
    const selectedDashboardSpy = jest.spyOn(actions, 'setSelectedDashboard');
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ id: 'ai-consumption' });

    render(<EnterpriseReportingDashboardPage />, { initialState });
    await waitFor(() => {
      expect(selectedDashboardSpy).toHaveBeenCalledWith(dashboardMetadata[1]);
    });
  });

  describe('Dashboard Navigation Links', () => {
    let hrefSpy, routerContextMock;

    const loadedState = {
      dashboardsData: dashboardMetadata,
      loading: false,
      loadError: null,
      selectedDashboard: { dashboardId: 'rolling-recap', dashboardPath: 'rolling_recap::rolling_recap' },
    };

    beforeEach(() => {
      hrefSpy = jest.fn('href').mockImplementation((stateName, stateParam) => `/${stateName}/${stateParam.id}`);
      routerContextMock = { href: hrefSpy };
      jest.spyOn(routerContext, 'useRouterState').mockReturnValue(routerContextMock);
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        id: 'rolling-recap',
      });
    });

    const renderComponent = (props) => render(<EnterpriseReportingDashboardPage {...props} />);

    it('renders a navigation link for dashboard when not on its page ', async () => {
      renderComponent(loadedState);
      const nav = screen.getByRole('navigation');

      const linkItemTests = (link, title, id) => {
        expect(link).toHaveTextContent(title);
        expect(link).toHaveAttribute('href', `/enterpriseReportingDashboard/${id}`);
        expect(hrefSpy).toHaveBeenCalledWith('enterpriseReportingDashboard', {
          id: id,
        });
      };

      const linkItems = await within(nav).findAllByRole('link');
      expect(linkItems.length).toBe(2);
      linkItemTests(linkItems[0], 'Success Metrics', 'success-metrics');
      linkItemTests(linkItems[1], 'Machine Learning AI', 'ai-consumption');
    });

    it("renders text instead of navigation link when on dashboard's page", async () => {
      renderComponent(loadedState);
      const nav = screen.getByRole('navigation');

      const rollingRecap = await screen.findByText('Rolling Recap Dashboard');
      expect(rollingRecap).toBeInTheDocument();
      expect(rollingRecap.tagName).toBe('SPAN');

      const rollingRecapLink = within(nav).queryByRole('link', { name: 'Rolling Recap Dashboard' });
      expect(rollingRecapLink).not.toBeInTheDocument();
    });

    it('separates dashboards into Enterprise and Data Insights categories', async () => {
      renderComponent(loadedState);
      const nav = screen.getByRole('navigation');
      expect(nav).toBeInTheDocument();

      const enterprise = within(nav).getByRole('heading', { name: 'Enterprise Dashboards:' });
      const dataInsights = within(nav).getByRole('heading', { name: 'Data Insights:' });
      expect(enterprise).toBeInTheDocument();
      expect(dataInsights).toBeInTheDocument();

      const enterpriseList = await within(enterprise.nextElementSibling).findAllByRole('link');
      expect(enterpriseList.length).toBe(1);
      expect(enterpriseList[0]).toHaveTextContent('Success Metrics');

      const dataInsightsList = await within(dataInsights.nextElementSibling).findAllByRole('link');
      expect(dataInsightsList.length).toBe(1);
      expect(dataInsightsList[0]).toHaveTextContent('Machine Learning AI');
    });

    describe('disabled', () => {
      beforeEach(() => {
        window.clmServerVersion = '1.182.0-SNAPSHOT';
        const updatedDashbaord = {
          dashboardId: 'success-metrics',
          title: 'Success Metrics',
          category: 'enterprise',
          description: 'Explore your vulnerability discovery and remediation patterns',
          features: ['Follow remediation activity', 'Analyze violation pattners', 'Explore apps an components'],
          accessButtonText: 'View Success Metrics',
          previewImage: '',
          previewImageIcon: '',
          priority: -20,
          spotlight: true,
          sinceIQVersion: '184',
        };
        axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, { dashboardMetadata: [updatedDashbaord] });
      });

      it('disables links when sinceIQVersion is greater than serverVersion', async () => {
        renderPage();

        const successMetricsLink = await screen.findByRole('link');
        expect(successMetricsLink).toHaveTextContent('Success Metrics');
        expect(successMetricsLink).toHaveAttribute('aria-disabled', 'true');
      });

      it('renders a tooltip on hover', async () => {
        const user = userEvent.setup();
        renderPage();

        const successMetricsLink = await screen.findByRole('link');
        expect(successMetricsLink).toHaveAttribute('aria-disabled', 'true');

        await user.hover(successMetricsLink);
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Upgrade to IQ version 184 to access this insight');
      });
    });
  });
});
