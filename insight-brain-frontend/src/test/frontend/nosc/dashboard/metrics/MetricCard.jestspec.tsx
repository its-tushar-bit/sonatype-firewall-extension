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
  it('renders the title as a heading and the hero value', () => {
    renderCard(<MetricCard title="Applications" value={1234} testId="metric-card-applications" />);

    expect(screen.getByRole('heading', { level: 2, name: 'Applications' })).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('1,234');
  });

  it('renders a zero total honestly (not blank, not a dash)', () => {
    renderCard(<MetricCard title="Applications" value={0} testId="metric-card-applications" />);
    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('0');
  });

  it('renders an accessible unavailable state with filter dimensions and no metric number', () => {
    renderCard(
      <MetricCard
        title="Applications"
        unavailableDimensions={['stageIds', 'tagIds']}
        href="#/dashboard/applications"
        testId="metric-card-applications"
      />
    );

    expect(screen.getByRole('status')).toHaveTextContent('Unavailable for Stage and Tag filters');
    expect(screen.queryByTestId('metric-card-applications-value')).not.toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('renders METRIC_UNAVAILABLE as temporarily unavailable without falling back to 0', () => {
    renderCard(
      <MetricCard
        title="Applications"
        metricUnavailable
        href="#/dashboard/applications"
        testId="metric-card-applications"
      />
    );

    expect(screen.getByRole('status')).toHaveTextContent('Temporarily unavailable');
    expect(screen.queryByTestId('metric-card-applications-value')).not.toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('renders sub-metrics with a text label AND value (severity not by color alone)', () => {
    renderCard(
      <MetricCard
        title="Violations"
        value={7}
        testId="metric-card-violations"
        subMetrics={[
          { label: 'Critical', value: 1, tone: 'critical' },
          { label: 'Severe', value: 2, tone: 'severe' },
          { label: 'Moderate', value: 3, tone: 'moderate' },
          { label: 'Low', value: 1, tone: 'low' },
        ]}
      />
    );

    const breakdown = screen.getByTestId('metric-card-violations-breakdown');
    // Severity breakdown stays a semantic list so screen readers announce it as
    // a traversable "list of N items" (WCAG; AT-F16 / #16359 a11y contract).
    expect(breakdown.tagName).toBe('UL');
    expect(within(breakdown).getAllByRole('listitem')).toHaveLength(4);
    expect(within(breakdown).getByText('Critical')).toBeInTheDocument();
    expect(within(breakdown).getByText('Severe')).toBeInTheDocument();
    expect(within(breakdown).getByText('Moderate')).toBeInTheDocument();
    expect(within(breakdown).getByText('Low')).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-violations-sub-critical-value')).toHaveTextContent('1');
  });

  it('renders a whole-tile click-through link whose accessible name includes the title', () => {
    renderCard(
      <MetricCard title="Applications" value={5} href="#/dashboard/applications" testId="metric-card-applications" />
    );

    const link = screen.getByRole('link', { name: /Applications/ });
    expect(link).toHaveAttribute('href', '#/dashboard/applications');
  });

  it('is keyboard focusable with a visible focus target when interactive', async () => {
    const user = userEvent.setup();
    renderCard(<MetricCard title="Waivers" value={3} href="#/dashboard/waivers" testId="metric-card-waivers" />);

    await user.tab();
    expect(screen.getByRole('link', { name: /Waivers/ })).toHaveFocus();
  });

  it('renders a skeleton (no value) while loading', () => {
    renderCard(<MetricCard title="Applications" loading testId="metric-card-applications" />);

    expect(screen.getByTestId('metric-card-applications-skeleton')).toBeInTheDocument();
    expect(screen.queryByTestId('metric-card-applications-value')).not.toBeInTheDocument();
    // Chrome (the title) still renders immediately.
    expect(screen.getByRole('heading', { level: 2, name: 'Applications' })).toBeInTheDocument();
  });

  it('renders a dual-hero layout for Legal and Orgs cards (CLM-40937)', () => {
    renderCard(
      <MetricCard
        title="Legal Obligations"
        testId="metric-card-legal"
        dualHero={[
          { value: 5, label: 'Applications' },
          { value: 12, label: 'Components' },
        ]}
      />
    );

    expect(screen.queryByTestId('metric-card-legal-value')).not.toBeInTheDocument();
    expect(screen.getByTestId('metric-card-legal-dual-applications-value')).toHaveTextContent('5');
    expect(screen.getByTestId('metric-card-legal-dual-components-value')).toHaveTextContent('12');
  });

  it('renders a secondary stat row below the hero (Applications stages, Components related violations)', () => {
    renderCard(
      <MetricCard
        title="Applications"
        value={42}
        testId="metric-card-applications"
        secondaryStat={{ value: 5, label: 'Stages' }}
      />
    );

    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42');
    expect(screen.getByTestId('metric-card-applications-secondary-value')).toHaveTextContent('5');
    expect(screen.getByText('Stages')).toBeInTheDocument();
  });

  it('renders applications secondary stages as a chip when presentation is chip', () => {
    renderCard(
      <MetricCard
        title="Applications"
        value={248}
        secondaryStat={{ value: 6, label: 'Stages' }}
        secondaryStatPresentation="chip"
        railTone="apps"
        testId="metric-card-applications"
      />
    );
    expect(screen.getByTestId('metric-card-applications-stages-chip')).toHaveTextContent('6 Stages');
  });
});
