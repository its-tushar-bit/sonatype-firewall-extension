/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import DashboardWaiverRequestsTable from 'MainRoot/dashboard/results/waivers/DashboardWaiverRequestsTable';
import defaultFilter from 'MainRoot/dashboard/filter/defaultFilter';

import { getWaiverRequestsUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('DashboardWaiverRequestsTable', () => {
  let renderComponent;
  let state;
  let mock;

  beforeEach(() => {
    state = {
      dashboard: {
        waiverRequests: {
          results: [
            {
              id: '1',
              threatLevel: 5,
              requestTime: '2025-04-18T12:00:00Z',
              expiryTime: null,
              requesterName: 'requester name 1',
              policyId: 'testPolicyId1',
              policyName: 'test policy 1',
              scope: 'application',
              status: 'REQUESTED',
              ownerId: 'requestOwnerAppId',
              ownerName: 'test app',
              ownerType: 'application',
              componentMatchStrategy: 'EXACT_COMPONENT',
            },
            {
              id: '2',
              threatLevel: 7,
              requestTime: '2025-04-19T12:00:00Z',
              expiryTime: '2025-05-19T12:00:00Z',
              requesterName: 'requester name 2',
              policyId: 'testPolicyId2',
              policyName: 'test policy 2',
              scope: 'organization',
              status: 'REJECTED',
              ownerId: 'requestOwnerOrgId',
              ownerName: 'test org',
              ownerType: 'organization',
              componentMatchStrategy: 'ALL_VERSIONS',
            },
          ],
          hasNextPage: false,
          error: null,
          sortFields: ['-requestTime'],
          hasMultiplePages: false,
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
    mock.onPost(getWaiverRequestsUrl()).reply(200, {
      dashboardResults: state.dashboard.waiverRequests.results,
      hasNextPage: false,
    });

    renderComponent = (preloadedState = state) => render(<DashboardWaiverRequestsTable />, { preloadedState });
  });

  it('displays loading spinner, and then renders data from the mocked API response', async () => {
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();

    expect(await screen.findByText('5')).toBeVisible();
    expect(screen.getByText('2025-04-18')).toBeVisible();
    expect(screen.getByText('requester name 1')).toBeVisible();
    expect(screen.getByText('test policy 1')).toBeVisible();
    expect(screen.getByText('Application - test app')).toBeVisible();
    expect(screen.getByText('Requested')).toBeVisible();

    expect(await screen.findByText('7')).toBeVisible();
    expect(screen.getByText('2025-04-19')).toBeVisible();
    expect(screen.getByText('requester name 2')).toBeVisible();
    expect(screen.getByText('test policy 2')).toBeVisible();
    expect(screen.getByText('Organization - test org')).toBeVisible();
    expect(screen.getByText('Rejected')).toBeVisible();
  });

  it('renders a row with an alert message when the filter needs acknowledgement', async () => {
    state.dashboardFilter.needsAcknowledgement = true;
    renderComponent();

    expect(await screen.findByText(`Select your filter criteria and click 'apply' to see results.`)).toBeVisible();
  });

  it('renders a row with an empty message when there are no results to display', async () => {
    state.dashboard.waiverRequests.results = null;
    state.dashboardFilter.loading = false;

    mock.onPost(getWaiverRequestsUrl()).reply(200, {
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

    mock.onPost(getWaiverRequestsUrl()).reply(500, {
      dashboardResults: null,
      hasNextPage: false,
    });

    renderComponent();

    expect(await screen.findByText('An error occurred loading data. Error')).toBeVisible();
  });
});
