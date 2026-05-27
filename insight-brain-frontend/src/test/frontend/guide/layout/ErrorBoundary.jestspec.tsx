/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from '../test-utils';
import userEvent from '@testing-library/user-event';
import { ErrorBoundary } from 'GuideRoot/layout/ErrorBoundary';

// Suppress React's console.error for expected boundary errors
let consoleErrorSpy: jest.SpyInstance;

beforeEach(() => {
  consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
});

afterEach(() => {
  consoleErrorSpy.mockRestore();
});

function Bomb({ shouldThrow }: { shouldThrow: boolean }) {
  if (shouldThrow) throw new Error('test render crash');
  return <p>all good</p>;
}

describe('ErrorBoundary', () => {
  it('renders children when there is no error', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow={false} />
      </ErrorBoundary>
    );
    expect(screen.getByText('all good')).toBeInTheDocument();
  });

  it('renders the "We hit a snag." heading when a child throws', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    );
    expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
  });

  it('renders a Retry link pointing to "/" to escape crash-loops', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    );
    // Retry navigates to '/' (safe route) instead of re-rendering into the same crash.
    const retryLink = screen.getByRole('link', { name: /retry/i });
    expect(retryLink).toHaveAttribute('href', '/');
  });

  it('renders children again after the error resolves (happy path)', () => {
    let shouldThrow = true;
    const RecoverableBomb = () => {
      if (shouldThrow) throw new Error('boom');
      return <p>recovered</p>;
    };

    const { rerender } = render(
      <ErrorBoundary>
        <RecoverableBomb />
      </ErrorBoundary>
    );

    expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();

    shouldThrow = false;
    // Force a re-render by remounting the boundary (simulates navigating back to the page)
    rerender(
      <ErrorBoundary key="reset">
        <RecoverableBomb />
      </ErrorBoundary>
    );

    expect(screen.getByText('recovered')).toBeInTheDocument();
  });

  it('renders a "Go back" button that navigates back', async () => {
    const user = userEvent.setup();

    const backSpy = jest.spyOn(window.history, 'back').mockImplementation(() => {});

    render(
      <ErrorBoundary>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(screen.getByRole('button', { name: /go back/i })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /go back/i }));

    expect(backSpy).toHaveBeenCalledTimes(1);

    backSpy.mockRestore();
  });
});
