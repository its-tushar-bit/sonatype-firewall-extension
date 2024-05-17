/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import {
  getEnterpriseReportingGenerateEmbedTokensUrl,
  getEnterpriseReportingAcquireEmbedSessionUrl,
  getEnterpriseReportingBaseUrl,
  getProductFeaturesUrl,
} from 'MainRoot/util/CLMLocation';
import EnterpriseReportingDashboardPage from 'MainRoot/enterpriseReporting/dashboard/EnterpriseReportingDashboardPage';
import { screen } from '@testing-library/dom';
import { LookerEmbedSDK } from '@looker/embed-sdk';

describe('EnterpriseReportingDashboardPage', () => {
  let axiosMock, renderPage;

  const mockLookerBaseUrl = 'https://sonatypeinstance.looker.com';

  renderPage = () =>
    render(<EnterpriseReportingDashboardPage />, {
      preloadedState: {
        enterpriseReportingDashboard: {
          selectedDashboard: { dashboardId: 'rolling-recap', dashboardPath: 'dashboardPath' },
        },
      },
    });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-enterprise-reporting']);
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
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows error when there are API errors', async () => {
    axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(401, 'Unauthorized');

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
