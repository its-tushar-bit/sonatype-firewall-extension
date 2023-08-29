/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import DashboardWaivers from 'MainRoot/dashboard/results/waivers/DashboardWaivers';
import { getWaiversUrl } from 'MainRoot/util/CLMLocation';

// this tests is depending on the dashboardRestulAction and how they handle the promise rejection, will be fixed in CLM-22474
describe('DashboardWaivers', function () {
  let renderComponent;
  let waivers;
  let dashboardFilter;
  let axiosMock;
  let policyWaiversUrl;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(function () {
    dashboardFilter = {
      needsAcknowledgement: false,
      loading: false,
      filtersAreDirty: false,
      appliedFilter: { maxDaysOld: 20 },
    };

    const initialState = { dashboardFilter };

    waivers = {
      error: null,
      numResults: 200,
      sortFields: ['expiryTime'],
      results: [],
      pageCount: 2,
      page: 0,
    };

    for (let i = 0; i < 100; i++) {
      const resultObject = {
        id: '4ac5e46025c941e68a335b61eb3165d2' + i,
        threatLevel: 5,
        createTime: 1661532973306,
        expiryTime: null,
        policyId: 'ce6ca7e95261441586a0e3f1f934dd37',
        policyName: 'Figue-policy',
        ownerId: '642a106467c74f6eb5f90eade8ceb5f9',
        ownerName: 'root-org',
        ownerType: 'organization',
        scope: 'Organization - root-org',
        componentMatchStrategy: 'ALL_COMPONENTS',
        hash: null,
      };
      waivers.results.push(resultObject);
    }

    policyWaiversUrl = getWaiversUrl();
    axiosMock.onPost(policyWaiversUrl).reply(200, { dashboardResults: waivers.results, numResults: 200 });

    renderComponent = (additionalProps = {}, preloadedState = initialState) =>
      render(<DashboardWaivers {...additionalProps} />, { preloadedState });
  });

  it('renders a DashboardWaiversTable with the appropriate props', async function () {
    renderComponent();
    // finding by role rowgroup it return the thead and tbody of a table
    const [tableHeaders, tableEntries] = await screen.findAllByRole('rowgroup');
    expect(tableHeaders).toBeVisible();
    expect(tableEntries).toBeVisible();
    expect(tableEntries.children.length).toBe(100);
  });

  it('renders and error with a retry button', async () => {
    axiosMock.onPost(policyWaiversUrl).reply(500, 'some error');
    renderComponent();
    const alert = await screen.findByRole('alert');
    expect(alert).toBeVisible();
    expect(alert).toHaveTextContent('An error occurred loading data. some errorRetry');

    const retryButton = screen.getByRole('button', { name: 'Retry' });
    expect(retryButton).toBeVisible();
  });

  it('renders a form mask if filters are dirty', async () => {
    dashboardFilter.filtersAreDirty = true;
    axiosMock.onPost(policyWaiversUrl).reply(500, 'some error');
    renderComponent();
    expect(await screen.findByText('Please apply or revert filter to see results.')).toBeVisible();
  });

  it('renders informational alert for waiver view results', async () => {
    renderComponent();

    expect(
      await screen.getByText(
        'This list shows all existing waivers applied at the same or higher hierarchy level, based on your filter selections.'
      )
    ).toBeVisible();
  });

  it('renders "Learn more about waivers." link', async () => {
    renderComponent();

    expect(
      await screen.getByRole('link', {
        name: 'Learn more about waivers.',
      })
    ).toBeVisible();
  });
});
