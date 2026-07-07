/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { Flex, Grid, Text } from '@radix-ui/themes';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { usePreviewDashboardFilterGate } from 'MainRoot/nosc/dashboard/tabs/previewDashboardFilterGate';
import { MetricCard } from './MetricCard';
import { METRIC_CARD_DEFINITIONS } from './metricCardRegistry';
import { selectDashboardMetricsScope } from './dashboardMetricsScope';
import { useDashboardMetrics } from './useDashboardMetrics';
import { formatUpdatedAgo } from './freshness';
import type { DashboardMetricsResponse } from './dashboardMetricsTypes';
import type { DashboardMetricsStatus } from './useDashboardMetrics';

/**
 * Metric-card grid: the Nexus One preview dashboard landing (CLM-40905).
 *
 * Connects the active dashboard filter scope to a single `POST /rest/dashboard/metrics`
 * (via {@link useDashboardMetrics}) and renders the {@link METRIC_CARD_DEFINITIONS}
 * registry as a responsive grid. Every load state is explicit:
 *   - loading   → per-card skeletons (chrome visible immediately)
 *   - not-ready → friendly "metrics are building" panel + retry (409)
 *   - error     → inline error panel + retry when no stale data; otherwise stale cards
 *                 stay visible with a banner (failed refresh does not blank the grid)
 *   - ready     → available cards + a "Updated …" freshness line (zero totals render honestly)
 *
 * Metrics are not requested until the shared filter rail finishes loading (same gate as the
 * classic tab tables) so persisted filters are not preceded by an unscoped round-trip.
 *
 * This is themeless on purpose — the dashboard shell supplies the Radix `<Theme>`
 * and shell offsets, mirroring `DashboardOverviewContent`.
 */

// Breakpoint ramp pending UX sign-off (see PR #16359); `md` intentionally omitted for now.
const GRID_COLUMNS = { initial: '1', sm: '2', lg: '3' } as const;

function GridShell({ children }: { readonly children: React.ReactNode }): JSX.Element {
  return (
    <Grid columns={GRID_COLUMNS} gap="4" align="stretch" data-testid="preview-dashboard-metrics-grid">
      {children}
    </Grid>
  );
}

function MetricCards({ data }: { readonly data: DashboardMetricsResponse }): JSX.Element {
  const cards = METRIC_CARD_DEFINITIONS.filter((def) => def.isAvailable(data));
  return (
    <GridShell>
      {cards.map((def) => {
        const view = def.select(data);
        return (
          <MetricCard
            key={def.id}
            title={def.title}
            value={view.value}
            subMetrics={view.subMetrics}
            href={view.href}
            testId={def.testId}
          />
        );
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

function ReadyGrid({
  data,
  status,
  onRetry,
}: {
  readonly data: DashboardMetricsResponse;
  readonly status: DashboardMetricsStatus;
  readonly onRetry: () => void;
}): JSX.Element {
  const freshness = formatUpdatedAgo(data.lastUpdatedAt);
  const refreshBanner = status !== 'ready' ? <RefreshBanner status={status} onRetry={onRetry} /> : null;
  return (
    <Flex direction="column" gap="3" data-testid="dashboard-metrics-ready">
      {refreshBanner}
      <Flex justify="end" align="center" style={{ minHeight: 20 }}>
        {freshness && (
          <Text size="1" color="gray" data-testid="dashboard-metrics-freshness">
            {freshness}
          </Text>
        )}
      </Flex>
      <MetricCards data={data} />
    </Flex>
  );
}

export function MetricCardGrid(): JSX.Element {
  const scope = useSelector(selectDashboardMetricsScope);
  const { filterLoading, needsAcknowledgement } = usePreviewDashboardFilterGate();
  const filterReady = !filterLoading && !needsAcknowledgement;
  const { status, data, retry } = useDashboardMetrics(scope, filterReady);

  if (!filterReady || (status === 'loading' && data == null)) {
    return (
      <GridShell>
        {METRIC_CARD_DEFINITIONS.filter((def) => def.showWhileLoading).map((def) => (
          <MetricCard key={def.id} title={def.title} loading testId={def.testId} />
        ))}
      </GridShell>
    );
  }

  if (status === 'not-ready' && data == null) {
    return (
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
  }

  if (status === 'error' && data == null) {
    return (
      <AsyncPageState
        loading={false}
        error="Something went wrong fetching your metrics. The rest of the page is unaffected — you can retry."
        errorTestId="dashboard-metrics-error"
        errorTitle="Couldn’t load dashboard metrics"
        onRetry={retry}
      />
    );
  }

  if (data != null) {
    return <ReadyGrid data={data} status={status} onRetry={retry} />;
  }

  // Defensive: axios should always populate `data` on a 200; kept for unexpected null bodies.
  return (
    <AsyncPageState
      loading={false}
      error="Something went wrong fetching your metrics. The rest of the page is unaffected — you can retry."
      errorTestId="dashboard-metrics-error"
      errorTitle="Couldn’t load dashboard metrics"
      onRetry={retry}
    />
  );
}

export default MetricCardGrid;
