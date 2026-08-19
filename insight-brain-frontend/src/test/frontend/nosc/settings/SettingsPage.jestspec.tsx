/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';
import SettingsPage from 'MainRoot/nosc/settings/SettingsPage';
import { SETTINGS_PAGE_ITEMS } from 'MainRoot/nosc/settings/settingsPageItems';
import { comingSoonStateName } from 'MainRoot/nosc/comingSoon';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

const nonAdminState = { mainHeader: { permissions: {} } };

// Every permission + feature flag + license every hub item's showIf depends on
// (see settingsGating.ts), all enabled — the baseline for "everything shown".
// Crib of LeftNav.jestspec.tsx's fullyLicensedState plus the productFeatures
// keys read by productFeaturesSelectors.js for each selector in the gating
// context.
const fullyEnabledState = {
  mainHeader: {
    permissions: {
      CONFIGURE_SYSTEM: true,
      VIEW_ROLES: true,
      MANAGE_AUTOMATIC_APPLICATION_CREATION: true,
      MANAGE_AUTOMATIC_SCM_CONFIGURATION: true,
    },
  },
  productLicense: { loading: false, installed: true, license: { products: ['Sonatype Lifecycle Enterprise'] } },
  productFeatures: {
    productFeatures: {
      'single-tenant': true,
      'user-management-pages': true,
      'policy-monitoring': true,
      'ldap-configuration': true,
      'saml-enabled': true,
      'crowd-integration': true,
      'email-configuration': true,
      'proxy-configuration': true,
      'webhook-configuration': true,
      'webhooks-for-applications': true,
      'system-notice-configuration': true,
      'success-metrics-configuration': true,
      'automatic-application-configuration': true,
      'automatic-scm-configuration': true,
      'saas-lifecycle-scm-enabled': true,
      'product-license-configuration': true,
      'orgs-and-apps': true,
      'preview-nexus-one-ui': true,
      'oauth2-enabled': true,
    },
  },
};

// Zscaler needs a firewall-only/standalone-firewall license, and User Activity
// needs 'user-management-pages' OFF — both directly conflict with
// fullyEnabledState's Lifecycle Enterprise license and 'user-management-pages'
// true (needed to show Users). There's no single realistic state where every
// admin item is visible at once; each has its own dedicated test below.
const MUTUALLY_EXCLUSIVE_WITH_BASELINE = new Set(['zscaler', 'user-activity']);

const MY_LABELS = SETTINGS_PAGE_ITEMS.filter((i) => i.section === 'my').map((i) => i.label);
const ADMIN_LABELS = SETTINGS_PAGE_ITEMS.filter(
  (i) => i.section === 'admin' && !MUTUALLY_EXCLUSIVE_WITH_BASELINE.has(i.id),
).map((i) => i.label);

// The stubbed router returns `#/<stateName>` for any state, so a row's href
// reveals exactly which state it was asked to resolve.
const PLACEHOLDER_HREF = `#/${comingSoonStateName('settings')}`;

