/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import TotalSbomsStoredTile from 'MainRoot/sbomManager/features/dashboard/totalSbomsStoredTile/TotalSbomsStoredTile';
import { getTotalSbomsAnalyzedUrl } from 'MainRoot/util/CLMLocation';

describe('TotalSbomsStoredTile', () => {
  let renderTile;

  beforeEach(() => {
    const preloadedState = {
      sbomManagerDashboard: {
        totalSbomsStoredTile: {
          loading: true,
          errorMessage: null,
          total: null,
          threshold: null,
        },
      },
    };
    renderTile = (additionalPreloadedState = {}) =>
      render(<TotalSbomsStoredTile />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  it('renders correct page content', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getTotalSbomsAnalyzedUrl()).reply(200, {
      total: 1234,
      threshold: 2468,
    });
    renderTile();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(await screen.findByRole('heading', { name: /Total SBOMs Stored/i })).toBeVisible();

    expect(screen.getByTestId('total-sboms-stored-tile-progress-label')).toHaveTextContent('SBOM License Usage');
    expect(screen.getByTestId('total-sboms-stored-tile-total')).toHaveTextContent('1,234(all time)');
    expect(screen.getByTestId('total-sboms-stored-tile-progress-total')).toHaveTextContent('1,234SBOMs added');
    expect(screen.getByTestId('total-sboms-stored-tile-progress-threshold')).toHaveTextContent('2,468Threshold');
  });

  it('renders error message', async () => {
    const axiosMock = axiosMockAdapter();
    axiosMock.onGet(getTotalSbomsAnalyzedUrl()).reply(500, 'some error');
    renderTile();
    const error = await screen.findAllByRole('alert', /An error occurred loading data. some error/i);
    expect(error[0]).toBeVisible();
  });
});
