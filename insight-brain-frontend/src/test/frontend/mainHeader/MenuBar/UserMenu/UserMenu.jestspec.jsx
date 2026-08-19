/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import UserMenu from 'MainRoot/mainHeader/MenuBar/UserMenu/UserMenu';
import { render, screen, within } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';

describe('UserMenu', () => {
  let renderComponent,
    loadUserSpy = jest.fn(),
    resetPasswordStatusSpy = jest.fn();

  const minimalProps = {
    user: { displayName: 'Test User' },
    loadUser: loadUserSpy,
    resetPasswordStatus: resetPasswordStatusSpy,
  };

  beforeEach(() => {
    localStorage.clear();
    const defaultPreloadedState = {
      productFeatures: {
        productFeatures: {
          'dark-mode': true,
        },
      },
    };
    renderComponent = (preloadedState) => {
      render(<UserMenu {...minimalProps} />, { preloadedState: preloadedState || defaultPreloadedState });
    };
  });

  afterAll(() => localStorage.clear());

  it('should render the title with the user name', async () => {
    const user = userEvent.setup();

    renderComponent();
    const button = screen.getByRole('button');
    await user.click(button);
    expect(screen.getByText('Test User')).toBeInTheDocument();
  });

  it('should render a link for Display Theme', async () => {
    const user = userEvent.setup();

    renderComponent();
    const button = screen.getByRole('button');
    await user.click(button);

    expect(screen.getByText('Display Theme')).toBeInTheDocument();
  });

  it('should open a modal when Display Theme is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();
    const button = screen.getByRole('button');
    await user.click(button);

    const displayTheme = screen.getByText('Display Theme');
    await user.click(displayTheme);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeInTheDocument();
  });

  it('should close the modal when Close button is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();
    const button = screen.getByRole('button');
    await user.click(button);

    const displayTheme = screen.getByText('Display Theme');
    await user.click(displayTheme);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeInTheDocument();

    const closeButton = within(dialog).getByRole('button', { name: 'Close' });
    await user.click(closeButton);

    expect(dialog).not.toBeInTheDocument();
  });
});
