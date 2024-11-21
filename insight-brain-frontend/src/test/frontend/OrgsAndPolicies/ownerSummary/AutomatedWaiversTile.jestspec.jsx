/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import AutomatedWaiversTile from 'MainRoot/OrgsAndPolicies/ownerSummary/AutomatedWaiversTile';
import { getCompositeSourceControlUrl, getWaiversConfigurationURL } from 'MainRoot/util/CLMLocation';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('AutomatedWaiversTile', () => {
  let axiosMock, renderComponent, state;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });
  beforeEach(() => {
    state = {
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
            id: 'orgId',
          },
        },
      },
    };

    jest.spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    jest.spyOn(productFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue({
      href: jest.fn().mockReturnValue('editPageHref'),
    });
  });

  renderComponent = (preloadedState = state) => {
    render(<AutomatedWaiversTile />, { preloadedState: preloadedState });
  };

  it('renders loading indicator for application', () => {
    renderComponent();
    const loading = screen.getByText('Loading…');
    expect(loading).toBeInTheDocument();
  });

  describe('when is for organization', () => {
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

    it('renders error alert on load error for organization', async () => {
      axiosMock.onGet(getCompositeSourceControlUrl('organization', 'organizationId')).reply(500, 'loadError');

      renderComponent();

      const error = await screen.findByRole('alert');
      expect(error).toBeVisible();
    });

    it('renders when isDeveloperDashboardEnabled is true, isAutoWaiverTrue is true and isInherited is false', async () => {
      axiosMock.onGet(getWaiversConfigurationURL('organization', 'organizationId')).reply(200, {
        isAutoWaiverEnabled: true,
      });

      await renderComponent(initialState);

      const title = await screen.findByRole('heading', { name: 'Waivers' });
      expect(title).not.toBeNull();
      const statusMessage = await screen.findByText('Automated Waivers are enabled');
      expect(statusMessage).toBeVisible();
    });

    it('renders when isDeveloperDashboardEnabled is true isAutoWaiverEnabled is false', async () => {
      axiosMock.onGet(getWaiversConfigurationURL('organization', 'organizationId')).reply(200, {
        isAutoWaiverEnabled: false,
      });

      renderComponent(initialState);

      const title = await screen.findByRole('heading', { name: 'Waivers' });
      expect(title).not.toBeNull();
      const statusMessage = await screen.findByText('Automated Waivers are disabled');
      expect(statusMessage).toBeVisible();
    });

    it('renders when isDeveloperDashboardEnabled is true, isAutoWaiverEnabled is true and isInhetired is true', async () => {
      const correctUrl = getWaiversConfigurationURL('organization', 'organizationId');
      const autoPolicyWaiverOwnerName = 'OrgTestName';
      axiosMock.onGet(correctUrl).reply(200, {
        isAutoWaiverEnabled: true,
        isInherited: true,
        autoPolicyWaiverOwnerName,
      });

      renderComponent(initialState);

      const title = await screen.findByRole('heading', { name: 'Waivers' });
      expect(title).not.toBeNull();
      const statusMessage = await screen.findByText(
        `Automated Waivers are enabled (Inheriting from ${autoPolicyWaiverOwnerName})`
      );
      expect(statusMessage).toBeVisible();
    });

    it('does not render when isDeveloperDashboardEnabled is false', async () => {
      jest.spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(false);

      renderComponent(initialState);

      const title = screen.queryByText('Waivers');
      expect(title).toBeNull();
    });
  });

  describe('when is for application', () => {
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

    it('renders error alert on load error for application', async () => {
      axiosMock.onGet(getCompositeSourceControlUrl('application', 'appId')).reply(500, 'loadError');
      renderComponent();

      const error = await screen.findByRole('alert');

      expect(error).toBeVisible();
    });

    it('renders when isDeveloperDashboardEnabled is true, isAutoWaiverEnabled is true and inherited is false', async () => {
      const correctUrl = getWaiversConfigurationURL('application', 'appId');
      axiosMock.onGet(correctUrl).reply(200, {
        isAutoWaiverEnabled: true,
        isInherited: false,
        autoPolicyWaiverOwnerName: 'AppTestName',
      });

      renderComponent(initialState);

      const title = await screen.findByRole('heading', { name: 'Waivers' });
      expect(title).not.toBeNull();
      const statusMessage = await screen.findByText('Automated Waivers are enabled');
      expect(statusMessage).toBeVisible();
    });

    it('renders when isDeveloeprDashboardEnabled is true and isAutoWaiverEnabled is false', async () => {
      const correctUrl = getWaiversConfigurationURL('application', 'appId');
      axiosMock.onGet(correctUrl).reply(200, {
        isAutoWaiverEnabled: false,
      });

      renderComponent(initialState);

      const title = await screen.findByRole('heading', { name: 'Waivers' });
      expect(title).not.toBeNull();
      const statusMessage = await screen.findByText('Automated Waivers are disabled');
      expect(statusMessage).toBeVisible();
    });

    it('renders when isDeveloperDashboardEnabled is true, isAutoWaiverEnabled is true and isInhetired is true', async () => {
      const correctUrl = getWaiversConfigurationURL('application', 'appId');
      const autoPolicyWaiverOwnerName = 'AppTestName';
      axiosMock.onGet(correctUrl).reply(200, {
        isAutoWaiverEnabled: true,
        isInherited: true,
        autoPolicyWaiverOwnerName,
      });

      renderComponent(initialState);

      const title = await screen.findByRole('heading', { name: 'Waivers' });
      expect(title).not.toBeNull();
      const statusMessage = await screen.findByText(
        `Automated Waivers are enabled (Inheriting from ${autoPolicyWaiverOwnerName})`
      );
      expect(statusMessage).toBeVisible();
    });

    it('does not render when isDeveloperDashboardEnabled is false', async () => {
      jest.spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(false);

      renderComponent(initialState);

      const title = screen.queryByText('Waivers');
      expect(title).toBeNull();
    });

    it('does not render when isAutoWaiverEnabled is false', async () => {
      jest.spyOn(productFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(false);

      renderComponent(initialState);

      const title = screen.queryByText('Waivers');
      expect(title).toBeNull();
    });

    it('does not render when isSbomManager is true', async () => {
      jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(false);
      renderComponent(initialState);

      const title = screen.queryByText('Waivers');
      expect(title).toBeNull();
    });
  });
});
