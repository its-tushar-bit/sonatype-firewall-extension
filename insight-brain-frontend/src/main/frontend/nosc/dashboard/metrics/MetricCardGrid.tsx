/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Button, Flex, Grid, Heading, Text } from '@radix-ui/themes';
import { Card, tokens } from '@sonatype/nexus-one-components';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import {
  dashboardApiHref,
  dashboardEnterpriseReportingHref,
  dashboardSuccessMetricsHref,
} from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import { MetricCard } from './MetricCard';
import { METRIC_CARD_DEFINITIONS } from './metricCardRegistry';
import { useDashboardActiveFilter } from './useDashboardActiveFilter';
import { useDashboardMetrics } from './useDashboardMetrics';
import { formatUpdatedAgo } from './freshness';
import type {
  DashboardMetricsResponse,
  MetricEntry,
  UnsupportedMetricDimension,
} from './dashboardMetricsTypes';
import type { DashboardMetricsStatus } from './useDashboardMetrics';
import styles from './MetricCard.module.css';

/**
 * Metric-card grid: the Nexus One preview dashboard landing (CLM-40905).
 *
 * Renders {@link METRIC_CARD_DEFINITIONS} plus a bottom quick-link row. Load states
 * mirror #16359: filter-gated fetch, stale-data refresh banners, AsyncPageState panels.
 */

const GRID_COLUMNS = { initial: '1', sm: '2', lg: '3' } as const;

function unsupportedDimensions(
  ...entries: readonly (MetricEntry | undefined)[]
): readonly UnsupportedMetricDimension[] | undefined {
  const dimensions = entries.flatMap((entry) =>
    entry?.errorCode === 'UNSUPPORTED_FILTER_COMBINATION' ? entry.unsupportedDimensions ?? [] : []
  );
  const uniqueDimensions = [...new Set(dimensions)];
  return uniqueDimensions.length > 0 ? uniqueDimensions : undefined;
}

function isMetricUnavailable(...entries: readonly (MetricEntry | undefined)[]): boolean {
  return entries.some((entry) => entry?.errorCode === 'METRIC_UNAVAILABLE');
}

function unavailableDimensionsForCard(
  cardId: string,
  data: DashboardMetricsResponse
): readonly UnsupportedMetricDimension[] | undefined {
  switch (cardId) {
    case 'applications':
      return unsupportedDimensions(data.applications);
    case 'legal':
      return unsupportedDimensions(data.legal);
    case 'orgsAndPolicies':
      return unsupportedDimensions(data.organizations, data.policies);
    case 'components':
      return unsupportedDimensions(data.components);
    case 'violations':
      return unsupportedDimensions(data.violations);
    case 'vulnerabilities':
      return unsupportedDimensions(data.vulnerabilities);
    case 'waivers':
      return unsupportedDimensions(data.waivers);
    default:
      return undefined;
  }
}

function isCardMetricUnavailable(cardId: string, data: DashboardMetricsResponse): boolean {
  switch (cardId) {
    case 'applications':
      return isMetricUnavailable(data.applications);
    case 'legal':
      return isMetricUnavailable(data.legal);
    case 'orgsAndPolicies':
      return isMetricUnavailable(data.organizations, data.policies);
    case 'components':
      return isMetricUnavailable(data.components);
    case 'violations':
      return isMetricUnavailable(data.violations);
    case 'vulnerabilities':
      return isMetricUnavailable(data.vulnerabilities);
    case 'waivers':
      return isMetricUnavailable(data.waivers);
    default:
      return false;
  }
}

function GridShell({ children }: { readonly children: React.ReactNode }): JSX.Element {
  return (
    <Grid columns={GRID_COLUMNS} gap="4" align="stretch" data-testid="preview-dashboard-metrics-grid">
      {children}
    </Grid>
  );
}

interface QuickLink {
  readonly id: string;
  readonly title: string;
  readonly description: string;
  readonly getHref: () => string;
  readonly Icon: React.ComponentType<{ size?: number; 'aria-hidden'?: boolean }>;
}

