/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Card, Flex, Grid, Text } from '@radix-ui/themes';
import PreviewDashboardApplicationsAppNameLink from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardApplicationsAppNameLink';
import {
  ApplicationRiskScore,
  ApplicationStageRisk,
} from 'MainRoot/nosc/applications/applicationListTypes';
import { ApplicationSeverityBadge } from 'MainRoot/nosc/dashboard/tabs/ApplicationSeverityBadge';

function formatEvaluationDate(isoDate?: string): string {
  if (!isoDate) return '—';
  const parsed = new Date(isoDate);
  if (Number.isNaN(parsed.getTime())) return '—';
  return parsed.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
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

function StageTile({
  publicId,
  stage,
}: {
  readonly publicId: string;
  readonly stage: ApplicationStageRisk;
}): JSX.Element {
  // TODO(CLM-42224): wire stage tile click to nexusOneApplicationReportHref(publicId, scanId).
  void publicId;
  void stage.scanId;

  return (
    <Box
      p="2"
      role="group"
      aria-label={`${stage.stageTypeName} stage evaluation`}
      data-testid="evaluation-card-stage-tile"
      style={{
        border: '1px solid var(--gray-6)',
        borderRadius: 'var(--radius-2)',
        backgroundColor: 'var(--gray-2)',
      }}
    >
      <Flex direction="column" gap="2" align="start">
        <Text size="1" weight="medium">
          {stage.stageTypeName}
        </Text>
        <Text size="1" color="gray">
          {formatEvaluationDate(stage.evaluationDate)}
        </Text>
        <Flex gap="1" wrap="wrap" aria-label="Policy violations by severity">
          <ApplicationSeverityBadge value={stage.risk.criticalRisk} severity="critical" />
          <ApplicationSeverityBadge value={stage.risk.severeRisk} severity="severe" />
          <ApplicationSeverityBadge value={stage.risk.moderateRisk} severity="moderate" />
          <ApplicationSeverityBadge value={stage.risk.lowRisk} severity="low" />
        </Flex>
      </Flex>
    </Box>
  );
}

function EvaluationCard({ application }: { readonly application: ApplicationRiskScore }): JSX.Element {
  const lastEvaluation = latestEvaluationDate(application.stageRisks);
  const { totalApplicationRisk } = application;

  return (
    <Card data-testid="evaluation-card">
      <Flex direction="column" gap="3">
        <Flex direction="column" gap="1">
          <PreviewDashboardApplicationsAppNameLink
            publicId={application.applicationId}
            name={application.applicationName}
          />
          <Text size="2" color="gray">
            {application.organizationName}
          </Text>
          <Text size="1" color="gray" data-testid="evaluation-card-last-evaluation">
            Last evaluation: {formatEvaluationDate(lastEvaluation)}
          </Text>
        </Flex>

        <Flex gap="2" wrap="wrap" align="center" aria-label="Total application risk by severity">
          <Text size="1" color="gray">
            Total risk
          </Text>
          <ApplicationSeverityBadge value={totalApplicationRisk.totalRisk} severity="total" />
          <ApplicationSeverityBadge value={totalApplicationRisk.criticalRisk} severity="critical" />
          <ApplicationSeverityBadge value={totalApplicationRisk.severeRisk} severity="severe" />
          <ApplicationSeverityBadge value={totalApplicationRisk.moderateRisk} severity="moderate" />
          <ApplicationSeverityBadge value={totalApplicationRisk.lowRisk} severity="low" />
        </Flex>

        {application.stageRisks.length > 0 && (
          <Grid columns={{ initial: '1', sm: '2', md: '4' }} gap="2">
            {application.stageRisks.map((stage) => (
              <StageTile key={stage.scanId} publicId={application.applicationId} stage={stage} />
            ))}
          </Grid>
        )}
      </Flex>
    </Card>
  );
}

export interface EvaluationCardGridProps {
  readonly applications: ReadonlyArray<ApplicationRiskScore>;
}

/** Card grid for Martha V1 Applications (CLM-42223 / CLM-42224). */
export default function EvaluationCardGrid({ applications }: EvaluationCardGridProps): JSX.Element {
  return (
    <Flex direction="column" gap="3" data-testid="evaluation-card-grid">
      {applications.map((application) => (
        <EvaluationCard key={application.applicationId} application={application} />
      ))}
    </Flex>
  );
}
