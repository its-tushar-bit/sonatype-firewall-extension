/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen } from '@testing-library/dom';

import { render } from 'TestRoot/SpecUtil';
import SummaryTile from 'MainRoot/sbomManager/features/billOfMaterials/summaryTile/SummaryTile';

describe('SummaryTile', () => {
  const vulnerabilitiesSummaryProp = Object.freeze({
    low: 1000,
    medium: 200,
    high: 30,
    critical: 4,
  });

  const componentSummaryProp = Object.freeze({
    direct: 5000,
    transitive: 600,
    unspecified: 78,
  });

  const initialState = {
    billOfMaterialsPage: {
      sbomMetadata: {
        author: [],
        manufacturer: [],
        supplier: [],
        specification: 'CycloneDx',
        specVersion: '2.3',
        fileFormat: 'json',
      },
    },
  };

  it('renders the correct tile content', async () => {
    render(
      <SummaryTile
        componentSummary={componentSummaryProp}
        vulnerabilitiesSummary={vulnerabilitiesSummaryProp}
        annotatedVulnerabilitesPercentage={75}
      />,
      { preloadedState: { ...initialState } }
    );

    expect(screen.getByRole('heading', { name: /Bill of Material/ })).toBeVisible();
    expect(screen.getByRole('heading', { name: /Component Summary/ })).toBeVisible();
    expect(screen.getByRole('heading', { name: /Vulnerabilities Summary/ })).toBeVisible();

    const pieChartTotals = screen.getAllByTestId('pie-chart-total');
    expect(pieChartTotals[0]).toHaveTextContent('5,678');
    expect(pieChartTotals[1]).toHaveTextContent('1,234');

    expect(screen.getByText(/5,000 Direct/)).toBeVisible();
    expect(screen.getByText(/600 Transitive/)).toBeVisible();
    expect(screen.getByText(/78 Unspecified/)).toBeVisible();

    expect(screen.getByText(/1,000 Low/)).toBeVisible();
    expect(screen.getByText(/200 Medium/)).toBeVisible();
    expect(screen.getByText(/30 High/)).toBeVisible();
    expect(screen.getByText(/4 Critical/)).toBeVisible();

    const description = screen.getByTestId('annotated-vulnerabilities-summary-description');
    expect(description).toHaveTextContent('75% of vulnerabilities annotated with exploitability information');
  });

  it('renders the correct description when annotatedVulnerabilitesPercentage is null', async () => {
    render(
      <SummaryTile
        componentSummary={componentSummaryProp}
        vulnerabilitiesSummary={vulnerabilitiesSummaryProp}
        annotatedVulnerabilitesPercentage={null}
      />,
      { preloadedState: { ...initialState } }
    );

    const description = screen.getByTestId('annotated-vulnerabilities-summary-description');
    expect(description).toHaveTextContent('No vulnerabilities to annotate');
  });
});