const QUICK_LINKS: readonly QuickLink[] = [
  // Intentionally always visible to match the Martha mockup quick-link row.
  // LeftNav gates these behind license/feature flags and relabels Reports
  // (Enterprise vs Operational) (#16363); follow-up to align dashboard visibility
  // and labels once product confirms parity requirements.
  {
    id: 'success-metrics',
    title: 'Success Metrics',
    description: 'Track key performance indicators and improvement trends',
    getHref: dashboardSuccessMetricsHref,
    Icon: DomainIcons.SuccessMetrics,
  },
  {
    id: 'enterprise-reporting',
    title: 'Enterprise Reporting',
    description: 'Comprehensive analytics and insights across your organization',
    getHref: dashboardEnterpriseReportingHref,
    Icon: DomainIcons.EnterpriseReporting,
  },
  {
    id: 'api',
    title: 'API',
    description: 'Integrate security intelligence into your development workflow',
    getHref: dashboardApiHref,
    Icon: DomainIcons.Api,
  },
];

function QuickLinkCard({ link }: { readonly link: QuickLink }): JSX.Element {
  const { title, description, getHref, Icon } = link;
  return (
    <Card asChild className={styles.quickLinkCard}>
      <a href={getHref()} className={styles.cardLink} data-testid={`dashboard-quicklink-${link.id}`}>
        <Box p="4">
          <Flex align="center" gap="3" mb="2">
            <span className={styles.quickLinkIcon} aria-hidden>
              <Icon size={tokens.icon.iconButtons} aria-hidden />
            </span>
            <Heading {...tokens.typography.cardHeading} as="h3" trim="start">
              {title}
            </Heading>
          </Flex>
          <Text {...tokens.typography.description}>{description}</Text>
        </Box>
      </a>
    </Card>
  );
}

function MetricCards({
  data,
  heavyLoading,
}: {
  readonly data: DashboardMetricsResponse;
  readonly heavyLoading: boolean;
}): JSX.Element {
  return (
    <GridShell>
      {METRIC_CARD_DEFINITIONS.map((def) => {
        const unavailableDimensions = unavailableDimensionsForCard(def.id, data);
        if (unavailableDimensions) {
          return (
            <MetricCard
              key={def.id}
              title={def.title}
              unavailableDimensions={unavailableDimensions}
              testId={def.testId}
            />
          );
        }
        if (isCardMetricUnavailable(def.id, data)) {
          return <MetricCard key={def.id} title={def.title} metricUnavailable testId={def.testId} />;
        }
        if (def.isAvailable(data)) {
          const view = def.select(data);
          return (
            <MetricCard
              key={def.id}
              title={def.title}
              value={view.value}
              subMetrics={view.subMetrics}
              dualHero={view.dualHero}
              secondaryStat={view.secondaryStat}
              href={view.href}
              testId={def.testId}
            />
          );
        }
        if (heavyLoading && def.showWhileHeavyLoading) {
          return <MetricCard key={def.id} title={def.title} loading testId={def.testId} />;
        }
        return null;
      })}
    </GridShell>
  );
}

function RefreshBanner({
  status,
  onRetry,
}: {
  readonly status: DashboardMetricsStatus;
  readonly onRetry: () => void;
}): React.ReactNode {
  if (status === 'loading') {
    return (
      <Text size="2" color="gray" role="status" data-testid="dashboard-metrics-refreshing">
        Refreshing metrics…
      </Text>
    );
  }

  if (status === 'not-ready') {
    return (
      <AsyncPageState
        loading={false}
        error={null}
        info={{
          testId: 'dashboard-metrics-not-ready',
          title: 'Metrics are building',
          message:
            'The search index that powers these metrics is still being built. Showing the last successful values — try again shortly.',
        }}
        infoVariant="banner"
        onRetry={onRetry}
      />
    );
  }

  if (status === 'error') {
    return (
      <AsyncPageState
        loading={false}
        error="Something went wrong refreshing your metrics. Showing the last successful values — you can retry."
        errorTestId="dashboard-metrics-error"
        errorTitle="Couldn’t refresh dashboard metrics"
        errorVariant="banner"
        onRetry={onRetry}
      />
    );
  }

  return null;
}

function QuickLinksRow(): JSX.Element {
  return (
    <Grid columns={GRID_COLUMNS} gap="4" align="stretch" data-testid="dashboard-quicklinks">
      {QUICK_LINKS.map((link) => (
        <QuickLinkCard key={link.id} link={link} />
      ))}
    </Grid>
  );
}

