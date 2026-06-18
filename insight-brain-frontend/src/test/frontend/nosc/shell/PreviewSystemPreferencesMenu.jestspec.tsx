/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import userEvent from '@testing-library/user-event';
import { render, screen, within } from 'TestRoot/SpecUtil';
import PreviewSystemPreferencesMenu from 'MainRoot/nosc/shell/PreviewSystemPreferencesMenu';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

// The gear menu only needs a stable href() for visible items; isolate it from
// the real ui-router instance so the test exercises gating, not routing.
jest.mock('MainRoot/react/RouterStateContext', () => ({
  __esModule: true,
  useRouterState: () => ({ href: () => '#' }),
}));

beforeAll(installRadixJsdomShims);

function renderInTheme(preloadedState: object) {
  return render(
    <Theme appearance="light" accentColor="blue" radius="medium">
      <PreviewSystemPreferencesMenu />
    </Theme>,
    { preloadedState }
  );
}

describe('PreviewSystemPreferencesMenu', () => {
  // This protects the bootstrap wiring: the gear menu is empty unless
  // mainHeader permissions are loaded into the store (nexus-one/index.tsx
  // dispatches loadPermissions() on startup). Without CONFIGURE_SYSTEM the
  // admin items must NOT render.
  it('shows "No preferences available" when no permissions are present', async () => {
    const user = userEvent.setup();
    renderInTheme({ mainHeader: { permissions: {} } });
    await user.click(screen.getByRole('button', { name: 'System Preferences' }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByText('No preferences available')).toBeInTheDocument();
    expect(
      within(menu).queryByTestId('nexus-one-top-nav-settings-item-user-tokens')
    ).not.toBeInTheDocument();
  });

  it('renders admin items once CONFIGURE_SYSTEM permission is in the store', async () => {
    const user = userEvent.setup();
    renderInTheme({ mainHeader: { permissions: { CONFIGURE_SYSTEM: true } } });
    await user.click(screen.getByRole('button', { name: 'System Preferences' }));
    const menu = await screen.findByRole('menu');
    // "User Tokens Configuration" is gated solely on CONFIGURE_SYSTEM, so it is
    // a stable assertion regardless of product-feature/license slice defaults.
    expect(
      within(menu).getByTestId('nexus-one-top-nav-settings-item-user-tokens')
    ).toBeInTheDocument();
    expect(within(menu).queryByText('No preferences available')).not.toBeInTheDocument();
  });
});
