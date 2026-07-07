/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import { MetricCardGrid } from 'MainRoot/nosc/dashboard/metrics/MetricCardGrid';
import { setupNexusOneBundleLocation } from 'TestRoot/nosc/dashboard/dashboardTestHrefs';
import { getDashboardMetricsUrl } from 'MainRoot/util/CLMLocation';

/**
 * CLM-40905: end-to-end IA wiring for the metric-card landing grid.
 *
 * The overview route (`nexusOneDashboard.overview`) renders {@link MetricCardGrid},
 * not the legacy {@code DashboardOverviewContent} tile grid. This spec is the
 * integration check that each card's click-through lands on the correct dashboard
 * tab URL when the cards are composed into the landing the user actually sees.
 */

const METRICS_BODY = {
  applications: { total: 42, breakdown: null, source: 'index' },
  violations: {
    total: 9,
    breakdown: { critical: 2, severe: 3, moderate: 3, low: 1 },
    source: 'index',
  },
  waivers: { total: 5, breakdown: { existing: 4, requested: 1 }, source: 'sql' },
  components: { total: 137, breakdown: null, source: 'index' },
  lastUpdatedAt: 1_700_000_000_000,
};

describe('Metric card → tab IA wire-up (CLM-40905)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupNexusOneBundleLocation();
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, METRICS_BODY);
  });

  afterEach(() => {
    axiosMock.reset();
  });

  function renderGrid() {
    return render(
      <Theme>
        <MetricCardGrid />
      </Theme>,
      {
        preloadedState: {
          dashboardFilter: {
            appliedFilter: {
              organizations: new Set<string>(),
              applications: new Set<string>(),
              stages: new Set<string>(),
              categories: new Set<string>(),
            },
          },
        } as any,
      },
    );
  }

  it.each([
    ['Applications', 42, '#/dashboard/applications'],
    ['Policy Violations', 9, '#/dashboard/violations'],
    ['Waivers', 5, '#/dashboard/waivers'],
    ['Components', 137, '#/dashboard/components'],
  ])('card "%s" click-through → %s', async (cardName, total, expectedHref) => {
    renderGrid();
    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());
    expect(
      screen.getByRole('link', { name: `${cardName}, ${total.toLocaleString()} total, open list` }),
    ).toHaveAttribute('href', expectedHref);
  });

  it('does not pre-filter the destination tab by severity — facet drill-down deferred', async () => {
    renderGrid();
    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());

    const link = screen.getByRole('link', { name: 'Policy Violations, 9 total, open list' });
    expect(link).toHaveAttribute('href', '#/dashboard/violations');
    expect(link.getAttribute('href')).not.toContain('severity=');
  });
});
