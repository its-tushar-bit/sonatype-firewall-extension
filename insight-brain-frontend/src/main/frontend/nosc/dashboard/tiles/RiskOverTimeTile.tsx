/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { DashboardTile } from 'MainRoot/nosc/dashboard/DashboardTile';
import { useRiskOverTime, type RiskBucket } from './useRiskOverTime';

/**
 * Risk-over-Time tile (CLM-39641 / S2-PR-D-5 / F6 §9.3).
 *
 * Wraps the violations data feed in a 14-day SVG sparkline + day-bar
 * overlay. The whole tile body is a click-target back to the
 * Violations tab WITHOUT a filter (drill-down by date is deferred per
 * the spec — the F6 §9.3 click-target table reads "Risk-over-Time
 * tile (whole tile click) → /preview/dashboard/violations (no
 * filter)").
 *
 * Why a hand-rolled SVG instead of importing one of the d3 charts
 * under `labs/successMetrics/.../*Chart.jsx`: the latter expect a
 * `violationCounts[]` shape produced by the SuccessMetrics report
 * endpoint, NOT the dashboard `newestRisks` page. Bringing them in
 * would require a parallel endpoint wire-up and embed each chart's
 * heavy table chrome in a slim Overview tile. See `useRiskOverTime`
 * docstring for the full rationale.
 *
 * Visual chrome: 14 vertical bars, each scaled to the max count in
 * the window. Empty windows render a flat baseline + "No activity"
 * caption so the tile never looks broken at zero.
 */

import { dashboardViolationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';

const SVG_WIDTH = 240;
const SVG_HEIGHT = 56;
const BAR_GAP = 2;

interface SparklineProps {
  readonly buckets: ReadonlyArray<RiskBucket>;
}

function Sparkline({ buckets }: SparklineProps): JSX.Element {
  const max = Math.max(1, ...buckets.map((b) => b.count));
  const bandWidth = SVG_WIDTH / Math.max(1, buckets.length);
  const barWidth = Math.max(1, bandWidth - BAR_GAP);
  return (
    <svg
      width={SVG_WIDTH}
      height={SVG_HEIGHT}
      viewBox={`0 0 ${SVG_WIDTH} ${SVG_HEIGHT}`}
      role="img"
      aria-label="Daily violation counts for the last 14 days"
      data-testid="risk-over-time-sparkline"
    >
      {buckets.map((b, idx) => {
        const h = (b.count / max) * (SVG_HEIGHT - 4);
        const x = idx * bandWidth + (bandWidth - barWidth) / 2;
        const y = SVG_HEIGHT - h - 1;
        const titleDate = new Date(b.tsMillis).toISOString().slice(0, 10);
        return (
          <rect
            key={b.tsMillis}
            x={x}
            y={y}
            width={barWidth}
            height={h}
            fill="var(--accent-9)"
            data-testid={`risk-over-time-bar-${idx}`}
          >
            <title>{`${titleDate}: ${b.count}`}</title>
          </rect>
        );
      })}
    </svg>
  );
}

export function RiskOverTimeTile(): JSX.Element {
  const { status, buckets, total, retry } = useRiskOverTime();

  return (
    <DashboardTile
      title="Risk Over Time"
      status={status}
      onRetry={retry}
      errorMessage="Failed to load risk-over-time chart"
    >
      <a
        href={dashboardViolationsHref()}
        data-testid="risk-over-time-tile-body"
        style={{
          textDecoration: 'none',
          color: 'inherit',
          cursor: 'pointer',
          display: 'block',
        }}
      >
        <Flex direction="column" gap="3" py="2">
          <Flex align="baseline" gap="2">
            <Text size="6" weight="bold" data-testid="risk-over-time-total">
              {total}
            </Text>
            <Text size="2" color="gray">
              violations in the last 14 days
            </Text>
          </Flex>
          <Box>
            <Sparkline buckets={buckets} />
          </Box>
          {total === 0 && (
            <Text size="1" color="gray" data-testid="risk-over-time-empty">
              No activity in this window.
            </Text>
          )}
        </Flex>
      </a>
    </DashboardTile>
  );
}
