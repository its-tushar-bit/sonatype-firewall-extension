/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { filter, propEq } from 'ramda';
import { axiosMockAdapter, render, waitFor, within } from 'TestRoot/SpecUtil';
import EnterpriseReportingLandingPage from 'MainRoot/enterpriseReporting/EnterpriseReportingLandingPage';
import {
  getEnterpriseReportingDashboardsUrl,
  getProductFeaturesUrl,
  getIqVersion,
  getTelemetryStatusUrl,
} from 'MainRoot/util/CLMLocation';
import { screen } from '@testing-library/dom';
import { actions as dashboardActions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import { mockData } from './enterpriseReportingMockData';
import { initialState } from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSlice';

describe('EnterpriseReportingLandingPage', () => {
  let axiosMock, renderPage;

  const preloadedState = {
    enterpriseReportingLandingPage: initialState,
  };

  const iqVersionResponse = {
    version: '1.188.0-SNAPSHOT',
  };
  const telemetryData = {
    telemetryId: '12345',
    clusterId: '12345-678',
    advancedReportingEnabled: true,
    enterpriseReportingFeatureExists: true,
    userApplicationCount: 50,
    totalApplicationCount: 100,
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, mockData);
    axiosMock.onGet(getIqVersion()).reply(200, iqVersionResponse);
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-enterprise-reporting']);
    axiosMock.onGet(getTelemetryStatusUrl()).reply(200, telemetryData);
    renderPage = () => render(<EnterpriseReportingLandingPage />, { preloadedState });
  });

  it('shows loading before dashboards data is loaded', async () => {
    renderPage();

    const loadingSpinners = screen.getAllByText('Loading…');
    expect(loadingSpinners.length).toBe(5); //1 loading spinner per dashboard category (Rapid Response Reports, Enterprise Dashboards, Data Insights, Partner Solutions) + telemetryInfo
    expect(screen.getByRole('heading', { name: 'Enterprise Dashboards' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Data Insights' })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Contact Us' })).toBeInTheDocument();
    expect(screen.queryByText('Loading')).not.toBeInTheDocument();
  });

  it('renders a page description and NxInfoAlert', async () => {
    renderPage();

    const description = 'If you have disabled Advanced Reporting, application names will be obfuscated';
    expect(screen.queryByText(description, { exact: false })).toBeInTheDocument();

    const alertText =
      'Dashboards and Insights may appear incomplete and/or nonfunctional if there is insufficient data.';
    expect(screen.queryByText(alertText)).toBeInTheDocument();
  });

  it('shows dashboard data', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    });

    const dashboardGroupIds = mockData.dashboardGroupMetadata.map((dash) => dash.groupId);
    mockData.dashboardMetadata.forEach((dashboard) => {
      if (dashboardGroupIds.includes(dashboard.groupId)) {
        expect(screen.queryByRole('heading', { name: dashboard.title })).not.toBeInTheDocument();
        expect(screen.queryByText(dashboard.description)).not.toBeInTheDocument();
      } else {
        expect(screen.getByRole('heading', { name: dashboard.title })).toBeInTheDocument();
        expect(screen.queryByText(dashboard.description)).toBeInTheDocument();
        dashboard.features.forEach((feature) => {
          expect(screen.queryByText(feature)).toBeInTheDocument();
        });
        expect(screen.getByRole('button', { name: dashboard.accessButtonText })).toBeInTheDocument();
        if (dashboard.spotlight) {
          expect(screen.getAllByText('NEW').length).toBeGreaterThan(0);
        }
      }
    });
  });

  it('renders the group dashboard cards', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    });
    mockData.dashboardGroupMetadata.forEach((dash) => {
      expect(screen.getByRole('heading', { name: dash.title })).toBeInTheDocument();
      expect(screen.queryByText(dash.description)).toBeInTheDocument();
      dash.features.forEach((feature) => {
        //bolded words are surrounded by *, so these need to be removed from the string first
        const cleanedFeature = feature.replace(/\*/g, '').toString();
        const listElement = screen.getByText(
          (_, element) => element.tagName.toLowerCase() === 'span' && element.textContent === cleanedFeature
        );
        expect(listElement).toBeInTheDocument();
      });

      const getGroupedDashboards = filter(propEq('groupId', dash.groupId), mockData.dashboardMetadata);
      expect(screen.getByRole('button', { name: getGroupedDashboards[0].accessButtonText })).toBeInTheDocument();
    });
  });

  it('splits dashboards into assigned categories', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    });
    const enterpriseDashboardGroup = screen.getByRole('heading', { name: 'Enterprise Dashboards' }).nextElementSibling;
    const enterpriseDashboards = within(enterpriseDashboardGroup).getAllByRole('enterprise-reporting-dashboard-card');
    expect(enterpriseDashboards.length).toBe(2);

    const dataInsightsGroup = screen.getByRole('heading', { name: 'Data Insights' }).nextElementSibling;
    const insightsDashboards = within(dataInsightsGroup).getAllByRole('enterprise-reporting-dashboard-card');
    expect(insightsDashboards.length).toBe(3);
  });

  it('shows error when there are API errors', async () => {
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(401, 'Unauthorized');

    renderPage();
    await waitFor(() => {
      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    });
    const errorMessages = screen.getAllByText('An error occurred loading data. Unauthorized');
    expect(errorMessages.length).toBe(4); //error message for each dashboard category (Rapid Response Reports, Enterprise Dashboards, Data Insights, Partner Solutions)
  });

  it('shows error when there license does not have integrated-enterprise-reporting', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);

    renderPage();
    await waitFor(() => {
      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    });

    const errorMessages = screen.getAllByText(
      'An error occurred loading data. Enterprise Reporting feature not supported'
    );
    expect(errorMessages.length).toBe(4); //error message for each dashboard category (Rapid Response Reports, Enterprise Dashboards, Data Insights, Partner Solutions)
  });

  it('resets dashboard state on loading', () => {
    const resetSpy = jest.spyOn(dashboardActions, 'reset');
    renderPage();

    expect(resetSpy).toHaveBeenCalled();
  });

  it('renders three contact cards', () => {
    renderPage();

    const contactCards = screen.getByRole('heading', { name: 'Contact Us' }).nextElementSibling.children;
    expect(contactCards.length).toBe(3);

    expect(within(contactCards[0]).getByRole('heading')).toHaveTextContent('Schedule a Discussion');
    expect(within(contactCards[0]).getByRole('link')).toHaveAttribute('href', 'mailto:data-insights-pm@sonatype.com');
    expect(within(contactCards[1]).getByRole('heading')).toHaveTextContent('Suggest an Improvement');
    expect(within(contactCards[1]).getByRole('link')).toHaveAttribute(
      'href',
      'http://links.sonatype.com/products/nxiq/feedback/data-insights-ideas'
    );
    expect(within(contactCards[2]).getByRole('heading')).toHaveTextContent('Receive Technical Support');
    expect(within(contactCards[2]).getByRole('link')).toHaveAttribute(
      'href',
      'http://links.sonatype.com/products/nexus/pro/support'
    );
  });

  describe('status indicator', () => {
    it('renders a positive indicator with appropriate text when advancedReporting is enabled', async () => {
      renderPage();
      await waitFor(() => {
        const indicator = screen.getAllByRole('status')[0];
        expect(indicator).toBeInTheDocument();
        expect(indicator).toHaveTextContent('Advanced Reporting: On');
      });
    });

    it('renders a default indicator with appropriate text when advancedReporting is disabled', async () => {
      const telemetryData = {
        telemetryId: '12345',
        clusterId: '12345-678',
        advancedReportingEnabled: false,
        enterpriseReportingFeatureExists: true,
        userApplicationCount: 50,
        totalApplicationCount: 100,
      };
      axiosMock.onGet(getTelemetryStatusUrl()).reply(200, telemetryData);

      renderPage();

      await waitFor(() => {
        const indicator = screen.getAllByRole('status')[0];
        expect(indicator).toHaveTextContent('Advanced Reporting: Off');
      });
    });

    it('renders a text link alongside the status indicator', async () => {
      renderPage();
      const textLink = await screen.findByRole('link', { name: "What's this?" });
      expect(textLink).toHaveAttribute(
        'href',
        'https://links.sonatype.com/products/nxiq/doc/data-insights-advanced-reporting'
      );
    });

    it('does not render the status indicator if error in api call', async () => {
      axiosMock.onGet(getTelemetryStatusUrl()).reply(400, 'Bad Request');
      renderPage();

      expect(screen.getAllByText('Loading…').length).toBe(5);
      await waitFor(() => {
        expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });
    });
  });
});
