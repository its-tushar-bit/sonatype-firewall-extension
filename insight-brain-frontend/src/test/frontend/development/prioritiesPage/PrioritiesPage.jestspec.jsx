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
  fireEvent,
  setupPortalContainer,
  removePortalContainer,
} from 'TestRoot/SpecUtil';
import PrioritiesPage from 'MainRoot/development/prioritiesPage/PrioritiesPage';

import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE } from 'MainRoot/development/developmentDashboard/LicenseLockScreen';

import { metadata } from 'TestRoot/componentDetails/data';
import { getReportMetadataUrl } from 'MainRoot/util/CLMLocation';

import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

const publicAppId = 'testPublicAppId';
const scanId = 'testScanId';

describe('PrioritiesPage', () => {
  let renderComponent, selectIsDeveloperDashboardEnabled, axiosMock, stateGoSpy, hrefSpy;

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

    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    hrefSpy = jest.fn('href').mockImplementation((stateName) => stateName);
    const routerContextMock = { href: hrefSpy };
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

    const viewFullReportBtn = await screen.findByRole('button', { name: /view full report/i });
    expect(viewFullReportBtn).toBeInTheDocument();
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

    it('view full report button navigates to URL based on correct stateName', async () => {
      renderComponent(preloadedState);
      const viewFullReportBtn = await screen.findByRole('button', { name: /view full report/i });
      expect(viewFullReportBtn).toBeInTheDocument();

      fireEvent.click(viewFullReportBtn);

      expect(stateGoSpy).toHaveBeenCalledWith('appReportPageWithinPrioritiesPageContainerFromReports.policy', {
        publicId: publicAppId,
        scanId,
      });
    });

    it('back button navigates back to Reports Page', async () => {
      setupPortalContainer();
      renderComponent(preloadedState);

      const backBtn = await screen.findByRole('link', { name: /back to reports/i });
      expect(backBtn).toBeInTheDocument();
      expect(backBtn).toHaveAttribute('href', 'violations');
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

    it('view full report button navigates to URL based on correct stateName', async () => {
      renderComponent(preloadedState);

      const viewFullReportBtn = await screen.findByRole('button', { name: /view full report/i });
      expect(viewFullReportBtn).toBeInTheDocument();

      fireEvent.click(viewFullReportBtn);

      expect(stateGoSpy).toHaveBeenCalledWith('appReportPageWithinPrioritiesPageContainerFromAppReport.policy', {
        publicId: publicAppId,
        scanId,
      });
    });

    it('back button navigates back to App Reports Page', async () => {
      setupPortalContainer();
      renderComponent(preloadedState);

      const backBtn = await screen.findByRole('link', { name: /back to application report/i });
      expect(backBtn).toBeInTheDocument();
      expect(backBtn).toHaveAttribute('href', 'applicationReport.policy');
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

    it('view full report button navigates to URL based on correct stateName', async () => {
      renderComponent(preloadedState);
      const viewFullReportBtn = await screen.findByRole('button', { name: /view full report/i });
      expect(viewFullReportBtn).toBeInTheDocument();

      fireEvent.click(viewFullReportBtn);

      expect(stateGoSpy).toHaveBeenCalledWith('appReportPageWithinPrioritiesPageContainerFromDashboard.policy', {
        publicId: publicAppId,
        scanId,
      });
    });

    it('back button navigates back to Developer Dashboard', async () => {
      setupPortalContainer();
      renderComponent(preloadedState);

      const backBtn = await screen.findByRole('link', { name: /back to developer dashboard/i });
      expect(backBtn).toBeInTheDocument();
      expect(backBtn).toHaveAttribute('href', 'integrations');
    });
  });
});
