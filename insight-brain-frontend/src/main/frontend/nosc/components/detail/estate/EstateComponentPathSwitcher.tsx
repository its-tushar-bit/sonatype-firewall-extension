/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect, useMemo, useState } from 'react';
import type { ReactElement } from 'react';
import axios from 'axios';
import { Button, Flex, Link as RadixLink, Text } from '@radix-ui/themes';
import { applicationUsageReportHref, formatLastSeen } from './estateComponentDetailUtils';
import { COMPONENT_USAGE_PAGE_SIZE, fetchComponentUsageReports } from './estateComponentUsageApi';
import type { ComponentUsageApplicationRow, ComponentUsageReportRow } from './estateComponentUsageApi';

type LoadStatus = 'loading' | 'ready' | 'error';
type SelectableApplication = ComponentUsageApplicationRow & { readonly applicationId: string };
type SelectableReport = ComponentUsageReportRow & { readonly reportId: string };

export type EstateComponentPathSwitcherApplicationsUsage = {
  readonly applications: ReadonlyArray<ComponentUsageApplicationRow>;
  readonly total: number;
  readonly status: LoadStatus;
};

type EstateComponentPathSwitcherProps = {
  readonly componentHash: string;
  readonly applicationsUsage: EstateComponentPathSwitcherApplicationsUsage;
};

function applicationLabel(row: ComponentUsageApplicationRow): string {
  const primary = row.applicationName || row.applicationPublicId || row.applicationId || 'Application';
  return row.organizationName ? `${primary} - ${row.organizationName}` : primary;
}

function reportLabel(row: ComponentUsageReportRow): string {
  const stage = row.stageTypeId || 'Report';
  const seen = formatLastSeen(row.evaluationTime);
  return seen === '—' ? stage : `${stage} - ${seen}`;
}

function hasApplicationId(row: ComponentUsageApplicationRow): row is SelectableApplication {
  return Boolean(row.applicationId?.trim());
}

function hasReportId(row: ComponentUsageReportRow): row is SelectableReport {
  return Boolean(row.reportId?.trim());
}

export function EstateComponentPathSwitcher({
  componentHash,
  applicationsUsage,
}: EstateComponentPathSwitcherProps): ReactElement {
  const { applications, total: applicationsTotal, status: applicationsStatus } = applicationsUsage;
  const [selectedApplicationId, setSelectedApplicationId] = useState('');
  const [reportsStatus, setReportsStatus] = useState<LoadStatus>('ready');
  const [reports, setReports] = useState<ReadonlyArray<ComponentUsageReportRow>>([]);
  const [selectedReportId, setSelectedReportId] = useState('');

  useEffect(() => {
    setSelectedApplicationId('');
    setReports([]);
    setSelectedReportId('');
    setReportsStatus('ready');
  }, [componentHash]);

  useEffect(() => {
    if (!selectedApplicationId) {
      setReports([]);
      setSelectedReportId('');
      setReportsStatus('ready');
      return undefined;
    }

    const controller = new AbortController();
    setReportsStatus('loading');
    setReports([]);
    setSelectedReportId('');

    void fetchComponentUsageReports(
      componentHash,
      selectedApplicationId,
      0,
      COMPONENT_USAGE_PAGE_SIZE,
      controller.signal
    )
      .then((response) => {
        if (controller.signal.aborted) return;
        setReports(response.reports);
        setReportsStatus('ready');
      })
      .catch((err) => {
        if (axios.isCancel(err) || controller.signal.aborted) return;
        setReportsStatus('error');
      });

    return () => controller.abort();
  }, [componentHash, selectedApplicationId]);

  const selectableApplications = useMemo(() => applications.filter(hasApplicationId), [applications]);
  const selectableReports = useMemo(() => reports.filter(hasReportId), [reports]);
  const selectedApplication = selectableApplications.find(
    (application) => application.applicationId === selectedApplicationId
  );
  const selectedReport = selectableReports.find((report) => report.reportId === selectedReportId);
  const reportHref = applicationUsageReportHref(
    selectedApplication?.applicationPublicId,
    selectedReport?.reportId
  );

  return (
    <Flex direction="column" gap="2" data-testid="nosc-estate-component-path-switcher">
      <Text size="2" weight="medium">
        Path
      </Text>
      <Flex gap="3" wrap="wrap" align="end">
        <Flex direction="column" gap="1">
          <Text size="1" color="gray" as="label" htmlFor="estate-component-path-application">
            Application
          </Text>
          <select
            id="estate-component-path-application"
            aria-label="Application"
            value={selectedApplicationId}
            disabled={applicationsStatus !== 'ready' || selectableApplications.length === 0}
            onChange={(event) => setSelectedApplicationId(event.target.value)}
            data-testid="nosc-estate-component-path-switcher-application"
          >
            <option value="">
              {applicationsStatus === 'loading' ? 'Loading applications...' : 'Select application'}
            </option>
            {selectableApplications.map((application) => (
              <option key={application.applicationId} value={application.applicationId}>
                {applicationLabel(application)}
              </option>
            ))}
          </select>
        </Flex>

        <Flex direction="column" gap="1">
          <Text size="1" color="gray" as="label" htmlFor="estate-component-path-report">
            Report
          </Text>
          <select
            id="estate-component-path-report"
            aria-label="Report"
            value={selectedReportId}
            disabled={!selectedApplicationId || reportsStatus !== 'ready' || selectableReports.length === 0}
            onChange={(event) => setSelectedReportId(event.target.value)}
            data-testid="nosc-estate-component-path-switcher-report"
          >
            <option value="">{reportsStatus === 'loading' ? 'Loading reports...' : 'Select report'}</option>
            {selectableReports.map((report) => (
              <option key={report.reportId} value={report.reportId}>
                {reportLabel(report)}
              </option>
            ))}
          </select>
        </Flex>

        {reportHref && (
          <Button size="2" variant="soft" asChild>
            <RadixLink href={reportHref} data-testid="nosc-estate-component-path-switcher-report-link">
              Open report
            </RadixLink>
          </Button>
        )}
      </Flex>

      {applicationsStatus === 'error' && (
        <Text size="1" color="red">
          Applications could not be loaded.
        </Text>
      )}
      {reportsStatus === 'error' && (
        <Text size="1" color="red">
          Reports could not be loaded for this application.
        </Text>
      )}
      {applicationsStatus === 'ready' && applicationsTotal > applications.length && (
        <Text size="1" color="gray">
          Showing the first {applications.length} of {applicationsTotal} applications.
        </Text>
      )}
    </Flex>
  );
}
