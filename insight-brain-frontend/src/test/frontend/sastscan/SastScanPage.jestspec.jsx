/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import SastScanPage from 'MainRoot/sastScan/SastScanPage';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { getSastScanUrl } from 'MainRoot/util/CLMLocation';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE } from 'MainRoot/development/developmentDashboard/LicenseLockScreen';

describe('SastScanPage', () => {
  let axiosMock, renderComponent;
  const applicationPublicId = 'sandbox-application';
  const sastScanId = 'f257291c67614c00a28a390c7ea83d50';
  const result = {
    sastScanId,
    createdAt: 1702180668064,
    findings: [
      {
        sastFindingId: '28d78d2cf4e0439a898ef8bb40daddc1',
        coordinate: {
          namespace: 'com.sonatype.sast.rules.A01BrokenAccessControl',
          name: 'Cwe352SpringCsrfDisabledTest',
          methodName: 'csrfDisabled',
        },
        lineNumber: 30,
        cwe: 'CWE-352',
        severity: 'HIGH',
        confidence: 'HIGH',
        ruleName: 'spring-httpsecurity-csrf-disabled',
        description:
          'CSRF protection in Spring configuration is disabled. The web application does not, or can not, sufficiently verify whether a well-formed, valid, consistent request was intentionally provided by the user who submitted the request.',
        remediations: [],
      },
      {
        sastFindingId: '28d78d2cf4e0439a898ef8bdd0da25c1',
        coordinate: {
          namespace: 'com.sonatype.sast.rules.A01BrokenAccessControl',
          name: 'Cwe352SpringCsrfDisabledTest',
          methodName: 'csrfDisabled',
        },
        lineNumber: 30,
        cwe: 'CWE-352',
        severity: 'LOW',
        confidence: 'HIGH',
        ruleName: 'spring-httpsecurity-csrf-disabled',
        description:
          'CSRF protection in Spring configuration is disabled. The web application does not, or can not, sufficiently verify whether a well-formed, valid, consistent request was intentionally provided by the user who submitted the request.',
        remediations: [],
      },
    ],
  };

  const router = {
    currentParams: {
      applicationPublicId,
      sastScanId,
    },
  };

  beforeEach(() => {
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(router.currentParams);

    axiosMock = axiosMockAdapter();
    renderComponent = () => render(<SastScanPage />);
  });

  it('should make correct network requests', () => {
    renderComponent();
    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getSastScanUrl(applicationPublicId, sastScanId));
    expect(axiosMock.history.get[0].params).toBe(undefined);
  });

  it('should render correct title', async () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    axiosMock.onGet(getSastScanUrl(applicationPublicId, sastScanId)).reply(200, result);
    renderComponent();
    expect(await screen.findByRole('heading', { name: 'sandbox-application SAST Scan' })).toBeInTheDocument();
  });

  it('should have spinning loading spinner', () => {
    renderComponent();
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('should block access if feature flag is not enabled', async () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(false);
    axiosMock.onGet(getSastScanUrl(applicationPublicId, sastScanId)).reply(200, result);
    renderComponent();
    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toBeInTheDocument();
    expect(errorAlert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE);

    expect(screen.queryByRole('heading', { name: 'sandbox-application SAST Scan' })).not.toBeInTheDocument();
  });
});
