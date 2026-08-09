/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Button, Card, Flex, Link as RadixLink, Text } from '@radix-ui/themes';
import { estateComponentDetailHref } from 'MainRoot/nosc/components/detail/estateComponentDetailHref';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { useEstateComponentDetailShellContext } from './estateComponentDetailContext';

/** Overview preview only — full list lives on the Vulnerabilities tab. */
const SECURITY_ISSUES_PREVIEW_LIMIT = 5;

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
  return `${value} ${value === 1 ? singular : plural}`;
}

export function EstateComponentOverviewTab(): JSX.Element {
  const { componentHash, hdsStatus, details, blastRadiusCounts, retryHds } = useEstateComponentDetailShellContext();

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
  const previewIssues = securityIssues.slice(0, SECURITY_ISSUES_PREVIEW_LIMIT);
  const overflowCount = Math.max(0, securityIssues.length - previewIssues.length);
  const effective = licenseSummaryLabel(details.licenseData?.effectiveLicenses);

  return (
    <Flex direction="column" gap="4" mt="4" data-testid="nosc-estate-component-overview">
      <Card>
        <Flex direction="column" gap="3" p="4">
          <Text size="3" weight="medium">
            Identity
          </Text>
          <Text size="2">Display name: {details.displayName || '—'}</Text>
          <Text size="2">Package URL: {details.packageUrl || '—'}</Text>
          <Text size="2">Format: {details.format || '—'}</Text>
          <Text size="2">
            Hash: <Text style={{ fontFamily: 'var(--code-font-family)' }}>{details.hash || componentHash}</Text>
          </Text>
          {details.matchState && <Text size="2">Match state: {details.matchState}</Text>}
        </Flex>
      </Card>

      <Card>
        <Flex direction="column" gap="3" p="4">
          <Text size="3" weight="medium">
            License summary
          </Text>
          <Text size="2">Effective: {effective}</Text>
          <Text size="2">Status: {details.licenseData?.status || '—'}</Text>
        </Flex>
      </Card>

      <Card>
        <Flex direction="column" gap="3" p="4">
          <Text size="3" weight="medium">
            Estate reach
          </Text>
          <Flex gap="4" wrap="wrap">
            <Text size="2" data-testid="nosc-estate-component-overview-applications-count">
              Applications: {countLabel(blastRadiusCounts.applications, 'application', 'applications')}
            </Text>
            <Text size="2" data-testid="nosc-estate-component-overview-organizations-count">
              Organizations: {countLabel(blastRadiusCounts.organizations, 'organization', 'organizations')}
            </Text>
            <Text size="2" data-testid="nosc-estate-component-overview-violations-count">
              Policy violations: {countLabel(blastRadiusCounts.violations, 'violation', 'violations')}
            </Text>
          </Flex>
        </Flex>
      </Card>

      <Card>
        <Flex direction="column" gap="3" p="4">
          <Text size="3" weight="medium">
            Vulnerability summary
          </Text>
          {securityIssues.length === 0 ? (
            <Text size="2" color="gray">
              No security issues reported for this component.
            </Text>
          ) : (
            <>
              {previewIssues.map((issue, idx) => (
                <Text key={issue.reference ?? idx} size="2">
                  {issue.reference || 'Unknown'}
                  {typeof issue.severity === 'number' ? ` (severity ${issue.severity})` : ''}
                </Text>
              ))}
              {overflowCount > 0 && (
                <>
                  <Text size="2" color="gray" data-testid="nosc-estate-component-overview-security-overflow">
                    …and {overflowCount} more — see{' '}
                    <RadixLink
                      size="2"
                      href={estateComponentDetailHref(componentHash, 'vulnerabilities')}
                      data-testid="nosc-estate-component-overview-security-overflow-link"
                    >
                      Vulnerabilities
                    </RadixLink>
                  </Text>
                  <RadixLink
                    size="2"
                    href={estateComponentDetailHref(componentHash, 'vulnerabilities')}
                    data-testid="nosc-estate-component-overview-vulnerabilities-link"
                  >
                    View all vulnerabilities →
                  </RadixLink>
                </>
              )}
            </>
          )}
        </Flex>
      </Card>

      <Flex gap="4" wrap="wrap">
        <RadixLink
          size="2"
          href={estateComponentDetailHref(componentHash, 'violations')}
          data-testid="nosc-estate-component-overview-violations-link"
        >
          View policy violations →
        </RadixLink>
        <RadixLink
          size="2"
          href={estateComponentDetailHref(componentHash, 'applications')}
          data-testid="nosc-estate-component-overview-applications-link"
        >
          View applications →
        </RadixLink>
        <RadixLink
          size="2"
          href={estateComponentDetailHref(componentHash, 'organizations')}
          data-testid="nosc-estate-component-overview-organizations-link"
        >
          View organizations →
        </RadixLink>
      </Flex>
    </Flex>
  );
}
