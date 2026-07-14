/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { render, screen } from 'TestRoot/SpecUtil';
import LeftNav from 'MainRoot/nosc/shell/LeftNav';
import * as urlUtil from 'MainRoot/util/urlUtil';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import { COLLAPSED_KEY } from 'MainRoot/nosc/shell/useLeftNavCollapsed';

beforeAll(installRadixJsdomShims);

// bundleIndexUrl() builds an absolute URL from window.location.href, which jsdom's default
// test URL doesn't satisfy (no `/assets/` segment) — see ClassicToggleButton.jestspec.tsx for
// the same mock, used there for the same reason.
beforeEach(() => {
  urlUtil._setBaseUrlForTesting('http://localhost');
  jest
    .spyOn(urlUtil, 'bundleIndexUrl')
    .mockImplementation((_bundle, hashPath) => `http://localhost/assets/nexus-one/index.html#${hashPath ?? ''}`);
});

// Licensed Lifecycle tenant with the report-list feature ON, so a lingering
// Reports entry would render if it were still built.
const licensedLifecycleState = {
  userSession: { data: { username: 'admin' }, loading: false, error: null },
  productLicense: { installed: true, loading: false, license: { products: [] } },
  productFeatures: {
    loading: false,
    loadError: null,
    productFeatures: {
      dashboard: true,
      'orgs-and-apps': true,
      'reports-list': true,
    },
  },
  successMetricsConfiguration: {},
};

// Preloaded state with advanced-legal-pack enabled for Legal navigation tests
const legalEnabledState = {
  ...licensedLifecycleState,
  userSession: { data: { username: 'jdoe' } },
  productFeatures: {
    ...licensedLifecycleState.productFeatures,
    productFeatures: {
      ...licensedLifecycleState.productFeatures.productFeatures,
      'advanced-legal-pack': true,
    },
  },
};

function renderLeftNav({ hash = '#/home', preloadedState = licensedLifecycleState } = {}) {
  window.location.hash = hash;
  return render(
    <Theme accentColor={BRAND_ACCENT} grayColor="slate" radius="medium" scaling="100%">
      <LeftNav />
    </Theme>,
    { preloadedState }
  );
}

describe('LeftNav', () => {
  afterEach(() => {
    urlUtil._setBaseUrlForTesting('');
    window.location.hash = '';
    window.localStorage.removeItem(COLLAPSED_KEY);
  });

  it('shows a single Applications entry and no separate Reports entry', () => {
    renderLeftNav();

    expect(screen.getByRole('link', { name: 'Applications' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Reports' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Advanced Search' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).not.toBeInTheDocument();
  });

  it('marks Applications active on the unified surface', () => {
    renderLeftNav({ hash: '#/applications' });

    expect(screen.getByRole('link', { name: 'Applications', current: 'page' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current');
  });

  it('marks Applications active across the unified-surface sub-routes', () => {
    renderLeftNav({ hash: '#/applications/my-app/violations' });

    expect(screen.getByRole('link', { name: 'Applications', current: 'page' })).toBeInTheDocument();
  });

  it('hides Applications for an unlicensed tenant', () => {
    renderLeftNav({
      preloadedState: {
        ...licensedLifecycleState,
        productLicense: { installed: false, loading: false, license: { products: [] } },
      },
    });

    expect(screen.queryByRole('link', { name: 'Applications' })).not.toBeInTheDocument();
  });

  it('hides Applications when orgs-and-apps is disabled', () => {
    renderLeftNav({
      preloadedState: {
        ...licensedLifecycleState,
        productFeatures: {
          ...licensedLifecycleState.productFeatures,
          productFeatures: { ...licensedLifecycleState.productFeatures.productFeatures, 'orgs-and-apps': false },
        },
      },
    });

    expect(screen.queryByRole('link', { name: 'Applications' })).not.toBeInTheDocument();
  });

  it('highlights Legal on its own Coming Soon entry route', () => {
    renderLeftNav({ hash: '#/coming-soon/legal', preloadedState: legalEnabledState });
    expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
  });

  it('keeps Legal highlighted on the Applications tab sub-route via activeHrefs', () => {
    renderLeftNav({ hash: '#/legal/applicationsDashboard', preloadedState: legalEnabledState });
    expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
  });

  it('keeps Legal highlighted on the Components tab sub-route via activeHrefs', () => {
    renderLeftNav({ hash: '#/legal/componentsDashboard', preloadedState: legalEnabledState });
    expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
  });

  it('does not highlight Legal on an unrelated route', () => {
    renderLeftNav({ hash: '#/dashboard', preloadedState: legalEnabledState });
    expect(screen.getByRole('link', { name: 'Legal' })).not.toHaveAttribute('aria-current', 'page');
  });

  it('keeps Legal highlighted on an Application Details deep-link sub-route', () => {
    // Regression guard: every legalDeepLinkStates.ts state mounts in-shell now, not just the two
    // dashboard tabs — a user drilling into a row from the dashboard must not see the rail entry
    // de-highlight.
    renderLeftNav({ hash: '#/legal/application/app/stage/release', preloadedState: legalEnabledState });
    expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
  });

  it('keeps Legal highlighted on a Component Overview deep-link sub-route', () => {
    renderLeftNav({ hash: '#/legal/component/abc123hash', preloadedState: legalEnabledState });
    expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
  });

  it('keeps Legal highlighted when the hash carries a query string', () => {
    renderLeftNav({ hash: '#/legal/applicationsDashboard?tab=foo', preloadedState: legalEnabledState });
    expect(screen.getByRole('link', { name: 'Legal' })).toHaveAttribute('aria-current', 'page');
  });
});
