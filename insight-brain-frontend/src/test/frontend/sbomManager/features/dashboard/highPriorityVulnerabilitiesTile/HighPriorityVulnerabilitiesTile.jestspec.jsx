/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import HighPriorityVulnerabilitiesTile from 'MainRoot/sbomManager/features/dashboard/highPriorityVulnerabilitiesTile/HighPriorityVulnerabilitiesTile';
import { getSbomsHighPriorityVulnerabilitiesUrl } from 'MainRoot/util/CLMLocation';

describe('HighPriorityVulnerabilitiesTile', () => {
  let renderTile;

  const initialState = Object.freeze({
    sbomManagerDashboard: {
      highPriorityVulnerabilitiesTile: {
        loading: true,
        loadError: null,
        vulnerabilities: null,
      },
    },
  });

  const response = Object.freeze([
    {
      refId: 'CVE-1234-12345',
      severity: 10,
      severityStatus: 'critical',
      createdAt: '2024-04-29T00:00:00.000+0000',
    },
    {
      refId: 'CVE-5678-56789',
      severity: 9,
      severityStatus: 'medium',
      createdAt: '2024-04-30T00:00:00.000+0000',
    },
  ]);

  beforeEach(() => {
    renderTile = (additionalPreloadedState = {}) =>
      render(<HighPriorityVulnerabilitiesTile />, { preloadedState: { ...initialState, ...additionalPreloadedState } });
  });

  it('renders correct page content', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getSbomsHighPriorityVulnerabilitiesUrl()).reply(200, response);

    renderTile();

    expect(await screen.findByRole('heading', { name: /High Priority Vulnerabilities/i })).toBeVisible();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const severities = screen.getAllByTestId('high-priority-vulnerabilities-severity');
    expect(severities.length).toBe(2);

    expect(severities[0]).toHaveTextContent('10');
    expect(severities[1]).toHaveTextContent('9');

    expect(screen.getByText(/CVE-1234-12345/i)).toBeVisible();
    expect(screen.getByText(/CVE-5678-56789/i)).toBeVisible();
  });

  it('renders correct message when response is empty', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getSbomsHighPriorityVulnerabilitiesUrl()).reply(200, []);

    renderTile();

    expect(await screen.findByRole('heading', { name: /High Priority Vulnerabilities/i })).toBeVisible();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
    expect(screen.getByText('No Critical/High Vulnerabilities Found')).toBeVisible();
  });

  it('renders error message', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getSbomsHighPriorityVulnerabilitiesUrl()).reply(500, 'some error');

    renderTile();

    const error = await screen.findByText(/An error occurred loading data. some error/i);
    expect(error).toBeVisible();
  });
});
