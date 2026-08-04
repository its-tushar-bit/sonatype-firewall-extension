/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Card, Flex, Grid, Text } from '@radix-ui/themes';
import PreviewDashboardApplicationsAppNameLink from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardApplicationsAppNameLink';
import { nouxApplicationReportHref } from 'MainRoot/nosc/routing/nouxNavigation';
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

const ABSOLUTE_DATE = new Intl.DateTimeFormat(undefined, {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
});

/** Martha stage tiles and header use absolute calendar dates (e.g. Jun 22, 2026). */
function formatAbsoluteDate(isoDate?: string): string {
  if (!isoDate) return '—';
  const parsed = new Date(isoDate);
  if (Number.isNaN(parsed.getTime())) return '—';
  return ABSOLUTE_DATE.format(parsed);
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

/**
 * Sparse Martha stage columns: only stages that have an evaluation on this application.
 * Empty "Not evaluated" placeholders are intentionally omitted (prototype is sparse).
 */
export function resolveStageColumns(
  application: ApplicationRiskScore,
): ReadonlyArray<{ readonly id: string; readonly label: string; readonly stage: ApplicationStageRisk }> {
  const evaluated = application.stageRisks.filter((stage) => Boolean(stage.scanId));
  const byId = new Map(evaluated.map((stage) => [stage.stageTypeId, stage]));

  const ordered: Array<{
    readonly id: string;
    readonly label: string;
    readonly stage: ApplicationStageRisk;
  }> = [];
  const remaining = new Set(byId.keys());

  STAGE_COLUMN_ORDER.forEach((column) => {
    const stage = byId.get(column.id);
    if (!stage) return;
    ordered.push({
      id: column.id,
      label: stage.stageTypeName || column.label,
      stage,
    });
    remaining.delete(column.id);
  });

  Array.from(remaining)
    .sort((left, right) => left.localeCompare(right))
    .forEach((id) => {
      const stage = byId.get(id)!;
      ordered.push({
        id,
        label: stage.stageTypeName || id,
        stage,
      });
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
  readonly stage: ApplicationStageRisk;
}): JSX.Element {
  const risk = stage.risk;

  const body = (
    <Flex direction="column" gap="2" className="nosc-evaluation-stage-column__body">
      <Flex align="baseline" justify="between" gap="2" wrap="wrap">
        <Text size="2" weight="medium">
          {columnLabel}
        </Text>
        {risk && (
          <Text size="1" color="gray">
            {risk.totalRisk.toLocaleString()}
          </Text>
        )}
      </Flex>
      {risk && (
        <Flex gap="1" wrap="wrap" aria-label={`${columnLabel} policy violations by severity`}>
          <ApplicationSeverityBadge value={risk.criticalRisk} severity="critical" />
          <ApplicationSeverityBadge value={risk.severeRisk} severity="severe" />
          <ApplicationSeverityBadge value={risk.moderateRisk} severity="moderate" />
          <ApplicationSeverityBadge value={risk.lowRisk} severity="low" />
        </Flex>
      )}
      <Text size="1" color="gray" data-testid="evaluation-card-stage-date">
        {formatAbsoluteDate(stage.evaluationDate)}
      </Text>
    </Flex>
  );

  // The app name opens native application detail; a stage tile is a specific
  // evaluation, so it opens that stage's Classic report in Nexus One chrome.
  // resolveStageColumns only emits stages that carry a scanId.
  const reportHref = nouxApplicationReportHref({ publicId, scanId: stage.scanId });

  return (
    <Box
      asChild
      p="3"
      data-testid="evaluation-card-stage-tile"
      data-stage-id={columnId}
      className="nosc-evaluation-stage-column nosc-evaluation-stage-column--link"
    >
      <a href={reportHref} aria-label={`Open the ${columnLabel} report for this application`}>
        {body}
      </a>
    </Box>
  );
}

function EvaluationCard({ application }: { readonly application: ApplicationRiskScore }): JSX.Element {
  const lastEvaluation =
    application.lastEvaluationDate ?? latestEvaluationDate(application.stageRisks);
  const { totalApplicationRisk } = application;
  const stageColumns = resolveStageColumns(application);
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
            <Flex align="center" gap="3" wrap="wrap" className="nosc-evaluation-card__meta">
              <Text size="2" color="gray" className="nosc-evaluation-card__org">
                <Text as="span" weight="medium">
                  Organization:
                </Text>{' '}
                {application.organizationName || '—'}
              </Text>
              <Text size="2" color="gray" data-testid="evaluation-card-last-evaluation">
                <Text as="span" weight="medium">
                  Last Evaluation:
                </Text>{' '}
                {formatAbsoluteDate(lastEvaluation)}
              </Text>
            </Flex>
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

        {stageColumns.length > 0 ? (
          <Grid columns={{ initial: '1', sm: '2', md: String(columnCount) }} gap="2">
            {stageColumns.map((column) => (
              <StageColumn
                key={column.id}
                publicId={application.applicationId}
                columnId={column.id}
                columnLabel={column.label}
                stage={column.stage}
              />
            ))}
          </Grid>
        ) : (
          <Text size="2" color="gray" data-testid="evaluation-card-no-stages">
            No stage evaluations
          </Text>
        )}
      </Flex>
    </Card>
  );
}

export interface EvaluationCardGridProps {
  readonly applications: ReadonlyArray<ApplicationRiskScore>;
}

/** Card grid for Martha V1 Applications (CLM-42223 / CLM-42224 / CLM-43209). */
export default function EvaluationCardGrid({ applications }: EvaluationCardGridProps): JSX.Element {
  return (
    <Flex direction="column" gap="3" data-testid="evaluation-card-grid">
      {applications.map((application) => (
        <EvaluationCard key={application.applicationId} application={application} />
      ))}
    </Flex>
  );
}
