/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { ReactNode, useId } from 'react';
import { Box, Flex, Grid, Heading, Text } from '@radix-ui/themes';
import { Card, tokens } from '@sonatype/nexus-one-components';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import styles from './MetricCard.module.css';

/**
 * Reusable metric card for the Nexus One preview dashboard grid (CLM-40905).
 *
 * Supports single-hero, dual-hero (Legal, Orgs & Policies), severity breakdown,
 * and secondary stat rows. A11y: one h2 title per card; hero values are text,
 * not headings; the link carries an explicit aria-label and sits below the title.
 */

export type SubMetricTone = 'critical' | 'severe' | 'moderate' | 'low' | 'neutral';
export type SubMetricVariant = 'severity' | 'stat';

export interface SubMetric {
  readonly label: string;
  readonly value: number;
  readonly tone?: SubMetricTone;
  readonly variant?: SubMetricVariant;
}

export interface DualHeroStat {
  readonly value: number;
  readonly label: string;
}

export interface SecondaryStat {
  readonly value: number;
  readonly label: string;
}

export interface MetricCardProps {
  readonly title: string;
  readonly value?: number;
  readonly subMetrics?: readonly SubMetric[];
  readonly dualHero?: readonly [DualHeroStat, DualHeroStat];
  readonly secondaryStat?: SecondaryStat;
  readonly href?: string;
  readonly loading?: boolean;
  readonly testId?: string;
}

const TONE_TO_COLOR: Record<SubMetricTone, string> = {
  critical: tokens.colors.severity.critical.css,
  severe: tokens.colors.severity.high.css,
  moderate: tokens.colors.severity.medium.css,
  low: tokens.colors.severity.low.css,
  neutral: tokens.colors.severity.none.css,
};

function slugLabel(label: string): string {
  return label.toLowerCase().replace(/\s+/g, '-');
}

function cardLinkLabel(
  title: string,
  value: number | undefined,
  dualHero: readonly [DualHeroStat, DualHeroStat] | undefined,
): string {
  if (dualHero?.length === 2) {
    return `${title}, ${dualHero[0].value.toLocaleString()} ${dualHero[0].label}, ${dualHero[1].value.toLocaleString()} ${dualHero[1].label}, open list`;
  }
  return `${title}, ${(value ?? 0).toLocaleString()} total, open list`;
}

function SubMetricRow({ sub, testId }: { readonly sub: SubMetric; readonly testId: string }): JSX.Element {
  return (
    <li className={styles.subMetric} data-testid={testId}>
      <span
        aria-hidden
        className={styles.dot}
        style={{ backgroundColor: TONE_TO_COLOR[sub.tone ?? 'neutral'] }}
      />
      <Text {...tokens.typography.label} weight="bold" data-testid={`${testId}-value`}>
        {sub.value.toLocaleString()}
      </Text>
      <Text {...tokens.typography.description} className={styles.subLabel} title={sub.label}>
        {sub.label}
      </Text>
    </li>
  );
}

function StatSubMetricRow({ sub, testId }: { readonly sub: SubMetric; readonly testId: string }): JSX.Element {
  return (
    <Flex align="baseline" gap="2" className={styles.statRow} data-testid={testId}>
      <Text size="2" weight="bold" data-testid={`${testId}-value`}>
        {sub.value.toLocaleString()}
      </Text>
      <Text size="2" color="gray">
        {sub.label}
      </Text>
    </Flex>
  );
}

function DualHeroBody({
  dualHero,
  testId,
}: {
  readonly dualHero: readonly DualHeroStat[];
  readonly testId: string;
}): JSX.Element {
  return (
    <Grid columns="2" gap="3" width="100%" data-testid={`${testId}-dual-hero`}>
      {dualHero.map((hero) => (
        <Flex direction="column" key={hero.label}>
          <Text
            as="div"
            size="8"
            weight="bold"
            data-testid={`${testId}-dual-${slugLabel(hero.label)}-value`}
          >
            {hero.value.toLocaleString()}
          </Text>
          <Text size="2" color="gray" mt="3">
            {hero.label}
          </Text>
        </Flex>
      ))}
    </Grid>
  );
}

export function MetricCard({
  title,
  value,
  subMetrics,
  dualHero,
  secondaryStat,
  href,
  loading = false,
  testId = 'metric-card',
}: MetricCardProps): JSX.Element {
  const headingId = useId();
  const severitySubMetrics = subMetrics?.filter((sub) => (sub.variant ?? 'severity') === 'severity') ?? [];
  const statSubMetrics = subMetrics?.filter((sub) => sub.variant === 'stat') ?? [];

  const heroBody: ReactNode = loading ? (
    <LoadingSkeleton height={64} label={`Loading ${title}`} data-testid={`${testId}-skeleton`} />
  ) : dualHero ? (
    <DualHeroBody dualHero={dualHero} testId={testId} />
  ) : (
    <Text as="div" size="8" weight="bold" data-testid={`${testId}-value`}>
      {(value ?? 0).toLocaleString()}
    </Text>
  );

  const details: ReactNode = loading ? null : (
    <>
      {secondaryStat && (
        <Flex align="baseline" gap="2" className={styles.statRow} data-testid={`${testId}-secondary-stat`}>
          <Text size="2" weight="bold" data-testid={`${testId}-secondary-value`}>
            {secondaryStat.value.toLocaleString()}
          </Text>
          <Text size="2" color="gray">
            {secondaryStat.label}
          </Text>
        </Flex>
      )}
      {statSubMetrics.length > 0 && (
        <Flex direction="column" gap="1" className={styles.statStack} data-testid={`${testId}-stat-rows`}>
          {statSubMetrics.map((sub) => (
            <StatSubMetricRow key={sub.label} sub={sub} testId={`${testId}-sub-${slugLabel(sub.label)}`} />
          ))}
        </Flex>
      )}
      {severitySubMetrics.length > 0 && (
        <ul className={styles.breakdown} data-testid={`${testId}-breakdown`}>
          {severitySubMetrics.map((sub) => (
            <SubMetricRow key={sub.label} sub={sub} testId={`${testId}-sub-${slugLabel(sub.label)}`} />
          ))}
        </ul>
      )}
    </>
  );

  const interactiveBody = (
    <Flex direction="column" gap="3">
      {href && !loading ? (
        <a
          href={href}
          className={styles.cardLink}
          data-testid={`${testId}-link`}
          aria-label={cardLinkLabel(title, value, dualHero)}
        >
          {heroBody}
        </a>
      ) : (
        heroBody
      )}
      {details}
    </Flex>
  );

  return (
    <Card data-testid={testId}>
      <section aria-labelledby={headingId}>
        <Box p="4">
          <Heading id={headingId} as="h2" {...tokens.typography.label} trim="start" mb="3">
            {title}
          </Heading>
          {interactiveBody}
        </Box>
      </section>
    </Card>
  );
}