function ReadyGrid({
  data,
  status,
  heavyLoading,
  heavyError,
  onRetry,
  onRetryHeavy,
}: {
  readonly data: DashboardMetricsResponse;
  readonly status: DashboardMetricsStatus;
  readonly heavyLoading: boolean;
  readonly heavyError: Error | null;
  readonly onRetry: () => void;
  readonly onRetryHeavy: () => void;
}): JSX.Element {
  const freshness = formatUpdatedAgo(data.lastUpdatedAt);
  const refreshBanner = status !== 'ready' ? <RefreshBanner status={status} onRetry={onRetry} /> : null;
  return (
    <Flex direction="column" gap="3" data-testid="dashboard-metrics-ready">
      {refreshBanner}
      {heavyError && (
        <Flex
          direction="column"
          gap="3"
          align="start"
          p="4"
          style={{ backgroundColor: 'var(--amber-3)', borderRadius: 'var(--radius-3)' }}
        >
          <Text role="status" data-testid="dashboard-heavy-metrics-delayed">
            Detailed metrics are delayed.
          </Text>
          <Button onClick={onRetryHeavy} data-testid="dashboard-heavy-metrics-retry">
            Retry detailed metrics
          </Button>
        </Flex>
      )}
      <Flex justify="end" align="center" style={{ minHeight: 20 }}>
        {freshness && (
          <Text size="1" color="gray" data-testid="dashboard-metrics-freshness">
            {freshness}
          </Text>
        )}
      </Flex>
      <MetricCards data={data} heavyLoading={heavyLoading} />
    </Flex>
  );
}

export function MetricCardGrid(): JSX.Element {
  const activeFilter = useDashboardActiveFilter();
  const metricsEnabled = !activeFilter.loading && activeFilter.error == null && !activeFilter.needsAcknowledgement;
  const { status, data, heavyLoading, heavyError, retry, retryHeavy } = useDashboardMetrics(
    activeFilter.scope,
    metricsEnabled
  );

  let content: React.ReactNode;

  if (activeFilter.error != null) {
    content = (
      <AsyncPageState
        loading={false}
        error="The active dashboard filter could not be loaded. You can retry."
        errorTestId="dashboard-active-filter-error"
        errorTitle="Couldn’t load dashboard filter"
        onRetry={activeFilter.retry}
      />
    );
  } else if (activeFilter.loading || activeFilter.needsAcknowledgement || (status === 'loading' && data == null)) {
    content = (
      <GridShell>
        {METRIC_CARD_DEFINITIONS.filter((def) => def.showWhileLoading).map((def) => (
          <MetricCard key={def.id} title={def.title} loading testId={def.testId} />
        ))}
      </GridShell>
    );
  } else if (status === 'not-ready' && data == null) {
    content = (
      <AsyncPageState
        loading={false}
        error={null}
        info={{
          testId: 'dashboard-metrics-not-ready',
          title: 'Metrics are building',
          message:
            'The search index that powers these metrics is still being built. This usually takes a few moments — try again shortly.',
        }}
        onRetry={retry}
      />
    );
  } else if (status === 'error' && data == null) {
    content = (
      <AsyncPageState
        loading={false}
        error="Something went wrong fetching your metrics. The rest of the page is unaffected — you can retry."
        errorTestId="dashboard-metrics-error"
        errorTitle="Couldn’t load dashboard metrics"
        onRetry={retry}
      />
    );
  } else if (data != null) {
    content = (
      <ReadyGrid
        data={data}
        status={status}
        heavyLoading={heavyLoading}
        heavyError={heavyError}
        onRetry={retry}
        onRetryHeavy={retryHeavy}
      />
    );
  } else {
    content = (
      <AsyncPageState
        loading={false}
        error="Something went wrong fetching your metrics. The rest of the page is unaffected — you can retry."
        errorTestId="dashboard-metrics-error"
        errorTitle="Couldn’t load dashboard metrics"
        onRetry={retry}
      />
    );
  }

  return (
    <Flex direction="column" gap="3">
      {content}
      <QuickLinksRow />
    </Flex>
  );
}

export default MetricCardGrid;
