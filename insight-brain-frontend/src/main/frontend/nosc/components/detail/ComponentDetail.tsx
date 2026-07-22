/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useState, type ReactElement } from 'react';
import axios from 'axios';
import { Badge, Button, Card, Flex, Link as RadixLink, Text } from '@radix-ui/themes';
import { PageHeading } from '@sonatype/nexus-one-components';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { EntityDetailLayout } from 'MainRoot/nosc/entityDetail/EntityDetailLayout';
import { resolveEntityDetailContext } from 'MainRoot/nosc/entityDetail/resolveEntityDetailContext';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { classicReportHrefForComponent } from 'MainRoot/nosc/applications/applicationDetailUtils';
import type { RawReportComponent, RawReportResponse } from 'MainRoot/nosc/applications/applicationDetailTypes';
import { getApplicationReportRawUrl, getApplicationUrl } from 'MainRoot/util/CLMLocation';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';

const TABS = [
  { value: 'overview', label: 'Overview', testId: 'nosc-component-detail-tab-overview' },
] as const;

type LoadStatus = 'loading' | 'ready' | 'error' | 'not-found';

function paramAsString(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}

function displayNameFor(component: RawReportComponent | null, hash: string): string {
  if (!component) return hash;
  if (component.displayName) return component.displayName;
  if (component.packageUrl) return component.packageUrl;
  const coords = component.componentIdentifier?.coordinates;
  if (coords) {
    return [coords.groupId || coords.group, coords.artifactId || coords.name, coords.version]
      .filter(Boolean)
      .join(':');
  }
  return hash;
}

