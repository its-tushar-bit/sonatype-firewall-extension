/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';
import UsageTabs from 'MainRoot/usage/UsageTabs';

describe('UsageTabs', () => {
  const defaultPreloadedState = {
    usage: {
      activeTab: 'overview',
    },
  };

  it('renders an Overview tab and a Trends tab', () => {
    render(<UsageTabs overview={<div>OV</div>} trends={<div>TR</div>} />, {
      preloadedState: defaultPreloadedState,
    });

    expect(screen.getByRole('tab', { name: /overview/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /trends/i })).toBeInTheDocument();
  });

  it('shows the active tab content (Overview by default)', () => {
    render(<UsageTabs overview={<div>OV</div>} trends={<div>TR</div>} />, {
      preloadedState: defaultPreloadedState,
    });

    expect(screen.getByText('OV')).toBeInTheDocument();
    expect(screen.queryByText('TR')).not.toBeInTheDocument();
  });

  it('switches via setActiveTab dispatch when Trends clicked', async () => {
    const user = userEvent.setup();
    const { store } = render(<UsageTabs overview={<div>OV</div>} trends={<div>TR</div>} />, {
      preloadedState: defaultPreloadedState,
    });

    await user.click(screen.getByRole('tab', { name: /trends/i }));

    expect(store.getState().usage.activeTab).toBe('trends');
  });
});
