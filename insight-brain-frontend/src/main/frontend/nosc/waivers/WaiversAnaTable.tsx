/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Flex, Link, Table, Text } from '@radix-ui/themes';
import { useRouter } from '@uirouter/react';
import type { AnaWaiverRow } from 'MainRoot/nosc/waivers/waiversListTypes';
import {
  normalizeWaiverOwnerTypeForApi,
  waiverDetailTypeParam,
} from 'MainRoot/nosc/waivers/waiverDisplayUtils';
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';
import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';

/**
 * Waivers table for the Ana IQ-index list (CLM-43204). Row shape mirrors the flattened
 * {@code AnaWaiverRow} projection of the {@code /rest/search/index-query} WAIVER response,
 * distinct from the legacy {@code PolicyWaiverDTO} rendered by {@code WaiversTable}
 * (still in use on the Applications tab + dashboard). Column set follows the Confluence V1
 * spec: Threat | Created | Expires | Policy | Scope (org/app) | Reason | Actions.
 *
 * Row click links to the existing native Waiver Detail page at
 * {@code /waivers/{ownerType}/{ownerId}/{waiverId}}, so detail retains its v2 API path.
 */
export interface WaiversAnaTableProps {
  readonly waivers: ReadonlyArray<AnaWaiverRow>;
  readonly testId?: string;
  /** Opaque marker appended as {@code ?from=<value>} to detail links (e.g. {@code waivers-list}). */
  readonly linkFrom?: string;
}

function formatExpiry(row: AnaWaiverRow): string {
  if (row.isAuto && !row.expiresAt) return 'Auto';
  if (row.expiresAt) return formatDateUtcYYYYMMDD(row.expiresAt);
  return 'Never';
}

function formatScope(row: AnaWaiverRow): string {
  if (row.applicationName) return `Application: ${row.applicationName}`;
  if (row.organizationName) return `Organization: ${row.organizationName}`;
  if (row.scopeOwnerType && row.scopeOwnerId) return `${row.scopeOwnerType}: ${row.scopeOwnerId}`;
  return '—';
}

function truncate(text: string | null, max: number): string {
  if (!text) return '—';
  if (text.length <= max) return text;
  return `${text.slice(0, max - 1)}…`;
}

export default function WaiversAnaTable({
  waivers,
  testId = 'waivers-ana-table',
  linkFrom,
}: WaiversAnaTableProps): JSX.Element {
  const { stateService } = useRouter();

  const waiverDetailHref = (row: AnaWaiverRow): string | null => {
    const ownerType = normalizeWaiverOwnerTypeForApi(row.scopeOwnerType);
    if (!ownerType || !row.scopeOwnerId) return null;
    return stateService.href('nexusOneWaiverDetail', {
      ownerType,
      ownerId: row.scopeOwnerId,
      waiverId: row.id,
      from: linkFrom,
      type: waiverDetailTypeParam(row.isAuto),
      requested: row.isRequested ? 'true' : undefined,
    });
  };

  return (
    <Table.Root variant="surface" size="2" data-testid={testId}>
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Created</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Expires</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Policy</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Scope</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Reason</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell justify="end">Actions</Table.ColumnHeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {waivers.map((row) => {
          const href = waiverDetailHref(row);
          return (
            <Table.Row key={row.id} data-testid={`${testId}-row`}>
              <Table.Cell>
                <Flex align="center" gap="2" wrap="wrap">
                  <Badge color={threatColorFor(row.threatLevel)} variant="solid">
                    {row.threatLevel}
                  </Badge>
                  {row.isAuto && (
                    <Badge color="green" variant="soft">
                      Auto
                    </Badge>
                  )}
                  {row.isRequested && row.status === 'REJECTED' && (
                    <Badge color="red" variant="soft">
                      Rejected
                    </Badge>
                  )}
                  {row.isRequested && row.status === 'REQUESTED' && (
                    <Badge color="orange" variant="soft">
                      Requested
                    </Badge>
                  )}
                </Flex>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{row.createdAt ? formatDateUtcYYYYMMDD(row.createdAt) : '—'}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{formatExpiry(row)}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{row.policyName ?? (row.isAuto ? 'Auto-generated' : '—')}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">
                  {formatScope(row)}
                </Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">
                  {truncate(row.reason, 60)}
                </Text>
              </Table.Cell>
              <Table.Cell justify="end">
                {href ? (
                  <Link
                    size="2"
                    href={href}
                    data-testid={`${testId}-row-detail-link`}
                  >
                    View Details →
                  </Link>
                ) : (
                  <Text size="2" color="gray">
                    —
                  </Text>
                )}
              </Table.Cell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table.Root>
  );
}
