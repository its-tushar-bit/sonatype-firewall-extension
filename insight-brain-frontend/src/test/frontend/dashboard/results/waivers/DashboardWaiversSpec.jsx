/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, fireEvent } from 'TestRoot/SpecUtil';
import DashboardWaivers from 'MainRoot/dashboard/results/waivers/DashboardWaivers';
import * as dashboardActions from 'MainRoot/dashboard/results/dashboardResultsActions';
import { getWaiversUrl } from 'MainRoot/util/CLMLocation';
import * as DashboardSelectors from 'MainRoot/dashboard/dashboardSelectors';

// this tests is depending on the dashboardRestulAction and how they handle the promise rejection, will be fixed in CLM-22474
describe('DashboardWaivers', function () {
  let renderComponent,
    waivers,
    dashboardFilter,
    loadWaiversResultsSpy,
    axiosMock,
    policyWaiversUrl,
    selectDashboardFilterSpy,
    selectWaiversResultsSpy;

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

    // these spys will be removed in CLM-22474
    selectDashboardFilterSpy = spyOn(DashboardSelectors, 'selectDashboardFilter').and.returnValue(dashboardFilter);
    selectWaiversResultsSpy = spyOn(DashboardSelectors, 'selectWaiversResults').and.returnValue(waivers);

    policyWaiversUrl = getWaiversUrl();
    axiosMock.onPost(policyWaiversUrl).reply(200, { dashboardResults: waivers.results, numResults: 200 });

    // this spy will be removed in CLM-22474
    loadWaiversResultsSpy = spyOn(dashboardActions, 'loadWaiverResults').and.callThrough();

    renderComponent = (additionalProps = {}) => render(<DashboardWaivers {...additionalProps} />);
  });

  it('renders a DashboardWaiversTable with the appropriate props', async function () {
    renderComponent();
    // finding by role rowgroup it return the thead and tbody of a table
    const [tableHeaders, tableEntries] = await screen.findAllByRole('rowgroup');
    expect(tableHeaders).toBeVisible();
    expect(tableEntries).toBeVisible();
    expect(tableEntries.children.length).toBe(100);

    // this spy will be removed in CLM-22474
    expect(loadWaiversResultsSpy).toHaveBeenCalled();
  });

  // To be checked at CLM-25840
  xit('renders and error with a retry button', async () => {
    waivers.error = 'error';
    // this spy will be removed in CLM-22474
    selectWaiversResultsSpy.and.returnValue(waivers);
    renderComponent();

    expect(await screen.findByRole('alert')).toBeVisible();

    const retryButton = await screen.findByRole('button', { name: 'Retry' });
    fireEvent.click(retryButton);

    expect(loadWaiversResultsSpy).toHaveBeenCalled();
  });

  // To be checked at CLM-25840
  xit('renders a form mask if filters are dirty', async () => {
    dashboardFilter.filtersAreDirty = true;
    waivers.error = 'error';

    // these spys will be removed in CLM-22474
    selectDashboardFilterSpy.and.returnValue(dashboardFilter);
    selectWaiversResultsSpy.and.returnValue(waivers);

    renderComponent();

    expect(await screen.getByText('Please apply or revert filter to see results.')).toBeVisible();
  });

  // To be checked at CLM-25840
  xit('renders informational alert for waiver view results', async () => {
    renderComponent();

    expect(
      await screen.getByText(
        'This list shows all existing waivers applied at the same or higher hierarchy level, based on your filter selections.'
      )
    ).toBeVisible();
  });

  // To be checked at CLM-25840
  xit('renders "Learn more about waivers." link', async () => {
    renderComponent();

    expect(
      await screen.getByRole('link', {
        name: 'Learn more about waivers.',
      })
    ).toBeVisible();
  });
});
