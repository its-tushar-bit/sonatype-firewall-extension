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
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

// The gear menu only needs a stable href() for visible items; isolate it from
// the real ui-router instance so the test exercises gating, not routing.
// mockHref is a jest.fn() spy so individual tests can assert on the exact state
// name passed to href(). Setup runs in beforeEach because setupJest.js clears
// all spies with jest.restoreAllMocks() in its afterEach — otherwise the spy
// survives only the first test in the file.
const mockHref = jest.fn(() => '#');
beforeEach(() => {
  mockHref.mockClear();
  jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({ href: mockHref } as ReturnType<
    typeof RouterStateContext.useRouterState
  >);
});

beforeAll(installRadixJsdomShims);
beforeEach(() => mockHref.mockClear());

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

  // Regression guard (CLM-42956): the SAML item must use the plain 'saml'
  // state name, never 'firewall.saml'. Reintroducing `prefix: firewallPrefix`
  // on the entry would flip href() to the prefixed name and break the
  // `.not.toHaveBeenCalledWith` assertion.
  //
  // selectIsSAMLEnabled reads state.productFeatures.productFeatures['saml-enabled']
  // (double-nested), so preloadedState must match that shape or the item is
  // hidden by its `showIf`.
  it('calls href with plain "saml" state name, never the firewall-prefixed variant', async () => {
    const user = userEvent.setup();

    renderInTheme({
      mainHeader: { permissions: { CONFIGURE_SYSTEM: true } },
      productFeatures: {
        productFeatures: { 'saml-enabled': true },
      },
    });

    // Radix DropdownMenu only renders its Content (including the <a> tags
    // whose href gets computed via mockHref) after the trigger is clicked.
    await user.click(screen.getByRole('button', { name: 'System Preferences' }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByTestId('nexus-one-top-nav-settings-item-saml')).toBeInTheDocument();

    expect(mockHref).toHaveBeenCalledWith('saml');
    expect(mockHref).not.toHaveBeenCalledWith('firewall.saml');
  });

  it('calls href with plain "advancedSearchConfig" state, never firewall-prefixed - CLM-42963', async () => {
    // Regression guard for the sibling bug pattern (firewall-prefixed href resolving
    // to a NOUX state that doesn't exist). Advanced Search's `showIf` explicitly
    // excludes firewall-only-license and standalone-firewall modes, so the item
    // is only rendered under non-firewall licenses where `firewallPrefix === ''`.
    // Under those conditions, `prefix: firewallPrefix` would still produce the
    // plain state name — so the effective guard is "the entry must not hardcode
    // `firewall.advancedSearchConfig` as its stateName". This test enforces that.
    const user = userEvent.setup();

    renderInTheme({
      mainHeader: { permissions: { CONFIGURE_SYSTEM: true } },
      productLicense: { license: { products: ['Sonatype Lifecycle'] } },
      productFeatures: { productFeatures: { 'advanced-search-configuration': true } },
    });

    await user.click(screen.getByRole('button', { name: 'System Preferences' }));
    const menu = await screen.findByRole('menu');
    const advancedSearchItem = within(menu).getByTestId(
      'nexus-one-top-nav-settings-item-advanced-search'
    );
    expect(advancedSearchItem).toBeInTheDocument();

    expect(mockHref).toHaveBeenCalledWith('advancedSearchConfig');
    expect(mockHref).not.toHaveBeenCalledWith('firewall.advancedSearchConfig');
  });

  it('hides Advanced Search entry entirely under firewall-only license - CLM-42963', async () => {
    // Advanced Search is not part of the Firewall product feature set, so the entry
    // must be hidden under a firewall-only license. This is the actual mechanism
    // that prevents the sibling gear-menu prefix bug for this page — the item is
    // never rendered in the license mode that would trigger the broken href.
    const user = userEvent.setup();

    renderInTheme({
      mainHeader: { permissions: { CONFIGURE_SYSTEM: true } },
      productLicense: { license: { products: ['Sonatype Repository Firewall'] } },
    });

    await user.click(screen.getByRole('button', { name: 'System Preferences' }));
    const menu = await screen.findByRole('menu');
    expect(
      within(menu).queryByTestId('nexus-one-top-nav-settings-item-advanced-search')
    ).not.toBeInTheDocument();
  });

  it('Waived Components links to the unprefixed state even under a firewall-only license', async () => {
    // firewallPrefix = 'firewall' in this state; the item must still target the NOUX state directly.
    const user = userEvent.setup();
    renderInTheme({
      mainHeader: { permissions: { CONFIGURE_SYSTEM: true } },
      productLicense: { license: { products: ['Sonatype Repository Firewall'] } },
    });
    await user.click(screen.getByRole('button', { name: 'System Preferences' }));
    await screen.findByRole('menu');
    expect(mockHref).toHaveBeenCalledWith('waivedComponentUpgradesConfiguration');
    expect(mockHref).not.toHaveBeenCalledWith('firewall.waivedComponentUpgradesConfiguration');
  });
});
