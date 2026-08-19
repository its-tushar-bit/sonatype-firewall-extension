/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Flex, Link, Text } from '@radix-ui/themes';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { DashboardTile } from 'MainRoot/nosc/dashboard/DashboardTile';
import {
  useTopPolicyViolations,
  type TopPolicyRow,
} from './useTopPolicyViolations';

/**
 * Top Policy Violations tile (CLM-39641 / S2-PR-D-5 / F6 §9.3).
 *
 * Renders a fixed-height list of the top-N policies sorted desc by
 * violation count. Each row click-throughs to the Violations tab
 * to the Violations tab. Per-policy drill-down filters are deferred
 * until the violations slice grows a `policyIds` facet (CLM-40018 follow-up).
 *
 * Row chrome mirrors `LegalObligationsTile`'s rows on purpose: the
 * two tiles sit side-by-side in the Overview grid (per F6 §9.3 layout
 * spec) and matching row heights / padding reduce visual chatter.
 */

import { dashboardViolationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';

function violationsTabHref(): string {
  return dashboardViolationsHref();
}

/**
 * Tint the count badge by load — same ramp as the LegalObligations
 * review-count badge so the two tiles speak the same color language.
 */
function countBadgeColor(count: number): 'red' | 'orange' | 'yellow' | 'gray' {
  if (count >= 25) return 'red';
  if (count >= 10) return 'orange';
  if (count >= 1) return 'yellow';
  return 'gray';
}

function PolicyRow({ row }: { row: TopPolicyRow }): JSX.Element {
  return (
    <Flex
      align="center"
      justify="between"
      gap="3"
      data-testid="top-policy-row"
      style={{ padding: '8px 0', borderBottom: '1px solid var(--gray-4)' }}
    >
      <Link
        size="2"
        weight="medium"
        href={violationsTabHref()}
        data-testid="top-policy-row-link"
        style={{ flex: 1, minWidth: 0 }}
      >
        {row.policyName}
      </Link>
      <Badge color={countBadgeColor(row.count)} radius="full" size="2">
        {row.count}
      </Badge>
    </Flex>
  );
}

function EmptyBody(): JSX.Element {
  return (
    <Flex
      direction="column"
      align="center"
      gap="2"
      py="4"
      data-testid="top-policy-empty-body"
    >
      <DomainIcons.Policies size={28} color="var(--gray-9)" />
      <Text size="2" color="gray">
        No policy violations.
      </Text>
    </Flex>
  );
}

export function TopPolicyViolationsTile(): JSX.Element {
  const { status, rows, retry } = useTopPolicyViolations();

  return (
    <DashboardTile
      title="Top Policy Violations"
      status={status}
      onRetry={retry}
      errorMessage="Failed to load top policy violations"
    >
      {rows.length === 0 ? (
        <EmptyBody />
      ) : (
        <Flex direction="column" gap="2" data-testid="top-policy-list">
          {rows.map((row) => (
            <PolicyRow key={row.policyId} row={row} />
          ))}
        </Flex>
      )}
    </DashboardTile>
  );
}
