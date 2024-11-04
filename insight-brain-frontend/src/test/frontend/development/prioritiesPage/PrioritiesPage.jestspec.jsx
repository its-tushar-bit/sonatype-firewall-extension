/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  render,
  screen,
  axiosMockAdapter,
  setupPortalContainer,
  removePortalContainer,
  within,
  fireEvent,
} from 'TestRoot/SpecUtil';
import PrioritiesPage from 'MainRoot/development/prioritiesPage/PrioritiesPage';

import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE } from 'MainRoot/development/developmentDashboard/LicenseLockScreen';

import { getReportBomUrl, getReportMetadataUrl } from 'MainRoot/util/CLMLocation';

import * as routerStateContext from 'MainRoot/react/RouterStateContext';

const publicAppId = 'testPublicAppId';
const scanId = 'testScanId';
const metadata = {
  reportTime: 1703098535137,
  reportTitle: 'Release Report',
  application: {
    name: 'TestApp',
    nameLowercaseNoWhitespace: 'testapp',
    id: '726e6f86d1d54ad0ae86439853d88fef',
    publicId: 'testapp',
    publicIdLowercase: 'testapp',
  },
  stageId: 'release',
};

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

  describe('network requests', () => {
    it('makes network requests to get the metadata on load', async () => {
      renderComponent();

      expect(axiosMock.history.get.length).toBe(2);
      expect(axiosMock.history.get[0].url).toBe(getReportBomUrl(publicAppId, scanId));
      expect(axiosMock.history.get[1].url).toBe(getReportMetadataUrl(publicAppId, scanId));
    });

    describe('when failed', () => {
      beforeEach(() => {
        axiosMock.onGet(getReportBomUrl(publicAppId, scanId)).reply(500, 'something went wrong');
        axiosMock.onGet(getReportMetadataUrl(publicAppId, scanId)).reply(500, 'something went wrong');
      });

      it('renders an alert with a retry button makes network call to get metadata', async () => {
        renderComponent();

        expect(axiosMock.history.get.length).toBe(2);

        const alert = await screen.findByRole('alert');
        const retryBtn = within(alert).getByRole('button', { name: /retry/i });
        expect(retryBtn).toBeInTheDocument();
        fireEvent.click(retryBtn);
        expect(axiosMock.history.get.length).toBe(4);
        expect(axiosMock.history.get[2].url).toBe(getReportBomUrl(publicAppId, scanId));
        expect(axiosMock.history.get[3].url).toBe(getReportMetadataUrl(publicAppId, scanId));
      });
    });
  });

  describe('back button', () => {
    describe('when priorities page is navigated from Reports page', () => {
      const preloadedState = {
        router: {
          ...defaultPreloadedState.router,
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

    describe('when priorities page is navigated from Developer Dashboard', () => {
      const preloadedState = {
        router: {
          ...defaultPreloadedState.router,
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
});
