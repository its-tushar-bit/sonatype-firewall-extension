/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import { Badge, Box, Card, Flex, Grid, Text } from '@radix-ui/themes';
import PreviewDashboardApplicationsAppNameLink from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardApplicationsAppNameLink';
import { applicationDetailHref } from 'MainRoot/nosc/applications/applicationDetailUtils';
import {
  ApplicationRiskScore,
  ApplicationStageRisk,
} from 'MainRoot/nosc/applications/applicationListTypes';
import { ApplicationSeverityBadge } from 'MainRoot/nosc/dashboard/tabs/ApplicationSeverityBadge';
import './EvaluationCardGrid.scss';

/** Display order for known IQ stages; unknown stage ids are appended alphabetically. */
const STAGE_COLUMN_ORDER: ReadonlyArray<{ readonly id: string; readonly label: string }> = [
  { id: 'develop', label: 'Develop' },
  { id: 'source', label: 'Source' },
  { id: 'build', label: 'Build' },
  { id: 'stage-release', label: 'Stage Release' },
  { id: 'release', label: 'Release' },
  { id: 'operate', label: 'Operate' },
];

/** Prototype baseline columns always rendered for layout stability. */
const ALWAYS_VISIBLE_STAGE_IDS: ReadonlySet<string> = new Set([
  'source',
  'build',
  'stage-release',
  'release',
]);

function formatRelativeTime(isoDate?: string): string {
  if (!isoDate) return '—';
  const parsed = new Date(isoDate);
  if (Number.isNaN(parsed.getTime())) return '—';
  const deltaMs = parsed.getTime() - Date.now();
  const absMs = Math.abs(deltaMs);
  const minute = 60_000;
  const hour = 60 * minute;
  const day = 24 * hour;
  const month = 30 * day;
  const year = 365 * day;
  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });
  if (absMs < hour) return rtf.format(Math.round(deltaMs / minute), 'minute');
  if (absMs < day) return rtf.format(Math.round(deltaMs / hour), 'hour');
  if (absMs < month) return rtf.format(Math.round(deltaMs / day), 'day');
  if (absMs < year) return rtf.format(Math.round(deltaMs / month), 'month');
  return rtf.format(Math.round(deltaMs / year), 'year');
}

function latestEvaluationDate(stageRisks: ReadonlyArray<ApplicationStageRisk>): string | undefined {
  let latest: string | undefined;
  let latestTime = -Infinity;
  stageRisks.forEach((stage) => {
    if (!stage.evaluationDate) return;
    const time = new Date(stage.evaluationDate).getTime();
    if (Number.isNaN(time)) return;
    if (time > latestTime) {
      latestTime = time;
      latest = stage.evaluationDate;
    }
  });
  return latest;
}

function stageById(
  stageRisks: ReadonlyArray<ApplicationStageRisk>,
): ReadonlyMap<string, ApplicationStageRisk> {
  const map = new Map<string, ApplicationStageRisk>();
  stageRisks.forEach((stage) => map.set(stage.stageTypeId, stage));
  return map;
}

/**
 * Baseline source/build/stage-release/release columns, plus any other stages present on the
 * page (develop, operate, custom) so rail filters never drop risk into an invisible column.
 */
export function resolveStageColumns(
  applications: ReadonlyArray<ApplicationRiskScore>,
): ReadonlyArray<{ readonly id: string; readonly label: string }> {
  const labels = new Map<string, string>();
  applications.forEach((application) => {
    application.stageRisks.forEach((stage) => {
      if (!labels.has(stage.stageTypeId)) {
        labels.set(stage.stageTypeId, stage.stageTypeName || stage.stageTypeId);
      }
    });
  });

  const idsToShow = new Set<string>(ALWAYS_VISIBLE_STAGE_IDS);
  labels.forEach((_, id) => idsToShow.add(id));

  const ordered: Array<{ readonly id: string; readonly label: string }> = [];
  const remaining = new Set(idsToShow);
  STAGE_COLUMN_ORDER.forEach((column) => {
    if (!remaining.has(column.id)) return;
    ordered.push({ id: column.id, label: labels.get(column.id) ?? column.label });
    remaining.delete(column.id);
  });
  Array.from(remaining)
    .sort((left, right) => left.localeCompare(right))
    .forEach((id) => {
      ordered.push({ id, label: labels.get(id) ?? id });
    });

  return ordered;
}

