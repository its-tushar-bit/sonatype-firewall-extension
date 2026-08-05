/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback } from 'react';
import { Card, Flex, Inset, Table, Text } from '@radix-ui/themes';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { useEstateComponentDetailShellContext } from './estateComponentDetailContext';
import { formatLastSeen } from './estateComponentDetailUtils';
import {
  COMPONENT_USAGE_PAGE_SIZE,
  fetchComponentUsageOrganizations,
  type ComponentUsageOrganizationRow,
} from './estateComponentUsageApi';
import { useEstateComponentPagedTab } from './useEstateComponentPagedTab';

export function EstateComponentOrganizationsTab(): JSX.Element {
  const { componentHash } = useEstateComponentDetailShellContext();

  const fetchPage = useCallback(
    async (pageIndex: number, signal: AbortSignal) => {
      const data = await fetchComponentUsageOrganizations(
        componentHash,
        pageIndex,
        COMPONENT_USAGE_PAGE_SIZE,
        signal,
      );
      return {
        rows: data.organizations,
        total: data.total,
        hasNextPage: data.hasNextPage,
      };
    },
    [componentHash],
  );

  const { loading, error, rows, total, hasNextPage, page, setPage, onRetry } =
    useEstateComponentPagedTab<ComponentUsageOrganizationRow>({
      componentHash,
      endpointLabel: 'components/usage/organizations',
      fetchPage,
      loadErrorMessage: 'Could not load organizations using this component.',
    });

  return (
    <Flex direction="column" gap="3" mt="4">
      <AsyncPageState
        loading={loading}
        error={error}
        onRetry={onRetry}
        loadingHeight={200}
        loadingTestId="nosc-estate-component-organizations-loading"
        errorTestId="nosc-estate-component-organizations-error"
        errorTitle="Failed to load organizations"
      >
        {rows.length === 0 ? (
          <Flex
            direction="column"
            gap="2"
            data-testid="nosc-estate-component-organizations-empty"
          >
            <Text size="2" color="gray">
              This component was not found in any readable organizations.
            </Text>
          </Flex>
        ) : (
          <Card data-testid="nosc-estate-component-organizations">
            <Inset>
              <Table.Root data-testid="nosc-estate-component-organizations-table">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell>Organization</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Applications</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Last seen</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {rows.map((row, idx) => (
                    <Table.Row
                      key={row.organizationId || `org-${idx}`}
                      data-testid="nosc-estate-component-organizations-row"
                    >
                      <Table.Cell>
                        <Text size="2">{row.organizationName || row.organizationId || '—'}</Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2">
                          {typeof row.applicationCount === 'number' ? row.applicationCount : '—'}
                        </Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2">{formatLastSeen(row.lastSeenTime)}</Text>
                      </Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            </Inset>
            <Pagination
              page={page + 1}
              pageSize={COMPONENT_USAGE_PAGE_SIZE}
              totalItems={total}
              hasNextPage={hasNextPage}
              onPageChange={(next1Based) => setPage(Math.max(0, next1Based - 1))}
              data-testid="nosc-estate-component-organizations-pagination"
            />
          </Card>
        )}
      </AsyncPageState>
    </Flex>
  );
}
