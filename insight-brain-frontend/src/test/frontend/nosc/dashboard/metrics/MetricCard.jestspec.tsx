/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MetricCard } from 'MainRoot/nosc/dashboard/metrics/MetricCard';

function renderCard(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('MetricCard (CLM-40905 AT-F16: reusable metric card)', () => {
  it('renders the title as a level-2 heading and the hero value as text', () => {
    renderCard(<MetricCard title="Applications" value={1234} testId="metric-card-applications" />);

    const heading = screen.getByRole('heading', { level: 2, name: 'Applications' });
    expect(heading).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('1,234');
    expect(screen.queryByRole('heading', { name: '1,234' })).not.toBeInTheDocument();
  });

  it('renders a zero total honestly (not blank, not a dash)', () => {
    renderCard(<MetricCard title="Applications" value={0} testId="metric-card-applications" />);
    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('0');
  });

  it('renders sub-metrics as a list with a text label AND value (severity not by color alone)', () => {
    renderCard(
      <MetricCard
        title="Policy Violations"
        value={7}
        testId="metric-card-violations"
        subMetrics={[
          { label: 'Critical', value: 1, tone: 'critical' },
          { label: 'Severe', value: 2, tone: 'severe' },
          { label: 'Moderate', value: 3, tone: 'moderate' },
          { label: 'Low', value: 1, tone: 'low' },
        ]}
      />,
    );

    const breakdown = screen.getByTestId('metric-card-violations-breakdown');
    expect(breakdown.tagName).toBe('UL');
    expect(within(breakdown).getByText('Critical')).toBeInTheDocument();
    expect(within(breakdown).getByText('Severe')).toBeInTheDocument();
    expect(within(breakdown).getByText('Moderate')).toBeInTheDocument();
    expect(within(breakdown).getByText('Low')).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-violations-sub-critical-value')).toHaveTextContent('1');
  });

  it('renders a hero click-through link with an explicit aria-label (title + total only)', () => {
    renderCard(
      <MetricCard title="Applications" value={5} href="#/dashboard/applications" testId="metric-card-applications" />,
    );

    const link = screen.getByRole('link', { name: 'Applications, 5 total, open list' });
    expect(link).toHaveAttribute('href', '#/dashboard/applications');
  });

  it('is keyboard focusable when interactive', async () => {
    const user = userEvent.setup();
    renderCard(
      <MetricCard title="Waivers" value={3} href="#/dashboard/waivers" testId="metric-card-waivers" />,
    );

    await user.tab();
    expect(screen.getByRole('link', { name: 'Waivers, 3 total, open list' })).toHaveFocus();
  });

  it('renders a skeleton (no value) while loading', () => {
    renderCard(<MetricCard title="Applications" loading testId="metric-card-applications" />);

    expect(screen.getByTestId('metric-card-applications-skeleton')).toBeInTheDocument();
    expect(screen.queryByTestId('metric-card-applications-value')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Applications' })).toBeInTheDocument();
  });
});