function StageColumn({
  publicId,
  columnId,
  columnLabel,
  stage,
}: {
  readonly publicId: string;
  readonly columnId: string;
  readonly columnLabel: string;
  readonly stage?: ApplicationStageRisk;
}): JSX.Element {
  const detailHref = applicationDetailHref(publicId);
  const hasEvaluation = Boolean(stage?.scanId);
  const risk = stage?.risk;

  const body = hasEvaluation && risk ? (
    <Flex direction="column" gap="2" className="nosc-evaluation-stage-column__body">
      <Flex align="baseline" justify="between" gap="2" wrap="wrap">
        <Text size="2" weight="medium">
          {stage?.stageTypeName ?? columnLabel}
        </Text>
        <Text size="1" color="gray">
          {risk.totalRisk.toLocaleString()}
        </Text>
      </Flex>
      <Flex gap="1" wrap="wrap" aria-label={`${columnLabel} policy violations by severity`}>
        <ApplicationSeverityBadge value={risk.criticalRisk} severity="critical" />
        <ApplicationSeverityBadge value={risk.severeRisk} severity="severe" />
        <ApplicationSeverityBadge value={risk.moderateRisk} severity="moderate" />
        <ApplicationSeverityBadge value={risk.lowRisk} severity="low" />
      </Flex>
      <Text size="1" color="gray">
        {formatRelativeTime(stage?.evaluationDate)}
      </Text>
    </Flex>
  ) : (
    <Flex direction="column" gap="2" className="nosc-evaluation-stage-column__body">
      <Text size="2" weight="medium">
        {columnLabel}
      </Text>
      <Text size="2" color="gray" data-testid="evaluation-card-stage-not-evaluated">
        Not evaluated
      </Text>
    </Flex>
  );

  // Purged/missing reports on large estates make direct report deep-links fail; route to
  // application detail (recommended safe click-through) when a stage evaluation exists.
  if (hasEvaluation) {
    return (
      <Box
        asChild
        p="3"
        data-testid="evaluation-card-stage-tile"
        data-stage-id={columnId}
        className="nosc-evaluation-stage-column nosc-evaluation-stage-column--link"
      >
        <a href={detailHref} aria-label={`Open ${columnLabel} for application`}>
          {body}
        </a>
      </Box>
    );
  }

  return (
    <Box
      p="3"
      data-testid="evaluation-card-stage-tile"
      data-stage-id={columnId}
      className="nosc-evaluation-stage-column"
      aria-label={`${columnLabel} has no evaluation`}
    >
      {body}
    </Box>
  );
}

function EvaluationCard({
  application,
  stageColumns,
}: {
  readonly application: ApplicationRiskScore;
  readonly stageColumns: ReadonlyArray<{ readonly id: string; readonly label: string }>;
}): JSX.Element {
  const lastEvaluation =
    application.lastEvaluationDate ?? latestEvaluationDate(application.stageRisks);
  const { totalApplicationRisk } = application;
  const stages = stageById(application.stageRisks);
  const columnCount = Math.min(Math.max(stageColumns.length, 1), 6);

  return (
    <Card data-testid="evaluation-card" className="nosc-evaluation-card">
      <Flex direction="column" gap="4">
        <Flex align="start" justify="between" gap="3" wrap="wrap">
          <Flex direction="column" gap="1" className="nosc-evaluation-card__identity">
            <PreviewDashboardApplicationsAppNameLink
              publicId={application.applicationId}
              name={application.applicationName}
            />
            <Text size="2" color="gray" className="nosc-evaluation-card__org">
              {application.organizationName}
            </Text>
            <Text size="1" color="gray" data-testid="evaluation-card-last-evaluation">
              Last evaluation: {formatRelativeTime(lastEvaluation)}
            </Text>
          </Flex>
          <Badge
            size="2"
            color="yellow"
            variant="outline"
            radius="full"
            data-testid="evaluation-card-total-risk"
            className="nosc-evaluation-card__total-risk"
          >
            Total Risk {totalApplicationRisk.totalRisk.toLocaleString()}
          </Badge>
        </Flex>

        <Grid columns={{ initial: '1', sm: '2', md: String(columnCount) }} gap="2">
          {stageColumns.map((column) => (
            <StageColumn
              key={column.id}
              publicId={application.applicationId}
              columnId={column.id}
              columnLabel={column.label}
              stage={stages.get(column.id)}
            />
          ))}
        </Grid>
      </Flex>
    </Card>
  );
}

export interface EvaluationCardGridProps {
  readonly applications: ReadonlyArray<ApplicationRiskScore>;
}

/** Card grid for Martha V1 Applications (CLM-42223 / CLM-42224). */
export default function EvaluationCardGrid({ applications }: EvaluationCardGridProps): JSX.Element {
  const stageColumns = useMemo(() => resolveStageColumns(applications), [applications]);

  return (
    <Flex direction="column" gap="3" data-testid="evaluation-card-grid">
      {applications.map((application) => (
        <EvaluationCard
          key={application.applicationId}
          application={application}
          stageColumns={stageColumns}
        />
      ))}
    </Flex>
  );
}
