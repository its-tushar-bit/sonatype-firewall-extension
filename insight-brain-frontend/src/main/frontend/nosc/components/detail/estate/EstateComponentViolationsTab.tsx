/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback } from 'react';
import axios from 'axios';
import { Badge, Card, Flex, Inset, Link as RadixLink, Table, Text } from '@radix-ui/themes';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import {
  buildViolationsListRequest,
  VIOLATIONS_PAGE_SIZE,
} from 'MainRoot/nosc/violations/violationsListApi';
import { violationDetailHref } from 'MainRoot/nosc/violations/violationDetailHref';
import type { ViolationRow, ViolationsListResponse } from 'MainRoot/nosc/violations/violationListTypes';
import { getViolationsListUrl } from 'MainRoot/util/CLMLocation';
import { useEstateComponentDetailShellContext } from './estateComponentDetailContext';
import { useEstateComponentPagedTab } from './useEstateComponentPagedTab';

function policyViolationHref(policyViolationId: string | undefined): string | null {
  const trimmed = policyViolationId?.trim();
  if (!trimmed) {
    return null;
  }
  return violationDetailHref(trimmed);
}

export function EstateComponentViolationsTab(): JSX.Element {
  const { componentHash } = useEstateComponentDetailShellContext();

  const fetchPage = useCallback(
    async (pageIndex: number, signal: AbortSignal) => {
      const { data } = await axios.post<ViolationsListResponse>(
        getViolationsListUrl(),
        buildViolationsListRequest({
          page: pageIndex,
          pageSize: VIOLATIONS_PAGE_SIZE,
          includeFacets: false,
          componentHash,
        }),
        { signal },
      );
      return {
        rows: data.violations ?? [],
        total: data.total ?? 0,
        hasNextPage: Boolean(data.hasNextPage),
      };
    },
    [componentHash],
  );

  const { loading, error, rows, total, hasNextPage, page, setPage, onRetry } =
    useEstateComponentPagedTab<ViolationRow>({
      componentHash,
      endpointLabel: 'dashboard/violations/list',
      fetchPage,
      loadErrorMessage: 'Could not load policy violations for this component.',
    });

  return (
    <Flex direction="column" gap="3" mt="4">
      <AsyncPageState
        loading={loading}
        error={error}
        onRetry={onRetry}
        loadingHeight={200}
        loadingTestId="nosc-estate-component-violations-loading"
        errorTestId="nosc-estate-component-violations-error"
        errorTitle="Failed to load policy violations"
      >
        {rows.length === 0 ? (
          <Flex direction="column" gap="2" data-testid="nosc-estate-component-violations-empty">
            <Text size="2" color="gray">
              No policy violations found for this component.
            </Text>
          </Flex>
        ) : (
          <Card data-testid="nosc-estate-component-violations">
            <Inset>
              <Table.Root data-testid="nosc-estate-component-violations-table">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Policy</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Application</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>State</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {rows.map((row, idx) => {
                    const href = policyViolationHref(row.policyViolationId);
                    const label = row.policyName || row.policyViolationId || '—';
                    return (
                      <Table.Row
                        key={row.policyViolationId || `violation-${idx}`}
                        data-testid="nosc-estate-component-violations-row"
                      >
                        <Table.Cell>
                          {typeof row.threatLevel === 'number' ? (
                            <Badge size="1" variant="soft" color="gray">
                              {row.threatLevel}
                            </Badge>
                          ) : (
                            <Text size="2">—</Text>
                          )}
                        </Table.Cell>
                        <Table.Cell>
                          {href ? (
                            <RadixLink
                              size="2"
                              href={href}
                              data-testid="nosc-estate-component-violations-row-link"
                            >
                              {label}
                            </RadixLink>
                          ) : (
                            <Text size="2" data-testid="nosc-estate-component-violations-row-label">
                              {label}
                            </Text>
                          )}
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2">{row.applicationName || row.applicationPublicId || '—'}</Text>
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2">{row.state || '—'}</Text>
                        </Table.Cell>
                      </Table.Row>
                    );
                  })}
                </Table.Body>
              </Table.Root>
            </Inset>
            <Pagination
              page={page + 1}
              pageSize={VIOLATIONS_PAGE_SIZE}
              totalItems={total}
              hasNextPage={hasNextPage}
              onPageChange={(next1Based) => setPage(Math.max(0, next1Based - 1))}
              data-testid="nosc-estate-component-violations-pagination"
            />
          </Card>
        )}
      </AsyncPageState>
    </Flex>
  );
}
