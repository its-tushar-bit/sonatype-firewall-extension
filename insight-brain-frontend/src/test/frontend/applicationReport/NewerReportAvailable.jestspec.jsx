/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { NewerReportAvailable } from 'MainRoot/applicationReport/NewerReportAvailable';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import { getInitialState } from 'MainRoot/applicationReport/latestReportForStageSlice';

describe('NewerReportAvailable', () => {
  const givenStageOtherThanDevelop = 'build';

  const givenPublicAppIdForReport = 'some-public-app-id';
  const givenScanIdForReport = 'some-scan-id';
  const givenScanIdForLatestReport = 'some-other-scan-id';

  const givenLatestReportLink = 'https://www.example.com/latest-report';

  let hrefSpy;

  let aptrinsicSpy;

  const defaultState = {
    router: {
      currentParams: {
        scanId: givenScanIdForReport,
        publicId: givenPublicAppIdForReport,
      },
      currentState: {
        name: 'applicationReport.policy',
      },
    },
    latestReportForStage: getInitialState(),
    applicationReport: givenApplicationReportForStageOtherThanDevelop(),
  };

  beforeEach(() => {
    hrefSpy = jest.fn().mockReturnValue(givenLatestReportLink);
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue({ href: hrefSpy });

    aptrinsicSpy = jest.fn();
    window.aptrinsic = aptrinsicSpy;
  });

  it('should render when data is loaded and the scanId returned is not the same as the report being viewed', () => {
    renderComponent({
      latestReportForStage: givenALaterReportExists(),
    });

    const warning = screen.getByTestId('new-report-available-warning');
    expect(warning).toBeVisible();
    expect(warning.textContent).toEqual(
      'A new version of this report is available. Click here to navigate to the latest report.'
    );

    expect(hrefSpy).toHaveBeenCalledWith('applicationReport.policy', {
      scanId: givenScanIdForLatestReport,
      publicId: givenPublicAppIdForReport,
    });
    const link = screen.getByRole('link', { name: 'Click here' });
    expect(link).toBeVisible();
    expect(link).toHaveAttribute('href', givenLatestReportLink);

    expect(aptrinsicSpy).toHaveBeenCalledWith('track', 'EXPIRED_APP_REPORT_BANNER_SHOWN', {});
  });

  it('should not show for developer stage reports', () => {
    renderComponent({
      latestReportForStage: givenALaterReportExists(),
      applicationReport: givenApplicationReportForDevelopStage(),
    });

    const warning = screen.queryAllByTestId('new-report-available-warning');
    expect(warning.length).toBe(0);
    expect(aptrinsicSpy).not.toHaveBeenCalled();
  });

  it('should use firewall.containerReport state when viewing Firewall Docker report', () => {
    renderComponent({
      router: {
        currentParams: {
          scanId: givenScanIdForReport,
          publicId: givenPublicAppIdForReport,
        },
        currentState: {
          name: 'firewall.containerReport',
        },
      },
      latestReportForStage: givenALaterReportExists(),
    });

    const warning = screen.getByTestId('new-report-available-warning');
    expect(warning).toBeVisible();

    // Verify it uses firewall.containerReport state
    expect(hrefSpy).toHaveBeenCalledWith('firewall.containerReport', {
      scanId: givenScanIdForLatestReport,
      publicId: givenPublicAppIdForReport,
    });

    const link = screen.getByRole('link', { name: 'Click here' });
    expect(link).toBeVisible();
    expect(link).toHaveAttribute('href', givenLatestReportLink);

    expect(aptrinsicSpy).toHaveBeenCalledWith('track', 'EXPIRED_APP_REPORT_BANNER_SHOWN', {});
  });

  it('should default to applicationReport.policy for unknown contexts', () => {
    renderComponent({
      router: {
        currentParams: {
          scanId: givenScanIdForReport,
          publicId: givenPublicAppIdForReport,
        },
        currentState: {
          name: 'someOtherState.somePage',
        },
      },
      latestReportForStage: givenALaterReportExists(),
    });

    const warning = screen.getByTestId('new-report-available-warning');
    expect(warning).toBeVisible();

    // Should fallback to applicationReport.policy
    expect(hrefSpy).toHaveBeenCalledWith('applicationReport.policy', {
      scanId: givenScanIdForLatestReport,
      publicId: givenPublicAppIdForReport,
    });
  });

  function renderComponent(stateOverrides = {}) {
    const preloadedState = {
      ...defaultState,
      ...stateOverrides,
    };
    render(<NewerReportAvailable />, { preloadedState });
  }

  function givenALaterReportExists() {
    return {
      uninitialized: false,
      loading: false,
      error: null,
      latestReportForStage: { id: givenScanIdForLatestReport, exists: true },
    };
  }

  function givenApplicationReportForDevelopStage() {
    return {
      ...givenApplicationReportForStageOtherThanDevelop(),
      metadata: {
        stageId: 'develop',
      },
    };
  }

  function givenApplicationReportForStageOtherThanDevelop() {
    return {
      loadError: null,
      pendingLoads: [],
      metadata: {
        stageId: givenStageOtherThanDevelop,
      },
    };
  }
});
