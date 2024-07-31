/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, axiosMockAdapter, setupPortalContainer, removePortalContainer } from 'TestRoot/SpecUtil';
import PrioritiesPage from 'MainRoot/development/prioritiesPage/PrioritiesPage';

import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE } from 'MainRoot/development/developmentDashboard/LicenseLockScreen';

import { metadata } from 'TestRoot/componentDetails/data';
import { getReportMetadataUrl } from 'MainRoot/util/CLMLocation';

import * as routerStateContext from 'MainRoot/react/RouterStateContext';

const publicAppId = 'testPublicAppId';
const scanId = 'testScanId';

describe('PrioritiesPage', () => {
  let renderComponent, selectIsDeveloperDashboardEnabled, axiosMock, hrefSpy, getSpy;

  const defaultPreloadedState = {
    router: {
      currentParams: {
        publicAppId,
        scanId,
      },
    },
  };

  beforeEach(() => {
    selectIsDeveloperDashboardEnabled = jest
      .spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled')
      .mockReturnValue(true);

    axiosMock = axiosMockAdapter();

    renderComponent = (preloadedState) =>
      render(<PrioritiesPage />, { preloadedState: preloadedState || defaultPreloadedState });

    axiosMock.onGet(getReportMetadataUrl(publicAppId, scanId)).reply(200, metadata);

    hrefSpy = jest.fn('href').mockImplementation((stateName) => stateName);
    getSpy = jest.fn('get').mockImplementation((state) => state);
    const routerContextMock = { href: hrefSpy, get: getSpy };
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);
  });

  it('renders an alert in place of content given the feature is not enabled for the license', async () => {
    selectIsDeveloperDashboardEnabled.mockReturnValue(false);

    renderComponent();

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE);

    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
  });

  it('renders a loading spinner', () => {
    renderComponent();

    const loading = screen.getByText('Loading…');
    expect(loading).toBeInTheDocument();
  });

  it('renders an alert when there is a network error', async () => {
    axiosMock.onGet(getReportMetadataUrl(publicAppId, scanId)).reply(500, 'something went wrong');
    renderComponent();

    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  it('renders a "View Full Report" button', async () => {
    renderComponent();

    const viewFullReportBtn = await screen.findByRole('link', { name: /view full report/i });
    expect(viewFullReportBtn).toBeInTheDocument();
    expect(viewFullReportBtn).toHaveAttribute('target', '_blank');
  });

  describe('when priorities page is navigated from Reports page', () => {
    const preloadedState = {
      router: {
        currentParams: {
          publicAppId,
          scanId,
        },
        currentState: {
          name: 'prioritiesPageFromReports',
        },
      },
    };

    afterEach(() => removePortalContainer());

    it('back button navigates back to Reports Page', async () => {
      setupPortalContainer();
      renderComponent(preloadedState);

      const backBtn = await screen.findByRole('link', { name: /back to reports/i });
      expect(backBtn).toBeInTheDocument();
      expect(backBtn).toHaveAttribute('href', 'developer.reports');
    });
  });

  describe('when priorities page is navigated from App Report page', () => {
    const preloadedState = {
      router: {
        currentParams: {
          publicAppId,
          scanId,
        },
        currentState: {
          name: 'prioritiesPageFromAppReport',
        },
      },
    };

    afterEach(() => removePortalContainer());

    it('back button navigates back to reports Page', async () => {
      setupPortalContainer();
      renderComponent(preloadedState);

      const backBtn = await screen.findByRole('link', { name: /back to reports/i });
      expect(backBtn).toBeInTheDocument();
      expect(backBtn).toHaveAttribute('href', 'developer.reports');
    });
  });

  describe('when priorities page is navigated from Developer Dashboard', () => {
    const preloadedState = {
      router: {
        currentParams: {
          publicAppId,
          scanId,
        },
        currentState: {
          name: 'prioritiesPageFromDashboard',
        },
      },
    };

    afterEach(() => removePortalContainer());

    it('back button navigates back to Developer Dashboard', async () => {
      setupPortalContainer();
      renderComponent(preloadedState);

      const backBtn = await screen.findByRole('link', { name: /back to developer dashboard/i });
      expect(backBtn).toBeInTheDocument();
      expect(backBtn).toHaveAttribute('href', 'developer.dashboard');
    });
  });
});
