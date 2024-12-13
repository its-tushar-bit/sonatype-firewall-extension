/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen } from '@testing-library/dom';

import { render } from 'TestRoot/SpecUtil';
import SummaryTileReleaseStatus from 'MainRoot/sbomManager/features/billOfMaterials/summaryTile/summaryTileReleaseStatus/SummaryTileReleaseStatus';

describe('SummaryTileReleaseStatus', () => {
  it('renders the correct content', async () => {
    render(<SummaryTileReleaseStatus percentage={75} />);

    expect(screen.getByRole('heading', { name: /Release Status/ })).toBeVisible();

    const pieChartTotal = screen.getByTestId('pie-chart-total');
    expect(pieChartTotal).toHaveTextContent('75%');

    const description = screen.getByTestId('summary-tile-release-status-description');
    expect(description).toHaveTextContent(
      '75% of critical and high vulnerabilities have been annotated with exploitability information'
    );
  });

  it('renders the correct content when null is passed', async () => {
    render(<SummaryTileReleaseStatus percentage={null} />);

    expect(screen.getByRole('heading', { name: /Release Status/ })).toBeVisible();

    const pieChartTotal = screen.getByTestId('pie-chart-total');
    expect(pieChartTotal).toHaveTextContent('0%');

    const status = screen.getByTestId('summary-tile-release-status');
    expect(status).toHaveTextContent('Needs Attention');

    const description = screen.getByTestId('summary-tile-release-status-description');
    expect(description).toHaveTextContent(
      '0% of critical and high vulnerabilities have been annotated with exploitability information'
    );
  });

  describe('Release Status', () => {
    it('renders "Release Ready" when release status is 100', async () => {
      render(<SummaryTileReleaseStatus percentage={100} />);
      const status = screen.getByTestId('summary-tile-release-status');
      expect(status).toHaveTextContent('Release Ready');
    });

    it('renders "Partially Annotated" when release status between 0 and 100', async () => {
      render(<SummaryTileReleaseStatus percentage={50} />);
      const status = screen.getByTestId('summary-tile-release-status');
      expect(status).toHaveTextContent('Partially Annotated');
    });

    it('renders "Needs Attention" when release status is 0', async () => {
      render(<SummaryTileReleaseStatus percentage={0} />);
      const status = screen.getByTestId('summary-tile-release-status');
      expect(status).toHaveTextContent('Needs Attention');
    });
  });
});
