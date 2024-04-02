/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import { getEnterpriseReportingEmbedUrl, getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';
import EnterpriseReportingDashboardPage from 'MainRoot/enterpriseReporting/dashboard/EnterpriseReportingDashboardPage';
import { screen } from '@testing-library/dom';

describe('EnterpriseReportingDashboardPage', () => {
  let axiosMock, renderPage;

  const mockLookerEmbedURl = {
    url: 'https://sonatypeinstance.looker.com?embedUrl',
    baseUrl: 'https://sonatypeinstance.looker.com',
  };

  renderPage = () => render(<EnterpriseReportingDashboardPage />);

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-enterprise-reporting']);
    axiosMock.onPost(getEnterpriseReportingEmbedUrl()).reply(200, mockLookerEmbedURl);
  });

  describe('Rendering Enterprise Report iframe page', () => {
    it('Renders the page', async () => {
      renderPage();

      expect(await screen.findByRole('enterprise-reporting-dashboard')).toBeInTheDocument();
    });
  });

  it('shows loading before iframe is loaded', async () => {
    renderPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(await screen.findByRole('enterprise-reporting-dashboard')).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows error when there are API errors', async () => {
    axiosMock.onPost(getEnterpriseReportingEmbedUrl()).reply(401, 'Unauthorized');

    renderPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
    const errorMessage = await screen.findByText('An error occurred loading data. Unauthorized');
    expect(errorMessage).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows error when there license does not have data-insights', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);

    renderPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
    const errorMessage = await screen.findByText('An error occurred loading data. Data Insights feature not supported');
    expect(errorMessage).toBeVisible();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});
