/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from '../test-utils';
import userEvent from '@testing-library/user-event';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';

describe('ErrorPage', () => {
  it('renders the "We hit a snag" heading', () => {
    render(<ErrorPage />);
    expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
  });

  it('renders Retry link with custom href', () => {
    render(<ErrorPage retryHref="/components" />);
    expect(screen.getByRole('link', { name: /retry/i })).toHaveAttribute('href', '/components');
  });

  it('defaults Retry link to "/"', () => {
    render(<ErrorPage />);
    expect(screen.getByRole('link', { name: /retry/i })).toHaveAttribute('href', '/');
  });

  it('shows Go back button by default', () => {
    render(<ErrorPage />);
    expect(screen.getByRole('button', { name: /go back/i })).toBeInTheDocument();
  });

  it('hides Go back button when showGoBack is false', () => {
    render(<ErrorPage showGoBack={false} />);
    expect(screen.queryByRole('button', { name: /go back/i })).not.toBeInTheDocument();
  });

  it('Go back button calls window.history.back', async () => {
    const user = userEvent.setup();
    const backSpy = jest.spyOn(window.history, 'back').mockImplementation(() => {});

    render(<ErrorPage />);

    await user.click(screen.getByRole('button', { name: /go back/i }));

    expect(backSpy).toHaveBeenCalledTimes(1);

    backSpy.mockRestore();
  });
});
