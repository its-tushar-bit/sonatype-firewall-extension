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
import { RiskOverTimeTile } from 'MainRoot/nosc/dashboard/tiles/RiskOverTimeTile';
import { dashboardViolationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import { setupNexusOneBundleLocation } from 'TestRoot/nosc/dashboard/dashboardTestHrefs';
import { aggregateRiskOverTime } from 'MainRoot/nosc/dashboard/tiles/useRiskOverTime';
import { getNewestRisksUrl } from 'MainRoot/util/CLMLocation';

/**
 * S2-PR-D-5 / CLM-39641 (F6 §9.3): Risk-over-Time tile tests.
 */
describe('RiskOverTimeTile (Phase-1.5 F6)', () => {
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
        <RiskOverTimeTile />
      </Theme>,
    );

  describe('aggregator (pure)', () => {
    // Pin "now" to 2026-05-15T12:00:00Z so the 14-day window covers
    // 2026-05-02T00:00Z .. 2026-05-15T00:00Z.
    const NOW = Date.UTC(2026, 4, 15, 12, 0, 0);
    const MS_PER_DAY = 24 * 60 * 60 * 1000;

    it('returns 14 zero buckets when there are no violations', () => {
      const out = aggregateRiskOverTime([], NOW);
      expect(out).toHaveLength(14);
      expect(out.every((b) => b.count === 0)).toBe(true);
    });

    it('buckets violations into the correct day', () => {
      const today = Date.UTC(2026, 4, 15, 9, 30, 0);
      const yesterday = Date.UTC(2026, 4, 14, 23, 59, 0);
      const twoDaysAgo = Date.UTC(2026, 4, 13, 0, 0, 1);
      const out = aggregateRiskOverTime(
        [
          { firstOccurrenceTime: today },
          { firstOccurrenceTime: today },
          { firstOccurrenceTime: yesterday },
          { firstOccurrenceTime: twoDaysAgo },
        ],
        NOW,
      );
      // Last bucket = today.
      expect(out[13].count).toBe(2);
      expect(out[12].count).toBe(1);
      expect(out[11].count).toBe(1);
      // Older days are 0.
      expect(out.slice(0, 11).every((b) => b.count === 0)).toBe(true);
    });

    it('drops violations outside the window', () => {
      const tooOld = NOW - 30 * MS_PER_DAY;
      const future = NOW + 5 * MS_PER_DAY;
      const out = aggregateRiskOverTime(
        [{ firstOccurrenceTime: tooOld }, { firstOccurrenceTime: future }],
        NOW,
      );
      expect(out.every((b) => b.count === 0)).toBe(true);
    });

    it('handles ISO string firstOccurrenceTime values', () => {
      const out = aggregateRiskOverTime(
        [{ firstOccurrenceTime: '2026-05-15T08:00:00Z' }],
        NOW,
      );
      expect(out[13].count).toBe(1);
    });

    it('skips entries with missing or unparseable firstOccurrenceTime', () => {
      const out = aggregateRiskOverTime(
        [{}, { firstOccurrenceTime: 'not-a-date' }, { firstOccurrenceTime: NOW }],
        NOW,
      );
      const totalNonZero = out.reduce((sum, b) => sum + b.count, 0);
      expect(totalNonZero).toBe(1);
    });

    it('respects a custom window length', () => {
      const out = aggregateRiskOverTime([], NOW, 7);
      expect(out).toHaveLength(7);
    });
  });

  describe('rendering', () => {
    it('shows the dashboard-tile skeleton while the request is in flight', () => {
      axiosMock.onPost(URL).reply(() => new Promise(() => {}));
      renderTile();
      expect(screen.getByTestId('dashboard-tile-skeleton')).toBeInTheDocument();
    });

    it('renders the sparkline + total once the request resolves', async () => {
      axiosMock.onPost(URL).reply(200, {
        dashboardResults: [
          { firstOccurrenceTime: Date.now() },
          { firstOccurrenceTime: Date.now() - 60 * 1000 },
        ],
        hasNextPage: false,
      });

      renderTile();

      await waitFor(() => {
        expect(screen.getByTestId('risk-over-time-sparkline')).toBeInTheDocument();
      });

      // 14 day-bars rendered.
      for (let i = 0; i < 14; i += 1) {
        expect(screen.getByTestId(`risk-over-time-bar-${i}`)).toBeInTheDocument();
      }
      expect(screen.getByTestId('risk-over-time-total')).toHaveTextContent('2');
    });

    it('renders the empty-window caption when there is no activity in the window', async () => {
      // All violations 30 days old → outside the 14-day window.
      const old = Date.now() - 30 * 24 * 60 * 60 * 1000;
      axiosMock.onPost(URL).reply(200, {
        dashboardResults: [{ firstOccurrenceTime: old }],
        hasNextPage: false,
      });

      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('risk-over-time-empty')).toBeInTheDocument();
      });
      expect(screen.getByTestId('risk-over-time-total')).toHaveTextContent('0');
    });
  });

  describe('click target', () => {
    it('tile body href points to /preview/dashboard/violations (no filter)', async () => {
      axiosMock.onPost(URL).reply(200, { dashboardResults: [], hasNextPage: false });
      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('risk-over-time-tile-body')).toBeInTheDocument();
      });
      expect(screen.getByTestId('risk-over-time-tile-body')).toHaveAttribute(
        'href',
        dashboardViolationsHref(),
      );
    });
  });

  describe('error + retry', () => {
    it('renders the error chrome with Retry on 500', async () => {
      axiosMock.onPost(URL).reply(500, {});
      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('dashboard-tile-error')).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });

    it('Retry recovers on the second attempt', async () => {
      const user = userEvent.setup();
      axiosMock
        .onPost(URL)
        .replyOnce(500, {})
        .onPost(URL)
        .reply(200, {
          dashboardResults: [{ firstOccurrenceTime: Date.now() }],
          hasNextPage: false,
        });

      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('dashboard-tile-error')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /retry/i }));

      await waitFor(() => {
        expect(screen.getByTestId('risk-over-time-sparkline')).toBeInTheDocument();
      });
      expect(screen.queryByTestId('dashboard-tile-error')).not.toBeInTheDocument();
    });
  });
});
