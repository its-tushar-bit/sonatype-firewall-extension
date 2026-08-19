/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import NavigationContainer from 'MainRoot/navigationContainer/NavigationContainer';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

describe('NavigationContainer', () => {
  let mockRouterState, mock$rootScope, mock$state;

  const defaultPreloadedState = {
    advancedSearchConfig: {
      serverData: {
        isEnabled: false,
      },
    },
    successMetricsConfiguration: {
      serverData: {
        enabled: false,
      },
    },
    productFeatures: {
      productFeatures: {
        'advanced-legal-pack': false,
        firewall: false,
        dashboard: false,
        'waivers-dashboard': false,
        'reports-list': false,
        'api-page': false,
        'show-version': false,
        'developer-dashboard': false,
        'orgs-and-apps': false,
        'sbom-manager': false,
        'integrated-enterprise-reporting': false,
        'alp-for-sbom-manager': false,
      },
      loading: false,
      loadError: null,
    },
    router: {
      currentState: { name: 'home' },
      currentParams: {},
      isStandaloneFirewall: false,
      isSbomManager: false,
      isStandaloneDeveloper: false,
    },
    productLicense: {
      isSbomManagerOnlyLicense: false,
      isFirewallOnlyLicense: false,
      loadingProducts: false,
      installed: true,
      license: {
        products: ['Sonatype Lifecycle'],
      },
    },
    userSession: {
      data: {
        username: 'testuser',
      },
      loading: false,
      error: null,
      shouldDisplayPasswordWarning: false,
    },
  };

  beforeEach(() => {
    mock$rootScope = {
      $on: jest.fn().mockReturnValue(jest.fn()),
    };

    mock$state = {
      current: { name: 'home' },
      href: jest.fn().mockReturnValue('#/home'),
      get: jest.fn(),
      includes: jest.fn(),
    };

    mockRouterState = {
      current: { name: 'home' },
      href: jest.fn().mockReturnValue('#/home'),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);
  });

  const renderComponent = (props = {}, preloadedState) => {
    return render(
      <NavigationContainer
        productEdition="lifecycle"
        clmServerVersion="123.456.789"
        $rootScope={mock$rootScope}
        $state={mock$state}
        {...props}
      />,
      {
        preloadedState: preloadedState || defaultPreloadedState,
      }
    );
  };

  it('renders navigation sidebar', () => {
    renderComponent();
    expect(screen.getByRole('navigation', { name: 'global sidebar' })).toBeInTheDocument();
  });

  it('shows licensed navigation links when user is logged in and licensed', () => {
    renderComponent();

    expect(screen.getByRole('link', { name: /Orgs and Policies/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Vulnerability Lookup/i })).toBeInTheDocument();
  });

  it('hides licensed navigation links when user is not logged in', () => {
    const notLoggedInState = {
      ...defaultPreloadedState,
      userSession: {
        ...defaultPreloadedState.userSession,
        data: null,
      },
    };

    renderComponent({}, notLoggedInState);

    expect(screen.queryByRole('link', { name: /Orgs and Policies/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Vulnerability Lookup/i })).not.toBeInTheDocument();
  });

  it('hides licensed navigation links when not licensed', () => {
    const notLicensedState = {
      ...defaultPreloadedState,
      productLicense: {
        ...defaultPreloadedState.productLicense,
        installed: false,
        license: {
          products: [],
        },
      },
    };

    renderComponent({}, notLicensedState);

    expect(screen.queryByRole('link', { name: /Orgs and Policies/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Vulnerability Lookup/i })).not.toBeInTheDocument();
  });

  it('shows Advanced Search link when advanced search is enabled', () => {
    const advancedSearchEnabledState = {
      ...defaultPreloadedState,
      advancedSearchConfig: {
        serverData: {
          isEnabled: true,
        },
      },
    };

    renderComponent({}, advancedSearchEnabledState);

    expect(screen.getByRole('link', { name: /Advanced Search/i })).toBeInTheDocument();
  });

  it('hides Advanced Search link when advanced search is disabled', () => {
    const advancedSearchDisabledState = {
      ...defaultPreloadedState,
      advancedSearchConfig: {
        serverData: {
          isEnabled: false,
        },
      },
    };

    renderComponent({}, advancedSearchDisabledState);

    expect(screen.queryByRole('link', { name: /Advanced Search/i })).not.toBeInTheDocument();
  });

  it('shows Success Metrics link when success metrics is enabled, orgs/apps enabled, and user is logged in with license', () => {
    const successMetricsEnabledState = {
      ...defaultPreloadedState,
      successMetricsConfiguration: {
        serverData: {
          enabled: true,
        },
      },
      productFeatures: {
        ...defaultPreloadedState.productFeatures,
        productFeatures: {
          ...defaultPreloadedState.productFeatures.productFeatures,
          'orgs-and-apps': true,
          'reports-list': true,
        },
      },
    };

    renderComponent({}, successMetricsEnabledState);

    expect(screen.getByRole('link', { name: /Success Metrics/i })).toBeInTheDocument();
  });

  it('shows Legal link when legal pack is supported and user is logged in with license', () => {
    const legalEnabledState = {
      ...defaultPreloadedState,
      productFeatures: {
        ...defaultPreloadedState.productFeatures,
        productFeatures: {
          ...defaultPreloadedState.productFeatures.productFeatures,
          'advanced-legal-pack': true,
        },
      },
    };

    renderComponent({}, legalEnabledState);

    expect(screen.getByRole('link', { name: /Legal/i })).toBeInTheDocument();
  });
});
