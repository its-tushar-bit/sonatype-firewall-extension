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
import { TopPolicyViolationsTile } from 'MainRoot/nosc/dashboard/tiles/TopPolicyViolationsTile';
import { dashboardViolationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import { setupNexusOneBundleLocation } from 'TestRoot/nosc/dashboard/dashboardTestHrefs';
import { aggregateTopPolicyViolations } from 'MainRoot/nosc/dashboard/tiles/useTopPolicyViolations';
import { getNewestRisksUrl } from 'MainRoot/util/CLMLocation';

/**
 * S2-PR-D-5 / CLM-39641 (F6 §9.3): Top-Policy-Violations tile tests.
 */
describe('TopPolicyViolationsTile (Phase-1.5 F6)', () => {
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
        <TopPolicyViolationsTile />
      </Theme>,
    );

  describe('aggregator (pure)', () => {
    it('groups by policyId, sums counts, sorts desc by count', () => {
      const out = aggregateTopPolicyViolations([
        { policyId: 'p1', policyName: 'No-GPL' },
        { policyId: 'p2', policyName: 'No-Critical' },
        { policyId: 'p1', policyName: 'No-GPL' },
        { policyId: 'p3', policyName: 'No-AGPL' },
        { policyId: 'p1', policyName: 'No-GPL' },
        { policyId: 'p2', policyName: 'No-Critical' },
      ]);
      expect(out).toEqual([
        { policyId: 'p1', policyName: 'No-GPL', count: 3 },
        { policyId: 'p2', policyName: 'No-Critical', count: 2 },
        { policyId: 'p3', policyName: 'No-AGPL', count: 1 },
      ]);
    });

    it('caps results at topN (default 4)', () => {
      const violations = Array.from({ length: 10 }, (_, i) => ({
        policyId: `p${i}`,
        policyName: `Policy ${i}`,
      }));
      const out = aggregateTopPolicyViolations(violations);
      expect(out).toHaveLength(4);
    });

    it('caps at the supplied topN value', () => {
      const violations = Array.from({ length: 6 }, (_, i) => ({
        policyId: `p${i}`,
        policyName: `Policy ${i}`,
      }));
      const out = aggregateTopPolicyViolations(violations, 2);
      expect(out).toHaveLength(2);
    });

    it('skips violations with no policyId', () => {
      const out = aggregateTopPolicyViolations([
        { policyName: 'orphan' },
        { policyId: 'p1', policyName: 'Real' },
      ]);
      expect(out).toEqual([{ policyId: 'p1', policyName: 'Real', count: 1 }]);
    });

    it('breaks ties on count by lexicographic policyName', () => {
      const out = aggregateTopPolicyViolations([
        { policyId: 'p1', policyName: 'Bravo' },
        { policyId: 'p2', policyName: 'Alpha' },
        { policyId: 'p3', policyName: 'Charlie' },
      ]);
      expect(out.map((r) => r.policyName)).toEqual(['Alpha', 'Bravo', 'Charlie']);
    });

    it('returns [] for empty / undefined input', () => {
      expect(aggregateTopPolicyViolations(undefined)).toEqual([]);
      expect(aggregateTopPolicyViolations([])).toEqual([]);
    });
  });

  describe('rendering', () => {
    it('shows the dashboard-tile skeleton while the request is in flight', () => {
      axiosMock.onPost(URL).reply(() => new Promise(() => {}));
      renderTile();
      expect(screen.getByTestId('dashboard-tile-skeleton')).toBeInTheDocument();
    });

    it('renders top 4 rows with policy names, count badges, and click hrefs', async () => {
      const violations = [
        ...Array.from({ length: 5 }, () => ({ policyId: 'p1', policyName: 'No-GPL' })),
        ...Array.from({ length: 3 }, () => ({ policyId: 'p2', policyName: 'No-Critical' })),
        ...Array.from({ length: 2 }, () => ({ policyId: 'p3', policyName: 'No-AGPL' })),
        { policyId: 'p4', policyName: 'No-Banned' },
        { policyId: 'p5', policyName: 'Should-Not-Render' },
      ];
      axiosMock.onPost(URL).reply(200, { dashboardResults: violations, hasNextPage: false });

      renderTile();

      await waitFor(() => {
        expect(screen.getByTestId('top-policy-list')).toBeInTheDocument();
      });

      const rows = screen.getAllByTestId('top-policy-row');
      expect(rows).toHaveLength(4);

      expect(screen.getByText('No-GPL')).toBeInTheDocument();
      expect(screen.getByText('No-Critical')).toBeInTheDocument();
      expect(screen.getByText('No-AGPL')).toBeInTheDocument();
      expect(screen.getByText('No-Banned')).toBeInTheDocument();
      expect(screen.queryByText('Should-Not-Render')).not.toBeInTheDocument();

      // Counts shown.
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument();
    });

    it('row link points to /preview/dashboard/violations (facet drill-down deferred)', async () => {
      axiosMock.onPost(URL).reply(200, {
        dashboardResults: [{ policyId: 'p-no-gpl', policyName: 'No-GPL' }],
        hasNextPage: false,
      });
      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('top-policy-row-link')).toBeInTheDocument();
      });
      expect(screen.getByTestId('top-policy-row-link')).toHaveAttribute(
        'href',
        dashboardViolationsHref(),
      );
    });

    it('renders the empty body when there are no violations', async () => {
      axiosMock.onPost(URL).reply(200, { dashboardResults: [], hasNextPage: false });
      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('top-policy-empty-body')).toBeInTheDocument();
      });
      expect(screen.getByText(/no policy violations/i)).toBeInTheDocument();
      expect(screen.queryByTestId('top-policy-row')).not.toBeInTheDocument();
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
          dashboardResults: [{ policyId: 'p1', policyName: 'Recovered' }],
          hasNextPage: false,
        });

      renderTile();
      await waitFor(() => {
        expect(screen.getByTestId('dashboard-tile-error')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /retry/i }));

      await waitFor(() => {
        expect(screen.getByTestId('top-policy-list')).toBeInTheDocument();
      });
      expect(screen.getByText('Recovered')).toBeInTheDocument();
      expect(screen.queryByTestId('dashboard-tile-error')).not.toBeInTheDocument();
    });
  });
});
