/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { LegalObligationsTile } from 'MainRoot/nosc/dashboard/tiles/LegalObligationsTile';
import { dashboardViolationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import { setupNexusOneBundleLocation } from 'TestRoot/nosc/dashboard/dashboardTestHrefs';
import { getDashboardLegalObligationsUrl } from 'MainRoot/util/CLMLocation';

/**
 * Tests for the Phase-1.5 ALP-variant Legal Obligations tile
 * (CLM-39604 / S2-PR-D-2). Covers each branch of the discriminated
 * `/rest/dashboard/legalObligations` payload (ALP, top-violations,
 * permission-denied, empty) plus loading + error chrome from
 * `DashboardTile`.
 *
 * The slim S1-PR7 tile that hit `/rest/licenseThreatGroup/.../counts` is
 * gone — these tests intentionally don't reference that older endpoint.
 */
describe('LegalObligationsTile (Phase-1.5 ALP variant)', () => {
  let axiosMock: any;
  const URL = getDashboardLegalObligationsUrl();

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupNexusOneBundleLocation();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  const renderTile = () =>
    render(
      <Theme>
        <LegalObligationsTile />
      </Theme>,
    );

  it('shows the DashboardTile skeleton while the request is in flight', () => {
    // No reply registered → axios-mock-adapter leaves the request pending.
    axiosMock.onGet(URL).reply(() => new Promise(() => {}));

    renderTile();
    expect(screen.getByTestId('dashboard-tile-skeleton')).toBeInTheDocument();
  });

  it('ALP variant: renders subtitle, rows, unreviewed-count badges, and trend arrows', async () => {
    axiosMock.onGet(URL).reply(200, {
      variant: 'ALP',
      groups: [
        { id: 'g-banned', name: 'Banned', reviewCount: 27, trendPct: 12 },
        { id: 'g-copyleft', name: 'Copyleft', reviewCount: 8, trendPct: -4 },
        { id: 'g-weak', name: 'Weak Copyleft', reviewCount: 0, trendPct: 0 },
      ],
    });

    renderTile();

    await waitFor(() => {
      expect(screen.getByTestId('legal-obligations-tile-body-alp')).toBeInTheDocument();
    });

    expect(screen.getByText(/unreviewed components by threat group/i)).toBeInTheDocument();
    expect(screen.getByText('Banned')).toBeInTheDocument();
    expect(screen.getByText('Copyleft')).toBeInTheDocument();
    expect(screen.getByText('Weak Copyleft')).toBeInTheDocument();

    // Unreviewed-component-count badges are rendered as visible numbers.
    expect(screen.getByText('27')).toBeInTheDocument();
    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();

    // Trend arrows: one per row with direction matching trendPct sign.
    expect(screen.getByTestId('legal-tile-trend-up')).toBeInTheDocument();
    expect(screen.getByTestId('legal-tile-trend-down')).toBeInTheDocument();
    expect(screen.getByTestId('legal-tile-trend-flat')).toBeInTheDocument();

    // One row per group.
    expect(screen.getAllByTestId('legal-obligation-row')).toHaveLength(3);
  });

  it('ALP variant: caps rendered rows at 4 and shows a "showing top N of M" footnote', async () => {
    const groups = Array.from({ length: 7 }, (_, i) => ({
      id: `g${i}`,
      name: `Group ${i}`,
      reviewCount: 5,
      trendPct: 0,
    }));
    axiosMock.onGet(URL).reply(200, { variant: 'ALP', groups });

    renderTile();
    await waitFor(() => {
      expect(screen.getByTestId('legal-obligations-tile-body-alp')).toBeInTheDocument();
    });

    expect(screen.getAllByTestId('legal-obligation-row')).toHaveLength(4);
    expect(screen.getByText(/showing top 4 of 7 threat groups with unreviewed components/i)).toBeInTheDocument();
  });

  it('ALP variant: row link points to /preview/dashboard/violations (facet drill-down deferred)', async () => {
    axiosMock.onGet(URL).reply(200, {
      variant: 'ALP',
      groups: [{ id: 'g-banned', name: 'Banned', reviewCount: 1, trendPct: 0 }],
    });

    renderTile();
    await waitFor(() => {
      expect(screen.getByText('Banned')).toBeInTheDocument();
    });

    const link = screen.getByTestId('legal-obligation-row-link');
    expect(link).toHaveAttribute('href', dashboardViolationsHref());
  });

  it('TOP_LEGAL_VIOLATIONS variant: renders rows with policy name + count', async () => {
    axiosMock.onGet(URL).reply(200, {
      variant: 'TOP_LEGAL_VIOLATIONS',
      violations: [
        { policyId: 'p1', policyName: 'No-GPL', openViolationCount: 17 },
        { policyId: 'p2', policyName: 'License-Required', openViolationCount: 9 },
        { policyId: 'p3', policyName: 'Banned-License', openViolationCount: 4 },
        { policyId: 'p4', policyName: 'Attribution-Needed', openViolationCount: 2 },
      ],
    });

    renderTile();

    await waitFor(() => {
      expect(screen.getByTestId('legal-obligations-tile-body-top')).toBeInTheDocument();
    });

    expect(screen.getByText(/top open license policy violations/i)).toBeInTheDocument();
    expect(screen.getByText('No-GPL')).toBeInTheDocument();
    expect(screen.getByText('License-Required')).toBeInTheDocument();
    expect(screen.getByText('Banned-License')).toBeInTheDocument();
    expect(screen.getByText('Attribution-Needed')).toBeInTheDocument();
    expect(screen.getByText('17')).toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getAllByTestId('legal-obligation-row')).toHaveLength(4);
  });

  it('TOP_LEGAL_VIOLATIONS variant: row link points to /preview/dashboard/violations (facet drill-down deferred)', async () => {
    axiosMock.onGet(URL).reply(200, {
      variant: 'TOP_LEGAL_VIOLATIONS',
      violations: [{ policyId: 'p-no-gpl', policyName: 'No-GPL', openViolationCount: 1 }],
    });

    renderTile();
    await waitFor(() => {
      expect(screen.getByText('No-GPL')).toBeInTheDocument();
    });

    const link = screen.getByTestId('legal-obligation-row-link');
    expect(link).toHaveAttribute('href', dashboardViolationsHref());
  });

  it('permissionDenied: renders greyed body with the no-access copy and no rows', async () => {
    axiosMock.onGet(URL).reply(200, { permissionDenied: true });

    renderTile();
    await waitFor(() => {
      expect(screen.getByTestId('legal-tile-permission-denied-body')).toBeInTheDocument();
    });

    expect(
      screen.getByText(/you don't have access to legal data in any scoped application/i),
    ).toBeInTheDocument();
    expect(screen.queryByTestId('legal-obligation-row')).not.toBeInTheDocument();

    // No View-details link in this state per UX-F11-005.
    expect(screen.queryByTestId('legal-tile-view-details')).not.toBeInTheDocument();
  });

  it('empty: renders the no unreviewed components in scope body', async () => {
    axiosMock.onGet(URL).reply(200, { empty: true });

    renderTile();
    await waitFor(() => {
      expect(screen.getByTestId('legal-tile-empty-body')).toBeInTheDocument();
    });

    expect(screen.getByText(/no unreviewed components in scope/i)).toBeInTheDocument();
    expect(screen.queryByTestId('legal-obligation-row')).not.toBeInTheDocument();
  });

  it('error: renders the DashboardTile error state with a Retry button', async () => {
    axiosMock.onGet(URL).reply(500, {});

    renderTile();
    await waitFor(() => {
      expect(screen.getByTestId('dashboard-tile-error')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('error → retry: clicking Retry re-fires the request and recovers on success', async () => {
    const user = userEvent.setup();

    // First call fails; second call succeeds with an empty payload so we can
    // assert the tile transitions out of the error state.
    axiosMock
      .onGet(URL)
      .replyOnce(500, {})
      .onGet(URL)
      .reply(200, { empty: true });

    renderTile();
    await waitFor(() => {
      expect(screen.getByTestId('dashboard-tile-error')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /retry/i }));

    await waitFor(() => {
      expect(screen.getByTestId('legal-tile-empty-body')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('dashboard-tile-error')).not.toBeInTheDocument();
  });
});
