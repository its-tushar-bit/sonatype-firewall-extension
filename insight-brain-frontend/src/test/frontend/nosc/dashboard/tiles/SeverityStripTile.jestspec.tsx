/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import { SeverityStripTile } from 'MainRoot/nosc/dashboard/tiles/SeverityStripTile';
import {
  DASHBOARD_VIOLATIONS_HREF,
  setupNexusOneBundleLocation,
} from 'TestRoot/nosc/dashboard/dashboardTestHrefs';
import { dashboardViolationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import {
  aggregateSeverityCounts,
  bucketForThreatLevel,
} from 'MainRoot/nosc/dashboard/tiles/useSeverityCounts';
import { getNewestRisksUrl } from 'MainRoot/util/CLMLocation';

/**
 * S2-PR-D-5 / CLM-39641 (F6 §9.3): Severity-strip tile tests. Covers
 * the four DashboardTile chrome states (loading, ready, error, retry)
 * + click-target hrefs for each of the four severity cards + the
 * pure aggregator helpers.
 */
describe('SeverityStripTile (Phase-1.5 F6)', () => {
  let axiosMock: any;
  const URL = getNewestRisksUrl();

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
        <SeverityStripTile />
      </Theme>,
    );

  describe('aggregator helpers (pure)', () => {
    it.each([
      [10, 'critical'],
      [9, 'critical'],
      [8, 'critical'],
      [7, 'severe'],
      [4, 'severe'],
      [3, 'moderate'],
      [2, 'moderate'],
      [1, 'low'],
      [0, 'low'],
    ])('bucketForThreatLevel(%i) → %s', (lvl, bucket) => {
      expect(bucketForThreatLevel(lvl)).toBe(bucket);
    });

    it('aggregateSeverityCounts buckets a mixed list of violations', () => {
      const result = aggregateSeverityCounts([
        { threatLevel: 10 },
        { threatLevel: 8 },
        { threatLevel: 7 },
        { threatLevel: 4 },
        { threatLevel: 3 },
        { threatLevel: 1 },
        { threatLevel: 0 },
      ]);
      expect(result).toEqual({ critical: 2, severe: 2, moderate: 1, low: 2 });
    });

    it('aggregateSeverityCounts handles an empty / undefined input', () => {
      expect(aggregateSeverityCounts(undefined)).toEqual({
        critical: 0,
        severe: 0,
        moderate: 0,
        low: 0,
      });
      expect(aggregateSeverityCounts([])).toEqual({
        critical: 0,
        severe: 0,
        moderate: 0,
        low: 0,
      });
    });

    it('aggregateSeverityCounts treats missing threatLevel as 0 (low)', () => {
      const result = aggregateSeverityCounts([{}, {}, { threatLevel: 5 }]);
      expect(result).toEqual({ critical: 0, severe: 1, moderate: 0, low: 2 });
    });
  });

  describe('rendering', () => {
    it('shows the 4-up skeleton while the request is in flight', () => {
      // Pending — never resolve.
      axiosMock.onPost(URL).reply(() => new Promise(() => {}));
      renderTile();
      expect(screen.getByTestId('severity-strip-skeleton')).toBeInTheDocument();
    });

    it('renders 4 severity cards with live counts once the request resolves', async () => {
      axiosMock.onPost(URL).reply(200, {
        dashboardResults: [
          { threatLevel: 10 },
          { threatLevel: 9 },
          { threatLevel: 6 },
          { threatLevel: 4 },
          { threatLevel: 3 },
          { threatLevel: 0 },
        ],
        hasNextPage: false,
      });

      renderTile();

      await waitFor(() => {
        expect(screen.getByTestId('severity-strip')).toBeInTheDocument();
      });

      expect(screen.getByTestId('severity-card-critical')).toBeInTheDocument();
      expect(screen.getByTestId('severity-card-severe')).toBeInTheDocument();
      expect(screen.getByTestId('severity-card-moderate')).toBeInTheDocument();
      expect(screen.getByTestId('severity-card-low')).toBeInTheDocument();

      expect(screen.getByTestId('severity-card-count-critical')).toHaveTextContent('2');
      expect(screen.getByTestId('severity-card-count-severe')).toHaveTextContent('2');
      expect(screen.getByTestId('severity-card-count-moderate')).toHaveTextContent('1');
      expect(screen.getByTestId('severity-card-count-low')).toHaveTextContent('1');
    });

    it('renders zero counts when the endpoint returns no violations', async () => {
      axiosMock.onPost(URL).reply(200, { dashboardResults: [], hasNextPage: false });
      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('severity-strip')).toBeInTheDocument();
      });
      expect(screen.getByTestId('severity-card-count-critical')).toHaveTextContent('0');
      expect(screen.getByTestId('severity-card-count-low')).toHaveTextContent('0');
    });
  });

  describe('click targets', () => {
    it.each([
      ['critical', `${DASHBOARD_VIOLATIONS_HREF}?severity=critical`],
      ['severe', `${DASHBOARD_VIOLATIONS_HREF}?severity=severe`],
      ['moderate', `${DASHBOARD_VIOLATIONS_HREF}?severity=moderate`],
      ['low', `${DASHBOARD_VIOLATIONS_HREF}?severity=low`],
    ])('%s card href is %s', async (bucket, expectedHref) => {
      axiosMock.onPost(URL).reply(200, { dashboardResults: [], hasNextPage: false });
      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId(`severity-card-${bucket}`)).toBeInTheDocument();
      });
      expect(screen.getByTestId(`severity-card-${bucket}`)).toHaveAttribute(
        'href',
        expectedHref,
      );
    });
  });

  describe('error + retry', () => {
    it('renders the dashboard-tile error chrome with a Retry button when the endpoint 500s', async () => {
      axiosMock.onPost(URL).reply(500, {});
      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('dashboard-tile-error')).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });

    it('Retry re-fires the POST and recovers on the second attempt', async () => {
      const user = userEvent.setup();

      axiosMock
        .onPost(URL)
        .replyOnce(500, {})
        .onPost(URL)
        .reply(200, { dashboardResults: [{ threatLevel: 9 }], hasNextPage: false });

      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('dashboard-tile-error')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /retry/i }));

      await waitFor(() => {
        expect(screen.getByTestId('severity-strip')).toBeInTheDocument();
      });
      expect(screen.getByTestId('severity-card-count-critical')).toHaveTextContent('1');
      expect(screen.queryByTestId('dashboard-tile-error')).not.toBeInTheDocument();
    });
  });
});
