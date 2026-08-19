/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import DashboardWaiversTable from 'MainRoot/dashboard/results/waivers/DashboardWaiversTable';
import defaultFilter from 'MainRoot/dashboard/filter/defaultFilter';
import { getWaiversUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('DashboardWaiversTable', function () {
  let renderComponent;
  let state;
  let mock;

  beforeEach(function () {
    state = {
      dashboard: {
        waivers: {
          results: [
            {
              id: '9ba41779ad63456788bbdb223ae5322a',
              threatLevel: 10,
              createTime: '2025-04-18T12:00:00Z',
              expiryTime: '2026-04-18T12:00:00Z',
              policyId: '766012e0acf8464dbd7973ec928e2210',
              policyName: 'Security-Critical',
              ownerId: '0305d75f92c04c459b7d24c8bc406f7e',
              ownerName: 'app1',
              ownerType: 'application',
              scope: 'Applicationapp - app1',
              componentMatchStrategy: 'ALL_COMPONENTS',
              hash: null,
            },
          ],
          error: null,
          hasNextPage: true,
          sortFields: ['expiryTime'],
          hasMultiplePages: true,
          page: null,
        },
      },
      dashboardFilter: {
        loading: false,
        needsAcknowledgement: false,
        filtersAreDirty: false,
        appliedFilter: {
          ...defaultFilter,
        },
      },
    };

    mock = axiosMockAdapter();
    mock.onPost(getWaiversUrl()).reply(200, {
      dashboardResults: state.dashboard.waivers.results,
      hasNextPage: false,
    });

    renderComponent = (preloadedState = state) => render(<DashboardWaiversTable />, { preloadedState });
  });

  it('displays a loading spinner and then renders data from the mocked API response', async () => {
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();

    expect(await screen.findByText('10')).toBeVisible(); // threat level
    expect(screen.getByText('2025-04-18')).toBeVisible(); // creation
    expect(screen.getByText('2026-04-18')).toBeVisible(); // expiration
    expect(screen.getByText('Security-Critical')).toBeVisible(); // policy
    expect(screen.getByText('Application - app1')).toBeVisible(); // scope
    expect(screen.getByText('All Components')).toBeVisible(); // components
  });

  it('renders a row with an alert message when the filter needs acknowledgement', async () => {
    state.dashboardFilter.needsAcknowledgement = true;
    renderComponent();

    expect(await screen.findByText(`Select your filter criteria and click 'apply' to see results.`)).toBeVisible();
  });

  it('renders a row with an empty message when there are no results to display', async () => {
    state.dashboard.waivers.results = null;
    state.dashboardFilter.loading = false;

    mock.onPost(getWaiversUrl()).reply(200, {
      dashboardResults: [],
      hasNextPage: false,
    });

    renderComponent();

    expect(
      await screen.findByText('No data available in the last 30 days given the applied filters and permissions.')
    ).toBeVisible();
  });

  it('renders an error message if loading fails', async () => {
    state.dashboardFilter.loading = false;

    mock.onPost(getWaiversUrl()).reply(500, {
      dashboardResults: null,
      hasNextPage: false,
    });

    renderComponent();

    expect(await screen.findByText('An error occurred loading data. Error')).toBeVisible();
  });
});
