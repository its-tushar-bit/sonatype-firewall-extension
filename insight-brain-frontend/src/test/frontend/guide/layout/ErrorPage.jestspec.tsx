/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from '../test-utils';
import userEvent from '@testing-library/user-event';
import { useNavigate } from 'react-router';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';
import { getErrorRetryCount } from 'GuideRoot/utils/navigation';

jest.mock('react-router', () => ({
  ...jest.requireActual('react-router'),
  useNavigate: jest.fn(),
}));

jest.mock('GuideRoot/utils/navigation', () => ({
  reloadPage: jest.fn(),
  clearErrorRetries: jest.fn(),
  getErrorRetryCount: jest.fn().mockReturnValue(0),
}));

const mockGetErrorRetryCount = getErrorRetryCount as jest.Mock;
const mockUseNavigate = useNavigate as jest.Mock;
const mockNavigate = jest.fn();

describe('ErrorPage', () => {
  beforeEach(() => {
    mockGetErrorRetryCount.mockReturnValue(0);
    mockUseNavigate.mockReturnValue(mockNavigate);
    mockNavigate.mockClear();
  });

  it('renders the "We hit a snag" heading', () => {
    render(<ErrorPage />);
    expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
  });

  it('renders Retry as a button', () => {
    render(<ErrorPage />);
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /retry/i })).not.toBeInTheDocument();
  });

  it('shows Go back button by default', () => {
    render(<ErrorPage />);
    expect(screen.getByRole('button', { name: /go back/i })).toBeInTheDocument();
  });

  it('hides Go back button when showGoBack is false', () => {
    render(<ErrorPage showGoBack={false} />);
    expect(screen.queryByRole('button', { name: /go back/i })).not.toBeInTheDocument();
  });

  it('calls onRetry callback when Retry button is clicked', async () => {
    const user = userEvent.setup();
    const onRetry = jest.fn();
    render(<ErrorPage onRetry={onRetry} />);

    await user.click(screen.getByRole('button', { name: /retry/i }));

    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders Retry as a button (not a link) when onRetry is provided', () => {
    render(<ErrorPage onRetry={jest.fn()} />);
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /retry/i })).not.toBeInTheDocument();
  });

  it('replaces Retry with a contact support message after MAX_RETRIES exhausted', () => {
    mockGetErrorRetryCount.mockReturnValue(3);
    render(<ErrorPage />);
    expect(screen.queryByRole('button', { name: /retry/i })).not.toBeInTheDocument();
    expect(screen.getByText(/still not working/i)).toBeInTheDocument();
  });

  it('still shows Go back when retries are exhausted', () => {
    mockGetErrorRetryCount.mockReturnValue(3);
    render(<ErrorPage />);
    expect(screen.getByRole('button', { name: /go back/i })).toBeInTheDocument();
  });

  it('shows contact support message and Go back together when retries are exhausted', () => {
    mockGetErrorRetryCount.mockReturnValue(3);
    render(<ErrorPage showGoBack={true} />);
    // Both elements must be present — the grid switches to 1-column so they
    // stack rather than creating an asymmetric 2-column row.
    expect(screen.getByText(/still not working/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /go back/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /retry/i })).not.toBeInTheDocument();
  });

  it('Go back calls navigate("/") when at the start of browser history (length <= 1)', async () => {
    const user = userEvent.setup();
    // jsdom starts each test file with history.length === 1 and replaceState
    // does not add an entry, so history.length stays at 1 here.
    window.history.replaceState({}, '');

    render(<ErrorPage />);

    await user.click(screen.getByRole('button', { name: /go back/i }));

    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('Go back calls navigate(-1) when prior browser history exists (length > 1)', async () => {
    const user = userEvent.setup();
    // pushState adds a new entry, making history.length > 1.
    window.history.pushState({}, '');

    render(<ErrorPage />);

    await user.click(screen.getByRole('button', { name: /go back/i }));

    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });
});
