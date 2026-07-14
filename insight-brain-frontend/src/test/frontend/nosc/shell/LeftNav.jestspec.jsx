/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { render, screen } from 'TestRoot/SpecUtil';
import LeftNav from 'MainRoot/nosc/shell/LeftNav';
import { COLLAPSED_KEY } from 'MainRoot/nosc/shell/useLeftNavCollapsed';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';

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
  advancedSearchConfig: {},
  successMetricsConfiguration: {},
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
  beforeEach(() => {
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    _setBaseUrlForTesting('');
    window.location.hash = '';
    window.localStorage.removeItem(COLLAPSED_KEY);
  });

  it('shows a single Applications entry and no separate Reports entry', () => {
    renderLeftNav();

    expect(screen.getByRole('link', { name: 'Applications' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Reports' })).not.toBeInTheDocument();
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
});
