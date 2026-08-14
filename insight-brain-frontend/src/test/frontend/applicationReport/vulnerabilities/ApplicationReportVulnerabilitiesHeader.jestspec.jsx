/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ApplicationReportVulnerabilitiesHeader from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesHeader';

// The header title is picked by getReportDisplayName(metadata, routerParams): application.name
// first, then componentDisplayName (URL), then hrcId (URL). These tests pin that chain for the
// application-report and HRC-report entry points. The legacy origin='hostedRepoComponents' path
// was removed alongside the CLM-44275 entry-point rewire (goToHrcReport goes to the native HRC
// report route now, no synthetic-app detour).
describe('ApplicationReportVulnerabilitiesHeader', () => {
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
    render(<ApplicationReportVulnerabilitiesHeader metadata={metadata} />, {
      preloadedState: {
        router: {
          currentParams: routerParams,
        },
      },
    });

  it('renders application.name as the H1 for application reports', () => {
    renderWithRouter(appMetadata, {});

    expect(
      screen.getByRole('heading', { name: 'Vulnerabilities for my-app Release Report' })
    ).toBeInTheDocument();
  });

  it('renders componentDisplayName as the H1 for HRC reports when application is null', () => {
    renderWithRouter(hrcMetadata, { hrcId: 'hrc-uuid-1', componentDisplayName: 'ansible 2.8.0 (.tar.gz)' });

    expect(
      screen.getByRole('heading', { name: 'Vulnerabilities for ansible 2.8.0 (.tar.gz) Release Report' })
    ).toBeInTheDocument();
  });

  it('falls back to hrcId as the H1 when application is null and componentDisplayName is missing', () => {
    renderWithRouter(hrcMetadata, { hrcId: 'hrc-uuid-1' });

    expect(
      screen.getByRole('heading', { name: 'Vulnerabilities for hrc-uuid-1 Release Report' })
    ).toBeInTheDocument();
  });

  it('renders the formatted reportTime as the subtitle for reportTime=0 (guards against && short-circuit)', () => {
    // Regression guard for a bug where `metadata?.reportTime && formatDate(...)` rendered "0"
    // for the epoch — the null-check must accept 0 as a valid timestamp.
    renderWithRouter({ ...appMetadata, reportTime: 0 }, {});

    // formatDate produces a full timestamp string; assert the heading still renders and the
    // subtitle contains something non-empty (the exact date string is timezone-dependent).
    const subtitle = document.querySelector('.nx-tile-header__subtitle');
    expect(subtitle).toBeInTheDocument();
    expect(subtitle.textContent).not.toBe('0');
    expect(subtitle.textContent.length).toBeGreaterThan(0);
  });
});
