/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import DashboardOverviewContent from 'MainRoot/nosc/dashboard/DashboardOverviewContent';
import {
  dashboardApplicationsHref,
  dashboardViolationsHref,
} from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import {
  DASHBOARD_APPLICATIONS_HREF,
  DASHBOARD_VIOLATIONS_HREF,
  setupNexusOneBundleLocation,
} from 'TestRoot/nosc/dashboard/dashboardTestHrefs';
import {
  getApplicationsUrl,
  getDashboardLegalObligationsUrl,
  getNewestRisksUrl,
} from 'MainRoot/util/CLMLocation';

/**
 * S2-PR-D-5 / CLM-39641 (F6 §9.3): end-to-end IA wiring assertions.
 *
 * Renders the entire `DashboardOverviewContent` tile grid with every
 * tile's underlying axios endpoint mocked so all five tiles can
 * reach `status='ready'`. Then asserts the 9 click-target hrefs from
 * the F6 §9.3 IA wire-up table against the rendered DOM.
 *
 * Why one test file for all 9 targets: the tiles each have their own
 * unit specs that exercise loading / error / retry states. This spec
 * is the single integration check that the IA contract — "click
 * THIS tile, land on THAT URL" — holds when the tiles are composed
 * into the Overview grid the user actually sees. A break here
 * means the Phase-1.5 IA promise broke; specific-tile breaks land
 * in the per-tile spec.
 */

describe('Tile → Tab IA wire-up (S2-PR-D-5 / F6 §9.3)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupNexusOneBundleLocation();
    // AppsScanned hits /rest/application (GET array).
    axiosMock.onGet(getApplicationsUrl()).reply(200, [
      { id: 'a1', publicId: 'apple', name: 'Apple' },
    ]);
    // LegalObligations hits the new /rest/dashboard/legalObligations.
    axiosMock.onGet(getDashboardLegalObligationsUrl()).reply(200, {
      variant: 'ALP',
      groups: [
        { id: 'copyleft', name: 'Copyleft', reviewCount: 5, trendPct: 0 },
      ],
    });
    // SeverityStrip + TopPolicyViolations + RiskOverTime all aggregate
    // the same POST /rest/dashboard/policy/newestRisks payload, so a
    // single mock with policyId + threatLevel + firstOccurrenceTime
    // covers all three.
    axiosMock.onPost(getNewestRisksUrl()).reply(200, {
      dashboardResults: [
        {
          policyId: 'p-no-gpl',
          policyName: 'No-GPL',
          threatLevel: 9,
          firstOccurrenceTime: Date.now(),
        },
        {
          policyId: 'p-no-gpl',
          policyName: 'No-GPL',
          threatLevel: 6,
          firstOccurrenceTime: Date.now() - 60_000,
        },
      ],
      hasNextPage: false,
    });
  });

  afterEach(() => {
    axiosMock.reset();
  });

  function renderGrid() {
    return render(
      <Theme>
        <DashboardOverviewContent />
      </Theme>,
    );
  }

  it('row 1: AppsScanned tile body href → /preview/dashboard/applications', async () => {
    renderGrid();
    await waitFor(() => {
      expect(screen.getByTestId('apps-scanned-tile-body')).toBeInTheDocument();
    });
    expect(screen.getByTestId('apps-scanned-tile-body')).toHaveAttribute(
      'href',
      dashboardApplicationsHref(),
    );
  });

  it.each([
    ['critical', `${DASHBOARD_VIOLATIONS_HREF}?severity=critical`],
    ['severe', `${DASHBOARD_VIOLATIONS_HREF}?severity=severe`],
    ['moderate', `${DASHBOARD_VIOLATIONS_HREF}?severity=moderate`],
    ['low', `${DASHBOARD_VIOLATIONS_HREF}?severity=low`],
  ])(
    'row 1: SeverityStrip "%s" card href → %s',
    async (bucket, expectedHref) => {
      renderGrid();
      await waitFor(() => {
        expect(screen.getByTestId(`severity-card-${bucket}`)).toBeInTheDocument();
      });
      expect(screen.getByTestId(`severity-card-${bucket}`)).toHaveAttribute(
        'href',
        expectedHref,
      );
    },
  );

  it('row 2: LegalObligations ALP row href → /preview/dashboard/violations (facet drill-down deferred)', async () => {
    renderGrid();
    await waitFor(() => {
      expect(screen.getByTestId('legal-obligation-row-link')).toBeInTheDocument();
    });
    expect(screen.getByTestId('legal-obligation-row-link')).toHaveAttribute(
      'href',
      dashboardViolationsHref(),
    );
  });

  it('row 2: LegalObligations non-ALP variant row href → /preview/dashboard/violations (facet drill-down deferred)', async () => {
    // Re-mount with the non-ALP variant so we cover both legal click
    // targets in the tile→tab table. This is the only test that
    // remounts mid-run; the row-link selector is shared.
    axiosMock.onGet(getDashboardLegalObligationsUrl()).reply(200, {
      variant: 'TOP_LEGAL_VIOLATIONS',
      violations: [{ policyId: 'p-no-gpl', policyName: 'No-GPL', openViolationCount: 1 }],
    });
    renderGrid();
    await waitFor(() => {
      expect(screen.getByTestId('legal-obligations-tile-body-top')).toBeInTheDocument();
    });
    expect(screen.getByTestId('legal-obligation-row-link')).toHaveAttribute(
      'href',
      dashboardViolationsHref(),
    );
  });

  it('row 2: TopPolicyViolations row href → /preview/dashboard/violations (facet drill-down deferred)', async () => {
    renderGrid();
    await waitFor(() => {
      expect(screen.getByTestId('top-policy-row-link')).toBeInTheDocument();
    });
    expect(screen.getByTestId('top-policy-row-link')).toHaveAttribute(
      'href',
      dashboardViolationsHref(),
    );
  });

  it('row 3: RiskOverTime tile body href → /preview/dashboard/violations (no filter)', async () => {
    renderGrid();
    await waitFor(() => {
      expect(screen.getByTestId('risk-over-time-tile-body')).toBeInTheDocument();
    });
    expect(screen.getByTestId('risk-over-time-tile-body')).toHaveAttribute(
      'href',
      dashboardViolationsHref(),
    );
  });
});
