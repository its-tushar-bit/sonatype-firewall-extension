/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ApplicationReportRawDataHeader from 'MainRoot/applicationReport/rawData/ApplicationReportRawDataHeader';

// The header title is picked by getReportDisplayName(metadata, routerParams): application.name
// first, then componentDisplayName (URL), then hrcId (URL). These tests pin that chain for the
// application-report and HRC-report entry points. The legacy origin='hostedRepoComponents' path
// was removed alongside the CLM-44275 entry-point rewire (goToHrcReport goes to the native HRC
// report route now, no synthetic-app detour).
describe('ApplicationReportRawDataHeader', () => {
  const appMetadata = {
    reportTime: 1702041439230,
    reportTitle: 'Release Report',
    application: {
      name: 'my-app',
    },
  };

  const hrcMetadata = {
    reportTime: 1702041439230,
    reportTitle: 'Release Report',
    application: null,
  };

  const renderWithRouter = (metadata, routerParams = {}) =>
    render(<ApplicationReportRawDataHeader metadata={metadata} />, {
      preloadedState: {
        router: {
          currentParams: routerParams,
        },
      },
    });

  it('renders application.name as the H1 for application reports', () => {
    renderWithRouter(appMetadata, {});

    expect(
      screen.getByRole('heading', { name: 'Raw Data for my-app Release Report' })
    ).toBeInTheDocument();
  });

  it('renders componentDisplayName as the H1 for HRC reports when application is null', () => {
    renderWithRouter(hrcMetadata, { hrcId: 'hrc-uuid-1', componentDisplayName: 'ansible 2.8.0 (.tar.gz)' });

    expect(
      screen.getByRole('heading', { name: 'Raw Data for ansible 2.8.0 (.tar.gz) Release Report' })
    ).toBeInTheDocument();
  });

  it('falls back to hrcId as the H1 when application is null and componentDisplayName is missing', () => {
    renderWithRouter(hrcMetadata, { hrcId: 'hrc-uuid-1' });

    expect(
      screen.getByRole('heading', { name: 'Raw Data for hrc-uuid-1 Release Report' })
    ).toBeInTheDocument();
  });

  it('renders the formatted reportTime as the subtitle for reportTime=0 (guards against && short-circuit)', () => {
    renderWithRouter({ ...appMetadata, reportTime: 0 }, {});

    const subtitle = document.querySelector('.nx-page-title__description');
    expect(subtitle).toBeInTheDocument();
    expect(subtitle.textContent).not.toBe('0');
    expect(subtitle.textContent.length).toBeGreaterThan(0);
  });
});