export default function ComponentDetail(): ReactElement {
  const { params } = useCurrentStateAndParams();
  const { stateService } = useRouter();

  const publicId = paramAsString(params.publicId) || '';
  const componentHash = paramAsString(params.componentHash) || '';
  const scanId = paramAsString(params.scanId);

  const [status, setStatus] = useState<LoadStatus>('loading');
  const [component, setComponent] = useState<RawReportComponent | null>(null);
  const [appName, setAppName] = useState<string | undefined>();

  const load = useCallback(
    async (signal?: AbortSignal): Promise<void> => {
      if (!publicId || !componentHash) {
        setStatus('error');
        return;
      }
      setStatus('loading');
      try {
        if (!scanId) {
          const appRes = await axios.get<{ name?: string }>(getApplicationUrl(publicId), { signal });
          if (signal?.aborted) return;
          setAppName(appRes.data?.name);
          setComponent(null);
          setStatus('ready');
          return;
        }

        const [appRes, rawRes] = await Promise.all([
          axios.get<{ name?: string }>(getApplicationUrl(publicId), { signal }),
          axios.get<RawReportResponse>(getApplicationReportRawUrl(publicId, scanId), { signal }),
        ]);
        if (signal?.aborted) return;
        setAppName(appRes.data?.name);

        const match = (rawRes.data.components ?? []).find((c) => c.hash === componentHash) ?? null;
        if (!match) {
          setComponent(null);
          setStatus('not-found');
          return;
        }
        setComponent(match);
        setStatus('ready');
      } catch (err) {
        if (axios.isCancel(err) || signal?.aborted) return;
        setStatus('error');
      }
    },
    [componentHash, publicId, scanId],
  );

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const label = displayNameFor(component, componentHash);
  const securityIssues = component?.securityData?.securityIssues ?? [];
  const firstSecurityIssueRef = securityIssues[0]?.reference;

  const context = useMemo(
    () =>
      resolveEntityDetailContext({
        current: 'component',
        applicationPublicId: publicId,
        applicationName: appName,
        componentHash,
        componentDisplayName: label,
        scanId: scanId || undefined,
        vulnId: firstSecurityIssueRef,
      }),
    [appName, componentHash, firstSecurityIssueRef, label, publicId, scanId],
  );

  const breadcrumb = useMemo(
    () => (
      <Flex align="center" gap="2" data-testid="nosc-component-detail-breadcrumb">
        <RadixLink
          size="2"
          color="gray"
          href={stateService.href('nexusOneApplicationsDetail.overview', { publicId })}
        >
          <Flex align="center" gap="1">
            <ActionIcons.Back size={14} />
            {appName || publicId}
          </Flex>
        </RadixLink>
        <Text size="2" color="gray">
          /
        </Text>
        <Text size="2" weight="medium">
          Component
        </Text>
      </Flex>
    ),
    [appName, publicId, stateService],
  );

  const header = useMemo(
    () => (
      <>
        {status === 'loading' && (
          <LoadingSkeleton height={72} data-testid="nosc-component-detail-header-loading" />
        )}
        {status === 'error' && (
          <Flex direction="column" gap="3" align="start" data-testid="nosc-component-detail-header-error">
            <Text size="2" color="red">
              Failed to load component.
            </Text>
            <Button size="2" variant="soft" onClick={() => void load()} data-testid="nosc-component-detail-retry">
              Retry
            </Button>
          </Flex>
        )}
        {status === 'not-found' && (
          <Flex direction="column" gap="3" align="start" data-testid="nosc-component-detail-header-not-found">
            <Text size="2" color="red">
              Component <code>{componentHash}</code> was not found in scan <code>{scanId}</code>.
            </Text>
            <Button size="2" variant="soft" onClick={() => void load()} data-testid="nosc-component-detail-retry">
              Retry
            </Button>
          </Flex>
        )}
        {status === 'ready' && (
          <Flex direction="column" gap="2" data-testid="nosc-component-detail-header">
            <PageHeading>{label}</PageHeading>
            <Flex gap="3" wrap="wrap" align="center">
              <Text size="2" color="gray" style={{ fontFamily: 'var(--code-font-family)' }}>
                {componentHash}
              </Text>
              {component?.matchState && (
                <Badge size="1" color="gray" variant="soft">
                  {component.matchState}
                </Badge>
              )}
            </Flex>
          </Flex>
        )}
      </>
    ),
    [component?.matchState, componentHash, label, load, scanId, status],
  );

  const securityEmptyMessage = !scanId
    ? 'Select a scan to see security issues for this component.'
    : 'No security issues on this component in the selected scan.';

  return (
    <EntityDetailLayout
      breadcrumb={breadcrumb}
      header={header}
      context={context}
      tabs={TABS}
      activeTab="overview"
      onTabChange={() => undefined}
      mainTestId="nosc-component-detail-page"
      testIdPrefix="nosc-component-detail"
    >
      {status === 'ready' && (
        <Flex direction="column" gap="4" mt="4">
          <Card>
            <Flex direction="column" gap="3" p="4">
              <Text size="3" weight="medium">
                Identity
              </Text>
              <Text size="2">Package URL: {component?.packageUrl || '—'}</Text>
              <Text size="2">Format: {component?.componentIdentifier?.format || '—'}</Text>
              {scanId && (
                <RadixLink size="2" href={classicReportHrefForComponent(publicId, scanId, componentHash)}>
                  View in Classic report →
                </RadixLink>
              )}
            </Flex>
          </Card>

          <Card>
            <Flex direction="column" gap="3" p="4">
              <Text size="3" weight="medium">
                Security issues
              </Text>
              {securityIssues.length === 0 ? (
                <Text size="2" color="gray" data-testid="nosc-component-detail-security-empty">
                  {securityEmptyMessage}
                </Text>
              ) : (
                securityIssues.map((issue, idx) => (
                  <Flex key={issue.reference ?? idx} align="center" gap="3" wrap="wrap">
                    {issue.reference ? (
                      <RadixLink
                        size="2"
                        href={vulnerabilityDetailHref({
                          vulnId: issue.reference,
                          applicationPublicId: publicId,
                          componentHash,
                          scanId: scanId || undefined,
                        })}
                      >
                        {issue.reference}
                      </RadixLink>
                    ) : (
                      <Text size="2" color="gray">
                        Unknown reference
                      </Text>
                    )}
                    {typeof issue.severity === 'number' && (
                      <Badge size="1" color="orange" variant="soft">
                        {issue.severity}
                      </Badge>
                    )}
                  </Flex>
                ))
              )}
            </Flex>
          </Card>

          <Card>
            <Flex direction="column" gap="2" p="4">
              <Text size="3" weight="medium">
                Related
              </Text>
              <Text size="2" color="gray">
                Policy failures open from Application Violations or Violation detail.
              </Text>
              <RadixLink
                size="2"
                href={stateService.href('nexusOneApplicationsDetail.violations', { publicId })}
              >
                Open application violations →
              </RadixLink>
            </Flex>
          </Card>
        </Flex>
      )}
    </EntityDetailLayout>
  );
}
