/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import LegacyScannerBanner from 'MainRoot/applicationReport/LegacyScannerBanner';

describe('LegacyScannerBanner', () => {
  const givenPublicAppIdForReport = 'some-public-app-id';
  const givenScanIdForReport = 'some-scan-id';

  const defaultState = {
    router: {
      currentParams: {
        scanId: givenScanIdForReport,
        publicId: givenPublicAppIdForReport,
      },
    },
    applicationReport: givenApplicationReportWithNeuVectorScanning(),
  };

  it('should render when containerScanningMode is neuvector', () => {
    renderComponent({
      applicationReport: givenApplicationReportWithNeuVectorScanning(),
    });

    expect(screen.getByText('Legacy Scanner Used')).toBeVisible();
    expect(screen.getByText('Learn more about the new container scanner')).toBeVisible();

    const link = screen.getByRole('link', { name: 'Learn more about the new container scanner' });
    expect(link).toHaveAttribute('href', 'https://links.sonatype.com/products/nxiq/doc/container-scanning-with-fw');
    expect(link).toHaveAttribute('target', '_blank');
  });

  it('should show tooltip when hovering over info icon', () => {
    renderComponent({
      applicationReport: givenApplicationReportWithNeuVectorScanning(),
    });

    const infoIcon = document.querySelector('.iq-enterprise-reporting__dashboard-grouping__icon');
    expect(infoIcon).toBeInTheDocument();
  });

  it('should not render when containerScanningMode is not neuvector', () => {
    renderComponent({
      applicationReport: givenApplicationReportWithoutNeuVectorScanning(),
    });

    expect(screen.queryByText('Legacy Scanner Used')).not.toBeInTheDocument();
  });

  it('should not render when containerScanningMode is undefined', () => {
    renderComponent({
      applicationReport: givenApplicationReportWithUndefinedScanningMode(),
    });

    expect(screen.queryByText('Legacy Scanner Used')).not.toBeInTheDocument();
  });

  function renderComponent(stateOverrides = {}) {
    const preloadedState = {
      ...defaultState,
      ...stateOverrides,
    };
    render(<LegacyScannerBanner />, { preloadedState });
  }

  function givenApplicationReportWithNeuVectorScanning() {
    return {
      loadError: null,
      pendingLoads: [],
      metadata: {
        containerScanningMode: 'neuvector',
      },
    };
  }

  function givenApplicationReportWithoutNeuVectorScanning() {
    return {
      loadError: null,
      pendingLoads: [],
      metadata: {
        containerScanningMode: 'sonatype',
      },
    };
  }

  function givenApplicationReportWithUndefinedScanningMode() {
    return {
      loadError: null,
      pendingLoads: [],
      metadata: {},
    };
  }
});
