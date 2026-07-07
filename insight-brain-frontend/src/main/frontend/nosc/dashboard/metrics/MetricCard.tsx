/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { ReactNode, useId } from 'react';
import { Box, Card, Flex, Heading, Text } from '@radix-ui/themes';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import styles from './MetricCard.module.css';

/**
 * Reusable metric card for the Nexus One preview dashboard grid (CLM-40905).
 *
 * Renders a titled tile with a hero number and an optional set of sub-metrics
 * (e.g. a severity breakdown). Optionally click-throughs to a destination.
 *
 * A11y (WCAG 2.2 AA):
 *   - One `<h2>` title per card; the hero value is styled text, not a heading.
 *   - The title sits outside the link; the link targets the hero number only and
 *     carries an explicit `aria-label` (title + total) so the breakdown list is
 *     not spoken as one run-on string.
 *   - Sub-metrics render as a traversable list beneath the link.
 *   - Sub-metric color is a swatch ALONGSIDE a text label + value — severity is
 *     never signaled by color alone.
 *   - Keyboard focus shows a visible ring (see MetricCard.module.css).
 */

export type SubMetricTone = 'critical' | 'severe' | 'moderate' | 'low' | 'neutral';

export interface SubMetric {
  readonly label: string;
  readonly value: number;
  readonly tone?: SubMetricTone;
}

export interface MetricCardProps {
  readonly title: string;
  /** Hero number. Omit while loading. */
  readonly value?: number;
  readonly subMetrics?: readonly SubMetric[];
  /** Destination for the hero click-through. Omit for a non-interactive card. */
  readonly href?: string;
  /** When true, renders chrome + a skeleton instead of the value. */
  readonly loading?: boolean;
  /** Stable testid root; sub-elements derive from it (`{testId}-value`, …). */
  readonly testId?: string;
}

const TONE_TO_COLOR: Record<SubMetricTone, string> = {
  critical: 'var(--crimson-9)',
  severe: 'var(--red-9)',
  moderate: 'var(--amber-9)',
  low: 'var(--gray-9)',
  neutral: 'var(--gray-8)',
};

function cardLinkLabel(title: string, value: number | undefined): string {
  return `${title}, ${(value ?? 0).toLocaleString()} total, open list`;
}

function SubMetricRow({ sub, testId }: { readonly sub: SubMetric; readonly testId: string }): JSX.Element {
  return (
    <Box asChild>
      <li data-testid={testId}>
        <Flex align="center" gap="2">
          <span
            aria-hidden
            className={styles.dot}
            style={{ backgroundColor: TONE_TO_COLOR[sub.tone ?? 'neutral'] }}
          />
          <Text size="2" color="gray">
            {sub.label}
          </Text>
          <Text size="2" weight="medium" data-testid={`${testId}-value`}>
            {sub.value.toLocaleString()}
          </Text>
        </Flex>
      </li>
    </Box>
  );
}

export function MetricCard({
  title,
  value,
  subMetrics,
  href,
  loading = false,
  testId = 'metric-card',
}: MetricCardProps): JSX.Element {
  const headingId = useId();

  const hero: ReactNode = loading ? (
    <LoadingSkeleton height={64} label={`Loading ${title}`} data-testid={`${testId}-skeleton`} />
  ) : (
    <Text as="div" size="8" weight="bold" data-testid={`${testId}-value`}>
      {(value ?? 0).toLocaleString()}
    </Text>
  );

  const breakdown =
    !loading && subMetrics && subMetrics.length > 0 ? (
      <Box asChild>
        <ul className={styles.breakdownList} data-testid={`${testId}-breakdown`}>
          {subMetrics.map((sub) => (
            <SubMetricRow key={sub.label} sub={sub} testId={`${testId}-sub-${sub.label.toLowerCase()}`} />
          ))}
        </ul>
      </Box>
    ) : null;

  const body = (
    <Flex direction="column" gap="3">
      {href && !loading ? (
        <a
          href={href}
          className={styles.cardLink}
          data-testid={`${testId}-link`}
          aria-label={cardLinkLabel(title, value)}
        >
          {hero}
        </a>
      ) : (
        hero
      )}
      {breakdown}
    </Flex>
  );

  return (
    <Card data-testid={testId}>
      <section aria-labelledby={headingId}>
        <Box p="4">
          <Heading id={headingId} as="h2" size="3" trim="start" mb="3">
            {title}
          </Heading>
          {body}
        </Box>
      </section>
    </Card>
  );
}
