/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import SbomReleaseStatusTile from 'MainRoot/sbomManager/features/dashboard/sbomReleaseStatusTile/SbomReleaseStatusTile';
import { getSbomReleaseStatusUrl } from 'MainRoot/util/CLMLocation';

describe('SbomReleaseStatusTile', () => {
  let renderTile;

  beforeEach(() => {
    const preloadedState = {
      sbomManagerDashboard: {
        sbomReleaseStatusTile: {
          loading: true,
          loadError: null,
          releaseReadyCount: null,
          partiallyReadyCount: null,
          needsAttentionCount: null,
        },
      },
    };
    renderTile = (additionalPreloadedState = {}) =>
      render(<SbomReleaseStatusTile />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  it('renders the correct title', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getSbomReleaseStatusUrl()).reply(200, {
      needsAttentionCount: 10,
      partiallyReadyCount: 20,
      releaseReadyCount: 30,
    });
    renderTile();

    expect(screen.getByRole('heading', { name: /SBOM Release Status/i })).toBeVisible();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
    const statuses = screen.getAllByTestId('sbom-release-status-progress-bar-status');
    expect(statuses[0]).toHaveTextContent('Needs Attention');
    expect(statuses[1]).toHaveTextContent('Partially Annotated');
    expect(statuses[2]).toHaveTextContent('Release Ready');

    const statusSbomCounts = screen.getAllByTestId('sbom-release-status-progress-bar-sbom-count');
    expect(statusSbomCounts[0]).toHaveTextContent('10');
    expect(statusSbomCounts[1]).toHaveTextContent('20');
    expect(statusSbomCounts[2]).toHaveTextContent('30');
  });

  it('renders error message', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getSbomReleaseStatusUrl()).reply(500, 'some error');
    renderTile();
    const error = await screen.findByText(/An error occurred loading data. some error/i);
    expect(error).toBeVisible();
  });
});
