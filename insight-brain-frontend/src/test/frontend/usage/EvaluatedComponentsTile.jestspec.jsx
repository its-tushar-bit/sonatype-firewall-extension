/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import EvaluatedComponentsTile from 'MainRoot/usage/EvaluatedComponentsTile';

describe('EvaluatedComponentsTile', () => {
  it('renders nothing when dailyHistory is null', () => {
    const { container } = render(<EvaluatedComponentsTile dailyHistory={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when entries, dailyAverage, and peakDay are all empty', () => {
    const { container } = render(
      <EvaluatedComponentsTile
        dailyHistory={{
          dailyHistory: [],
          dailyAverage: 0,
          peakDay: { count: 0, date: null },
        }}
      />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders when only dailyAverage is present (regression guard for empty-state logic)', () => {
    render(
      <EvaluatedComponentsTile
        dailyHistory={{
          dailyHistory: [],
          dailyAverage: 123,
          peakDay: null,
        }}
      />
    );
    expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    expect(screen.getByText('123')).toBeInTheDocument();
  });

  it('renders formatted dailyAverage and peakDay count with thousands separators', () => {
    render(
      <EvaluatedComponentsTile
        dailyHistory={{
          dailyHistory: [{ date: '2026-04-15', components: 1500, componentsCumulative: 1500 }],
          dailyAverage: 1234,
          peakDay: { count: 5678, date: '2026-04-15' },
        }}
      />
    );
    expect(screen.getByText('1,234')).toBeInTheDocument();
    expect(screen.getByText('5,678')).toBeInTheDocument();
  });

  it('renders peak date label when peakDay.date is present', () => {
    render(
      <EvaluatedComponentsTile
        dailyHistory={{
          dailyHistory: [{ date: '2026-04-15', components: 1, componentsCumulative: 1 }],
          dailyAverage: 100,
          peakDay: { count: 5000, date: '2026-04-15' },
        }}
      />
    );
    expect(screen.getByText('Apr 15')).toBeInTheDocument();
  });

  it('does not render peak date span when peakDay.date is missing', () => {
    render(
      <EvaluatedComponentsTile
        dailyHistory={{
          dailyHistory: [],
          dailyAverage: 100,
          peakDay: { count: 5000, date: null },
        }}
      />
    );
    // Peak count still rendered as '5,000', but no date string like 'Apr 15'
    expect(screen.getByText('5,000')).toBeInTheDocument();
    expect(screen.queryByText(/^[A-Z][a-z]{2} \d+$/)).not.toBeInTheDocument();
  });

  it('renders chart section title when entries are present', () => {
    render(
      <EvaluatedComponentsTile
        dailyHistory={{
          dailyHistory: [{ date: '2026-04-15', components: 1, componentsCumulative: 1 }],
          dailyAverage: 100,
          peakDay: { count: 500, date: '2026-04-15' },
        }}
      />
    );
    expect(screen.getByText('Usage Trend')).toBeInTheDocument();
  });

  it('does not render chart section when entries is empty', () => {
    render(
      <EvaluatedComponentsTile
        dailyHistory={{
          dailyHistory: [],
          dailyAverage: 100,
          peakDay: { count: 500, date: '2026-04-15' },
        }}
      />
    );
    expect(screen.queryByText('Usage Trend')).not.toBeInTheDocument();
  });
});
