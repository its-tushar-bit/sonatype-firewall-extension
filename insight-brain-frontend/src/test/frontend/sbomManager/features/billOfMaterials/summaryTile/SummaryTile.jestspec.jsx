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

  const policyViolationSummaryProp = Object.freeze({
    critical: 4444,
    severe: 3333,
    moderate: 2222,
    low: 1111,
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

  it('renders the correct tile content when sbom policies supported is true', async () => {
    render(
      <SummaryTile
        componentSummary={componentSummaryProp}
        vulnerabilitiesSummary={vulnerabilitiesSummaryProp}
        policyViolationSummary={policyViolationSummaryProp}
        isSbomPoliciesSupported={true}
        releaseStatusPercentage={75}
      />,
      { preloadedState: { ...initialState } }
    );

    expect(screen.getByRole('heading', { name: /Bill of Material/ })).toBeVisible();
    expect(screen.getByRole('heading', { name: /Component Summary/ })).toBeVisible();
    expect(screen.getByRole('heading', { name: /Vulnerabilities Summary/ })).toBeVisible();
    expect(screen.getByRole('heading', { name: /Policy Violation Summary/ })).toBeVisible();

    const pieChartTotals = screen.getAllByTestId('pie-chart-total');
    expect(pieChartTotals[0]).toHaveTextContent('5,678');
    expect(pieChartTotals[1]).toHaveTextContent('1,234');
    expect(pieChartTotals[2]).toHaveTextContent('75%');
    expect(pieChartTotals[3]).toHaveTextContent('11,110');

    expect(screen.getByText(/5,000 Direct/)).toBeVisible();
    expect(screen.getByText(/600 Transitive/)).toBeVisible();
    expect(screen.getByText(/78 Unspecified/)).toBeVisible();

    expect(screen.getByText(/1,111 Low/)).toBeVisible();
    expect(screen.getByText(/2,222 Moderate/)).toBeVisible();
    expect(screen.getByText(/3,333 Severe/)).toBeVisible();
    expect(screen.getByText(/4,444 Critical/)).toBeVisible();

    const description = screen.getByTestId('summary-tile-release-status-description');
    expect(description).toHaveTextContent(
      '75% of critical and high vulnerabilities have been annotated with exploitability information'
    );
  });

  it('renders the correct tile content when sbom policies supported is false', async () => {
    render(
      <SummaryTile
        componentSummary={componentSummaryProp}
        vulnerabilitiesSummary={vulnerabilitiesSummaryProp}
        policyViolationSummary={policyViolationSummaryProp}
        isSbomPoliciesSupported={false}
        releaseStatusPercentage={75}
      />,
      { preloadedState: { ...initialState } }
    );

    expect(screen.getByRole('heading', { name: /Bill of Material/ })).toBeVisible();
    expect(screen.getByRole('heading', { name: /Component Summary/ })).toBeVisible();
    expect(screen.getByRole('heading', { name: /Vulnerabilities Summary/ })).toBeVisible();
    expect(screen.queryByRole('heading', { name: /Policy Violation Summary/ })).not.toBeInTheDocument();

    const pieChartTotals = screen.getAllByTestId('pie-chart-total');
    expect(pieChartTotals[0]).toHaveTextContent('5,678');
    expect(pieChartTotals[1]).toHaveTextContent('1,234');
    expect(pieChartTotals[2]).toHaveTextContent('75%');

    expect(screen.getByText(/5,000 Direct/)).toBeVisible();
    expect(screen.getByText(/600 Transitive/)).toBeVisible();
    expect(screen.getByText(/78 Unspecified/)).toBeVisible();

    expect(screen.getByText(/1,000 Low/)).toBeVisible();
    expect(screen.getByText(/200 Medium/)).toBeVisible();
    expect(screen.getByText(/30 High/)).toBeVisible();
    expect(screen.getByText(/4 Critical/)).toBeVisible();

    expect(screen.queryByText(/1,111 Low/)).not.toBeInTheDocument();
    expect(screen.queryByText(/2,222 Moderate/)).not.toBeInTheDocument();
    expect(screen.queryByText(/3,333 Severe/)).not.toBeInTheDocument();
    expect(screen.queryByText(/4,444 Critical/)).not.toBeInTheDocument();

    const description = screen.getByTestId('summary-tile-release-status-description');
    expect(description).toHaveTextContent(
      '75% of critical and high vulnerabilities have been annotated with exploitability information'
    );
  });

  it('renders the correct description when releaseStatusPercentage is null', async () => {
    render(
      <SummaryTile
        componentSummary={componentSummaryProp}
        vulnerabilitiesSummary={vulnerabilitiesSummaryProp}
        policyViolationSummary={policyViolationSummaryProp}
        isSbomPoliciesSupported={true}
        releaseStatusPercentage={null}
      />,
      { preloadedState: { ...initialState } }
    );

    const status = screen.getByTestId('summary-tile-release-status');
    expect(status).toHaveTextContent('Needs Attention');
  });
});
