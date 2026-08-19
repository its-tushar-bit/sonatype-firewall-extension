/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import VulnerabilitiesByThreatLevelTile from 'MainRoot/sbomManager/features/dashboard/vulnerabilitiesByThreatLevelTile/VulnerabilitiesByThreatLevelTile';
import { getVulnerabilitesByThreatLevelUrl } from 'MainRoot/util/CLMLocation';

describe('VulnerabilitiesByThreatLevelTile', () => {
  let renderTile;

  const vulnerabilitesInitialState = Object.freeze({
    critical: {
      unannotated: null,
      annotated: null,
      total: null,
    },
    high: {
      unannotated: null,
      annotated: null,
      total: null,
    },
    medium: {
      unannotated: null,
      annotated: null,
      total: null,
    },
    low: {
      unannotated: null,
      annotated: null,
      total: null,
    },
  });

  const vulnerabiltiesTotalInitialState = Object.freeze({
    totalVulnerabilities: null,
    totalVulnerabilitiesAnnotated: null,
    totalVulnerabilitiesUnannotated: null,
  });

  const normalResponse = Object.freeze({
    low: 3003,
    lowAnnotated: 1001,
    lowUnannotated: 2002,
    medium: 7003,
    mediumAnnotated: 3001,
    mediumUnannotated: 4002,
    high: 11003,
    highAnnotated: 5001,
    highUnannotated: 6002,
    critical: 15003,
    criticalAnnotated: 7001,
    criticalUnannotated: 8002,
    totalVulnerabilities: 36012,
    totalVulnerabilitiesAnnotated: 20008,
    totalVulnerabilitiesUnannotated: 16004,
  });

  beforeEach(() => {
    const preloadedState = {
      sbomManagerDashboard: {
        vulnerabilitiesByThreatLevelTile: {
          loading: true,
          loadError: null,
          vulnerabilities: { ...vulnerabilitesInitialState },
          vulnerabilitiesTotal: { ...vulnerabiltiesTotalInitialState },
        },
      },
    };
    renderTile = (additionalPreloadedState = {}) =>
      render(<VulnerabilitiesByThreatLevelTile />, {
        preloadedState: { ...preloadedState, ...additionalPreloadedState },
      });
  });

  it('renders correct page content', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getVulnerabilitesByThreatLevelUrl()).reply(200, normalResponse);

    renderTile();

    expect(await screen.findByRole('heading', { name: /Vulnerabilities by Threat Level/ })).toBeVisible();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('cell', { name: /Critical/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /7,001/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /8,002/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /15,003/ })).toBeVisible();

    expect(screen.getByRole('cell', { name: /High/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /5,001/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /6,002/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /11,003/ })).toBeVisible();

    expect(screen.getByRole('cell', { name: /Medium/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /3,001/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /4,002/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /7,003/ })).toBeVisible();

    expect(screen.getByRole('cell', { name: /Low/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /1,001/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /2,002/ })).toBeVisible();
    expect(screen.getByRole('cell', { name: /3,003/ })).toBeVisible();

    const total = screen.getByTestId('vulnerabilities-by-threat-level-tile-total');
    expect(total).toHaveTextContent(/Total:36,012/);

    const totalAnnotated = screen.getByTestId('vulnerabilities-by-threat-level-tile-total-annotated');
    expect(totalAnnotated).toHaveTextContent(/Annotated:20,008/);

    const totalUnannotated = screen.getByTestId('vulnerabilities-by-threat-level-tile-total-unannotated');
    expect(totalUnannotated).toHaveTextContent(/Unannotated:16,004/);

    const applicationsPageLink = screen.getByText('View Applications by most vulnerabilities');
    expect(applicationsPageLink).toBeVisible();
  });

  it('renders error message', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getVulnerabilitesByThreatLevelUrl()).reply(500, 'some error');
    renderTile();
    const error = await screen.findByText(/An error occurred loading data. some error/i);
    expect(error).toBeVisible();
  });
});
