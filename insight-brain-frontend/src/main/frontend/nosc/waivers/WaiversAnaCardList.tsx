/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Card, Flex, Link, Text } from '@radix-ui/themes';
import { useRouter } from '@uirouter/react';
import type { AnaWaiverRow } from 'MainRoot/nosc/waivers/waiversListTypes';
import {
  normalizeWaiverOwnerTypeForApi,
  waiverDetailTypeParam,
} from 'MainRoot/nosc/waivers/waiverDisplayUtils';
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';
import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';

import './WaiversAnaCardList.scss';

/**
 * Vision-style card list for Ana waivers (FE pass — no index changes).
 * Omits Component: {@link AnaWaiverRow} does not project component identity.
 */
export interface WaiversAnaCardListProps {
  readonly waivers: ReadonlyArray<AnaWaiverRow>;
  readonly testId?: string;
  readonly linkFrom?: string;
}

function formatScopeLine(row: AnaWaiverRow): string {
  if (row.applicationName) return `application · ${row.applicationName}`;
  if (row.organizationName) return `organization · ${row.organizationName}`;
  if (row.scopeOwnerType && row.scopeOwnerId) {
    return `${String(row.scopeOwnerType).toLowerCase()} · ${row.scopeOwnerId}`;
  }
  return '—';
}

function truncate(text: string | null, max: number): string {
  if (!text) return '—';
  if (text.length <= max) return text;
  return `${text.slice(0, max - 1)}…`;
}

function statusBadge(row: AnaWaiverRow): {
  readonly label: string;
  readonly color: 'blue' | 'red' | 'green' | 'orange' | 'gray';
} {
  if (row.isRequested && row.status === 'REJECTED') return { label: 'Rejected', color: 'red' };
  if (row.isRequested && (row.status === 'REQUESTED' || !row.status)) {
    return { label: 'Requested', color: 'blue' };
  }
  if (row.isAuto) return { label: 'Auto-Waived', color: 'green' };
  if (row.expiresAt) {
    const ms = Date.parse(row.expiresAt);
    if (!Number.isNaN(ms) && ms < Date.now()) return { label: 'Expired', color: 'orange' };
    return { label: 'Active', color: 'green' };
  }
  return { label: 'Never', color: 'gray' };
}

function formatCreated(row: AnaWaiverRow): string {
  if (!row.createdAt) return 'Created: —';
  // Vision uses "Jul 28, 2026"; keep UTC YYYY-MM-DD for consistency with the rest of NOSC.
  return `Created: ${formatDateUtcYYYYMMDD(row.createdAt)}`;
}

export default function WaiversAnaCardList({
  waivers,
  testId = 'waivers-ana-list',
  linkFrom,
}: WaiversAnaCardListProps): JSX.Element {
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
    <Flex direction="column" gap="2" data-testid={testId}>
      {waivers.map((row) => {
        const href = waiverDetailHref(row);
        const status = statusBadge(row);
        const policyLabel = row.policyName ?? (row.isAuto ? 'Auto-generated' : '(unknown policy)');
        const body = (
          <Card
            size="2"
            className="nosc-waivers-ana-card"
            data-testid={`${testId}-card`}
          >
            <Flex justify="between" align="start" gap="4" wrap="wrap">
              <Flex direction="column" gap="1" minWidth="0" style={{ flex: 1 }}>
                <Flex align="center" gap="2" wrap="wrap">
                  <Badge color={threatColorFor(row.threatLevel)} variant="solid" radius="full">
                    {row.threatLevel}
                  </Badge>
                  <Text size="3" weight="medium">
                    {policyLabel}
                  </Text>
                </Flex>
                <Text size="2" color="gray">
                  <Text weight="medium" as="span">
                    Scope:
                  </Text>{' '}
                  {formatScopeLine(row)}
                </Text>
                <Text size="2" color="gray">
                  <Text weight="medium" as="span">
                    Reason:
                  </Text>{' '}
                  {truncate(row.reason, 80)}
                </Text>
              </Flex>
              <Flex direction="column" align="end" gap="2" shrink="0">
                <Text size="1" color="gray">
                  {formatCreated(row)}
                </Text>
                <Badge color={status.color} variant="soft">
                  {status.label}
                </Badge>
              </Flex>
            </Flex>
          </Card>
        );

        if (!href) {
          return (
            <Box key={row.id} data-testid={`${testId}-row`}>
              {body}
            </Box>
          );
        }

        return (
          <Link
            key={row.id}
            href={href}
            underline="none"
            highContrast
            className="nosc-waivers-ana-card-link"
            data-testid={`${testId}-row-detail-link`}
          >
            <Box data-testid={`${testId}-row`}>{body}</Box>
          </Link>
        );
      })}
    </Flex>
  );
}
