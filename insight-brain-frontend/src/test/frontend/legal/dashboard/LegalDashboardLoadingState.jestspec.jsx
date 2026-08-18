/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, waitFor, within, axiosMockAdapter } from 'TestRoot/SpecUtil';
import LegalDashboardContainer from 'MainRoot/legal/dashboard/LegalDashboardContainer';
import {
  getApplicationsUrl,
  getApplicationTagsUrl,
  getDashboardStageUrl,
  getLegalDashboardApplicationsUrl,
  getLegalDashboardFilters,
  getLegalDashboardSavedFilters,
  getOrganizationsUrl,
  getOwnerListUrl,
} from 'MainRoot/util/CLMLocation';

/**
 * Regression coverage for CLM-44448: the Legal application backlog must show a loading
 * affordance while its data is being fetched and must only show the "No applications found"
 * empty message after a completed fetch that returned zero results — never during a pending
 * request. On initial load the results request is deferred until the filter request resolves,
 * so this exercises the full connected container against a delayed applications response.
 */
describe('Legal dashboard applications backlog loading state', function () {
  const APPLICATIONS_TABLE_ID = 'legal-dashboard-applications-table';
  const EMPTY_MESSAGE = /No applications found given the applied filters and permissions/i;

  let mock;

  beforeAll(function () {
    mock = axiosMockAdapter();
  });

  const preloadedState = {
    router: {
      currentState: {
        name: 'legal.applicationsDashboard',
        url: '/legal/applicationsDashboard',
        data: { activeTab: 'applications' },
      },
      currentParams: {},
      prevState: {},
      prevParams: {},
    },
  };

  function applicationsTable() {
    return document.getElementById(APPLICATIONS_TABLE_ID);
  }

  function mockFilterEndpoints() {
    // The filter phase (loadFilter) runs before the results request on initial load.
    mock.onGet(getApplicationsUrl()).reply(200, []);
    mock.onGet(getOrganizationsUrl()).reply(200, []);
    mock.onGet(getApplicationTagsUrl()).reply(200, []);
    mock.onGet(getLegalDashboardFilters()).reply(200, { filter: null });
    mock.onGet(getLegalDashboardSavedFilters()).reply(200, []);
    mock.onGet(getDashboardStageUrl()).reply(200, []);
    mock.onGet(getOwnerListUrl()).reply(200, { organizations: [], applications: [] });
    // Anything else the initial load touches resolves harmlessly.
    mock.onAny().reply(200, []);
  }

  it('shows the loading affordance during the request and the empty message only after it resolves empty', async function () {
    // Delay the applications results so the pending state is observable. Register this BEFORE the
    // catch-all in mockFilterEndpoints() so the delayed handler wins (mock-adapter matches in order).
    mock
      .onPost(getLegalDashboardApplicationsUrl())
      .reply(
        () => new Promise((resolve) => setTimeout(() => resolve([200, { results: [], totalResultsCount: 0 }]), 50))
      );
    mockFilterEndpoints();

    render(<LegalDashboardContainer />, { preloadedState });

    const table = applicationsTable();
    expect(table).toBeInTheDocument();
    // While the filter + results requests are in flight the table shows a spinner...
    expect(within(table).getByText('Loading…')).toBeInTheDocument();
    // ...and must NOT tell the user there are no applications.
    expect(within(table).queryByText(EMPTY_MESSAGE)).not.toBeInTheDocument();

    // The empty message appears only once the request has completed with zero results.
    await waitFor(() => {
      expect(within(applicationsTable()).getByText(EMPTY_MESSAGE)).toBeInTheDocument();
    });
    expect(within(applicationsTable()).queryByText('Loading…')).not.toBeInTheDocument();
  });
});
