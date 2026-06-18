/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import userEvent from '@testing-library/user-event';
import { render, screen, within } from 'TestRoot/SpecUtil';
import PreviewUserMenu from 'MainRoot/nosc/shell/PreviewUserMenu';
import { logout } from 'MainRoot/user/userSessionSlice';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

// Replace only the `logout` thunk with a plain sentinel action so clicking
// "Log Out" exercises the dispatch wiring without the real thunk navigating
// the jsdom window. The slice's reducer (consumed by the root store) is kept
// intact via requireActual.
jest.mock('MainRoot/user/userSessionSlice', () => {
  const actual = jest.requireActual('MainRoot/user/userSessionSlice');
  return {
    __esModule: true,
    ...actual,
    logout: jest.fn(() => ({ type: 'userSession/logout/MOCK' })),
  };
});

beforeAll(installRadixJsdomShims);

// setupJest.js runs jest.restoreAllMocks() in a global afterEach, which clears
// this factory mock's implementation (and call count) between tests. Without
// re-establishing the sentinel return value here, logout() returns undefined
// in later tests and dispatch throws "Actions must be plain objects".
beforeEach(() => {
  (logout as jest.Mock).mockReturnValue({ type: 'userSession/logout/MOCK' });
});

function userSessionState(data: object | null) {
  return { userSession: { data } };
}

function renderInTheme(preloadedState: object) {
  return render(
    <Theme appearance="light" accentColor="blue" radius="medium">
      <PreviewUserMenu />
    </Theme>,
    { preloadedState }
  );
}

describe('PreviewUserMenu', () => {
  it('renders a trigger with aria-label "User menu"', () => {
    renderInTheme(userSessionState({ displayName: 'Jane Doe', username: 'jdoe' }));
    expect(screen.getByRole('button', { name: 'User menu' })).toBeInTheDocument();
  });

  it('opens on click and shows the display name plus a Log Out action', async () => {
    const user = userEvent.setup();
    renderInTheme(userSessionState({ displayName: 'Jane Doe', username: 'jdoe' }));
    await user.click(screen.getByRole('button', { name: 'User menu' }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByTestId('nexus-one-top-nav-user-name')).toHaveTextContent('Jane Doe');
    expect(within(menu).getByTestId('nexus-one-top-nav-user-logout')).toBeInTheDocument();
  });

  it('falls back to the username when displayName is missing', async () => {
    const user = userEvent.setup();
    renderInTheme(userSessionState({ username: 'jdoe' }));
    await user.click(screen.getByRole('button', { name: 'User menu' }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByTestId('nexus-one-top-nav-user-name')).toHaveTextContent('jdoe');
  });

  it('falls back to "User" when both displayName and username are absent/blank', async () => {
    // A whitespace-only displayName must not win over the fallback — it
    // resolves to "User" (and the avatar initial is derived from the same
    // resolved value, so the two always agree).
    const user = userEvent.setup();
    renderInTheme(userSessionState({ displayName: '   ' }));
    await user.click(screen.getByRole('button', { name: 'User menu' }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByTestId('nexus-one-top-nav-user-name')).toHaveTextContent('User');
  });

  it('renders and falls back to "User" when the session is null', async () => {
    // Session not yet loaded / cleared: user is null at the top level, which
    // must not throw and must resolve to the "User" fallback.
    const user = userEvent.setup();
    renderInTheme(userSessionState(null));
    await user.click(screen.getByRole('button', { name: 'User menu' }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByTestId('nexus-one-top-nav-user-name')).toHaveTextContent('User');
  });

  it('dispatches the logout thunk when Log Out is selected', async () => {
    const user = userEvent.setup();
    renderInTheme(userSessionState({ displayName: 'Jane Doe', username: 'jdoe' }));
    await user.click(screen.getByRole('button', { name: 'User menu' }));
    await screen.findByRole('menu');
    await user.click(screen.getByTestId('nexus-one-top-nav-user-logout'));
    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('exposes the test-id slot used by the TopNav layout', () => {
    renderInTheme(userSessionState({ displayName: 'Jane Doe' }));
    expect(screen.getByTestId('nexus-one-top-nav-user-menu-slot')).toBeInTheDocument();
  });
});
