/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import ApplicationsHistoryTile from 'MainRoot/sbomManager/features/dashboard/applicationsHistoryTile/ApplicationsHistoryTile';
import { getSbomsHistoryUrl } from 'MainRoot/util/CLMLocation';

describe('ApplicationsHistoryTile', () => {
  let renderTile;

  beforeEach(() => {
    const preloadedState = {
      sbomManagerDashboard: {
        applicationsHistoryTile: {
          loading: true,
          loadError: null,
          totalScannedApplications: null,
          applicationsUpdatedLastYear: null,
          applicationsUpdatedLastMonth: null,
          applicationsUpdatedLastWeek: null,
        },
      },
    };
    renderTile = (additionalPreloadedState = {}) =>
      render(<ApplicationsHistoryTile />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  it('renders correct page content', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getSbomsHistoryUrl()).reply(200, {
      totalScannedApplications: 1000,
      applicationsUpdatedLastYear: 2000,
      applicationsUpdatedLastMonth: 3000,
      applicationsUpdatedLastWeek: 4000,
    });
    renderTile();

    expect(await screen.findByRole('heading', { name: /Applications History/i })).toBeVisible();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByText('Total scanned applications (all time)')).toBeVisible();
    expect(screen.getByText('Applications updated last year')).toBeVisible();
    expect(screen.getByText('Applications updated last month')).toBeVisible();
    expect(screen.getByText('Applications updated last week')).toBeVisible();

    expect(screen.getByTestId('applications-history-tile-total-scanned-applications')).toHaveTextContent('1,000');
    expect(screen.getByTestId('applications-history-tile-applications-updated-last-year')).toHaveTextContent('2,000');
    expect(screen.getByTestId('applications-history-tile-applications-updated-last-month')).toHaveTextContent('3,000');
    expect(screen.getByTestId('applications-history-tile-applications-updated-last-week')).toHaveTextContent('4,000');

    const applicationsPageLink = screen.getByText('View Latest Application Versions');
    expect(applicationsPageLink).toBeVisible();
  });

  it('renders error message', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getSbomsHistoryUrl()).reply(500, 'some error');
    renderTile();
    const error = await screen.findByText(/An error occurred loading data. some error/i);
    expect(error).toBeVisible();
  });
});
