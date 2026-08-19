/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Badge, Box, Button, Card, Flex, Heading, Inset, Table, Text } from '@radix-ui/themes';
import { SectionHeading } from '@sonatype/nexus-one-components';
import { DomainIcons, StatusIcons } from 'MainRoot/nosc/icons';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { nouxApplicationReportHref } from 'MainRoot/nosc/routing/nouxNavigation';
import {
  EVALUATIONS_PER_STAGE,
  EvaluationRow,
  StageEvaluations,
  evaluationStageLabel,
} from './evaluationsApi';

export interface EvaluationsTabProps {
  readonly publicId: string;
  readonly stages: ReadonlyArray<StageEvaluations>;
  readonly loading: boolean;
  readonly errored: boolean;
  readonly onRetry: () => void;
  /** True when reports loaded but the application has no scan yet. */
  readonly showNoScanYet?: boolean;
}

const EVALUATION_DATE = new Intl.DateTimeFormat(undefined, {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
});

function formatEvaluationDate(isoDate: string): string {
  const parsed = new Date(isoDate);
  if (Number.isNaN(parsed.getTime())) return '—';
  return EVALUATION_DATE.format(parsed);
}

/** Trigger label, marking continuous monitoring and re-evaluations since neither is a fresh scan. */
function triggerLabel(row: EvaluationRow): string {
  if (row.isForMonitoring) return 'Continuous Monitoring';
  const base = row.scanTriggerTypeDisplayName?.trim();
  if (!base) return '—';
  return row.isReevaluation ? `${base} (re-evaluation)` : base;
}

function SeverityCount({
  count,
  color,
  label,
}: {
  readonly count: number | undefined;
  readonly color: 'red' | 'orange' | 'yellow';
  readonly label: string;
}): JSX.Element {
  // Absent (historical row never recorded this severity) must not look like a verified zero.
  const display = count == null ? '—' : String(count);
  return (
    <Badge size="1" variant="soft" color={color} radius="full" title={label}>
      {display}
    </Badge>
  );
}

function StageSection({
  stage,
  publicId,
}: {
  readonly stage: StageEvaluations;
  readonly publicId: string;
}): JSX.Element {
  const label = evaluationStageLabel(stage.stageId);

  if (stage.errored) {
    return (
      <Card mt="4" data-testid="nosc-app-detail-evaluations-stage-error">
        <Flex direction="column" gap="2" p="4">
          <SectionHeading>{label}</SectionHeading>
          <Text size="2" color="red">
            Could not load evaluations for this stage.
          </Text>
        </Flex>
      </Card>
    );
  }

  if (!stage.rows.length) {
    return (
      <Card mt="4" data-testid="nosc-app-detail-evaluations-stage-empty">
        <Flex direction="column" gap="2" p="4">
          <SectionHeading>{label}</SectionHeading>
          <Text size="2" color="gray">
            No evaluation reports are available for this stage. Reports are removed once they pass
            their retention period.
          </Text>
        </Flex>
      </Card>
    );
  }

  return (
    <Card mt="4" data-testid="nosc-app-detail-evaluations-stage">
      <Flex direction="column" gap="1" p="4" pb="2">
        <SectionHeading>{label}</SectionHeading>
        <Text size="1" color="gray">
          {stage.rows.length === EVALUATIONS_PER_STAGE
            ? `Showing up to ${EVALUATIONS_PER_STAGE} most recent`
            : `${stage.rows.length} evaluation${stage.rows.length === 1 ? '' : 's'}`}
        </Text>
      </Flex>
      <Inset side="x">
        <Table.Root data-testid="nosc-app-detail-evaluations-table">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell>Evaluated</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Trigger</Table.ColumnHeaderCell>
              {/* Low is not carried by PolicyEvaluationResult, so only these three are offered. */}
              <Table.ColumnHeaderCell>Critical</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Severe</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Moderate</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Components</Table.ColumnHeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {stage.rows.map((row) => {
              const counts = row.policyEvaluationResult;
              return (
                <Table.Row key={row.scanId} data-testid="nosc-app-detail-evaluations-row">
                  <Table.Cell>
                    <a
                      href={nouxApplicationReportHref({ publicId, scanId: row.scanId })}
                      aria-label={`Open the ${label} report evaluated ${formatEvaluationDate(row.evaluationDate)}`}
                    >
                      {formatEvaluationDate(row.evaluationDate)}
                    </a>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{triggerLabel(row)}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <SeverityCount
                      count={counts?.criticalPolicyViolationCount}
                      color="red"
                      label="Critical policy violations"
                    />
                  </Table.Cell>
                  <Table.Cell>
                    <SeverityCount
                      count={counts?.severePolicyViolationCount}
                      color="orange"
                      label="Severe policy violations"
                    />
                  </Table.Cell>
                  <Table.Cell>
                    <SeverityCount
                      count={counts?.moderatePolicyViolationCount}
                      color="yellow"
                      label="Moderate policy violations"
                    />
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{counts?.totalComponentCount ?? '—'}</Text>
                  </Table.Cell>
                </Table.Row>
              );
            })}
          </Table.Body>
        </Table.Root>
      </Inset>
    </Card>
  );
}

export function EvaluationsTab({
  publicId,
  stages,
  loading,
  errored,
  onRetry,
  showNoScanYet = false,
}: EvaluationsTabProps): JSX.Element {
  if (loading) {
    return (
      <Box mt="4">
        <LoadingSkeleton height={240} data-testid="nosc-app-detail-evaluations-loading" />
      </Box>
    );
  }

  if (showNoScanYet) {
    return (
      <Card mt="4" data-testid="nosc-app-detail-evaluations-no-scan">
        <Flex direction="column" align="center" gap="3" p="6">
          <StatusIcons.Info size={32} color="var(--gray-9)" />
          <SectionHeading>No scans yet</SectionHeading>
          <Text size="2" color="gray" align="center">
            Run an IQ scan against this application to see its evaluation history here.
          </Text>
        </Flex>
      </Card>
    );
  }

  if (errored) {
    return (
      <Flex
        direction="column"
        gap="3"
        align="start"
        p="4"
        mt="4"
        data-testid="nosc-app-detail-evaluations-error"
        style={{ backgroundColor: 'var(--red-3)', borderRadius: 'var(--radius-3)' }}
      >
        <Text size="2" color="red">
          Failed to load evaluations.
        </Text>
        <Button size="2" variant="soft" onClick={onRetry} data-testid="nosc-app-detail-evaluations-retry">
          Retry
        </Button>
      </Flex>
    );
  }

  if (!stages.length) {
    return (
      <Card mt="4" data-testid="nosc-app-detail-evaluations-empty">
        <Flex direction="column" align="center" gap="3" p="6">
          <DomainIcons.Stage size={40} color="var(--gray-9)" />
          <Heading size="4">No evaluations</Heading>
          <Text size="2" color="gray" align="center">
            This application has no evaluations in any stage.
          </Text>
        </Flex>
      </Card>
    );
  }

  return (
    <Box mt="4" data-testid="nosc-app-detail-evaluations">
      {stages.map((stage) => (
        <StageSection key={stage.stageId} stage={stage} publicId={publicId} />
      ))}
    </Box>
  );
}
