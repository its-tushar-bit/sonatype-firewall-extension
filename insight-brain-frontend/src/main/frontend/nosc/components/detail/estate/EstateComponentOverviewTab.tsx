/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Badge, Button, Card, Flex, Grid, Heading, Link as RadixLink, Text } from '@radix-ui/themes';
import { estateComponentDetailHref } from 'MainRoot/nosc/components/detail/estateComponentDetailHref';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { useEstateComponentDetailShellContext } from './estateComponentDetailContext';
import type { EstateComponentSecurityIssue } from './estateComponentDetailsApi';

type ThreatBand = 'critical' | 'severe' | 'moderate' | 'low';

const THREAT_BAND_LABELS: Record<ThreatBand, string> = {
  critical: 'Critical',
  severe: 'Severe',
  moderate: 'Moderate',
  low: 'Low',
};

const THREAT_BAND_COLORS: Record<ThreatBand, 'red' | 'orange' | 'amber' | 'gray'> = {
  critical: 'red',
  severe: 'orange',
  moderate: 'amber',
  low: 'gray',
};

function licenseSummaryLabel(
  licenses: ReadonlyArray<{ licenseId?: string; licenseName?: string }> | undefined
): string {
  if (!licenses?.length) {
    return '—';
  }
  return licenses
    .map((l) => l.licenseName || l.licenseId)
    .filter(Boolean)
    .join(', ');
}

function countLabel(value: number | undefined, singular: string, plural: string): string {
  if (typeof value !== 'number') {
    return '—';
  }
  return `${value.toLocaleString()} ${value === 1 ? singular : plural}`;
}

function threatBandForIssue(issue: EstateComponentSecurityIssue): ThreatBand {
  const category = issue.threatCategory?.trim().toUpperCase();
  if (category === 'CRITICAL') {
    return 'critical';
  }
  if (category === 'SEVERE' || category === 'HIGH') {
    return 'severe';
  }
  if (category === 'MODERATE' || category === 'MEDIUM') {
    return 'moderate';
  }
  if (category === 'LOW') {
    return 'low';
  }
  if (typeof issue.severity === 'number' && Number.isFinite(issue.severity)) {
    if (issue.severity >= 9) {
      return 'critical';
    }
    if (issue.severity >= 7) {
      return 'severe';
    }
    if (issue.severity >= 4) {
      return 'moderate';
    }
  }
  return 'low';
}

function aggregateThreatCounts(
  issues: ReadonlyArray<EstateComponentSecurityIssue>
): Record<ThreatBand, number> {
  return issues.reduce<Record<ThreatBand, number>>(
    (counts, issue) => {
      const band = threatBandForIssue(issue);
      counts[band] += 1;
      return counts;
    },
    { critical: 0, severe: 0, moderate: 0, low: 0 }
  );
}

function highestCvss(issues: ReadonlyArray<EstateComponentSecurityIssue>): number | null {
  let max: number | null = null;
  for (const issue of issues) {
    if (typeof issue.severity === 'number' && Number.isFinite(issue.severity)) {
      max = max == null ? issue.severity : Math.max(max, issue.severity);
    }
  }
  return max;
}

function coordinateValue(
  coordinates: Record<string, unknown> | undefined,
  key: string
): string | undefined {
  const value = coordinates?.[key];
  return typeof value === 'string' && value.trim() ? value : undefined;
}

function formatStageLabel(stageTypeId: string | undefined): string | undefined {
  if (!stageTypeId?.trim()) {
    return undefined;
  }
  return stageTypeId
    .split('-')
    .map((part) => (part ? part[0].toUpperCase() + part.slice(1).toLowerCase() : part))
    .join('-');
}

function ThreatBandGrid({
  counts,
  testIdPrefix,
}: {
  readonly counts: Record<ThreatBand, number>;
  readonly testIdPrefix: string;
}): JSX.Element {
  return (
    <Grid columns="4" gap="1" align="center" data-testid={`${testIdPrefix}-threat-grid`}>
      {(Object.keys(THREAT_BAND_LABELS) as ThreatBand[]).map((band) => (
        <Flex key={band} direction="column" gap="1" align="start">
          <Badge color={THREAT_BAND_COLORS[band]} variant="soft" size="1">
            {THREAT_BAND_LABELS[band]}
          </Badge>
          <Text size="3" weight="medium" data-testid={`${testIdPrefix}-threat-${band}`}>
            {counts[band]}
          </Text>
        </Flex>
      ))}
    </Grid>
  );
}

