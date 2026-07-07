/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';

function renderState(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('AsyncPageState', () => {
  it('renders a loading skeleton when loading is true', () => {
    renderState(<AsyncPageState loading error={null} loadingTestId="async-loading" />);
    expect(screen.getByTestId('async-loading')).toBeInTheDocument();
  });

  it('renders an info card announced as status with a working retry', async () => {
    const onRetry = jest.fn();
    const user = userEvent.setup();
    renderState(
      <AsyncPageState
        loading={false}
        error={null}
        info={{ testId: 'async-info', title: 'Building', message: 'Please wait.' }}
        onRetry={onRetry}
      />,
    );

    const panel = screen.getByTestId('async-info');
    expect(within(panel).getByRole('status')).toBeInTheDocument();
    expect(within(panel).getByText('Building')).toBeInTheDocument();
    await user.click(within(panel).getByRole('button', { name: /retry/i }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders an info banner when infoVariant is banner', () => {
    renderState(
      <AsyncPageState
        loading={false}
        error={null}
        info={{ testId: 'async-info-banner', title: 'Refreshing', message: 'Hold on.' }}
        infoVariant="banner"
      />,
    );

    const banner = screen.getByTestId('async-info-banner');
    expect(banner).toHaveAttribute('role', 'status');
    expect(banner).toHaveTextContent('Refreshing');
  });

  it('renders an error card and invokes onRetry', async () => {
    const onRetry = jest.fn();
    const user = userEvent.setup();
    renderState(
      <AsyncPageState
        loading={false}
        error="Boom"
        errorTestId="async-error"
        errorTitle="Failed"
        onRetry={onRetry}
      />,
    );

    const panel = screen.getByTestId('async-error');
    await user.click(within(panel).getByRole('button', { name: /retry/i }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders an error banner when errorVariant is banner', () => {
    renderState(
      <AsyncPageState
        loading={false}
        error="Stale values shown"
        errorTestId="async-error-banner"
        errorTitle="Refresh failed"
        errorVariant="banner"
      />,
    );

    expect(screen.getByTestId('async-error-banner')).toHaveTextContent(/stale values shown/i);
  });

  it('renders children when not loading, error, or info', () => {
    renderState(
      <AsyncPageState loading={false} error={null}>
        <div data-testid="async-ready">Ready</div>
      </AsyncPageState>,
    );
    expect(screen.getByTestId('async-ready')).toBeInTheDocument();
  });
});
