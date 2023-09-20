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
import { within } from '@testing-library/react';

describe('CiCard', () => {
  const statsSectionId = 'iq-integrations-cicard--stats-section';
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

    const { getByText } = within(screen.getByTestId(statsSectionId));
    expect(getByText(loadingMessage)).toBeInTheDocument();
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

      const expectedText =
        expectedPercent === 0
          ? 'All of your apps are integrated with CI'
          : `${expectedPercent}% of your apps are not integrated with CI`;

      const { findByText, queryByText, queryByRole } = within(screen.getByTestId(statsSectionId));
      expect(await findByText(expectedText)).toBeInTheDocument();
      await assertDonutRenderedWithCorrectValue(expectedPercent);

      expect(queryByText(loadingMessage)).not.toBeInTheDocument();
      expect(queryByRole(errorMessageRole)).not.toBeInTheDocument();

      cleanup();
    }
  });

  it('should correctly render an error based on the response when data fetch fails', async () => {
    givenCiUsageRequestFails();

    renderComponent();

    const { findByRole, queryByText, queryByTestId } = within(screen.getByTestId(statsSectionId));
    expect(await findByRole(errorMessageRole)).toBeInTheDocument();
    expect(queryByText(loadingMessage)).not.toBeInTheDocument();
    expect(queryByTestId(donutTestId)).not.toBeInTheDocument();
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
