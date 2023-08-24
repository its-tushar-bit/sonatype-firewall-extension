/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { axiosMockAdapter, render, screen, generateList } from 'TestRoot/SpecUtil';
import { getAppsWithoutRecentCiUsageUrl } from 'MainRoot/util/CLMLocation';
import { faker } from '@faker-js/faker';
import { CiUsageAppPreviewTable } from 'MainRoot/integrations/sections/overview/CiUsageAppPreviewTable';
import { within } from '@testing-library/react';

describe('CiUsageAppPreviewTable', () => {
  const appsWithoutRecentUsageTestId = 'iq-integrations-apps-without-recent-usage-preview';
  const loadingMessage = 'Loading…';
  const errorMessageRole = 'alert';

  let axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    jasmine.clock().install();
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  it('should initially show a loading indicator', () => {
    givenAppsWithoutRecentCiUsageRequestSucceeds(generateRandomAppsWithoutRecentCiUsageResponse());

    renderComponent();

    assertTableIsInLoadingState();
  });

  it('should correctly render table given data is successfully loaded', async () => {
    const givenAppQueryResults = generateRandomAppsWithoutRecentCiUsageResponse();

    givenAppsWithoutRecentCiUsageRequestSucceeds(givenAppQueryResults);

    renderComponent();

    await assertTableIsRenderedCorrectly(givenAppQueryResults.dashboardResults);
  });

  describe('view all apps button', () => {
    it('should be visible if response data contains apps without ci integrations', async () => {
      const givenAppQueryResults = generateRandomAppsWithoutRecentCiUsageResponse();

      givenAppsWithoutRecentCiUsageRequestSucceeds(givenAppQueryResults);

      renderComponent();

      expect(await screen.findByRole('button', { name: /view all apps/i })).toBeInTheDocument();
    });

    it('should not be visible if response data is empty', async () => {
      const givenAppQueryResults = generateEmptyResultResponse();

      givenAppsWithoutRecentCiUsageRequestSucceeds(givenAppQueryResults);

      renderComponent();

      expect(screen.queryByRole('button', { name: /view all apps/i })).not.toBeInTheDocument();
    });

    it('should not be visible if num of apps is <= PREVIEW_PAGE_SIZE (6)', async () => {
      const dashboardResults = generateList(generateRandomApplication, { min: 1, max: 6 });

      const givenAppQueryResults = {
        dashboardResults,
        numResults: dashboardResults.length,
      };

      givenAppsWithoutRecentCiUsageRequestSucceeds(givenAppQueryResults);

      renderComponent();

      await assertTableIsRenderedCorrectly(givenAppQueryResults.dashboardResults);

      expect(screen.queryByRole('button', { name: /view all apps/i })).not.toBeInTheDocument();
    });

    it('should be visible if num of apps is > PREVIEW_PAGE_SIZE (6)', async () => {
      const dashboardResults = generateList(generateRandomApplication, { min: 6, max: 6 });

      const givenAppQueryResults = {
        dashboardResults,
        numResults: faker.datatype.number({ min: 7 }),
      };

      givenAppsWithoutRecentCiUsageRequestSucceeds(givenAppQueryResults);

      renderComponent();

      await assertTableIsRenderedCorrectly(givenAppQueryResults.dashboardResults);

      expect(await screen.findByRole('button', { name: /view all apps/i })).toBeInTheDocument();
    });
  });

  it('should correctly show an error indicator given the data request failed', async () => {
    givenAppsWithoutRecentCiUsageRequestFails();

    renderComponent();

    await assertTableIsInFailState();
  });

  it('should invoke the endpoint to fetch a small sample of apps with ci usage since the last 3 months', async () => {
    givenAppsWithoutRecentCiUsageRequestSucceeds(generateRandomAppsWithoutRecentCiUsageResponse());

    const givenTime = new Date();
    jasmine.clock().mockDate(givenTime);

    renderComponent();

    expect(await screen.findByTestId(appsWithoutRecentUsageTestId)).toBeInTheDocument();

    const expectedTimestampForQuery = givenTime.setMonth(givenTime.getMonth() - 3);

    expect(axiosMock.history.post.length).toEqual(1);
    expect(axiosMock.history.post[0].data).toBe(
      JSON.stringify({
        sinceUtcTimestamp: expectedTimestampForQuery,
        pageSize: 6,
        page: 0,
      })
    );
  });

  async function assertTableIsRenderedCorrectly(givenAppsReturnedFromServer) {
    expect(await screen.findByTestId(appsWithoutRecentUsageTestId)).toBeInTheDocument();
    const { getAllByRole } = within(screen.getByTestId(appsWithoutRecentUsageTestId));

    const rows = getAllByRole('row');
    expect(rows.length - 1).toEqual(givenAppsReturnedFromServer.length);

    rows.forEach((row, index) => {
      if (index > 0) {
        const appNameFromServer = givenAppsReturnedFromServer[index - 1].applicationName;
        const totalRiskFromServer = givenAppsReturnedFromServer[index - 1].totalRisk;

        expect(within(row).getAllByRole('cell')[0]).toHaveTextContent(appNameFromServer);
        expect(within(row).getAllByRole('cell')[1]).toHaveTextContent(totalRiskFromServer);
      }
    });
  }

  function assertTableIsInLoadingState() {
    const { getByText } = within(screen.getByTestId(appsWithoutRecentUsageTestId));
    expect(getByText(loadingMessage)).toBeInTheDocument();
  }

  async function assertTableIsInFailState() {
    const { findByRole } = within(screen.getByTestId(appsWithoutRecentUsageTestId));
    expect(await findByRole(errorMessageRole)).toBeInTheDocument();
  }

  function givenAppsWithoutRecentCiUsageRequestSucceeds(response) {
    axiosMock.onPost(getAppsWithoutRecentCiUsageUrl()).reply(200, response);
  }

  function givenAppsWithoutRecentCiUsageRequestFails() {
    axiosMock.onPost(getAppsWithoutRecentCiUsageUrl()).reply(500, faker.lorem.sentence());
  }

  function generateRandomAppsWithoutRecentCiUsageResponse() {
    const dashboardResults = generateList(generateRandomApplication, { min: 1, max: 6 });

    return {
      dashboardResults,
      numResults: faker.datatype.number({ min: dashboardResults.length }),
    };
  }

  function generateEmptyResultResponse() {
    const dashboardResults = [];

    return {
      dashboardResults,
      numResults: 0,
    };
  }

  function generateRandomApplication() {
    return {
      applicationPublicId: faker.lorem.word(),
      applicationName: faker.lorem.word(),
      totalRisk: faker.datatype.number(),
    };
  }
  function renderComponent() {
    return render(<CiUsageAppPreviewTable />);
  }
});
