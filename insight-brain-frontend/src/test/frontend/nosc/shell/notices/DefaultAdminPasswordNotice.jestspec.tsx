/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { DefaultAdminPasswordNotice } from 'MainRoot/nosc/shell/notices/DefaultAdminPasswordNotice';

describe('DefaultAdminPasswordNotice', () => {
  const defaultPreloadedState = {
    userSession: {
      data: { username: 'admin' },
      shouldDisplayPasswordWarning: false,
      loading: false,
      error: null,
    },
  };

  const renderComponent = (preloadedState = defaultPreloadedState) =>
    render(<DefaultAdminPasswordNotice />, { preloadedState });

  it('does not render when shouldDisplayPasswordWarning is false', () => {
    renderComponent();
    expect(screen.queryByText(/Change Administrator Password/i)).not.toBeInTheDocument();
  });

  it('tells the default admin user to change it via Classic\'s user menu', () => {
    renderComponent({
      userSession: { ...defaultPreloadedState.userSession, data: { username: 'admin' }, shouldDisplayPasswordWarning: true },
    });

    expect(screen.getByText(/Change Administrator Password/i)).toBeInTheDocument();
    expect(screen.getByText(/change the default administrator password/i)).toBeInTheDocument();
    // No in-Preview mechanism exists yet, so the copy points to Classic's user menu instead.
    expect(screen.getByText(/switch to classic ui and use the user menu/i)).toBeInTheDocument();
  });

  it('tells a non-admin user to sign in as admin, then change it via Classic\'s user menu', () => {
    renderComponent({
      userSession: { ...defaultPreloadedState.userSession, data: { username: 'someoneElse' }, shouldDisplayPasswordWarning: true },
    });

    expect(screen.getByText(/"admin" user has the default password set/i)).toBeInTheDocument();
    expect(screen.getByText(/sign in as that account/i)).toBeInTheDocument();
    expect(screen.getByText(/switch to classic ui and use the user menu/i)).toBeInTheDocument();
    // No link/button — this is copy guidance, not a CTA; matches Classic's non-dismissible behavior.
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('renders as a warning-severity notice with role="status"', () => {
    renderComponent({
      userSession: { ...defaultPreloadedState.userSession, shouldDisplayPasswordWarning: true },
    });

    expect(screen.getByTestId('nosc-default-admin-password-notice')).toHaveAttribute('role', 'status');
  });
});
