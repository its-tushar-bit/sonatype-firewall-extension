/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Link, Table, Text } from '@radix-ui/themes';
import PreviewDashboardApplicationsAppNameLink from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardApplicationsAppNameLink';
import { PreviewDashboardApplication } from 'MainRoot/nosc/dashboard/tabs/previewDashboardApplicationsSelectors';
import {
  BadgeColor,
  ComponentScoreKind,
  severityColor as scoreColor,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardSeverity';

type Severity = 'critical' | 'severe' | 'moderate' | 'low' | 'total';

const SEVERITY_TO_SCORE_KIND: Record<Exclude<Severity, 'total'>, ComponentScoreKind> = {
  critical: 'crit',
  severe: 'sev',
  moderate: 'mod',
  low: 'low',
};

/**
 * Severity badge color. Replaces the Classic `DashboardHeatMapCell` saturation gradient with discrete Radix
 * accent colors — semantic, accessible, dark-mode-clean. Per-tier colors come from the shared
 * {@link scoreColor} helper so hues stay consistent across Preview surfaces.
 *
 * `total` is a roll-up cell, not a severity tier; it goes red when there are any violations and green
 * otherwise (intentionally distinct from the components grid's threat-bucketed total).
 */
function severityColor(value: number, severity: Severity): BadgeColor {
  if (value === 0) return 'green';
  if (severity === 'total') return 'red';
  return scoreColor(value, SEVERITY_TO_SCORE_KIND[severity]);
}

function SeverityBadge({
  value,
  severity,
}: {
  value: number;
  severity: Severity;
}): JSX.Element {
  return (
    <Badge color={severityColor(value, severity)} variant="soft" radius="full">
      {value}
    </Badge>
  );
}

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
          <SeverityBadge value={totalApplicationRisk.totalRisk} severity="total" />
        </Table.Cell>
        <Table.Cell justify="end">
          <SeverityBadge value={totalApplicationRisk.criticalRisk} severity="critical" />
        </Table.Cell>
        <Table.Cell justify="end">
          <SeverityBadge value={totalApplicationRisk.severeRisk} severity="severe" />
        </Table.Cell>
        <Table.Cell justify="end">
          <SeverityBadge value={totalApplicationRisk.moderateRisk} severity="moderate" />
        </Table.Cell>
        <Table.Cell justify="end">
          <SeverityBadge value={totalApplicationRisk.lowRisk} severity="low" />
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
