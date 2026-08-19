/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Fragment, useCallback, useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { Badge, Button, Card, Flex, Inset, Link as RadixLink, Table, Text, VisuallyHidden } from '@radix-ui/themes';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { applicationDetailHref } from 'MainRoot/nosc/applications/applicationDetailUtils';
import { useEstateComponentDetailShellContext } from './estateComponentDetailContext';
import { applicationUsageReportHref, formatLastSeen } from './estateComponentDetailUtils';
import {
  COMPONENT_USAGE_PAGE_SIZE,
  fetchComponentUsageApplications,
  fetchComponentUsageReports,
} from './estateComponentUsageApi';
import type { ComponentUsageApplicationRow, ComponentUsageReportRow } from './estateComponentUsageApi';
import { useEstateComponentPagedTab } from './useEstateComponentPagedTab';

type ReportsLoadState = {
  readonly status: 'loading' | 'ready' | 'error';
  readonly reports: ReadonlyArray<ComponentUsageReportRow>;
  readonly total?: number;
};

function applicationHref(publicId: string | undefined): string | null {
  const trimmed = publicId?.trim();
  if (!trimmed) {
    return null;
  }
  return applicationDetailHref(trimmed);
}

function clearLoadingReportsEntry(
  current: Record<string, ReportsLoadState>,
  applicationId: string
): Record<string, ReportsLoadState> {
  if (current[applicationId]?.status !== 'loading') {
    return current;
  }
  const { [applicationId]: _removed, ...rest } = current;
  return rest;
}

