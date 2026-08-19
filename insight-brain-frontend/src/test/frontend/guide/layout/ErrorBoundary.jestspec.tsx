/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from '../test-utils';
import userEvent from '@testing-library/user-event';
import { useNavigate } from 'react-router';
import { ErrorBoundary } from 'GuideRoot/layout/ErrorBoundary';
import { reloadPage } from 'GuideRoot/utils/navigation';

jest.mock('GuideRoot/utils/navigation', () => ({
  reloadPage: jest.fn(),
  clearErrorRetries: jest.fn(),
  getErrorRetryCount: jest.fn().mockReturnValue(0),
}));

jest.mock('react-router', () => ({
  ...jest.requireActual('react-router'),
  useNavigate: jest.fn(),
}));

const mockNavigate = jest.fn();

// Suppress React's console.error for expected boundary errors
let consoleErrorSpy: jest.SpyInstance;

beforeEach(() => {
  consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  (useNavigate as jest.Mock).mockReturnValue(mockNavigate);
  mockNavigate.mockClear();
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

  it('calls reloadPage when Retry is clicked', async () => {
    const user = userEvent.setup();
    render(
      <ErrorBoundary>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    );

    await user.click(screen.getByRole('button', { name: /retry/i }));

    expect(reloadPage as jest.Mock).toHaveBeenCalledTimes(1);
  });

  it('clears error state when the boundary remounts (e.g. key change)', () => {
    // This tests the component-instance reset path (full remount via React key).
    // The production navigation-based reset is covered by
    // "resets and renders children after navigation to a new route".
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
    rerender(
      <ErrorBoundary key="reset">
        <RecoverableBomb />
      </ErrorBoundary>
    );

    expect(screen.getByText('recovered')).toBeInTheDocument();
  });

  it('resets and renders children after navigation to a new route', async () => {
    // The NavHelper needs real React Router navigation to update the location so
    // ErrorBoundary's getDerivedStateFromProps can detect the route change and reset.
    (useNavigate as jest.Mock).mockImplementation(jest.requireActual('react-router').useNavigate);

    const user = userEvent.setup();
    let shouldThrow = true;
    const ControllableBomb = () => {
      if (shouldThrow) throw new Error('test crash');
      return <p>all good</p>;
    };

    function NavHelper() {
      const navigate = useNavigate();
      return <button onClick={() => navigate('/other-page')}>navigate</button>;
    }

    render(
      <>
        <NavHelper />
        <ErrorBoundary>
          <ControllableBomb />
        </ErrorBoundary>
      </>
    );

    expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();

    shouldThrow = false;
    await user.click(screen.getByRole('button', { name: 'navigate' }));

    expect(screen.getByText('all good')).toBeInTheDocument();
  });

  it('renders a "Go back" button that triggers navigation when clicked', async () => {
    const user = userEvent.setup();
    render(
      <ErrorBoundary>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(screen.getByRole('button', { name: /go back/i })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /go back/i }));

    expect(mockNavigate).toHaveBeenCalled();
  });
});
