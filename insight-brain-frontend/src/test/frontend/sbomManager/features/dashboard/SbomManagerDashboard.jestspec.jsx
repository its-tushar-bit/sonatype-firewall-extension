/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import SbomManagerDashboard from 'MainRoot/sbomManager/features/dashboard/SbomManagerDashboard';
import { screen } from '@testing-library/dom';
import { getSbomReleaseStatusUrl, getTotalSbomsAnalyzedUrl } from 'MainRoot/util/CLMLocation';

describe('SbomManagerDashboard page', () => {
  let renderComponent;
  let state;
  let axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    state = {
      sbomManagerDashboard: {
        sbomCounts: {
          loading: true,
          loadError: null,
          needsAttentionCount: null,
          partiallyReadyCount: null,
          releaseReadyCount: null,
          totalSbomCount: null,
          sbomMaxThreshold: null,
        },
      },
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
        },
      },
      router: { currentState: { name: 'sbomManager.dashboard' } },
    };

    renderComponent = (preloadedState = state) => render(<SbomManagerDashboard />, { preloadedState });
  });

  it('Renders the dashboard loading the SBOM counts for the sub components', async () => {
    axiosMock.onGet(getTotalSbomsAnalyzedUrl()).reply(200, {
      total: 1234,
      threshold: 2468,
    });

    axiosMock.onGet(getSbomReleaseStatusUrl()).reply(200, {
      needsAttentionCount: 1,
      partiallyReadyCount: 2,
      releaseReadyCount: 3,
    });

    renderComponent();

    expect(screen.getByRole('heading', { name: /SBOM Manager Dashboard/i })).toBeVisible();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    checkSbomReleaseStatusTile();
    checkTotalSbomsStoredTile();
  });

  it('Passes the load method to the sub components and reloads data on retry click', async () => {
    axiosMock.onGet(getTotalSbomsAnalyzedUrl()).reply(200, {
      total: 1234,
      threshold: 2468,
    });
    axiosMock.onGet(getSbomReleaseStatusUrl()).replyOnce(500, 'error');

    renderComponent();

    expect(screen.getByRole('heading', { name: /SBOM Manager Dashboard/i })).toBeVisible();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    let errorComponents = await screen.findAllByText('An error occurred loading data. error');
    expect(errorComponents).toHaveLength(2);

    axiosMock.onGet(getSbomReleaseStatusUrl()).replyOnce(200, {
      needsAttentionCount: 1,
      partiallyReadyCount: 2,
      releaseReadyCount: 3,
    });

    const retryButtons = screen.getAllByText('Retry');
    retryButtons[0].parentElement.click();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    checkSbomReleaseStatusTile();
    checkTotalSbomsStoredTile();
    errorComponents = await screen.queryAllByText('An error occurred loading data. error');
    expect(errorComponents).toHaveLength(0);
  });

  it('shows error when the SBOM Manager license is disabled', async () => {
    state.productFeatures.productFeatures = {};
    renderComponent();

    const errorMessage = await screen.findByText(
      'An error occurred loading data. The SBOM Manager license feature is not enabled.'
    );
    expect(errorMessage).toBeVisible();
  });

  function checkSbomReleaseStatusTile() {
    const statusSbomCounts = screen.getAllByTestId('sbom-release-status-meter-bar-sbom-count');
    expect(statusSbomCounts[0]).toHaveTextContent('1');
    expect(statusSbomCounts[1]).toHaveTextContent('2');
    expect(statusSbomCounts[2]).toHaveTextContent('3');
  }

  function checkTotalSbomsStoredTile() {
    expect(screen.getByTestId('total-sboms-stored-tile-total')).toHaveTextContent('1,234(all time)');
    expect(screen.getByTestId('total-sboms-stored-tile-progress-total')).toHaveTextContent('1,234SBOMs added');
    expect(screen.getByTestId('total-sboms-stored-tile-progress-threshold')).toHaveTextContent('2,468Threshold');
  }
});
