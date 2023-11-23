/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import EnterpriseReportingLandingPage from 'MainRoot/enterpriseReporting/EnterpriseReportingLandingPage';
import { getEnterpriseReportingDashboardsUrl } from 'MainRoot/util/CLMLocation';
import { screen } from '@testing-library/dom';

describe('EnterpriseReportingDashboardPage', () => {
  let axiosMock, renderPage;

  const mockDashboardsData = {
    dashboardMetadata: [
      {
        dashboardId: 'rolling-recap',
        title: 'Rolling Recap Dashboard: Past 365 Days',
        description: 'Unlock trends by comparing your usage with the rest of the industry, over the past year.',
        features: ['Analyze app performance', 'Compare initial & latest scans', 'View security experts’ rating'],
        accessButtonText: 'View Rolling Recap',
        previewImage: '',
        priority: 1,
        spotlight: false,
      },
      {
        dashboardId: 'ai-consumption',
        title: 'ML/AI: Apps Using Machine Learning',
        description: 'Observe Machine Learning (ML) components and integrations within your software.',
        features: ['Sort components by AI type', 'Monitor AI within your apps', 'Isolate exact locations of AI'],
        accessButtonText: 'View ML/AI',
        previewImage: '',
        priority: 2,
        spotlight: true,
      },
      {
        dashboardId: 'component-eol',
        title: 'Component EOL: Retiring Old Code',
        description: 'Learn the specifics about the components that have the status of End of Life (EOL)',
        features: ['Note ratings by version', 'Notice apps using versions', 'Sort cumulative lists by type'],
        accessButtonText: 'View Component EOL',
        previewImage: '',
        priority: 3,
        spotlight: false,
      },
    ],
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, mockDashboardsData);
    renderPage = () => render(<EnterpriseReportingLandingPage />);
  });

  it('shows loading before dashboards data is loaded', async () => {
    renderPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Contact Us' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows dashboard data', async () => {
    renderPage();

    expect(screen.getByRole('status')).toBeVisible();
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

  it('shows error when there are API errors', async () => {
    axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(401, 'Unauthorized');

    renderPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Data Insights' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.queryByText('An error occurred loading data. Unauthorized')).toBeInTheDocument();
  });
});
