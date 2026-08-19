/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Badge, Box, Button, Card, DataList, Flex, Grid, Text } from '@radix-ui/themes';
import { SectionHeading } from '@sonatype/nexus-one-components';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { ButtonLink } from 'MainRoot/nosc/components/ButtonLink';
import {
  nouxApplicationPoliciesHref,
  nouxApplicationReportHref,
  nouxApplicationSourceControlHref,
  nouxApplicationWaiversHref,
  nouxReportsHref,
} from 'MainRoot/nosc/routing/nouxNavigation';

import { ApiApplicationReport, ApplicationDTO } from './applicationDetailTypes';
import './OverviewTab.scss';

type FetchStatus = 'idle' | 'loading' | 'ready' | 'error';

/**
 * Everything the Overview grid needs from {@link ApplicationDetail}. The parent
 * owns all data fetching (CLM-39709) — OverviewTab is a pure presentational
 * component that receives the already-computed values + retry callbacks as
 * props. Extracted out of ApplicationDetail per review comment #12.
 */
export interface OverviewTabProps {
  /** Application metadata (name/publicId/id/organizationName) from
   *  `GET /rest/application/{publicId}`. Null until the request resolves. */
  readonly appData: ApplicationDTO | null;
  /** The route's publicId, used as a fallback when {@link appData} is null. */
  readonly publicId: string;
  /** True while any of the application/reports/policy requests are in flight. */
  readonly overviewIsLoading: boolean;
  /** State of the policythreats.json fetch (drives the policy + risk cards). */
  readonly policyStatus: FetchStatus;
  /** State of the per-stage reports fetch (drives the scan-info card). */
  readonly reportsStatus: FetchStatus;
  /** Parsed scanId of the latest report, or null if never scanned. */
  readonly scanId: string | null;
  /** Most recent report across all stages, or null. */
  readonly latestReport: ApiApplicationReport | null;
  /** All per-stage reports, or null before they load. */
  readonly reports: ReadonlyArray<ApiApplicationReport> | null;
  readonly totalViolations: number;
  readonly openViolations: number;
  readonly waivedViolations: number;
  readonly criticalCount: number;
  readonly severeCount: number;
  readonly moderateCount: number;
  readonly componentCount: number;
  readonly maliciousCount: number;
  /** Re-attempt the policythreats.json fetch. */
  readonly onRetryPolicy: () => void;
  /** Re-attempt the reports fetch. */
  readonly onRetryReports: () => void;
}

const POLICY_COMPLIANCE_HEADING_ID = 'nosc-app-detail-policy-compliance-heading';
const RISK_METRICS_HEADING_ID = 'nosc-app-detail-risk-metrics-heading';
const SCAN_INFO_HEADING_ID = 'nosc-app-detail-scan-info-heading';
const APP_DETAILS_HEADING_ID = 'nosc-app-detail-app-details-heading';
const QUICK_ACTIONS_HEADING_ID = 'nosc-app-detail-quick-actions-heading';

