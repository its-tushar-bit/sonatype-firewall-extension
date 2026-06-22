/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';
import TopConsumingApps from 'MainRoot/usage/TopConsumingApps';

describe('TopConsumingApps', () => {
  function topApps(apps, totalConsumed) {
    return {
      apps,
      totalApps: apps.length,
      totalConsumed: totalConsumed ?? apps.reduce((a, b) => a + b.consumed, 0),
    };
  }

  it('renders nothing when topApps is missing', () => {
    const { container } = render(<TopConsumingApps topApps={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when apps list is empty', () => {
    const { container } = render(<TopConsumingApps topApps={topApps([])} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders app name when present', () => {
    render(
      <TopConsumingApps
        topApps={topApps([{ appId: 'internal-1', publicId: 'pub-1', name: 'My Application', consumed: 100 }])}
      />
    );
    expect(screen.getByText('My Application')).toBeInTheDocument();
  });

  it('falls back to publicId when name is missing', () => {
    render(
      <TopConsumingApps topApps={topApps([{ appId: 'internal-1', publicId: 'pub-1', name: null, consumed: 100 }])} />
    );
    expect(screen.getByText('pub-1')).toBeInTheDocument();
  });

  it('renders "Deleted application" when both name and publicId are null', () => {
    render(
      <TopConsumingApps topApps={topApps([{ appId: 'internal-orphan', publicId: null, name: null, consumed: 42 }])} />
    );
    expect(screen.getByText('Deleted application')).toBeInTheDocument();
  });

  it('shows percent as progress bar width relative to totalConsumed', () => {
    const { container } = render(
      <TopConsumingApps
        topApps={topApps([
          { appId: 'a', publicId: 'a', name: 'A', consumed: 25 },
          { appId: 'b', publicId: 'b', name: 'B', consumed: 75 },
        ])}
      />
    );
    const fills = container.querySelectorAll('.iq-usage-top-apps__bar-fill');
    expect(fills).toHaveLength(2);
    expect(fills[0].style.width).toBe('25%');
    expect(fills[1].style.width).toBe('75%');
  });

  it('caps visible rows at 10 and shows "Show More (N)"', () => {
    const apps = Array.from({ length: 13 }, (_, i) => ({
      appId: `a-${i}`,
      publicId: `pub-${i}`,
      name: `App ${i}`,
      consumed: 20 - i,
    }));
    render(<TopConsumingApps topApps={topApps(apps)} />);

    expect(screen.getByText(/Show More \(3\)/)).toBeInTheDocument();
    // Apps 0–9 visible; 10–12 hidden until expand.
    expect(screen.getByText('App 9')).toBeInTheDocument();
    expect(screen.queryByText('App 10')).not.toBeInTheDocument();
  });

  it('expands to show all rows when "Show More" clicked', async () => {
    const user = userEvent.setup();
    const apps = Array.from({ length: 13 }, (_, i) => ({
      appId: `a-${i}`,
      publicId: `pub-${i}`,
      name: `App ${i}`,
      consumed: 20 - i,
    }));
    render(<TopConsumingApps topApps={topApps(apps)} />);

    await user.click(screen.getByText(/Show More/));

    expect(screen.getByText('App 12')).toBeInTheDocument();
    expect(screen.getByText(/Show Less/)).toBeInTheDocument();
  });

  const topAppsFixture = {
    totalApps: 3,
    totalConsumed: 1000,
    apps: [
      { appId: '1', publicId: 'web-frontend', name: 'web-frontend', consumed: 600 },
      { appId: '2', publicId: 'api-service', name: 'api-service', consumed: 300 },
      { appId: '3', publicId: 'mobile-app', name: 'mobile-app', consumed: 100 },
    ],
  };

  it('renders a percentage element below the bar for each row', () => {
    const { container } = render(<TopConsumingApps topApps={topAppsFixture} />);
    const percents = container.querySelectorAll('.iq-usage-top-apps__percent');
    // First app: 600/1000 = 60%, second: 300/1000 = 30%, third: 100/1000 = 10%
    expect(percents).toHaveLength(3);
    expect(percents[0]).toHaveTextContent('60%');
    expect(percents[1]).toHaveTextContent('30%');
    expect(percents[2]).toHaveTextContent('10%');
  });

  it('renders a progress bar per row with width proportional to consumed/totalConsumed', () => {
    const { container } = render(<TopConsumingApps topApps={topAppsFixture} />);
    const fills = container.querySelectorAll('.iq-usage-top-apps__bar-fill');
    expect(fills).toHaveLength(3);
    // First app: 600/1000 = 60%
    expect(fills[0].style.width).toBe('60%');
    expect(fills[1].style.width).toBe('30%');
    expect(fills[2].style.width).toBe('10%');
  });

  it('exposes each progress bar as an accessible progressbar element', () => {
    render(<TopConsumingApps topApps={topAppsFixture} />);
    const bars = screen.getAllByRole('progressbar');
    expect(bars).toHaveLength(3);
    expect(bars[0]).toHaveAttribute('aria-valuenow', '60');
    expect(bars[0]).toHaveAttribute('aria-valuemax', '100');
  });

  it('renders app name and count together in the row-top div', () => {
    const { container } = render(<TopConsumingApps topApps={topAppsFixture} />);
    const rowTops = container.querySelectorAll('.iq-usage-top-apps__row-top');
    expect(rowTops).toHaveLength(3);
    // Each row-top contains both the name and the count
    expect(rowTops[0].querySelector('.iq-usage-top-apps__name')).toHaveTextContent('web-frontend');
    expect(rowTops[0].querySelector('.iq-usage-top-apps__count')).toHaveTextContent('600');
  });

  it('clamps aria-valuenow at 100 when consumed exceeds totalConsumed', () => {
    // Regression guard for the clamp added against a malformed-progressbar bot warning
    // (CLM-40967). Without the clamp, raw consumed/totalConsumed > 1 produces
    // aria-valuenow > aria-valuemax, which violates WAI-ARIA 1.2 and confuses screen readers.
    render(<TopConsumingApps topApps={topApps([{ appId: '1', publicId: 'a', name: 'A', consumed: 150 }], 100)} />);
    const [bar] = screen.getAllByRole('progressbar');
    expect(bar).toHaveAttribute('aria-valuenow', '100');
    expect(Number(bar.getAttribute('aria-valuenow'))).toBeLessThanOrEqual(Number(bar.getAttribute('aria-valuemax')));
  });
});
