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
import { getApplicationsUrl } from 'MainRoot/util/CLMLocation';
import { dashboardApplicationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';

/**
 * Apps-Scanned KPI tile (CLM-39641 / P1-F6 §9.4).
 *
 * Phase-1 demo tile: shows the count of applications in scope by reading
 * `GET /rest/application` and taking the length of the returned array.
 * The endpoint returns a top-level JSON array (no `{ applications: [...] }`
 * envelope) — verified against the live dev server. No new backend work;
 * uses the existing endpoint that Classic IQ also reads. At 10 apps (the
 * demo seed) this is trivially fast; the Phase-1.5 follow-up Epic (see
 * F6 §9.3) will replace this with a proper severity-count strip backed
 * by an aggregate endpoint.
 */
interface ApplicationSummary {
  id: string;
  publicId: string;
  name: string;
  organizationId?: string;
  organizationName?: string;
}

export function AppsScannedTile() {
  const { status, data, retry } = useTile<ApplicationSummary[]>(getApplicationsUrl());

  const count = Array.isArray(data) ? data.length : 0;

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
