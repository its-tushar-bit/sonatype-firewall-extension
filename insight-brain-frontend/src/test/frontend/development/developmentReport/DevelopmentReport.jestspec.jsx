/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import DevelopmentReport from 'MainRoot/development/developmentReport/DevelopmentReport';

import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE } from 'MainRoot/development/developmentDashboard/LicenseLockScreen';

import { bomData, metadata, policyThreatsData, reportData, dependenciesData } from 'TestRoot/componentDetails/data';
import {
  getDependenciesUrl,
  getReportBomUrl,
  getReportDataUrl,
  getReportMetadataUrl,
  getReportPartialMatchedUrl,
  getReportPolicyThreatsUrl,
} from 'MainRoot/util/CLMLocation';

const appId = 'testAppId';
const scanId = 'testScanId';

describe('DevelopmentReport', () => {
  let renderComponent, selectIsDeveloperDashboardEnabled, axiosMock;

  const defaultPreloadedState = {
    router: {
      currentParams: {
        appId,
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
      render(<DevelopmentReport />, { preloadedState: preloadedState || defaultPreloadedState });

    axiosMock.onGet(getReportBomUrl(appId, scanId)).reply(200, bomData);
    axiosMock.onGet(getReportMetadataUrl(appId, scanId)).reply(200, metadata);
    axiosMock.onGet(getReportPolicyThreatsUrl(appId, scanId)).reply(200, policyThreatsData);
    axiosMock.onGet(getReportDataUrl(appId, scanId)).reply(200, reportData);
    axiosMock.onGet(getReportPartialMatchedUrl(appId, scanId)).reply(200, { aaData: [] });
    axiosMock.onGet(getDependenciesUrl(appId, scanId)).reply(200, dependenciesData);
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
    axiosMock.onGet(getReportDataUrl(appId, scanId)).reply(404, 'something went wrong');
    renderComponent();

    const alert = await screen.findByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent('something went wrong');
  });
});
