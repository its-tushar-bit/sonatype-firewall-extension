/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Badge, Button, Card, Flex, Heading, Inset, Link as RadixLink, Table, Text } from '@radix-ui/themes';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';
import { useEstateComponentDetailShellContext } from './estateComponentDetailContext';
import type { EstateComponentSecurityIssue } from './estateComponentDetailsApi';

function vulnerabilityHref(reference: string | undefined, componentHash: string): string | null {
  const trimmed = reference?.trim();
  if (!trimmed) {
    return null;
  }
  return vulnerabilityDetailHref({ vulnId: trimmed, componentHash });
}

function formatSeverity(severity: number | undefined): string {
  if (typeof severity !== 'number' || !Number.isFinite(severity)) {
    return '—';
  }
  return severity.toFixed(1);
}

function VulnerabilityReference({
  issue,
  componentHash,
}: {
  readonly issue: EstateComponentSecurityIssue;
  readonly componentHash: string;
}): JSX.Element {
  const label = issue.reference?.trim() || 'Unknown';
  const href = vulnerabilityHref(issue.reference, componentHash);
  return href ? (
    <RadixLink size="2" href={href} data-testid="nosc-estate-component-vulnerabilities-reference-link">
      {label}
    </RadixLink>
  ) : (
    <Text size="2">{label}</Text>
  );
}

export function EstateComponentVulnerabilitiesTab(): JSX.Element {
  const { componentHash, hdsStatus, details, retryHds } = useEstateComponentDetailShellContext();

  if (hdsStatus === 'loading') {
    return <LoadingSkeleton height={180} data-testid="nosc-estate-component-vulnerabilities-loading" />;
  }

  if (hdsStatus === 'error') {
    return (
      <Flex direction="column" gap="3" align="start" mt="4" data-testid="nosc-estate-component-vulnerabilities-error">
        <Text size="2" color="red">
          Vulnerability details are temporarily unavailable. Policy Violations and Applications remain available.
        </Text>
        <Button size="2" variant="soft" onClick={retryHds} data-testid="nosc-estate-component-vulnerabilities-retry">
          Retry
        </Button>
      </Flex>
    );
  }

  const securityIssues = details?.securityIssues ?? [];

  if (hdsStatus === 'empty' || !details || securityIssues.length === 0) {
    return (
      <Flex direction="column" gap="2" mt="4" data-testid="nosc-estate-component-vulnerabilities-empty">
        <Text size="2" color="gray">
          No vulnerabilities were reported for this component.
        </Text>
      </Flex>
    );
  }

  return (
    <Flex direction="column" gap="3" mt="4" data-testid="nosc-estate-component-vulnerabilities">
      <Flex direction="column" gap="1">
        <Heading as="h2" size="4">
          Vulnerabilities
        </Heading>
        <Text size="2" color="gray">
          Security issues reported by the component catalog for this hash.
        </Text>
      </Flex>
      <Card>
        <Inset>
          <Table.Root data-testid="nosc-estate-component-vulnerabilities-table">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Reference</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Severity</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Threat category</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {securityIssues.map((issue, idx) => (
                <Table.Row
                  key={issue.reference || `issue-${idx}`}
                  data-testid="nosc-estate-component-vulnerabilities-row"
                >
                  <Table.Cell>
                    <VulnerabilityReference issue={issue} componentHash={componentHash} />
                  </Table.Cell>
                  <Table.Cell>
                    <Badge size="1" variant="soft" color="gray">
                      {formatSeverity(issue.severity)}
                    </Badge>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{issue.threatCategory || '—'}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{issue.status || '—'}</Text>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        </Inset>
      </Card>
    </Flex>
  );
}
