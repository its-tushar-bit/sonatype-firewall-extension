/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Flex, Link, Text } from '@radix-ui/themes';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { DashboardTile } from 'MainRoot/nosc/dashboard/DashboardTile';
import { dashboardViolationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import { useLegalObligations } from './useLegalObligations';
import {
  isAlpVariant,
  isTopLegalViolationsVariant,
} from './legalObligationsTypes';
import {
  LegalObligationsAlpRow,
  LegalObligationsTopViolationRow,
} from './legalObligationsTileRows';

import './legalObligationsTile.css';

/**
 * Legal Obligations tile (CLM-39604 / S2-PR-D-2 frontend half).
 *
 * Consumes the tenant-cached `/rest/dashboard/legalObligations` endpoint and renders
 * one of four UX-F11-002..005 states based on a server-side discriminated payload.
 */

const TOP_N_ALP = 4;

function EmptyBody() {
  return (
    <Flex
      direction="column"
      align="center"
      gap="2"
      py="4"
      data-testid="legal-tile-empty-body"
    >
      <DomainIcons.Legal size={28} color="var(--gray-9)" />
      <Text size="2" color="gray">
        No unreviewed components in scope.
      </Text>
    </Flex>
  );
}

function PermissionDeniedBody() {
  return (
    <Flex
      direction="column"
      align="center"
      gap="2"
      py="4"
      data-testid="legal-tile-permission-denied-body"
      className="legal-obligation-tile__permission-denied-body"
    >
      <DomainIcons.Legal size={28} color="var(--gray-9)" />
      <Text size="2" color="gray" align="center">
        You don&apos;t have access to legal data in any scoped application.
      </Text>
    </Flex>
  );
}

export function LegalObligationsTile() {
  const { status, data, retry } = useLegalObligations();

  const headerExtra =
    status === 'ready' ? (
      <Link
        size="2"
        href={dashboardViolationsHref()}
        data-testid="legal-tile-view-details"
      >
        View violations →
      </Link>
    ) : undefined;

  const chromeStatus =
    status === 'loading'
      ? 'loading'
      : status === 'error'
        ? 'error'
        : 'ready';

  return (
    <DashboardTile
      title="Legal Obligations"
      status={chromeStatus}
      onRetry={retry}
      errorMessage="Failed to load legal obligations"
      headerExtra={headerExtra}
    >
      {status === 'permission-denied' && <PermissionDeniedBody />}
      {status === 'empty' && <EmptyBody />}
      {status === 'ready' && data && isAlpVariant(data) && (
        <Flex direction="column" gap="2" data-testid="legal-obligations-tile-body-alp">
          <Text size="1" color="gray" data-testid="legal-tile-alp-subtitle">
            Unreviewed components by threat group
          </Text>
          {data.groups.slice(0, TOP_N_ALP).map((group) => (
            <LegalObligationsAlpRow key={group.id} group={group} />
          ))}
          {data.groups.length === 0 && <EmptyBody />}
          {data.groups.length > TOP_N_ALP && (
            <Box pt="1">
              <Text size="1" color="gray">
                Showing top {TOP_N_ALP} of {data.groups.length} threat group
                {data.groups.length === 1 ? '' : 's'} with unreviewed components.
              </Text>
            </Box>
          )}
        </Flex>
      )}
      {status === 'ready' && data && isTopLegalViolationsVariant(data) && (
        <Flex direction="column" gap="2" data-testid="legal-obligations-tile-body-top">
          <Text size="1" color="gray" data-testid="legal-tile-top-subtitle">
            Top open license policy violations
          </Text>
          {data.violations.map((violation) => (
            <LegalObligationsTopViolationRow key={violation.policyId} violation={violation} />
          ))}
          {data.violations.length === 0 && <EmptyBody />}
        </Flex>
      )}
    </DashboardTile>
  );
}
