/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Link, Table, Text } from '@radix-ui/themes';
import PreviewDashboardApplicationsAppNameLink from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardApplicationsAppNameLink';
import { PreviewDashboardApplication } from 'MainRoot/nosc/dashboard/tabs/previewDashboardApplicationsSelectors';
import { ApplicationSeverityBadge } from 'MainRoot/nosc/dashboard/tabs/ApplicationSeverityBadge';

/**
 * Classic deep-link to the per-stage policy report. There is no
 * Preview-side report view yet, so this points to the Classic
 * route. Same behavior as the Classic
 * `DashboardApplicationsTableStageRiskRow` (which uses ui-router
 * state `applicationReport.policy`); switching hashes drops the
 * user out of the Preview shell into the Classic report.
 */
function stageReportHref(publicId: string, scanId: string): string {
  return (
    `#/management/view/application/${encodeURIComponent(publicId)}` +
    `/report/${encodeURIComponent(scanId)}/policy`
  );
}

export default function PreviewDashboardApplicationsRow({
  application,
}: {
  application: PreviewDashboardApplication;
}): JSX.Element {
  const { applicationId, applicationName, totalApplicationRisk, stageRisks } = application;
  return (
    <>
      <Table.Row data-testid="nosc-dashboard-applications-row">
        <Table.RowHeaderCell>
          <PreviewDashboardApplicationsAppNameLink
            publicId={applicationId}
            name={applicationName}
          />
        </Table.RowHeaderCell>
        <Table.Cell justify="end">
          <ApplicationSeverityBadge value={totalApplicationRisk.totalRisk} severity="total" />
        </Table.Cell>
        <Table.Cell justify="end">
          <ApplicationSeverityBadge value={totalApplicationRisk.criticalRisk} severity="critical" />
        </Table.Cell>
        <Table.Cell justify="end">
          <ApplicationSeverityBadge value={totalApplicationRisk.severeRisk} severity="severe" />
        </Table.Cell>
        <Table.Cell justify="end">
          <ApplicationSeverityBadge value={totalApplicationRisk.moderateRisk} severity="moderate" />
        </Table.Cell>
        <Table.Cell justify="end">
          <ApplicationSeverityBadge value={totalApplicationRisk.lowRisk} severity="low" />
        </Table.Cell>
      </Table.Row>
      {stageRisks.map((stage) => (
        <Table.Row
          key={stage.scanId}
          data-testid="nosc-dashboard-applications-stage-row"
        >
          <Table.Cell style={{ paddingLeft: 'var(--space-6)' }}>
            <Link href={stageReportHref(applicationId, stage.scanId)} underline="hover">
              <Text size="2" color="gray">
                {stage.stageTypeName}
              </Text>
            </Link>
          </Table.Cell>
          <Table.Cell justify="end">
            <Text size="2" color="gray">{stage.risk.totalRisk}</Text>
          </Table.Cell>
          <Table.Cell justify="end">
            <Text size="2" color="gray">{stage.risk.criticalRisk}</Text>
          </Table.Cell>
          <Table.Cell justify="end">
            <Text size="2" color="gray">{stage.risk.severeRisk}</Text>
          </Table.Cell>
          <Table.Cell justify="end">
            <Text size="2" color="gray">{stage.risk.moderateRisk}</Text>
          </Table.Cell>
          <Table.Cell justify="end">
            <Text size="2" color="gray">{stage.risk.lowRisk}</Text>
          </Table.Cell>
        </Table.Row>
      ))}
    </>
  );
}
