/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import SbomReleaseStatusTile from 'MainRoot/sbomManager/features/dashboard/sbomReleaseStatusTile/SbomReleaseStatusTile';
import { getByText } from '@testing-library/react';

describe('SbomReleaseStatusTile', () => {
  it('renders the correct title', async () => {
    const props = {
      load: () => {},
      loading: false,
      loadError: null,
      needsAttentionCount: 10,
      partiallyReadyCount: 20,
      releaseReadyCount: 30,
      totalSbomCount: 1234,
      sbomMaxThreshold: 2468,
    };

    render(<SbomReleaseStatusTile {...props} />);

    expect(screen.getByRole('heading', { name: /SBOM Release Status/i })).toBeVisible();
    expect(screen.queryByText('Loading…')).toBeNull();

    const statuses = screen.getAllByTestId('sbom-release-status-meter-bar-status');
    expect(statuses[0]).toHaveTextContent('Needs Attention');
    expect(statuses[1]).toHaveTextContent('Partially Annotated');
    expect(statuses[2]).toHaveTextContent('Release Ready');

    const meters = screen.getAllByTestId('sbom-release-status-meter');
    expect(meters[0]).toHaveAttribute('value', '10');
    expect(meters[1]).toHaveAttribute('value', '20');
    expect(meters[2]).toHaveAttribute('value', '30');

    expect(meters[0]).toHaveAttribute('max', '1234');
    expect(meters[1]).toHaveAttribute('max', '1234');
    expect(meters[2]).toHaveAttribute('max', '1234');

    expect(getByText(meters[0], '10 out of 1234')).toBeVisible();
    expect(getByText(meters[1], '20 out of 1234')).toBeVisible();
    expect(getByText(meters[2], '30 out of 1234')).toBeVisible();

    const statusSbomCounts = screen.getAllByTestId('sbom-release-status-meter-bar-sbom-count');
    expect(statusSbomCounts[0]).toHaveTextContent('10');
    expect(statusSbomCounts[1]).toHaveTextContent('20');
    expect(statusSbomCounts[2]).toHaveTextContent('30');
  });

  it('renders the tile with 0 values when null is passed in', async () => {
    const props = {
      load: () => {},
      loading: false,
      loadError: null,
      needsAttentionCount: null,
      partiallyReadyCount: null,
      releaseReadyCount: null,
      totalSbomCount: null,
      sbomMaxThreshold: null,
    };

    render(<SbomReleaseStatusTile {...props} />);

    expect(screen.getByRole('heading', { name: /SBOM Release Status/i })).toBeVisible();
    expect(screen.queryByText('Loading…')).toBeNull();

    const statuses = screen.getAllByTestId('sbom-release-status-meter-bar-status');
    expect(statuses[0]).toHaveTextContent('Needs Attention');
    expect(statuses[1]).toHaveTextContent('Partially Annotated');
    expect(statuses[2]).toHaveTextContent('Release Ready');

    const meters = screen.getAllByTestId('sbom-release-status-meter');
    expect(meters[0]).toHaveAttribute('value', '0');
    expect(meters[1]).toHaveAttribute('value', '0');
    expect(meters[2]).toHaveAttribute('value', '0');

    expect(meters[0]).toHaveAttribute('max', '0');
    expect(meters[1]).toHaveAttribute('max', '0');
    expect(meters[2]).toHaveAttribute('max', '0');

    expect(getByText(meters[0], '0 out of 0')).toBeVisible();
    expect(getByText(meters[1], '0 out of 0')).toBeVisible();
    expect(getByText(meters[2], '0 out of 0')).toBeVisible();

    const statusSbomCounts = screen.getAllByTestId('sbom-release-status-meter-bar-sbom-count');
    expect(statusSbomCounts[0]).toHaveTextContent('0');
    expect(statusSbomCounts[1]).toHaveTextContent('0');
    expect(statusSbomCounts[2]).toHaveTextContent('0');
  });

  it('renders the loading panel', async () => {
    const props = {
      load: () => {},
      loading: true,
      loadError: null,
      needsAttentionCount: 10,
      partiallyReadyCount: 20,
      releaseReadyCount: 30,
      totalSbomCount: 1234,
      sbomMaxThreshold: 2468,
    };

    render(<SbomReleaseStatusTile {...props} />);

    expect(screen.getByRole('heading', { name: /SBOM Release Status/i })).toBeVisible();
    expect(screen.queryByText('Loading…')).toBeVisible();
  });

  it('renders error message', async () => {
    const props = {
      load: () => {},
      loading: false,
      loadError: 'some error',
      needsAttentionCount: 10,
      partiallyReadyCount: 20,
      releaseReadyCount: 30,
      totalSbomCount: 1234,
      sbomMaxThreshold: 2468,
    };

    render(<SbomReleaseStatusTile {...props} />);

    const error = await screen.findByText(/An error occurred loading data. some error/i);
    expect(error).toBeVisible();
  });
});