export function EstateComponentApplicationsTab(): JSX.Element {
  const { componentHash } = useEstateComponentDetailShellContext();
  const [expandedApplicationId, setExpandedApplicationId] = useState<string | null>(null);
  const [reportsByApplicationId, setReportsByApplicationId] = useState<Record<string, ReportsLoadState>>({});
  const reportsAbortRef = useRef<AbortController | null>(null);

  const abortReportsLoad = useCallback(() => {
    reportsAbortRef.current?.abort();
    reportsAbortRef.current = null;
  }, []);

  useEffect(() => {
    abortReportsLoad();
    setExpandedApplicationId(null);
    setReportsByApplicationId({});
    return abortReportsLoad;
  }, [abortReportsLoad, componentHash]);

  const fetchPage = useCallback(
    async (pageIndex: number, signal: AbortSignal) => {
      const data = await fetchComponentUsageApplications(componentHash, pageIndex, COMPONENT_USAGE_PAGE_SIZE, signal);
      return {
        rows: data.applications,
        total: data.total,
        hasNextPage: data.hasNextPage,
      };
    },
    [componentHash]
  );

  const {
    loading,
    error,
    rows,
    total,
    hasNextPage,
    page,
    setPage,
    onRetry,
  } = useEstateComponentPagedTab<ComponentUsageApplicationRow>({
    componentHash,
    endpointLabel: 'components/usage/applications',
    fetchPage,
    loadErrorMessage: 'Could not load applications using this component.',
  });

  const loadReportsForApplication = useCallback(
    async (row: ComponentUsageApplicationRow) => {
      const applicationId = row.applicationId?.trim();
      if (!applicationId) {
        return;
      }

      if (expandedApplicationId === applicationId) {
        // Expanded error: same control retries. Otherwise collapse.
        if (reportsByApplicationId[applicationId]?.status !== 'error') {
          abortReportsLoad();
          setExpandedApplicationId(null);
          setReportsByApplicationId((current) => clearLoadingReportsEntry(current, applicationId));
          return;
        }
      }

      const previousApplicationId = expandedApplicationId;
      abortReportsLoad();
      if (previousApplicationId && previousApplicationId !== applicationId) {
        setReportsByApplicationId((current) => clearLoadingReportsEntry(current, previousApplicationId));
      }
      setExpandedApplicationId(applicationId);
      const cachedReports = reportsByApplicationId[applicationId];
      // Allow retry after a failed load; ready stays cached. Loading is never retained after abort.
      if (cachedReports && cachedReports.status === 'ready') {
        return;
      }

      setReportsByApplicationId((current) => ({
        ...current,
        [applicationId]: { status: 'loading', reports: [], total: 0 },
      }));

      const controller = new AbortController();
      reportsAbortRef.current = controller;
      try {
        const data = await fetchComponentUsageReports(
          componentHash,
          applicationId,
          0,
          COMPONENT_USAGE_PAGE_SIZE,
          controller.signal
        );
        if (controller.signal.aborted) {
          setReportsByApplicationId((current) => clearLoadingReportsEntry(current, applicationId));
          return;
        }
        setReportsByApplicationId((current) => ({
          ...current,
          [applicationId]: { status: 'ready', reports: data.reports, total: data.total },
        }));
      } catch (err) {
        if (axios.isCancel(err) || controller.signal.aborted) {
          setReportsByApplicationId((current) => clearLoadingReportsEntry(current, applicationId));
          return;
        }
        setReportsByApplicationId((current) => ({
          ...current,
          [applicationId]: { status: 'error', reports: [], total: 0 },
        }));
      }
    },
    [abortReportsLoad, componentHash, expandedApplicationId, reportsByApplicationId]
  );

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
          <Flex direction="column" gap="2" data-testid="nosc-estate-component-applications-empty">
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
                    const rowKey = row.applicationId || row.applicationPublicId || `app-${idx}`;
                    const applicationId = row.applicationId?.trim();
                    const isExpanded = Boolean(applicationId && expandedApplicationId === applicationId);
                    const reportsState = applicationId ? reportsByApplicationId[applicationId] : undefined;
                    return (
                      <Fragment key={rowKey}>
                        <Table.Row data-testid="nosc-estate-component-applications-row">
                          <Table.Cell>
                            {href ? (
                              <RadixLink size="2" href={href} data-testid="nosc-estate-component-applications-row-link">
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
                            <Flex direction="column" gap="2" align="start">
                              <Text size="2">{formatLastSeen(row.lastSeenTime)}</Text>
                              <Button
                                type="button"
                                size="1"
                                variant="soft"
                                disabled={!applicationId}
                                aria-expanded={isExpanded}
                                aria-controls={applicationId ? `reports-row-${rowKey}` : undefined}
                                onClick={() => loadReportsForApplication(row)}
                              >
                                {isExpanded
                                  ? reportsState?.status === 'error'
                                    ? 'Retry reports'
                                    : 'Hide reports'
                                  : 'View reports'}
                                <VisuallyHidden> for {label}</VisuallyHidden>
                              </Button>
                            </Flex>
                          </Table.Cell>
                        </Table.Row>
                        {isExpanded && (
                          <Table.Row
                            id={`reports-row-${rowKey}`}
                            data-testid="nosc-estate-component-applications-reports-row"
                          >
                            <Table.Cell colSpan={4}>
                              {reportsState?.status === 'loading' && (
                                <Text size="2" color="gray">
                                  Loading reports...
                                </Text>
                              )}
                              {reportsState?.status === 'error' && (
                                <Text size="2" color="red">
                                  Reports could not be loaded for this application. Choose Retry reports to try again.
                                </Text>
                              )}
                              {reportsState?.status === 'ready' && reportsState.reports.length === 0 && (
                                <Text size="2" color="gray">
                                  No reports found for this application.
                                </Text>
                              )}
                              {reportsState?.status === 'ready' && reportsState.reports.length > 0 && (
                                <Flex direction="column" gap="2">
                                  {reportsState.reports.map((report, reportIdx) => {
                                    const reportHref = applicationUsageReportHref(
                                      row.applicationPublicId,
                                      report.reportId
                                    );
                                    const stage = report.stageTypeId || 'selected';
                                    const reportLabel = `${stage} report`;
                                    return (
                                      <Flex key={report.reportId || `report-${reportIdx}`} gap="3" align="center">
                                        {reportHref ? (
                                          <RadixLink size="2" href={reportHref}>
                                            Open {reportLabel}
                                          </RadixLink>
                                        ) : (
                                          <Text size="2">{report.reportId || 'Report'}</Text>
                                        )}
                                        <Text size="2" color="gray">
                                          {formatLastSeen(report.evaluationTime)}
                                        </Text>
                                      </Flex>
                                    );
                                  })}
                                  {typeof reportsState.total === 'number' &&
                                    reportsState.total > reportsState.reports.length && (
                                      <Text
                                        size="1"
                                        color="gray"
                                        data-testid="nosc-estate-component-applications-reports-truncated"
                                      >
                                        Showing the first {reportsState.reports.length} of {reportsState.total} reports.
                                      </Text>
                                    )}
                                </Flex>
                              )}
                            </Table.Cell>
                          </Table.Row>
                        )}
                      </Fragment>
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
