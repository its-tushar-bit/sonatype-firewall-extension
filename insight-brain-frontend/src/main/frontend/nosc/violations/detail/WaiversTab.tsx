/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, type ReactElement } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useRouter } from '@uirouter/react';
import { Badge, Box, Flex, Link, Table, Text } from '@radix-ui/themes';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { formatWaiverCalendarDate } from 'MainRoot/nosc/waivers/waiverDisplayUtils';
import { fetchViolationWaivers } from 'MainRoot/nosc/violations/detail/violationDetailSlice';
import {
  selectViolationDetailId,
  selectViolationDetailWaiversState,
} from 'MainRoot/nosc/violations/detail/violationDetailSelectors';
import type { ApplicableWaiverDTO } from 'MainRoot/nosc/violations/detail/violationDetailTypes';

function formatScope(scopeOwnerType: string): string {
  return scopeOwnerType
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ');
}

function formatExpiry(waiver: ApplicableWaiverDTO): string {
  return waiver.expiryTime ? formatWaiverCalendarDate(waiver.expiryTime) : 'Never';
}

function WaiversTable({
  waivers,
  testId,
}: {
  readonly waivers: ReadonlyArray<ApplicableWaiverDTO>;
  readonly testId: string;
}): ReactElement {
  const { stateService } = useRouter();

  const waiverDetailHref = (waiver: ApplicableWaiverDTO): string =>
    stateService.href('nexusOneWaiverDetail', {
      ownerType: waiver.scopeOwnerType,
      ownerId: waiver.scopeOwnerId,
      waiverId: waiver.policyWaiverId,
    });

  return (
    <Table.Root variant="surface" size="2" data-testid={testId}>
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeaderCell>Owner</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Scope</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Expiry</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Comment</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell justify="end">Actions</Table.ColumnHeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {waivers.map((waiver) => (
          <Table.Row key={waiver.policyWaiverId}>
            <Table.Cell>
              <Text size="2">{waiver.scopeOwnerName}</Text>
            </Table.Cell>
            <Table.Cell>
              <Text size="2" color="gray">
                {formatScope(waiver.scopeOwnerType)}
              </Text>
            </Table.Cell>
            <Table.Cell>
              <Text size="2">{formatExpiry(waiver)}</Text>
            </Table.Cell>
            <Table.Cell>
              <Text size="2">{waiver.comment || '-'}</Text>
            </Table.Cell>
            <Table.Cell justify="end">
              <Link
                size="2"
                href={waiverDetailHref(waiver)}
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

export function WaiversTab(): ReactElement {
  const dispatch = useDispatch<any>();
  const violationId = useSelector(selectViolationDetailId);
  const waiversState = useSelector(selectViolationDetailWaiversState);
  const activeWaivers = waiversState.active;
  const expiredWaivers = waiversState.expired;
  const loading = waiversState.status === 'loading' || waiversState.status === 'idle';
  const hasWaivers = activeWaivers.length > 0 || expiredWaivers.length > 0;
  const retry = useCallback(() => {
    if (!violationId) return;
    void dispatch(fetchViolationWaivers({ violationId }));
  }, [dispatch, violationId]);

  if (loading || waiversState.error) {
    return (
      <Box pt="4" data-testid="nosc-violation-detail-waivers-tab">
        <AsyncPageState
          loading={loading}
          error={waiversState.error}
          onRetry={retry}
          loadingHeight={220}
          loadingTestId="nosc-violation-detail-waivers-loading"
          errorTestId="nosc-violation-detail-waivers-error"
          errorTitle="Failed to load applicable waivers"
          errorVariant="banner"
        />
      </Box>
    );
  }

  return (
    <Box pt="4" data-testid="nosc-violation-detail-waivers-tab">
      <Flex direction="column" gap="4">
        <Flex direction="column" gap="1">
          <Flex align="center" gap="2" wrap="wrap">
            <Text size="3" weight="medium">
              Applicable waivers
            </Text>
            <Badge color="blue" variant="soft" radius="full">
              {activeWaivers.length} active
            </Badge>
            <Badge color="gray" variant="soft" radius="full">
              {expiredWaivers.length} expired
            </Badge>
          </Flex>
          <Text size="2" color="gray">
            Active and expired waivers that apply to this violation.
          </Text>
        </Flex>

        {!hasWaivers && (
          <Flex
            direction="column"
            align="center"
            gap="2"
            py="8"
            data-testid="nosc-violation-detail-waivers-empty"
          >
            <DomainIcons.Waivers size={32} color="var(--gray-9)" />
            <Text size="3" color="gray">
              No applicable waivers
            </Text>
            <Text size="2" color="gray" align="center" style={{ maxWidth: 520 }}>
              Active and expired waivers that apply to this violation will appear here.
            </Text>
          </Flex>
        )}

        {activeWaivers.length > 0 && (
          <Flex direction="column" gap="2">
            <Text size="2" weight="medium">
              Active waivers
            </Text>
            <WaiversTable waivers={activeWaivers} testId="nosc-violation-detail-active-waivers-table" />
          </Flex>
        )}

        {expiredWaivers.length > 0 && (
          <Flex direction="column" gap="2">
            <Text size="2" weight="medium">
              Expired waivers
            </Text>
            <WaiversTable waivers={expiredWaivers} testId="nosc-violation-detail-expired-waivers-table" />
          </Flex>
        )}
      </Flex>
    </Box>
  );
}
