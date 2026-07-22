/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen } from 'TestRoot/SpecUtil';
import LeftNav from 'MainRoot/nosc/shell/LeftNav';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import { COLLAPSED_KEY } from 'MainRoot/nosc/shell/useLeftNavCollapsed';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';

beforeAll(installRadixJsdomShims);

beforeEach(() => {
  window.localStorage.removeItem(COLLAPSED_KEY);
  _setBaseUrlForTesting('http://localhost');
});

afterEach(() => {
  window.location.hash = '';
});

/** Fully-licensed, fully-feature-flagged tenant — every item visible. */
const fullyLicensedState = {
  userSession: { data: { username: 'admin' } },
  productLicense: {
    loading: false,
    installed: true,
    license: { products: ['Sonatype Lifecycle Enterprise'] },
  },
  productFeatures: {
    loading: false,
    productFeatures: {
      dashboard: true,
      'orgs-and-apps': true,
      'hosted-repository-evaluation': true,
      'advanced-legal-pack': true,
      'api-page': true,
      'integrated-enterprise-reporting': true,
      'reports-list': true,
    },
  },
  successMetricsConfiguration: { serverData: { enabled: true } },
  router: { currentState: { name: 'dashboard' } },
};

function renderLeftNav(preloadedState: object, hash = '') {
  // LeftNav derives active-route highlighting from the current hash path
  // (useCurrentHashPath), so set it before render for active-state tests.
  window.location.hash = hash;
  return render(
    <Theme appearance="light" accentColor="blue" radius="medium">
      <LeftNav />
    </Theme>,
    { preloadedState }
  );
}

/** Matches the data-testid on the LeftNav group divider, avoiding Radix internal class names. */
const SEPARATOR_SELECTOR = '[data-testid="nosc-leftnav-separator"]';

/** Order matches the Phase-1 final IA table. */
const EXPECTED_ORDER = [
  'dashboard',
  'applications',
  'components',
  'hosted-repos',
  'legal',
  'orgs-policies',
  'violations',
  'vulnerabilities',
  'waivers',
  'success-metrics',
  'enterprise-reporting',
  'api',
  'settings',
];