export function EstateComponentOverviewTab(): JSX.Element {
  const { componentHash, hdsStatus, details, blastRadiusCounts, pathSelection, retryHds } =
    useEstateComponentDetailShellContext();
  const pathContext = {
    organizationId: pathSelection.organizationId,
    applicationId: pathSelection.applicationId,
    reportId: pathSelection.reportId,
  };

  if (hdsStatus === 'loading') {
    return <LoadingSkeleton height={180} data-testid="nosc-estate-component-overview-loading" />;
  }

  if (hdsStatus === 'error') {
    return (
      <Flex direction="column" gap="3" align="start" mt="4" data-testid="nosc-estate-component-overview-error">
        <Text size="2" color="red">
          Component details are temporarily unavailable. Other tabs still work.
        </Text>
        <Button size="2" variant="soft" onClick={retryHds} data-testid="nosc-estate-component-overview-retry">
          Retry
        </Button>
      </Flex>
    );
  }

  if (hdsStatus === 'empty' || !details) {
    return (
      <Flex direction="column" gap="2" mt="4" data-testid="nosc-estate-component-overview-empty">
        <Text size="2" color="gray">
          No catalog details were found for this component hash.
        </Text>
        <Text size="2" style={{ fontFamily: 'var(--code-font-family)' }}>
          {componentHash}
        </Text>
      </Flex>
    );
  }

  const securityIssues = details.securityIssues ?? [];
  const threatCounts = aggregateThreatCounts(securityIssues);
  const maxCvss = highestCvss(securityIssues);
  const effective = licenseSummaryLabel(details.licenseData?.effectiveLicenses);
  const coordinates = details.componentIdentifier?.coordinates;
  const version = coordinateValue(coordinates, 'version');
  const artifactId = coordinateValue(coordinates, 'artifactId') || coordinateValue(coordinates, 'name');
  const scanCaption =
    pathSelection.reportLabel ||
    formatStageLabel(pathSelection.stageTypeId) ||
    (pathSelection.reportId ? pathSelection.reportId : undefined);

  return (
    <Flex direction="column" gap="4" mt="4" data-testid="nosc-estate-component-overview">
      <Grid columns={{ initial: '1', md: '1fr 1fr 1fr' }} gap="4">
        <Card data-testid="nosc-estate-component-overview-violations-card">
          <Flex direction="column" gap="3" p="4">
            <Flex align="baseline" gap="2" justify="between" wrap="wrap">
              <Heading size="4">Violations</Heading>
              <Text size="2" color="gray" data-testid="nosc-estate-component-overview-violations-count">
                {countLabel(blastRadiusCounts.violations, 'violation', 'violations')}
              </Text>
            </Flex>
            <Text size="2" color="gray">
              Estate policy violations for this component hash.
            </Text>
            <RadixLink
              size="2"
              href={estateComponentDetailHref(componentHash, 'violations', pathContext)}
              data-testid="nosc-estate-component-overview-violations-link"
            >
              View violations →
            </RadixLink>
          </Flex>
        </Card>

        <Card data-testid="nosc-estate-component-overview-vulnerabilities-card">
          <Flex direction="column" gap="3" p="4">
            <Flex align="baseline" gap="2" justify="between" wrap="wrap">
              <Heading size="4">Vulnerabilities</Heading>
              <RadixLink
                size="2"
                href={estateComponentDetailHref(componentHash, 'vulnerabilities', pathContext)}
                data-testid="nosc-estate-component-overview-vulnerabilities-link"
              >
                View all →
              </RadixLink>
            </Flex>
            {securityIssues.length === 0 ? (
              <Text size="2" color="gray">
                No security issues reported for this component.
              </Text>
            ) : (
              <>
                <ThreatBandGrid
                  counts={threatCounts}
                  testIdPrefix="nosc-estate-component-overview"
                />
                <Text size="2">
                  Highest CVSS:{' '}
                  <Text weight="medium" data-testid="nosc-estate-component-overview-highest-cvss">
                    {maxCvss != null ? maxCvss.toFixed(1) : '—'}
                  </Text>
                </Text>
              </>
            )}
          </Flex>
        </Card>

        <Card data-testid="nosc-estate-component-overview-license-card">
          <Flex direction="column" gap="3" p="4">
            <Heading size="4">License</Heading>
            <Text size="2" data-testid="nosc-estate-component-overview-license-effective">
              Effective: {effective}
            </Text>
            <Text size="2" data-testid="nosc-estate-component-overview-license-status">
              Status: {details.licenseData?.status || '—'}
            </Text>
          </Flex>
        </Card>
      </Grid>

      <Card data-testid="nosc-estate-component-overview-estate-usage">
        <Flex direction="column" gap="3" p="4">
          <Heading size="4">Estate Usage</Heading>
          <Grid columns={{ initial: '1', sm: '2' }} gap="4">
            <Flex direction="column" gap="1">
              <Text size="6" weight="bold" style={{ lineHeight: 1 }} data-testid="nosc-estate-component-overview-applications-count">
                {typeof blastRadiusCounts.applications === 'number'
                  ? blastRadiusCounts.applications.toLocaleString()
                  : '—'}
              </Text>
              <Text size="2" color="gray">
                {blastRadiusCounts.applications === 1 ? 'Application affected' : 'Applications affected'}
              </Text>
            </Flex>
            <Flex direction="column" gap="1">
              <Text size="6" weight="bold" style={{ lineHeight: 1 }} data-testid="nosc-estate-component-overview-organizations-count">
                {typeof blastRadiusCounts.organizations === 'number'
                  ? blastRadiusCounts.organizations.toLocaleString()
                  : '—'}
              </Text>
              <Text size="2" color="gray">
                {blastRadiusCounts.organizations === 1 ? 'Organization affected' : 'Organizations affected'}
              </Text>
            </Flex>
          </Grid>
          <RadixLink
            size="2"
            href={estateComponentDetailHref(componentHash, 'applications', pathContext)}
            data-testid="nosc-estate-component-overview-applications-link"
          >
            View applications →
          </RadixLink>
        </Flex>
      </Card>

      <Card data-testid="nosc-estate-component-overview-identity">
        <Grid columns={{ initial: '1', md: '1fr 1fr' }} gap="4" p="4">
          <Flex direction="column" gap="3">
            <Heading size="4">Component Details</Heading>
            {scanCaption && (
              <Text size="1" color="gray" data-testid="nosc-estate-component-overview-scan-caption">
                From scan: {scanCaption}
              </Text>
            )}
            <Text size="2" data-testid="nosc-estate-component-overview-match-state">
              Match State: {details.matchState || '—'}
            </Text>
            <Text size="2" data-testid="nosc-estate-component-overview-identification-source">
              Identification Source: —
            </Text>
            <Text size="2" data-testid="nosc-estate-component-overview-occurrences">
              Occurrences: —
            </Text>
            <Text size="2" data-testid="nosc-estate-component-overview-dependency-type">
              Dependency Type: —
            </Text>
            <Text size="2">Display name: {details.displayName || '—'}</Text>
            <Text size="2">
              Hash:{' '}
              <Text style={{ fontFamily: 'var(--code-font-family)' }}>{details.hash || componentHash}</Text>
            </Text>
          </Flex>
          <Flex direction="column" gap="3">
            <Heading size="4">Coordinates</Heading>
            <Text size="2">Type: {details.format || '—'}</Text>
            <Text size="2">ID: {artifactId || details.displayName || '—'}</Text>
            <Text size="2">Version: {version || '—'}</Text>
            <Text size="2" data-testid="nosc-estate-component-overview-package-url">
              Package URL: {details.packageUrl || '—'}
            </Text>
          </Flex>
        </Grid>
      </Card>
    </Flex>
  );
}
