/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Flex, Link, Table, Text } from '@radix-ui/themes';
import { useRouter } from '@uirouter/react';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import type { PolicyWaiverDTO } from './waiverTypes';
import {
  formatWaiverCalendarDate,
  formatWaiverComponentLabel,
  formatWaiverListExpiry,
  waiverDetailTypeParam,
  waiverThreatColor,
} from './waiverDisplayUtils';

/**
 * Shared Waivers table (CLM-39545 / CLM-39709) used by both `WaiversListPage`
 * (global) and `ApplicationDetail` (scoped to a single application).
 *
 * Column set was hard-ported from Classic's `DashboardWaiversTable`:
 *   Threat | Date Created | Expiration | Policy | Scope | Components
 *
 * Click-through goes to `/waivers/{ownerType}/{ownerId}/{waiverId}`,
 * the native Nexus One detail page. We do *not* deep-link to Classic's
 * waiver-details page from here — that would break the "stay in Nexus One"
 * UX promise. The detail page itself surfaces a "Continue in Classic"
 * action for users who want the full Classic feature set.
 *
 * Empty state is intentionally calm: this dev IQ legitimately has zero
 * waivers, and customers without waivers see the same message. We do not
 * fake data.
 */
export interface WaiversTableProps {
  waivers: ReadonlyArray<PolicyWaiverDTO>;
  loading: boolean;
  error: string | null;
  onRetry?: () => void;
  emptyMessage?: string;
  emptySubMessage?: string;
  testId?: string;
  /**
   * Opaque marker appended as `?from=<value>` to every detail-link href.
   * The WaiverDetailPage reads this to render a back-link that routes
   * to the correct origin (e.g. `dashboard` → Dashboard's Waivers tab,
   * unset → standalone /waivers).
   */
  linkFrom?: string;
}

export default function WaiversTable({
  waivers,
  loading,
  error,
  onRetry,
  emptyMessage = 'No waivers in scope',
  emptySubMessage = 'Waivers suppress matching policy violations. Create one from any violation in Classic IQ to see it here.',
  testId = 'nosc-waivers-table',
  linkFrom,
}: WaiversTableProps) {
  const { stateService } = useRouter();
  // Detail-link href via the UI-Router state registry. `from` (set by the
  // caller via linkFrom) is an opaque marker the Waiver Detail page reads to
  // route its back-link to the right origin; omitted from the URL when unset.
  const waiverDetailHref = (w: PolicyWaiverDTO): string =>
    stateService.href('nexusOneWaiverDetail', {
      ownerType: w.ownerType,
      ownerId: w.ownerId,
      waiverId: w.id,
      from: linkFrom,
      type: waiverDetailTypeParam(w.isAutoWaiver),
    });

  if (loading || error) {
    return (
      <AsyncPageState
        loading={loading}
        error={error}
        onRetry={onRetry}
        loadingHeight={240}
        loadingTestId={`${testId}-loading`}
        errorTestId={`${testId}-error`}
        errorTitle="Failed to load waivers"
        errorVariant="banner"
      />
    );
  }

  if (waivers.length === 0) {
    return (
      <Flex
        direction="column"
        align="center"
        gap="2"
        py="8"
        data-testid={`${testId}-empty`}
      >
        <DomainIcons.Waivers size={32} color="var(--gray-9)" />
        <Text size="3" color="gray">
          {emptyMessage}
        </Text>
        <Text size="2" color="gray" align="center" style={{ maxWidth: 480 }}>
          {emptySubMessage}
        </Text>
      </Flex>
    );
  }

  return (
    <Table.Root variant="surface" size="2" data-testid={testId}>
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Created</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Expires</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Policy</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Scope</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Components</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell justify="end">Actions</Table.ColumnHeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {waivers.map((w) => (
          <Table.Row key={w.id} data-testid={`${testId}-row`}>
            <Table.Cell>
              <Flex align="center" gap="2">
                <Badge color={waiverThreatColor(w.threatLevel)} variant="solid">
                  {w.threatLevel}
                </Badge>
                {w.isAutoWaiver && (
                  <Badge color="green" variant="soft">
                    Auto
                  </Badge>
                )}
              </Flex>
            </Table.Cell>
            <Table.Cell>
              <Text size="2">{formatWaiverCalendarDate(w.createTime)}</Text>
            </Table.Cell>
            <Table.Cell>
              <Text size="2">{formatWaiverListExpiry(w)}</Text>
            </Table.Cell>
            <Table.Cell>
              <Text size="2">{w.policyName ?? '—'}</Text>
            </Table.Cell>
            <Table.Cell>
              <Text size="2" color="gray">
                {w.scope}
              </Text>
            </Table.Cell>
            <Table.Cell>
              <Text size="2" style={{ fontFamily: 'var(--code-font-family)' }}>
                {formatWaiverComponentLabel(w)}
              </Text>
            </Table.Cell>
            <Table.Cell justify="end">
              <Link
                size="2"
                href={waiverDetailHref(w)}
                data-testid={`${testId}-row-detail-link`}
              >
                View Details →
              </Link>
            </Table.Cell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table.Root>
  );
}
