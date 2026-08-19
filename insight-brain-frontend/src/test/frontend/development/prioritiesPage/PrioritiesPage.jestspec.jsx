/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, axiosMockAdapter, within, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import PrioritiesPage from 'MainRoot/development/prioritiesPage/PrioritiesPage';

import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE } from 'MainRoot/development/developmentDashboard/LicenseLockScreen';

import {
  getDependenciesUrl,
  getPrioritiesPageTableData,
  getReportBomUrl,
  getReportDataUrl,
  getReportMetadataUrl,
  getReportPartialMatchedUrl,
  getReportPolicyThreatsUrl,
} from 'MainRoot/util/CLMLocation';

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

    axiosMock.onGet(getReportBomUrl(publicAppId, scanId)).reply(200, { aaData: [] });
    axiosMock.onGet(getReportMetadataUrl(publicAppId, scanId)).reply(200, metadata);
    axiosMock.onGet(getReportPolicyThreatsUrl(publicAppId, scanId)).reply(200, { version: 3, aaData: [] });
    axiosMock.onGet(getReportDataUrl(publicAppId, scanId)).reply(200, {});
    axiosMock.onGet(getReportPartialMatchedUrl(publicAppId, scanId)).reply(200, { aaData: [] });
    axiosMock.onGet(getDependenciesUrl(publicAppId, scanId)).reply(200, { dependencyTree: { children: [] } });

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

      expect(axiosMock.history.get.length).toBe(6);
      expect(axiosMock.history.get[0].url).toBe(getReportBomUrl(publicAppId, scanId));
      expect(axiosMock.history.get[1].url).toBe(getReportMetadataUrl(publicAppId, scanId));
      expect(axiosMock.history.get[2].url).toBe(getReportPolicyThreatsUrl(publicAppId, scanId));
      expect(axiosMock.history.get[3].url).toBe(getReportDataUrl(publicAppId, scanId));
      expect(axiosMock.history.get[4].url).toBe(getReportPartialMatchedUrl(publicAppId, scanId));
      expect(axiosMock.history.get[5].url).toBe(getDependenciesUrl(publicAppId, scanId));
    });

    describe('when failed', () => {
      beforeEach(() => {
        axiosMock.onGet(getReportBomUrl(publicAppId, scanId)).reply(500, 'something went wrong');
        axiosMock.onGet(getReportMetadataUrl(publicAppId, scanId)).reply(500, 'something went wrong');
        axiosMock.onGet(getReportPolicyThreatsUrl(publicAppId, scanId)).reply(500, 'something went wrong');
        axiosMock.onGet(getReportDataUrl(publicAppId, scanId)).reply(500, 'something went wrong');
        axiosMock.onGet(getReportPartialMatchedUrl(publicAppId, scanId)).reply(500, 'something went wrong');
        axiosMock.onGet(getDependenciesUrl(publicAppId, scanId)).reply(500, 'something went wrong');
      });

      it('renders an alert with a retry button makes network call to get metadata', async () => {
        const user = userEvent.setup();
        renderComponent();

        // Wait for the initial error state to be handled
        const alert = await screen.findByRole('alert');
        expect(axiosMock.history.get.length).toBe(6);

        const retryBtn = within(alert).getByRole('button', { name: /retry/i });
        expect(retryBtn).toBeInTheDocument();

        // Reconfigure mocks to return success before retry
        axiosMock.onGet(getReportBomUrl(publicAppId, scanId)).reply(200, { aaData: [] });
        axiosMock.onGet(getReportMetadataUrl(publicAppId, scanId)).reply(200, metadata);
        axiosMock.onGet(getReportPolicyThreatsUrl(publicAppId, scanId)).reply(200, { version: 3, aaData: [] });
        axiosMock.onGet(getReportDataUrl(publicAppId, scanId)).reply(200, {});
        axiosMock.onGet(getReportPartialMatchedUrl(publicAppId, scanId)).reply(200, { aaData: [] });
        axiosMock.onGet(getDependenciesUrl(publicAppId, scanId)).reply(200, { dependencyTree: { children: [] } });

        await user.click(retryBtn);

        // Wait for retry requests to complete
        await waitFor(() => {
          expect(axiosMock.history.get.length).toBe(13);
        });

        // Verify all retry requests were made
        expect(axiosMock.history.get[6].url).toBe(getReportBomUrl(publicAppId, scanId));
        expect(axiosMock.history.get[7].url).toBe(getReportMetadataUrl(publicAppId, scanId));
        expect(axiosMock.history.get[8].url).toBe(getReportPolicyThreatsUrl(publicAppId, scanId));
        expect(axiosMock.history.get[9].url).toBe(getReportDataUrl(publicAppId, scanId));
        expect(axiosMock.history.get[10].url).toBe(getReportPartialMatchedUrl(publicAppId, scanId));
        expect(axiosMock.history.get[11].url).toBe(getDependenciesUrl(publicAppId, scanId));
        expect(axiosMock.history.get[12].url).toBe(getPrioritiesPageTableData(publicAppId, scanId));
      });
    });
  });
});
