/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Flex, Heading, Text } from '@radix-ui/themes';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { DashboardTile } from 'MainRoot/nosc/dashboard/DashboardTile';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';
import { getDashboardMetricsUrl } from 'MainRoot/util/CLMLocation';
import { dashboardApplicationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import type { DashboardMetricsResponse } from 'MainRoot/nosc/dashboard/metrics/dashboardMetricsTypes';

/**
 * Apps-Scanned KPI tile (CLM-39641 / P1-F6 §9.4).
 *
 * Shows the RBAC-scoped application count from
 * {@code POST /rest/dashboard/metrics} with {@code includeHeavyMetrics: false}
 * (the summary tier already returns {@code applications.total}).
 */
export function AppsScannedTile() {
  const { status, data, retry } = useTile<DashboardMetricsResponse>(getDashboardMetricsUrl(), undefined, {
    method: 'post',
    body: { includeHeavyMetrics: false },
  });

  const count = data?.applications?.total ?? 0;

  return (
    <DashboardTile title="Apps Scanned" status={status} onRetry={retry} errorMessage="Failed to load apps">
      <a
        href={dashboardApplicationsHref()}
        data-testid="apps-scanned-tile-body"
        style={{
          textDecoration: 'none',
          color: 'inherit',
          cursor: 'pointer',
          display: 'block',
        }}
      >
        <Flex direction="column" gap="2">
          <Flex align="center" gap="3">
            <DomainIcons.Applications size={32} color="var(--accent-9)" />
            <Heading size="8" weight="bold">
              {count}
            </Heading>
          </Flex>
          <Text size="2" color="gray">
            {count === 1 ? 'application' : 'applications'} in scope
          </Text>
        </Flex>
      </a>
    </DashboardTile>
  );
}
