/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor, within } from 'TestRoot/SpecUtil';
import EnterpriseReportingLandingPage from 'MainRoot/enterpriseReporting/EnterpriseReportingLandingPage';
import { getEnterpriseReportingDashboardsUrl, getProductFeaturesUrl, getIqVersion } from 'MainRoot/util/CLMLocation';
import { screen } from '@testing-library/dom';
import { actions as dashboardActions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';

describe('EnterpriseReportingLandingPage', () => {
  let axiosMock, renderPage;

  const mockDashboardsData = {
    dashboardMetadata: [
      {
        dashboardId: 'rolling-recap',
        title: 'Rolling Recap Dashboard',
        category: 'dataInsight',
        description: 'Unlock trends by comparing your usage with the rest of the industry, over the past year.',
        features: ['Analyze app performance', 'Compare initial & latest scans', 'View security experts’ rating'],
        accessButtonText: 'View Rolling Recap',
        previewImage: '',
        previewImageIcon: 'faThumbsUp',
        priority: 1,
        spotlight: false,
      },
      {
        dashboardId: 'success-metrics',
        category: 'enterprise',
        title: 'Success Metrics',
        description: 'Review your applications and vulnerabilities in this foundational dashboard',
        features: ['Discover high-level trends', 'Explore team performance', 'See your risk ratio'],
        accessButtonText: 'View Success Metrics',
        previewImage: '',
        previewImageIcon: 'faThumbsUp',
        priorityOrder: 1,
        priority: 2,
        spotlight: true,
      },
      {
        dashboardId: 'component-eol',
        category: 'dataInsight',
        title: 'Component EOL: Retiring Old Code',
        description: 'Learn the specifics about the components that have the status of End of Life (EOL)',
        features: ['Note ratings by version', 'Notice apps using versions', 'Sort cumulative lists by type'],
        accessButtonText: 'View Component EOL',
        previewImage: '',
        previewImageIcon: 'faThumbsUp',
        priority: 3,
        spotlight: false,
      },
    ],
  };
  const iqVersionResponse = {
    version: '1.188.0-SNAPSHOT',
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, mockDashboardsData);
    axiosMock.onGet(getIqVersion()).reply(200, iqVersionResponse);
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-enterprise-reporting']);
    renderPage = () => render(<EnterpriseReportingLandingPage />);
  });

  it('shows loading before dashboards data is loaded', async () => {
    renderPage();

    const loadingSpinners = screen.getAllByRole('status');
    expect(loadingSpinners.length).toBe(2); //1 loading spinner per dashboard category
    expect(screen.getByRole('heading', { name: 'Enterprise Dashboards' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Data Insights' })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Contact Us' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
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

    expect(screen.getAllByRole('status').length).toBe(2);
    expect(await screen.findByRole('heading', { name: 'Contact Us' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    mockDashboardsData.dashboardMetadata.forEach((dashboard) => {
      expect(screen.getByRole('heading', { name: dashboard.title })).toBeInTheDocument();
      expect(screen.queryByText(dashboard.description)).toBeInTheDocument();
      dashboard.features.forEach((feature) => {
        expect(screen.queryByText(feature)).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: dashboard.accessButtonText })).toBeInTheDocument();
      if (dashboard.spotlight) {
        expect(screen.queryByText('NEW')).toBeInTheDocument();
      }
    });
  });

  it('splits dashboards into assigned categories', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });
    const enterpriseDashboards = screen.getByRole('heading', { name: 'Enterprise Dashboards' }).nextElementSibling;
    const enterpriseChildren = within(enterpriseDashboards).getAllByRole('enterprise-reporting-dashboard-card');
    expect(enterpriseChildren.length).toBe(1);

    const dataInsights = screen.getByRole('heading', { name: 'Data Insights' }).nextElementSibling;
    const insightsChildren = within(dataInsights).getAllByRole('enterprise-reporting-dashboard-card');
    expect(insightsChildren.length).toBe(2);
  });

  it('shows error when there are API errors', async () => {
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(401, 'Unauthorized');

    renderPage();

    expect(screen.getAllByRole('status').length).toBe(2);
    expect(await screen.findByRole('heading', { name: 'Data Insights' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    const errorMessages = screen.getAllByText('An error occurred loading data. Unauthorized');
    expect(errorMessages.length).toBe(2); //error message for each dashboard category
  });

  it('shows error when there license does not have integrated-enterprise-reporting', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);

    renderPage();

    expect(screen.getAllByRole('status').length).toBe(2);
    expect(await screen.findByRole('heading', { name: 'Data Insights' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    const errorMessages = screen.getAllByText(
      'An error occurred loading data. Enterprise Reporting feature not supported'
    );
    expect(errorMessages.length).toBe(2); //error message for each dashboard category
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
});