describe('SettingsPage', () => {
  beforeEach(() => {
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({
      href: jest.fn((name: string) => `#/${name}`),
      get: jest.fn(),
      includes: jest.fn(),
    } as unknown as ReturnType<typeof RouterStateContext.useRouterState>);
  });

  it('shows the full My Settings list and hides every Admin Console item for a non-admin', () => {
    render(<SettingsPage />, { preloadedState: nonAdminState });

    expect(screen.getByRole('heading', { name: 'My Settings' })).toBeInTheDocument();
    MY_LABELS.forEach((label) => expect(screen.getByText(label)).toBeInTheDocument());

    expect(screen.queryByRole('heading', { name: 'Admin Console' })).not.toBeInTheDocument();
    ADMIN_LABELS.forEach((label) => expect(screen.queryByText(label)).not.toBeInTheDocument());
  });

  it('shows both sections with their full item lists when every permission/feature/license is enabled', () => {
    render(<SettingsPage />, { preloadedState: fullyEnabledState });

    expect(screen.getByRole('heading', { name: 'My Settings' })).toBeInTheDocument();
    MY_LABELS.forEach((label) => expect(screen.getByText(label)).toBeInTheDocument());

    expect(screen.getByRole('heading', { name: 'Admin Console' })).toBeInTheDocument();
    ADMIN_LABELS.forEach((label) => expect(screen.getByText(label)).toBeInTheDocument());
  });

  it('links each row to its embedded state when wired, and to the placeholder otherwise', () => {
    render(<SettingsPage />, { preloadedState: fullyEnabledState });

    // Durable check keyed on `stateName`: a wired row resolves to its own state;
    // an unwired row falls back to the shared coming-soon placeholder. Zscaler/User
    // Activity aren't visible under this baseline (see MUTUALLY_EXCLUSIVE_WITH_BASELINE)
    // — covered by their own dedicated tests below.
    SETTINGS_PAGE_ITEMS.filter((item) => !MUTUALLY_EXCLUSIVE_WITH_BASELINE.has(item.id)).forEach((item) => {
      const link = screen.getByRole('link', { name: new RegExp(`^${item.label}:`) });
      const expectedHref = item.stateName ? `#/${item.stateName}` : PLACEHOLDER_HREF;
      expect(link).toHaveAttribute('href', expectedHref);
    });
  });

  it('filters entries across both sections as the user types', async () => {
    const user = userEvent.setup();
    render(<SettingsPage />, { preloadedState: fullyEnabledState });

    await user.type(screen.getByRole('textbox', { name: 'Search settings' }), 'ldap');

    expect(screen.getByText('LDAP')).toBeInTheDocument();
    expect(screen.queryByText('Change Password')).not.toBeInTheDocument();
    expect(screen.queryByText('Users')).not.toBeInTheDocument();
  });

  it('shows an empty-state message that echoes the query when nothing matches', async () => {
    const user = userEvent.setup();
    render(<SettingsPage />, { preloadedState: fullyEnabledState });

    await user.type(screen.getByRole('textbox', { name: 'Search settings' }), 'zzzzz');

    // The user sees the searched term echoed back plus guidance to try again.
    expect(
      screen.getByText(/No settings match .*zzzzz.*\.\s*Try another search term\./i),
    ).toBeInTheDocument();
  });

  it('hides an item whose feature flag is off while the rest of its section stays visible', () => {
    const state = {
      ...fullyEnabledState,
      productFeatures: {
        productFeatures: { ...fullyEnabledState.productFeatures.productFeatures, 'ldap-configuration': false },
      },
    };
    render(<SettingsPage />, { preloadedState: state });

    expect(screen.getByRole('heading', { name: 'Admin Console' })).toBeInTheDocument();
    expect(screen.queryByText('LDAP')).not.toBeInTheDocument();
    expect(screen.getByText('Users')).toBeInTheDocument();
  });

  it('hides the Nexus One UI item when the preview flag is off, shows it when on', () => {
    const flagOff = {
      ...fullyEnabledState,
      productFeatures: {
        productFeatures: { ...fullyEnabledState.productFeatures.productFeatures, 'preview-nexus-one-ui': false },
      },
    };
    render(<SettingsPage />, { preloadedState: flagOff });
    expect(screen.queryByText('Nexus One UI')).not.toBeInTheDocument();

    render(<SettingsPage />, { preloadedState: fullyEnabledState });
    const link = screen.getByRole('link', { name: /^Nexus One UI:/ });
    expect(link).toHaveAttribute('href', '#/nexusOneUiSettings');
  });

  it('shows only Roles in Admin Console for a VIEW_ROLES-only, non-CONFIGURE_SYSTEM user', () => {
    const state = {
      mainHeader: { permissions: { VIEW_ROLES: true } },
      productLicense: fullyEnabledState.productLicense,
      productFeatures: fullyEnabledState.productFeatures,
    };
    render(<SettingsPage />, { preloadedState: state });

    expect(screen.getByRole('heading', { name: 'Admin Console' })).toBeInTheDocument();
    expect(screen.getByText('Roles')).toBeInTheDocument();
    ADMIN_LABELS.filter((label) => label !== 'Roles').forEach((label) =>
      expect(screen.queryByText(label)).not.toBeInTheDocument(),
    );
  });

  it('shows only Automatic Applications in Admin Console for a MANAGE_AUTOMATIC_APPLICATION_CREATION-only user', () => {
    const state = {
      mainHeader: { permissions: { MANAGE_AUTOMATIC_APPLICATION_CREATION: true } },
      productLicense: fullyEnabledState.productLicense,
      productFeatures: fullyEnabledState.productFeatures,
    };
    render(<SettingsPage />, { preloadedState: state });

    expect(screen.getByRole('heading', { name: 'Admin Console' })).toBeInTheDocument();
    expect(screen.getByText('Automatic Applications')).toBeInTheDocument();
    ADMIN_LABELS.filter((label) => label !== 'Automatic Applications').forEach((label) =>
      expect(screen.queryByText(label)).not.toBeInTheDocument(),
    );
  });

  it('shows only Automatic SCM Configuration in Admin Console for a MANAGE_AUTOMATIC_SCM_CONFIGURATION-only user', () => {
    const state = {
      mainHeader: { permissions: { MANAGE_AUTOMATIC_SCM_CONFIGURATION: true } },
      productLicense: fullyEnabledState.productLicense,
      productFeatures: fullyEnabledState.productFeatures,
    };
    render(<SettingsPage />, { preloadedState: state });

    expect(screen.getByRole('heading', { name: 'Admin Console' })).toBeInTheDocument();
    expect(screen.getByText('Automatic SCM Configuration')).toBeInTheDocument();
    ADMIN_LABELS.filter((label) => label !== 'Automatic SCM Configuration').forEach((label) =>
      expect(screen.queryByText(label)).not.toBeInTheDocument(),
    );
  });

  it('shows User Activity (and hides Users) when user-management-pages is off and activity tracking is on', () => {
    const state = {
      ...fullyEnabledState,
      productFeatures: {
        productFeatures: {
          ...fullyEnabledState.productFeatures.productFeatures,
          'user-management-pages': false,
          'user-activity-tracking': true,
        },
      },
    };
    render(<SettingsPage />, { preloadedState: state });

    expect(screen.getByText('User Activity')).toBeInTheDocument();
    expect(screen.queryByText('Users')).not.toBeInTheDocument();
    const link = screen.getByRole('link', { name: /^User Activity:/ });
    expect(link).toHaveAttribute('href', '#/userActivity');
  });

  it('shows Zscaler under a firewall-only license, linked to the coming-soon placeholder (no NOUX embed yet)', () => {
    const state = {
      ...fullyEnabledState,
      productLicense: { loading: false, installed: true, license: { products: ['Sonatype Repository Firewall'] } },
      productFeatures: {
        productFeatures: { ...fullyEnabledState.productFeatures.productFeatures, zscaler: true },
      },
    };
    render(<SettingsPage />, { preloadedState: state });

    expect(screen.getByText('Zscaler')).toBeInTheDocument();
    const link = screen.getByRole('link', { name: /^Zscaler:/ });
    expect(link).toHaveAttribute('href', PLACEHOLDER_HREF);
  });

  it('hides OIDC under a multi-tenant configuration even when oauth2-enabled is on', () => {
    const state = {
      ...fullyEnabledState,
      productFeatures: {
        productFeatures: {
          ...fullyEnabledState.productFeatures.productFeatures,
          'single-tenant': false,
          'multi-tenant': true,
        },
      },
    };
    render(<SettingsPage />, { preloadedState: state });

    expect(screen.queryByText('OIDC')).not.toBeInTheDocument();
  });
});