describe('LeftNav', () => {
  it('renders every item in the fully-licensed tenant in the expected IA order', () => {
    renderLeftNav(fullyLicensedState);
    const nav = screen.getByTestId('nosc-leftnav');
    for (const id of EXPECTED_ORDER) {
      expect(screen.getByTestId(`nosc-leftnav-${id}`)).toBeInTheDocument();
    }

    const links = Array.from(nav.querySelectorAll('a[data-testid^="nosc-leftnav-"]'));
    const linkOrder = links.map((link) => link.getAttribute('data-testid')?.replace('nosc-leftnav-', ''));
    expect(linkOrder).toEqual(EXPECTED_ORDER);
  });

  it.each([
    ['components', '#/components'],
    ['violations', '#/violations'],
    ['waivers', '#/waivers'],
    ['api', '#/api'],
    ['success-metrics', '#/success-metrics'],
    ['enterprise-reporting', '#/reports'],
    ['legal', '#/legal'],
    ['orgs-policies', '#/orgs-and-policies'],
    ['hosted-repos', '#/repositories'],
  ])('renders the native/embedded %s item pointing at %s', (id, expectedHash) => {
    renderLeftNav(fullyLicensedState);
    const link = screen.getByTestId(`nosc-leftnav-${id}`);
    expect(link.getAttribute('href')).toContain(expectedHash);
    expect(link.getAttribute('href')).not.toContain('coming-soon');
  });

  it.each([
    ['vulnerabilities', '#/coming-soon/vulnerabilities'],
    ['settings', '#/coming-soon/settings'],
  ])('renders the Coming Soon %s item pointing at %s', (id, expectedHash) => {
    renderLeftNav(fullyLicensedState);
    const link = screen.getByTestId(`nosc-leftnav-${id}`);
    expect(link.getAttribute('href')).toContain(expectedHash);
  });

  it('renders a divider before Success Metrics and before API', () => {
    renderLeftNav(fullyLicensedState);
    const nav = screen.getByTestId('nosc-leftnav');
    const separators = nav.querySelectorAll(SEPARATOR_SELECTOR);
    expect(separators.length).toBe(2);

    const successMetricsBox = screen.getByTestId('nosc-leftnav-success-metrics').closest('div');
    const apiBox = screen.getByTestId('nosc-leftnav-api').closest('div');
    expect(successMetricsBox?.querySelector(SEPARATOR_SELECTOR)).not.toBeNull();
    expect(apiBox?.querySelector(SEPARATOR_SELECTOR)).not.toBeNull();
  });

  it('renders the divider on Enterprise Reporting when Success Metrics is disabled', () => {
    const state = {
      ...fullyLicensedState,
      successMetricsConfiguration: { serverData: { enabled: false } },
    };
    renderLeftNav(state);
    expect(screen.queryByTestId('nosc-leftnav-success-metrics')).not.toBeInTheDocument();

    const nav = screen.getByTestId('nosc-leftnav');
    const separators = nav.querySelectorAll(SEPARATOR_SELECTOR);
    expect(separators.length).toBe(2);

    const reportingBox = screen.getByTestId('nosc-leftnav-enterprise-reporting').closest('div');
    expect(reportingBox?.querySelector(SEPARATOR_SELECTOR)).not.toBeNull();
  });

  it('renders the divider on Settings when API is disabled', () => {
    const state = {
      ...fullyLicensedState,
      productFeatures: {
        ...fullyLicensedState.productFeatures,
        productFeatures: {
          ...fullyLicensedState.productFeatures.productFeatures,
          'api-page': false,
        },
      },
    };
    renderLeftNav(state);
    expect(screen.queryByTestId('nosc-leftnav-api')).not.toBeInTheDocument();

    const nav = screen.getByTestId('nosc-leftnav');
    const separators = nav.querySelectorAll(SEPARATOR_SELECTOR);
    expect(separators.length).toBe(2);

    const settingsBox = screen.getByTestId('nosc-leftnav-settings').closest('div');
    expect(settingsBox?.querySelector(SEPARATOR_SELECTOR)).not.toBeNull();
  });

  it('does not render a top-of-rail divider when a group is the first visible rail entry', () => {
    // Unlicensed + Dashboard disabled drops every item above the reporting/settings
    // groups, but Success Metrics and API are not license-gated — so the rail leads
    // with Success Metrics. Its leading divider must be suppressed (nothing sits above
    // it), while API keeps its divider (Success Metrics is above it).
    const state = {
      ...fullyLicensedState,
      productLicense: {
        loading: false,
        installed: false,
        license: { products: [] },
      },
      productFeatures: {
        ...fullyLicensedState.productFeatures,
        productFeatures: {
          ...fullyLicensedState.productFeatures.productFeatures,
          dashboard: false,
        },
      },
    };
    renderLeftNav(state);

    // The rail leads with Success Metrics, followed by API.
    expect(screen.getByTestId('nosc-leftnav-success-metrics')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-leftnav-api')).toBeInTheDocument();

    const nav = screen.getByTestId('nosc-leftnav');
    // Exactly one divider — above API — with none stranded above the top item.
    expect(nav.querySelectorAll(SEPARATOR_SELECTOR).length).toBe(1);

    const successMetricsBox = screen.getByTestId('nosc-leftnav-success-metrics').closest('div');
    const apiBox = screen.getByTestId('nosc-leftnav-api').closest('div');
    expect(successMetricsBox?.querySelector(SEPARATOR_SELECTOR)).toBeNull();
    expect(apiBox?.querySelector(SEPARATOR_SELECTOR)).not.toBeNull();
  });

  it('shows Operational Reporting instead of Enterprise Reporting when integrated-enterprise-reporting is off', () => {
    const state = {
      ...fullyLicensedState,
      productFeatures: {
        ...fullyLicensedState.productFeatures,
        productFeatures: {
          ...fullyLicensedState.productFeatures.productFeatures,
          'integrated-enterprise-reporting': false,
        },
      },
    };
    renderLeftNav(state);
    expect(screen.queryByTestId('nosc-leftnav-enterprise-reporting')).not.toBeInTheDocument();
    expect(screen.getByTestId('nosc-leftnav-operational-reporting')).toBeInTheDocument();
  });

  it.each(['components', 'violations', 'vulnerabilities', 'waivers', 'settings'])(
    'hides the net-new %s item when orgs-and-apps is disabled',
    (id) => {
      const state = {
        ...fullyLicensedState,
        productFeatures: {
          ...fullyLicensedState.productFeatures,
          productFeatures: {
            ...fullyLicensedState.productFeatures.productFeatures,
            'orgs-and-apps': false,
          },
        },
      };
      renderLeftNav(state);
      expect(screen.queryByTestId(`nosc-leftnav-${id}`)).not.toBeInTheDocument();
    }
  );

  it('hides every license-gated item for an unlicensed tenant', () => {
    const state = {
      ...fullyLicensedState,
      productLicense: {
        loading: false,
        installed: false,
        license: { products: [] },
      },
    };
    renderLeftNav(state);
    // Dashboard, Success Metrics, and API aren't gated on isLicensed, so they still render.
    const licenseGatedIds = EXPECTED_ORDER.filter((id) => !['dashboard', 'success-metrics', 'api'].includes(id));
    for (const id of licenseGatedIds) {
      expect(screen.queryByTestId(`nosc-leftnav-${id}`)).not.toBeInTheDocument();
    }

    // Dashboard leads, then the reporting group (Success Metrics) and the settings
    // group (API) each contribute one divider — no orphaned dividers for this state.
    const nav = screen.getByTestId('nosc-leftnav');
    expect(nav.querySelectorAll(SEPARATOR_SELECTOR).length).toBe(2);
  });

  it('hides Applications when orgs-and-apps is disabled', () => {
    const state = {
      ...fullyLicensedState,
      productFeatures: {
        ...fullyLicensedState.productFeatures,
        productFeatures: {
          ...fullyLicensedState.productFeatures.productFeatures,
          'orgs-and-apps': false,
        },
      },
    };
    renderLeftNav(state);
    expect(screen.queryByTestId('nosc-leftnav-applications')).not.toBeInTheDocument();
  });

  it('does not render the retired Reports, Advanced Search, or Vulnerability Lookup entries (CLM-42168)', () => {
    renderLeftNav(fullyLicensedState);
    expect(screen.queryByRole('link', { name: 'Reports' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Advanced Search' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).not.toBeInTheDocument();
  });

  describe('active-route highlighting', () => {
    it('marks Applications active on the unified surface', () => {
      renderLeftNav(fullyLicensedState, '#/applications');
      expect(screen.getByRole('link', { name: 'Applications', current: 'page' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current');
    });

    it('marks Applications active across the unified-surface sub-routes', () => {
      renderLeftNav(fullyLicensedState, '#/applications/my-app/violations');
      expect(screen.getByRole('link', { name: 'Applications', current: 'page' })).toBeInTheDocument();
    });

    it('keeps Components highlighted after its /components alias redirects to the Coming Soon stub', () => {
      renderLeftNav(fullyLicensedState, '#/coming-soon/components');
      expect(screen.getByRole('link', { name: 'Components' })).toHaveAttribute('aria-current', 'page');
    });

    it('highlights Legal on its clean embed entry path', () => {
      renderLeftNav(fullyLicensedState, '#/legal');
      expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
    });

    it('keeps Legal highlighted on the Applications tab sub-route via activeHrefs', () => {
      renderLeftNav(fullyLicensedState, '#/legal/applicationsDashboard');
      expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
    });

    it('keeps Legal highlighted on the Components tab sub-route via activeHrefs', () => {
      renderLeftNav(fullyLicensedState, '#/legal/componentsDashboard');
      expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
    });

    it('does not highlight Legal on an unrelated route', () => {
      renderLeftNav(fullyLicensedState, '#/dashboard');
      expect(screen.getByRole('link', { name: 'Legal' })).not.toHaveAttribute('aria-current', 'page');
    });

    it('keeps Legal highlighted on an Application Details deep-link sub-route', () => {
      // Regression guard: every legalDeepLinkStates.ts state mounts in-shell now, not just the two
      // dashboard tabs — a user drilling into a row from the dashboard must not see the rail entry
      // de-highlight.
      renderLeftNav(fullyLicensedState, '#/legal/application/app/stage/release');
      expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
    });

    it('keeps Legal highlighted on a Component Overview deep-link sub-route', () => {
      renderLeftNav(fullyLicensedState, '#/legal/component/abc123hash');
      expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
    });

    it('keeps Legal highlighted when the hash carries a query string', () => {
      renderLeftNav(fullyLicensedState, '#/legal/applicationsDashboard?tab=foo');
      expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
    });

    it('highlights Orgs & Policies on its clean embed entry path', () => {
      renderLeftNav(fullyLicensedState, '#/orgs-and-policies');
      expect(screen.getByRole('link', { name: 'Orgs & Policies' })).toHaveAttribute('aria-current', 'page');
    });

    it('keeps Orgs & Policies highlighted on the embedded root org view sub-route via activeHrefs', () => {
      // The entry redirects onto /management/view/organization/... — a user landing on the root org
      // view must not see the rail entry de-highlight.
      renderLeftNav(fullyLicensedState, '#/management/view/organization/ROOT_ORGANIZATION_ID');
      expect(screen.getByRole('link', { name: 'Orgs & Policies' })).toHaveAttribute('aria-current', 'page');
    });

    it('keeps Orgs & Policies highlighted on a deep management editor sub-route via activeHrefs', () => {
      renderLeftNav(fullyLicensedState, '#/management/edit/organization/ROOT_ORGANIZATION_ID/policy/abc');
      expect(screen.getByRole('link', { name: 'Orgs & Policies' })).toHaveAttribute('aria-current', 'page');
    });

    it('does not highlight Orgs & Policies on an unrelated route', () => {
      renderLeftNav(fullyLicensedState, '#/dashboard');
      expect(screen.getByRole('link', { name: 'Orgs & Policies' })).not.toHaveAttribute('aria-current', 'page');
    });

    it('highlights Success Metrics on the clean list path', () => {
      renderLeftNav(fullyLicensedState, '#/success-metrics');
      expect(screen.getByRole('link', { name: 'Success Metrics' })).toHaveAttribute('aria-current', 'page');
    });

    it('highlights Success Metrics on a report sub-route', () => {
      renderLeftNav(fullyLicensedState, '#/success-metrics/report-123');
      expect(screen.getByRole('link', { name: 'Success Metrics' })).toHaveAttribute('aria-current', 'page');
    });

    it('highlights API on the clean embed path', () => {
      renderLeftNav(fullyLicensedState, '#/api');
      expect(screen.getByRole('link', { name: 'API' })).toHaveAttribute('aria-current', 'page');
    });
  });
});
