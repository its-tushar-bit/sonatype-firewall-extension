/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback } from 'react';
import { Badge, Card, Flex, Inset, Link as RadixLink, Table, Text } from '@radix-ui/themes';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { applicationDetailHref } from 'MainRoot/nosc/applications/applicationDetailUtils';
import { useEstateComponentDetailShellContext } from './estateComponentDetailContext';
import { formatLastSeen } from './estateComponentDetailUtils';
import {
  COMPONENT_USAGE_PAGE_SIZE,
  fetchComponentUsageApplications,
  type ComponentUsageApplicationRow,
} from './estateComponentUsageApi';
import { useEstateComponentPagedTab } from './useEstateComponentPagedTab';

function applicationHref(publicId: string | undefined): string | null {
  const trimmed = publicId?.trim();
  if (!trimmed) {
    return null;
  }
  return applicationDetailHref(trimmed);
}

export function EstateComponentApplicationsTab(): JSX.Element {
  const { componentHash } = useEstateComponentDetailShellContext();

  const fetchPage = useCallback(
    async (pageIndex: number, signal: AbortSignal) => {
      const data = await fetchComponentUsageApplications(
        componentHash,
        pageIndex,
        COMPONENT_USAGE_PAGE_SIZE,
        signal,
      );
      return {
        rows: data.applications,
        total: data.total,
        hasNextPage: data.hasNextPage,
      };
    },
    [componentHash],
  );

  const { loading, error, rows, total, hasNextPage, page, setPage, onRetry } =
    useEstateComponentPagedTab<ComponentUsageApplicationRow>({
      componentHash,
      endpointLabel: 'components/usage/applications',
      fetchPage,
      loadErrorMessage: 'Could not load applications using this component.',
    });

  return (
    <Flex direction="column" gap="3" mt="4">
      <AsyncPageState
        loading={loading}
        error={error}
        onRetry={onRetry}
        loadingHeight={200}
        loadingTestId="nosc-estate-component-applications-loading"
        errorTestId="nosc-estate-component-applications-error"
        errorTitle="Failed to load applications"
      >
        {rows.length === 0 ? (
          <Flex
            direction="column"
            gap="2"
            data-testid="nosc-estate-component-applications-empty"
          >
            <Text size="2" color="gray">
              This component was not found in any readable applications.
            </Text>
          </Flex>
        ) : (
          <Card data-testid="nosc-estate-component-applications">
            <Inset>
              <Table.Root data-testid="nosc-estate-component-applications-table">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell>Application</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Organization</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Stages</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Last seen</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {rows.map((row, idx) => {
                    const href = applicationHref(row.applicationPublicId);
                    const label = row.applicationName || row.applicationPublicId || 'Application';
                    return (
                      <Table.Row
                        key={row.applicationId || row.applicationPublicId || `app-${idx}`}
                        data-testid="nosc-estate-component-applications-row"
                      >
                        <Table.Cell>
                          {href ? (
                            <RadixLink
                              size="2"
                              href={href}
                              data-testid="nosc-estate-component-applications-row-link"
                            >
                              {label}
                            </RadixLink>
                          ) : (
                            <Text size="2">{label}</Text>
                          )}
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2">{row.organizationName || '—'}</Text>
                        </Table.Cell>
                        <Table.Cell>
                          <Flex gap="1" wrap="wrap">
                            {(row.stageTypeIds ?? []).length === 0 ? (
                              <Text size="2">—</Text>
                            ) : (
                              (row.stageTypeIds ?? []).map((stage) => (
                                <Badge key={stage} size="1" variant="soft" color="gray">
                                  {stage}
                                </Badge>
                              ))
                            )}
                          </Flex>
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2">{formatLastSeen(row.lastSeenTime)}</Text>
                        </Table.Cell>
                      </Table.Row>
                    );
                  })}
                </Table.Body>
              </Table.Root>
            </Inset>
            <Pagination
              page={page + 1}
              pageSize={COMPONENT_USAGE_PAGE_SIZE}
              totalItems={total}
              hasNextPage={hasNextPage}
              onPageChange={(next1Based) => setPage(Math.max(0, next1Based - 1))}
              data-testid="nosc-estate-component-applications-pagination"
            />
          </Card>
        )}
      </AsyncPageState>
    </Flex>
  );
}
