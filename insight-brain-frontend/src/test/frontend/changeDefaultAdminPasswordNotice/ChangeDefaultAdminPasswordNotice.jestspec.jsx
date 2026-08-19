/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ChangeDefaultAdminPasswordNotice from 'MainRoot/changeDefaultAdminPasswordNotice/ChangeDefaultAdminPasswordNotice';

describe('ChangeDefaultAdminPasswordNotice', () => {
  const defaultPreloadedState = {
    userSession: {
      data: {
        username: 'admin',
      },
      shouldDisplayPasswordWarning: false,
      loading: false,
      error: null,
    },
  };

  const renderComponent = (preloadedState) => {
    return render(<ChangeDefaultAdminPasswordNotice />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });
  };

  it('does not render when shouldDisplayNotice is false', () => {
    renderComponent();

    expect(screen.queryByText(/Change Administrator Password/i)).not.toBeInTheDocument();
  });

  it('renders notice when shouldDisplayNotice is true and user is default admin', () => {
    const stateWithNotice = {
      ...defaultPreloadedState,
      userSession: {
        ...defaultPreloadedState.userSession,
        data: {
          username: 'admin',
        },
        shouldDisplayPasswordWarning: true,
      },
    };

    renderComponent(stateWithNotice);

    expect(screen.getByText(/Change Administrator Password/i)).toBeInTheDocument();
    expect(
      screen.getByText(/For security reasons, please change your password by clicking the user menu in the header/i)
    ).toBeInTheDocument();
  });

  it('renders notice when shouldDisplayNotice is true and user is not default admin', () => {
    const stateWithNotice = {
      ...defaultPreloadedState,
      userSession: {
        ...defaultPreloadedState.userSession,
        data: {
          username: 'otheruser',
        },
        shouldDisplayPasswordWarning: true,
      },
    };

    renderComponent(stateWithNotice);

    expect(screen.getByText(/Change Administrator Password/i)).toBeInTheDocument();
    expect(
      screen.getByText(
        /The "admin" user has the default password set. Login under that username to change the password/i
      )
    ).toBeInTheDocument();
  });

  it('renders with correct ID and CSS classes', () => {
    const stateWithNotice = {
      ...defaultPreloadedState,
      userSession: {
        ...defaultPreloadedState.userSession,
        shouldDisplayPasswordWarning: true,
      },
    };

    const { container } = renderComponent(stateWithNotice);
    const notice = container.querySelector('#change-default-admin-password-notice');

    expect(notice).toBeInTheDocument();
    expect(notice).toHaveClass('nx-system-notice');
    expect(notice).toHaveClass('nx-system-notice--alert');
  });
});
