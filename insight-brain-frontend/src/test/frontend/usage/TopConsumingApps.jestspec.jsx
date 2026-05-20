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

  it('shows percent relative to totalConsumed', () => {
    render(
      <TopConsumingApps
        topApps={topApps([
          { appId: 'a', publicId: 'a', name: 'A', consumed: 25 },
          { appId: 'b', publicId: 'b', name: 'B', consumed: 75 },
        ])}
      />
    );
    expect(screen.getByText('25%')).toBeInTheDocument();
    expect(screen.getByText('75%')).toBeInTheDocument();
  });

  it('caps visible rows at 5 and shows "Show More (N)"', () => {
    const apps = Array.from({ length: 8 }, (_, i) => ({
      appId: `a-${i}`,
      publicId: `pub-${i}`,
      name: `App ${i}`,
      consumed: 10 - i,
    }));
    render(<TopConsumingApps topApps={topApps(apps)} />);

    expect(screen.getByText(/Show More \(3\)/)).toBeInTheDocument();
    expect(screen.queryByText('App 7')).not.toBeInTheDocument();
  });

  it('expands to show all rows when "Show More" clicked', async () => {
    const user = userEvent.setup();
    const apps = Array.from({ length: 8 }, (_, i) => ({
      appId: `a-${i}`,
      publicId: `pub-${i}`,
      name: `App ${i}`,
      consumed: 10 - i,
    }));
    render(<TopConsumingApps topApps={topApps(apps)} />);

    await user.click(screen.getByText(/Show More/));

    expect(screen.getByText('App 7')).toBeInTheDocument();
    expect(screen.getByText(/Show Less/)).toBeInTheDocument();
  });
});
