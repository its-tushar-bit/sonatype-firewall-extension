/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import { getApplicableAutoWaiversURL } from 'MainRoot/util/CLMLocation';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import AutoWaiversTile from 'MainRoot/OrgsAndPolicies/ownerSummary/AutoWaiversTile';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import {
  mockResponse_Application_All_Local,
  mockResponse_Application_Local_Org_RootOrg,
  mockResponse_Application_Local_RootOrg,
  mockResponse_Organization_Local,
  mockResponse_Organization_Local_RootOrg,
  mockResponse_RootOrg_Local,
} from '../autoWaiversConfiguration/mockApplicableWaiversResponses';

describe('AutoWaiversTile', () => {
  let axiosMock, renderComponent;

  const defaultPreloadedState = {
    router: {
      currentState: { name: 'organization' },
      currentParams: {
        organizationId: 'organizationId',
      },
    },
    orgsAndPolicies: {
      sourceControl: {
        loading: false,
      },
      root: {
        selectedOwner: {
          name: 'org',
          id: 'organizationId',
        },
      },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    jest.spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    jest.spyOn(productFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    jest.spyOn(productFeaturesSelectors, 'selectHasAutoWaiverManagement').mockReturnValue(true);
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue({
      href: jest.fn().mockReturnValue('editPageHref'),
    });
  });

  renderComponent = (preloadedState) => {
    render(<AutoWaiversTile />, { preloadedState: preloadedState || defaultPreloadedState });
  };

  it('renders loading indicator initially', () => {
    renderComponent();
    const loading = screen.getByText('Loading…');
    expect(loading).toBeInTheDocument();
  });

  it('renders error message when load fails', async () => {
    axiosMock.onGet(getApplicableAutoWaiversURL('organization', 'organizationId')).reply(404, 'some_error');
    renderComponent();
    const error = await screen.findByRole('alert');
    expect(error).toBeInTheDocument();
    expect(error).toHaveTextContent('some_error');
  });

  it('does not render when isDeveloperDashboardEnabled is false', async () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(false);

    renderComponent();
    expect(screen.queryByTestId('iq-auto-waivers-tile')).not.toBeInTheDocument();
  });

  it('does not render when selectIsAutoWaiversEnabled is false', async () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(false);

    renderComponent();
    expect(screen.queryByTestId('iq-auto-waivers-tile')).not.toBeInTheDocument();
  });

  it('renders contents when feature flags are enabled', async () => {
    renderComponent();
    expect(await screen.findByTestId('iq-auto-waivers-tile')).toBeInTheDocument();
  });

  it('renders a clickable link to the edit page', async () => {
    axiosMock
      .onGet(getApplicableAutoWaiversURL('organization', 'organizationId'))
      .reply(200, mockResponse_Organization_Local);

    renderComponent();

    const editLink = await screen.findByRole('link', { name: '3 local, 0 inherited' });
    expect(editLink).toBeInTheDocument();
    expect(editLink).toHaveAttribute('href', 'editPageHref');
  });

  describe('when at the organization level', () => {
    const initialState = {
      router: {
        currentState: { name: 'organization' },
        currentParams: {
          organizationId: 'organizationId',
        },
      },
      orgsAndPolicies: {
        waivers: {
          loading: false,
        },
        root: {
          selectedOwner: {
            name: 'org',
            id: 'organizationId',
          },
        },
      },
    };

    it('for root org only renders local autowaivers', async () => {
      const initialState = {
        router: {
          currentState: { name: 'organization' },
          currentParams: {
            organizationId: 'ROOT_ORGANIZATION_ID',
          },
        },
        orgsAndPolicies: {
          waivers: {
            loading: false,
          },
          root: {
            selectedOwner: {
              name: 'Root Organization',
              id: 'ROOT_ORGANIZATION_ID',
            },
          },
        },
      };
      axiosMock
        .onGet(getApplicableAutoWaiversURL('organization', 'ROOT_ORGANIZATION_ID'))
        .reply(200, mockResponse_RootOrg_Local);

      renderComponent(initialState);

      expect(await screen.findByRole('heading', { name: 'Auto-Waivers' })).toBeInTheDocument();
      expect(screen.getByText('3 local')).toBeInTheDocument();
      expect(screen.queryByText('inherited')).not.toBeInTheDocument();
    });

    it('for non-root org with 3 local autowaivers', async () => {
      axiosMock
        .onGet(getApplicableAutoWaiversURL('organization', 'organizationId'))
        .reply(200, mockResponse_Organization_Local);

      renderComponent(initialState);

      expect(await screen.findByRole('heading', { name: 'Auto-Waivers' })).toBeInTheDocument();
      expect(screen.getByText('3 local, 0 inherited')).toBeInTheDocument();
    });

    it('for non-root org with 2 local autowaivers, and 1 inherited autowaiver', async () => {
      axiosMock
        .onGet(getApplicableAutoWaiversURL('organization', 'organizationId'))
        .reply(200, mockResponse_Organization_Local_RootOrg);

      renderComponent(initialState);

      expect(await screen.findByRole('heading', { name: 'Auto-Waivers' })).toBeInTheDocument();
      expect(screen.getByText('2 local, 1 inherited')).toBeInTheDocument();
    });
  });

  describe('when at the application level', () => {
    const initialState = {
      router: {
        currentState: { name: 'application' },
        currentParams: {
          applicationId: 'appId',
        },
      },
      orgsAndPolicies: {
        waivers: {
          loading: false,
        },
        root: {
          selectedOwner: {
            name: 'app',
            id: 'appId',
            publicId: 'appPublicId',
          },
        },
      },
    };

    it('renders correctly with 3 local autowaivers', async () => {
      axiosMock
        .onGet(getApplicableAutoWaiversURL('application', 'appId'))
        .reply(200, mockResponse_Application_All_Local);

      renderComponent(initialState);

      expect(await screen.findByRole('heading', { name: 'Auto-Waivers' })).toBeInTheDocument();
      expect(screen.getByText('3 local, 0 inherited')).toBeInTheDocument();
    });

    it('renders correctly with 1 local autowaiver and 2 inherited from root org', async () => {
      axiosMock
        .onGet(getApplicableAutoWaiversURL('application', 'appId'))
        .reply(200, mockResponse_Application_Local_RootOrg);

      renderComponent(initialState);

      expect(await screen.findByRole('heading', { name: 'Auto-Waivers' })).toBeInTheDocument();
      expect(screen.getByText('1 local, 2 inherited')).toBeInTheDocument();
    });

    it('renders correctly with 1 local autowaiver, 1 inherited from org, and 1 inherited from root org', async () => {
      axiosMock
        .onGet(getApplicableAutoWaiversURL('application', 'appId'))
        .reply(200, mockResponse_Application_Local_Org_RootOrg);

      renderComponent(initialState);

      expect(await screen.findByRole('heading', { name: 'Auto-Waivers' })).toBeInTheDocument();
      expect(screen.getByText('1 local, 2 inherited')).toBeInTheDocument();
    });
  });

  describe('Pro Tier Gating', () => {
    beforeEach(() => {
      jest.spyOn(productFeaturesSelectors, 'selectHasAutoWaiverManagement').mockReturnValue(false);
    });

    it('shows enterprise banner instead of auto-waivers content when feature is absent', () => {
      renderComponent();
      expect(screen.getAllByRole('heading', { name: 'Auto-Waivers' }).length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText(/Automatically apply waivers/)).toBeInTheDocument();
    });

    it('does not show loading indicator or waiver data', () => {
      renderComponent();
      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    });
  });
});
