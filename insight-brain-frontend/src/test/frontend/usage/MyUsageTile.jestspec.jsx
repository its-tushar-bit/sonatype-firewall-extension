/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import MyUsageTile from 'MainRoot/usage/MyUsageTile';

const baseSummary = {
  consumed: 7234,
  limit: 10000,
  percentUsed: 72.34,
  remaining: 2766,
  resetDate: '2026-07-01',
  billingWindowStart: '2026-06-01',
  activityBreakdown: {
    'App Scan + Re-evaluate': 4000,
    'Continuous Monitoring': 1500,
    'Component Details': 800,
    'Version Recommendations': 600,
    'Reachability Analysis': 200,
    APIs: 134,
  },
};

it('renders header "My usage"', () => {
  // Lower-case "usage" matches the Mateo Figma mockup; the previous "My Usage" was a draft header.
  render(<MyUsageTile summary={baseSummary} />);
  expect(screen.getByText('My usage')).toBeInTheDocument();
});

it('renders consumed/limit copy in 7,234 / 10,000 format', () => {
  render(<MyUsageTile summary={baseSummary} />);
  expect(screen.getByText('7,234')).toBeInTheDocument();
  expect(screen.getByText(/10,000/)).toBeInTheDocument();
});

it('renders an accessible progress bar with rounded percent', () => {
  render(<MyUsageTile summary={baseSummary} />);
  const bar = screen.getByRole('progressbar');
  expect(bar).toHaveAttribute('aria-valuenow', '72');
  expect(bar).toHaveAttribute('aria-valuemax', '100');
});

it('does NOT render Usage Categories (extracted to UsageCategoriesTile component)', () => {
  // The categories grid was moved into a sibling UsageCategoriesTile per Figma — verify
  // it no longer renders inside MyUsageTile to prevent duplicate rendering on the page.
  render(<MyUsageTile summary={baseSummary} />);
  expect(screen.queryByText('App Scan + Re-evaluate')).not.toBeInTheDocument();
  expect(screen.queryByText('APIs')).not.toBeInTheDocument();
});

it('renders nothing when summary is null', () => {
  const { container } = render(<MyUsageTile summary={null} />);
  expect(container).toBeEmptyDOMElement();
});

it('renders <UsagePeriodFilter /> in the tile header actions slot', () => {
  // Figma annotation #2: the page-level period filter lives in the My Usage
  // tile header, top-right. "Period:" is a static label outside the dropdown
  // button; the dropdown toggle shows only the range label. Assert each
  // independently — getByText cannot span separate DOM elements.
  render(<MyUsageTile summary={baseSummary} />);
  expect(screen.getByText('Period:')).toBeInTheDocument();
  expect(document.querySelector('.nx-dropdown__toggle')).toHaveTextContent('Current billing period');
});

it('renders em-dash when percentUsed is null but limit is set', () => {
  const summary = { consumed: 100, limit: 1000, percentUsed: null, remaining: 900, activityBreakdown: {} };
  render(<MyUsageTile summary={summary} />);
  expect(screen.getByText('—')).toBeInTheDocument();
});

it('announces "Over limit" via aria-valuetext when percentUsed is null but consumed exceeds limit', () => {
  // Defensive path: backend should always supply percentUsed when limit is set, but if it
  // omits it while consumed > limit, aria-valuetext must still announce the over-limit state
  // (otherwise aria-valuenow = 0 and screen readers announce "0%" — the opposite of intent).
  const summary = { consumed: 1100, limit: 1000, percentUsed: null, remaining: 0, activityBreakdown: {} };
  render(<MyUsageTile summary={summary} />);
  const bar = screen.getByRole('progressbar');
  expect(bar).toHaveAttribute('aria-valuetext', 'Over limit');
});

it('renders "Limit reached" when at exactly 100% (not strictly over)', () => {
  const summary = { consumed: 1000, limit: 1000, percentUsed: 100, remaining: 0, activityBreakdown: {} };
  render(<MyUsageTile summary={summary} />);
  expect(screen.getByText('Limit reached')).toBeInTheDocument();
  expect(screen.queryByText(/Over limit by/)).not.toBeInTheDocument();
});

it('renders "Over limit by N" when consumed strictly exceeds limit', () => {
  const summary = { consumed: 1100, limit: 1000, percentUsed: 110, remaining: 0, activityBreakdown: {} };
  render(<MyUsageTile summary={summary} />);
  expect(screen.getByText('Over limit by 100')).toBeInTheDocument();
});

it('applies the --over modifier on the progress fill when strictly over limit', () => {
  // Visual regression guard: the bar's red over-limit color comes from the
  // --over modifier class, not the "Over limit by N" text.
  const summary = { consumed: 1100, limit: 1000, percentUsed: 110, remaining: 0, activityBreakdown: {} };
  const { container } = render(<MyUsageTile summary={summary} />);
  expect(container.querySelector('.iq-my-usage-tile__progress-fill--over')).toBeInTheDocument();
  expect(container.querySelector('.iq-my-usage-tile__progress-track--over')).toBeInTheDocument();
});

it('does NOT apply the --over modifier at exactly 100% (limit reached, not over)', () => {
  // The progress-track--over modifier hits at >= 100 but progress-fill--over
  // requires strictly over. This test guards the boundary.
  const summary = { consumed: 1000, limit: 1000, percentUsed: 100, remaining: 0, activityBreakdown: {} };
  const { container } = render(<MyUsageTile summary={summary} />);
  expect(container.querySelector('.iq-my-usage-tile__progress-fill--over')).toBeNull();
});

it('renders no progress bar when summary has no limit', () => {
  const summary = { consumed: 100, limit: null, percentUsed: null, remaining: null, activityBreakdown: {} };
  const { container } = render(<MyUsageTile summary={summary} />);
  expect(container.querySelector('[role="progressbar"]')).toBeNull();
});

it('renders "Resets on …" with the resetDate formatted alongside remaining', () => {
  // Regression guard: the PR description promised "'Resets on …' / 'Over limit by …'
  // details below" but resetDate was silently dropped in the initial cut.
  // The field reaches the tile via summary.resetDate; render it via moment.
  render(<MyUsageTile summary={baseSummary} />);
  expect(screen.getByText(/Resets on Jul 1, 2026/)).toBeInTheDocument();
});

it('omits "Resets on …" when resetDate is missing', () => {
  const summary = { consumed: 100, limit: 1000, percentUsed: 10, remaining: 900, activityBreakdown: {} };
  render(<MyUsageTile summary={summary} />);
  expect(screen.queryByText(/Resets on/)).not.toBeInTheDocument();
});
