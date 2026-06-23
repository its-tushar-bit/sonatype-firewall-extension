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

  it('renders zero-count categories as "0" (layout stability across period filters)', () => {
    // Regression guard: when a custom period filter yields no consumption for
    // some or all categories, the tile must still render all 6 — empty values
    // appear as "0", not as a vanished tile. The previous behavior of hiding
    // zero-count categories read as a broken page rather than honest empty
    // data when the user selected a historical window with no activity.
    const summary = {
      activityBreakdown: { 'App Scan + Re-evaluate': 100, 'Continuous Monitoring': 0, APIs: 50 },
    };
    render(<UsageCategoriesTile summary={summary} />);
    expect(screen.getByText('App Scan + Re-evaluate')).toBeInTheDocument();
    expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
    expect(screen.getByText('APIs')).toBeInTheDocument();
    // Categories with no entry in activityBreakdown also surface as "0"
    expect(screen.getByText('Component Details')).toBeInTheDocument();
    // Sanity: exactly 6 listitems
    expect(screen.getAllByRole('listitem')).toHaveLength(6);
  });

  it('renders all 6 categories at 0 when activityBreakdown is empty', () => {
    // Pathological-but-realistic case: period filter selects a window with
    // truly zero consumption across all activity types. Tile still renders
    // the layout with 6 zeros so the page hierarchy is preserved.
    render(<UsageCategoriesTile summary={{ activityBreakdown: {} }} />);
    expect(screen.getAllByRole('listitem')).toHaveLength(6);
    expect(screen.getAllByText('0')).toHaveLength(6);
  });

  it('renders nothing when summary is null (no data loaded yet)', () => {
    const { container } = render(<UsageCategoriesTile summary={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders all 6 categories at 0 when summary has no activityBreakdown field', () => {
    // Tolerates summary objects from older API responses that omit the
    // activityBreakdown field — same layout, every count is 0.
    render(<UsageCategoriesTile summary={{ consumed: 100 }} />);
    expect(screen.getAllByRole('listitem')).toHaveLength(6);
    expect(screen.getAllByText('0')).toHaveLength(6);
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
