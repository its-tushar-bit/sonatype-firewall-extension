/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DashboardTile } from 'MainRoot/nosc/dashboard/DashboardTile';

describe('DashboardTile', () => {
  const renderTile = (props: Partial<React.ComponentProps<typeof DashboardTile>> = {}) =>
    render(
      <Theme>
        <DashboardTile title="Apps Scanned" status="ready" onRetry={() => undefined} {...props}>
          <div>tile body</div>
        </DashboardTile>
      </Theme>,
    );

  it('renders the title in a heading', () => {
    renderTile();
    expect(screen.getByRole('heading', { name: /apps scanned/i })).toBeInTheDocument();
  });

  it('renders the body when status is ready', () => {
    renderTile();
    expect(screen.getByText('tile body')).toBeInTheDocument();
  });

  it('renders a skeleton (not the body) when status is loading', () => {
    renderTile({ status: 'loading' });
    expect(screen.queryByText('tile body')).not.toBeInTheDocument();
    expect(screen.getByTestId('dashboard-tile-skeleton')).toBeInTheDocument();
  });

  it('renders an error message and a Retry button when status is error', async () => {
    const onRetry = jest.fn();
    renderTile({ status: 'error', errorMessage: 'Failed to load', onRetry });

    expect(screen.queryByText('tile body')).not.toBeInTheDocument();
    expect(screen.getByText(/failed to load/i)).toBeInTheDocument();

    const retryButton = screen.getByRole('button', { name: /retry/i });
    await userEvent.click(retryButton);
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('isolates rendering: one tile per region (role=region with aria-labelledby)', () => {
    renderTile();
    const region = screen.getByRole('region');
    expect(region).toHaveAttribute('aria-labelledby');
  });
});
