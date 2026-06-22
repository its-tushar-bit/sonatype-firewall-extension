/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Card, Flex, Heading, Text } from '@radix-ui/themes';
import { DashboardTile } from 'MainRoot/nosc/dashboard/DashboardTile';
import {
  useSeverityCounts,
  type SeverityBucket,
  type SeverityCounts,
} from './useSeverityCounts';

/**
 * Severity strip tile (CLM-39641 / S2-PR-D-5 / F6 §9.3).
 *
 * Four single-row cards (Critical / Severe / Moderate / Low) wrapped
 * in the standard `DashboardTile` chrome. Each card click-throughs
 * to the Violations tab with a `?severity=…` query so the
 * destination filter rail pre-applies the matching policy-threat
 * range — see `nosc/dashboard/tabs/PreviewViolationsTab.tsx` for the
 * canonical slug → range mapping.
 *
 * Loading state: 4 placeholder cards (single shared skeleton via the
 *   parent tile chrome — no per-card spinner; the strip is visually
 *   one tile).
 * Error state: parent tile renders the error + Retry button; cards
 *   are not shown.
 *
 * Why ROW colors are inline `var(--{color}-9)` instead of Radix
 * accent props on the card itself: Radix `Card` doesn't honor a
 * per-card accent override, and the strip is already inside a
 * region-tinted parent. A 4px wide accent bar to the left of each
 * count is the minimal, glanceable cue.
 */

import { dashboardViolationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';

interface SeverityCardSpec {
  readonly bucket: SeverityBucket;
  readonly label: string;
  readonly accentVar: string;
}

/**
 * Per-bucket display config. Accent CSS variables come from the Radix
 * scale tokens loaded by the global theme — no new CSS file. Order
 * matches the user's mental model (most-severe first → least-severe
 * last) so the strip reads left-to-right by gravity.
 */
const CARDS: readonly SeverityCardSpec[] = [
  { bucket: 'critical', label: 'Critical', accentVar: 'var(--crimson-9)' },
  { bucket: 'severe', label: 'Severe', accentVar: 'var(--red-9)' },
  { bucket: 'moderate', label: 'Moderate', accentVar: 'var(--amber-9)' },
  { bucket: 'low', label: 'Low', accentVar: 'var(--gray-9)' },
];

export function severityCardHref(bucket: SeverityBucket): string {
  return `${dashboardViolationsHref()}?severity=${encodeURIComponent(bucket)}`;
}

interface SeverityCardProps {
  readonly spec: SeverityCardSpec;
  readonly count: number;
}

function SeverityCard({ spec, count }: SeverityCardProps): JSX.Element {
  return (
    <Card asChild>
      <a
        href={severityCardHref(spec.bucket)}
        data-testid={`severity-card-${spec.bucket}`}
        data-severity-bucket={spec.bucket}
        style={{
          textDecoration: 'none',
          color: 'inherit',
          cursor: 'pointer',
          display: 'block',
          flex: 1,
          minWidth: 0,
        }}
      >
        <Flex align="stretch" gap="3" p="3">
          <Box
            aria-hidden
            style={{
              width: 4,
              borderRadius: 2,
              backgroundColor: spec.accentVar,
              alignSelf: 'stretch',
            }}
          />
          <Flex direction="column" gap="1" style={{ flex: 1, minWidth: 0 }}>
            <Text size="1" color="gray" weight="medium">
              {spec.label}
            </Text>
            <Heading
              size="7"
              weight="bold"
              data-testid={`severity-card-count-${spec.bucket}`}
            >
              {count}
            </Heading>
          </Flex>
        </Flex>
      </a>
    </Card>
  );
}

function SeveritySkeletonCard(): JSX.Element {
  return (
    <Card>
      <Box
        p="3"
        style={{
          height: 76,
          backgroundColor: 'var(--gray-3)',
          borderRadius: 'var(--radius-2)',
          animation: 'pulse 1.5s ease-in-out infinite',
        }}
      />
    </Card>
  );
}

export function SeverityStripTile(): JSX.Element {
  const { status, counts, retry } = useSeverityCounts();

  // Render 4 skeleton cards inside the parent tile chrome's body
  // slot when loading; the chrome's own skeleton would be one block,
  // but the strip's selling point is "4 cells" — the 4-up skeleton
  // previews that shape.
  if (status === 'loading') {
    return (
      <DashboardTile
        title="Policy violations by severity"
        status="ready"
        onRetry={retry}
        errorMessage="Failed to load severity counts"
      >
        <Flex
          gap="3"
          align="stretch"
          data-testid="severity-strip-skeleton"
          wrap="wrap"
        >
          {CARDS.map((spec) => (
            <Box key={spec.bucket} style={{ flex: '1 1 100px', minWidth: 100 }}>
              <SeveritySkeletonCard />
            </Box>
          ))}
        </Flex>
      </DashboardTile>
    );
  }

  return (
    <DashboardTile
      title="Policy violations by severity"
      status={status === 'error' ? 'error' : 'ready'}
      onRetry={retry}
      errorMessage="Failed to load severity counts"
    >
      <Flex
        gap="3"
        align="stretch"
        data-testid="severity-strip"
        wrap="wrap"
      >
        {CARDS.map((spec) => (
          <Box
            key={spec.bucket}
            style={{ flex: '1 1 100px', minWidth: 100 }}
          >
            <SeverityCard
              spec={spec}
              count={(counts as SeverityCounts)[spec.bucket] ?? 0}
            />
          </Box>
        ))}
      </Flex>
    </DashboardTile>
  );
}
