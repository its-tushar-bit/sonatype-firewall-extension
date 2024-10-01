/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import WaiversConfiguration from 'MainRoot/OrgsAndPolicies/waiversConfiguration/WaiversConfiguration';
import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { getReportMetadataUrl } from 'MainRoot/util/CLMLocation';
import { metadata } from 'TestRoot/componentDetails/data';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS } from 'MainRoot/OrgsAndPolicies/waiversConfiguration/LicenseLockScreenForWaivers';

const publicAppId = 'testPublicAppId';
const scanId = 'testScanId';

describe('WaiversConfiguration', () => {
  let renderComponent, selectIsDeveloperDashboardEnabled, axiosMock, hrefSpy;

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

    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);

    axiosMock = axiosMockAdapter();

    renderComponent = (preloadedState) =>
      render(<WaiversConfiguration />, { preloadedState: preloadedState || defaultPreloadedState });

    axiosMock.onGet(getReportMetadataUrl(publicAppId, scanId)).reply(200, metadata);

    hrefSpy = jest.fn('href').mockImplementation((stateName) => stateName);
    const routerContextMock = { href: hrefSpy };
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);
  });

  it('renders an alert in place of content given the feature is not enabled for the license', () => {
    selectIsDeveloperDashboardEnabled.mockReturnValue(false);

    renderComponent();

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS);
  });

  it('renders the content when the feature is enabled for the license', () => {
    renderComponent();

    expect(screen.queryByText(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS)).not.toBeInTheDocument();
  });
});