export function OverviewTab(props: OverviewTabProps): JSX.Element {
  const {
    appData,
    publicId,
    overviewIsLoading,
    policyStatus,
    reportsStatus,
    scanId,
    latestReport,
    reports,
    totalViolations,
    openViolations,
    waivedViolations,
    criticalCount,
    severeCount,
    moderateCount,
    componentCount,
    maliciousCount,
    onRetryPolicy,
    onRetryReports,
  } = props;

  return (
    <Box mt="4" style={{ overflow: 'hidden' }}>
      <Grid columns={{ initial: '1', lg: '2fr 1fr' }} gap="4" style={{ minWidth: 0 }}>
        <Flex direction="column" gap="4" minWidth="0">
          {/* Policy Compliance */}
          <Card asChild data-testid="nosc-app-detail-policy-compliance-card">
            <section aria-labelledby={POLICY_COMPLIANCE_HEADING_ID}>
              <Box p="3">
                <Flex align="center" gap="2" mb="3">
                  <DomainIcons.Policies size={18} color="var(--accent-9)" />
                  <SectionHeading id={POLICY_COMPLIANCE_HEADING_ID}>Policy Compliance</SectionHeading>
                </Flex>
                {overviewIsLoading && policyStatus !== 'ready' ? (
                  <LoadingSkeleton height={120} data-testid="nosc-app-detail-policy-compliance-loading" />
                ) : policyStatus === 'error' ? (
                  <Flex direction="column" gap="2" align="start">
                    <Text size="2" color="red">
                      Could not load policy data.
                    </Text>
                    <Button size="1" variant="soft" onClick={onRetryPolicy}>
                      Retry
                    </Button>
                  </Flex>
                ) : !scanId ? (
                  <Text size="2" color="gray">
                    This application has not been scanned yet.
                  </Text>
                ) : (
                  <Flex direction="column" gap="3">
                    <Box>
                      <Text size="2" color="gray" mb="1" as="div">
                        Total Violations
                      </Text>
                      <Text size="6" weight="bold" color={openViolations > 0 ? 'orange' : 'green'}>
                        {totalViolations}
                      </Text>
                    </Box>
                    <Box>
                      <Text size="2" color="gray" mb="1" as="div">
                        Open vs Waived
                      </Text>
                      <Flex align="center" gap="3">
                        <Flex align="center" gap="2">
                          <Badge size="1" color="orange" variant="solid" radius="full">
                            Open
                          </Badge>
                          <Text size="2">{openViolations}</Text>
                        </Flex>
                        <Flex align="center" gap="2">
                          <Badge size="1" color="blue" variant="solid" radius="full">
                            Waived
                          </Badge>
                          <Text size="2">{waivedViolations}</Text>
                        </Flex>
                      </Flex>
                    </Box>
                    <Box>
                      <Text size="2" color="gray" mb="1" as="div">
                        Open Violations by Severity
                      </Text>
                      <Flex direction="column" gap="1">
                        <Flex align="center" gap="2">
                          <Badge size="1" color="red" variant="solid" radius="full">
                            Critical
                          </Badge>
                          <Text size="2">{criticalCount}</Text>
                        </Flex>
                        <Flex align="center" gap="2">
                          <Badge size="1" color="orange" variant="solid" radius="full">
                            Severe
                          </Badge>
                          <Text size="2">{severeCount}</Text>
                        </Flex>
                        <Flex align="center" gap="2">
                          <Badge size="1" color="yellow" variant="solid" radius="full">
                            Moderate
                          </Badge>
                          <Text size="2">{moderateCount}</Text>
                        </Flex>
                      </Flex>
                    </Box>
                  </Flex>
                )}
              </Box>
            </section>
          </Card>

          {/* Risk & Trust Metrics */}
          <Card asChild data-testid="nosc-app-detail-risk-metrics-card">
            <section aria-labelledby={RISK_METRICS_HEADING_ID}>
              <Box p="3">
                <Flex align="center" gap="2" mb="3">
                  <DomainIcons.Vulnerability size={18} color="var(--red-9)" />
                  <SectionHeading id={RISK_METRICS_HEADING_ID}>Risk &amp; Trust Metrics</SectionHeading>
                </Flex>
                {policyStatus === 'ready' ? (
                  <DataList.Root>
                    <DataList.Item>
                      <DataList.Label>Critical Open Violations</DataList.Label>
                      <DataList.Value>
                        <Flex align="center" gap="2">
                          <DomainIcons.Vulnerability size={16} color="var(--red-9)" />
                          <Text size="6" weight="bold" color="red">
                            {criticalCount}
                          </Text>
                        </Flex>
                      </DataList.Value>
                    </DataList.Item>
                    <DataList.Item>
                      <DataList.Label>Malicious Component Indicators</DataList.Label>
                      <DataList.Value>
                        <Badge
                          color={maliciousCount > 0 ? 'red' : 'green'}
                          variant="soft"
                          size="1"
                          data-testid="nosc-app-detail-risk-malicious-badge"
                        >
                          {maliciousCount > 0 ? `${maliciousCount} detected` : 'Clean'}
                        </Badge>
                      </DataList.Value>
                    </DataList.Item>
                    <DataList.Item>
                      <DataList.Label>Components Scanned</DataList.Label>
                      <DataList.Value>
                        <Text size="3" weight="bold">
                          {componentCount}
                        </Text>
                      </DataList.Value>
                    </DataList.Item>
                  </DataList.Root>
                ) : policyStatus === 'error' ? (
                  <Text size="2" color="red">
                    Could not load risk metrics.
                  </Text>
                ) : !scanId ? (
                  <Text size="2" color="gray">
                    Risk metrics appear after the first scan.
                  </Text>
                ) : (
                  <LoadingSkeleton height={120} />
                )}
              </Box>
            </section>
          </Card>

          {/* Scan Information */}
          <Card asChild data-testid="nosc-app-detail-scan-info-card">
            <section aria-labelledby={SCAN_INFO_HEADING_ID}>
              <Box p="3">
                <Flex align="center" gap="2" mb="3">
                  <DomainIcons.Clock size={18} color="var(--accent-9)" />
                  <SectionHeading id={SCAN_INFO_HEADING_ID}>Scan Information</SectionHeading>
                </Flex>
                {reportsStatus === 'loading' ? (
                  <LoadingSkeleton height={80} />
                ) : reportsStatus === 'error' ? (
                  <Flex direction="column" gap="2" align="start">
                    <Text size="2" color="red">
                      Could not load scan history.
                    </Text>
                    <Button size="1" variant="soft" onClick={onRetryReports}>
                      Retry
                    </Button>
                  </Flex>
                ) : !latestReport ? (
                  <Text size="2" color="gray">
                    No scans on record yet.
                  </Text>
                ) : (
                  <Flex direction="column" gap="3">
                    <DataList.Root>
                      <DataList.Item>
                        <DataList.Label>Last Scan</DataList.Label>
                        <DataList.Value>
                          <Flex align="center" gap="2">
                            <DomainIcons.Calendar size={14} color="var(--gray-10)" />
                            <Text size="3" weight="bold">
                              {new Date(latestReport.evaluationDate).toLocaleString()}
                            </Text>
                          </Flex>
                        </DataList.Value>
                      </DataList.Item>
                      <DataList.Item>
                        <DataList.Label>Last Scan Stage</DataList.Label>
                        <DataList.Value>
                          <Badge size="1" variant="soft" color="gray">
                            {latestReport.stage}
                          </Badge>
                        </DataList.Value>
                      </DataList.Item>
                      <DataList.Item>
                        <DataList.Label>Stages Reporting</DataList.Label>
                        <DataList.Value>
                          <Flex align="center" gap="1" wrap="wrap">
                            {(reports ?? []).map((r) => (
                              <Badge key={r.stage} size="1" variant="surface" color="gray">
                                {r.stage}
                              </Badge>
                            ))}
                          </Flex>
                        </DataList.Value>
                      </DataList.Item>
                    </DataList.Root>
                    {scanId && (
                      <Box>
                        <ButtonLink
                          href={nouxApplicationReportHref({ publicId, scanId })}
                          size="2"
                          variant="soft"
                          color="blue"
                          data-testid="nosc-app-detail-view-full-report"
                        >
                          View full report
                        </ButtonLink>
                      </Box>
                    )}
                  </Flex>
                )}
              </Box>
            </section>
          </Card>
        </Flex>

        <Flex direction="column" gap="4">
          {/* Application Details */}
          <Card asChild data-testid="nosc-app-detail-app-details-card">
            <section aria-labelledby={APP_DETAILS_HEADING_ID}>
              <Box p="3">
                <SectionHeading id={APP_DETAILS_HEADING_ID} mb="3">
                  Application Details
                </SectionHeading>
                <DataList.Root>
                  <DataList.Item>
                    <DataList.Label>Name</DataList.Label>
                    <DataList.Value>
                      <Text size="2" weight="medium">
                        {appData?.name ?? '—'}
                      </Text>
                    </DataList.Value>
                  </DataList.Item>
                  <DataList.Item>
                    <DataList.Label>Public ID</DataList.Label>
                    <DataList.Value>
                      <Text size="2" weight="medium" style={{ fontFamily: 'var(--code-font-family)' }}>
                        {appData?.publicId ?? publicId}
                      </Text>
                    </DataList.Value>
                  </DataList.Item>
                  <DataList.Item>
                    <DataList.Label>Internal ID</DataList.Label>
                    <DataList.Value>
                      <Text size="2" style={{ fontFamily: 'var(--code-font-family)' }}>
                        {appData?.id ?? '—'}
                      </Text>
                    </DataList.Value>
                  </DataList.Item>
                  <DataList.Item>
                    <DataList.Label>Organization</DataList.Label>
                    <DataList.Value>
                      <Text size="2" weight="medium">
                        {appData?.organizationName ?? '—'}
                      </Text>
                    </DataList.Value>
                  </DataList.Item>
                  <DataList.Item>
                    <DataList.Label>Stages on file</DataList.Label>
                    <DataList.Value>
                      <Text size="2">{reports ? reports.length : '—'}</Text>
                    </DataList.Value>
                  </DataList.Item>
                </DataList.Root>
              </Box>
            </section>
          </Card>

          {/* Quick Actions */}
          <Card asChild data-testid="nosc-app-detail-quick-actions-card">
            <section aria-labelledby={QUICK_ACTIONS_HEADING_ID}>
              <Box p="3">
                <SectionHeading id={QUICK_ACTIONS_HEADING_ID} mb="3">
                  Quick Actions
                </SectionHeading>
                <Flex direction="column" gap="2">
                  <ButtonLink
                    href={nouxApplicationPoliciesHref(publicId)}
                    size="2"
                    variant="soft"
                    color="gray"
                    data-testid="nosc-app-detail-quick-action-policies"
                  >
                    <Flex align="center" gap="2">
                      <DomainIcons.Policies size={14} />
                      Configure Policies
                    </Flex>
                  </ButtonLink>
                  <ButtonLink
                    href={nouxApplicationWaiversHref(publicId)}
                    size="2"
                    variant="soft"
                    color="gray"
                    data-testid="nosc-app-detail-quick-action-waivers"
                  >
                    <Flex align="center" gap="2">
                      <DomainIcons.Waivers size={14} />
                      Manage Waivers
                    </Flex>
                  </ButtonLink>
                  <ButtonLink
                    href={nouxApplicationSourceControlHref(publicId)}
                    size="2"
                    variant="soft"
                    color="gray"
                    data-testid="nosc-app-detail-quick-action-source-control"
                  >
                    <Flex align="center" gap="2">
                      <DomainIcons.SourceControl size={14} />
                      Source Control
                    </Flex>
                  </ButtonLink>
                  <ButtonLink
                    href={nouxReportsHref()}
                    size="2"
                    variant="soft"
                    color="gray"
                    data-testid="nosc-app-detail-quick-action-reports"
                  >
                    <Flex align="center" gap="2">
                      <DomainIcons.Reports size={14} />
                      Enterprise Reporting
                    </Flex>
                  </ButtonLink>
                </Flex>
              </Box>
            </section>
          </Card>
        </Flex>
      </Grid>
    </Box>
  );
}
