/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { axiosMockAdapter, render, screen, cleanup } from 'TestRoot/SpecUtil';
import { getCiUsageUrl } from 'MainRoot/util/CLMLocation';
import CiCard from 'MainRoot/integrations/sections/overview/CiCard';
import { faker } from '@faker-js/faker';

describe('CiCard', () => {
  const donutTestId = 'iq-integrations-cicard__donut';
  const loadingMessage = 'Loading…';
  const errorMessageRole = 'alert';

  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  it('should initially show a loading screen', () => {
    givenCiUsageRequestSucceeds(generateRandomCiUsageResponse());

    renderComponent();

    expect(screen.getByText(loadingMessage)).toBeInTheDocument();
  });

  it('should correctly render pi graph based on the response when data is successfully fetched from the server', async () => {
    const given = [
      { response: { numAppsWithoutCITriggeredEvals: 77, numTotalApps: 100 }, expectedPercent: 77 },
      { response: { numAppsWithoutCITriggeredEvals: 55, numTotalApps: 102 }, expectedPercent: 54 },
      { response: { numAppsWithoutCITriggeredEvals: 0, numTotalApps: 22 }, expectedPercent: 0 },
      { response: { numAppsWithoutCITriggeredEvals: 0, numTotalApps: 0 }, expectedPercent: 0 },
      { response: { numAppsWithoutCITriggeredEvals: 0, numTotalApps: 10 }, expectedPercent: 0 },
    ];

    for (const { response, expectedPercent } of given) {
      givenCiUsageRequestSucceeds(response);
      renderComponent();

      const expectedText = `${expectedPercent}% of your apps are not integrated with CI`;

      expect(await screen.findByText(expectedText)).toBeInTheDocument();
      await assertDonutRenderedWithCorrectValue(expectedPercent);

      expect(screen.queryByText(loadingMessage)).not.toBeInTheDocument();
      expect(screen.queryByRole(errorMessageRole)).not.toBeInTheDocument();

      cleanup();
    }
  });

  it('should correctly render an error based on the response when data fetch fails', async () => {
    givenCiUsageRequestFails();

    renderComponent();

    expect(await screen.findByRole(errorMessageRole)).toBeInTheDocument();
    expect(screen.queryByText(loadingMessage)).not.toBeInTheDocument();
    expect(screen.queryByTestId(donutTestId)).not.toBeInTheDocument();
  });

  function givenCiUsageRequestSucceeds(response) {
    axiosMock.onGet(getCiUsageUrl()).reply(200, response);
  }

  function givenCiUsageRequestFails() {
    axiosMock.onGet(getCiUsageUrl()).reply(500, 'Error');
  }

  async function assertDonutRenderedWithCorrectValue(expectedPercent) {
    const donut = await screen.findByTestId('iq-integrations-cicard__donut');
    expect(donut).toBeInTheDocument();
    expect(donut).toHaveAttribute('aria-valuenow', `${expectedPercent}`);
  }

  function generateRandomCiUsageResponse() {
    return { numAppsWithoutCITriggeredEvals: faker.datatype.number(), numTotalApps: faker.datatype.number() };
  }

  function renderComponent() {
    return render(<CiCard />);
  }
});
