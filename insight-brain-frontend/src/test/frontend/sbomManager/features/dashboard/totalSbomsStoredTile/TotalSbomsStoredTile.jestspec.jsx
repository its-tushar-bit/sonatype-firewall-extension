/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import TotalSbomsStoredTile from 'MainRoot/sbomManager/features/dashboard/totalSbomsStoredTile/TotalSbomsStoredTile';

describe('TotalSbomsStoredTile', () => {
  it('renders correct page content', async () => {
    const props = {
      load: () => {},
      loading: false,
      loadError: null,
      totalSbomCount: 1234,
      sbomMaxThreshold: 2468,
    };

    render(<TotalSbomsStoredTile {...props} />);

    expect(await screen.findByRole('heading', { name: /Total SBOMs Stored/i })).toBeVisible();
    expect(screen.queryByText('Loading…')).toBeNull();

    expect(screen.getByTestId('total-sboms-stored-tile-progress-label')).toHaveTextContent('SBOM License Usage');
    expect(screen.getByTestId('total-sboms-stored-tile-total')).toHaveTextContent('1,234(all time)');
    expect(screen.getByTestId('total-sboms-stored-tile-progress-total')).toHaveTextContent('1,234SBOMs added');
    expect(screen.getByTestId('total-sboms-stored-tile-progress-threshold')).toHaveTextContent('2,468Threshold');
  });

  it('renders loading when loading', async () => {
    const props = {
      load: () => {},
      loading: true,
      loadError: null,
      totalSbomCount: 1234,
      sbomMaxThreshold: 2468,
    };

    render(<TotalSbomsStoredTile {...props} />);
    expect(screen.queryByText('Loading…')).toBeVisible();
  });

  it('renders error message', async () => {
    const props = {
      load: () => {},
      loading: false,
      loadError: 'some error',
      totalSbomCount: 1234,
      sbomMaxThreshold: 2468,
    };

    render(<TotalSbomsStoredTile {...props} />);

    const error = await screen.findByText(/An error occurred loading data. some error/i);
    expect(error).toBeVisible();
  });
});
