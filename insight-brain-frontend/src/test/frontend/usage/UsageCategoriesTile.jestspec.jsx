/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import UsageCategoriesTile from 'MainRoot/usage/UsageCategoriesTile';

describe('UsageCategoriesTile', () => {
  const baseSummary = {
    activityBreakdown: {
      'App Scan + Re-evaluate': 4000,
      'Continuous Monitoring': 1500,
      'Component Details': 800,
      'Version Recommendations': 600,
      'Reachability Analysis': 200,
      APIs: 134,
    },
  };

  it('renders the tile header "Usage Categories"', () => {
    render(<UsageCategoriesTile summary={baseSummary} />);
    expect(screen.getByText('Usage Categories')).toBeInTheDocument();
  });

  it('renders all 6 category tiles with labels and counts', () => {
    render(<UsageCategoriesTile summary={baseSummary} />);
    expect(screen.getByText('App Scan + Re-evaluate')).toBeInTheDocument();
    expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
    expect(screen.getByText('Component Details')).toBeInTheDocument();
    expect(screen.getByText('Version Recommendations')).toBeInTheDocument();
    expect(screen.getByText('Reachability Analysis')).toBeInTheDocument();
    expect(screen.getByText('APIs')).toBeInTheDocument();
    expect(screen.getByText('4,000')).toBeInTheDocument();
    expect(screen.getByText('1,500')).toBeInTheDocument();
    expect(screen.getByText('134')).toBeInTheDocument();
  });

  it('skips categories with zero count', () => {
    const summary = {
      activityBreakdown: { 'App Scan + Re-evaluate': 100, 'Continuous Monitoring': 0, APIs: 50 },
    };
    render(<UsageCategoriesTile summary={summary} />);
    expect(screen.getByText('App Scan + Re-evaluate')).toBeInTheDocument();
    expect(screen.queryByText('Continuous Monitoring')).not.toBeInTheDocument();
    expect(screen.getByText('APIs')).toBeInTheDocument();
  });

  it('renders nothing when summary is null', () => {
    const { container } = render(<UsageCategoriesTile summary={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when activityBreakdown is missing', () => {
    const { container } = render(<UsageCategoriesTile summary={{ consumed: 100 }} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders categories in CATEGORY_ORDER (APIs first per Figma)', () => {
    const { container } = render(<UsageCategoriesTile summary={baseSummary} />);
    const labels = Array.from(container.querySelectorAll('.iq-usage-categories-tile__category-label')).map(
      (el) => el.textContent
    );
    expect(labels[0]).toBe('APIs');
    expect(labels).toEqual([
      'APIs',
      'App Scan + Re-evaluate',
      'Component Details',
      'Continuous Monitoring',
      'Reachability Analysis',
      'Version Recommendations',
    ]);
  });
});
