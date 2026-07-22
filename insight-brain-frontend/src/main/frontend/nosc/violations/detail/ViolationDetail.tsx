/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect, useMemo, type ReactElement, type ReactNode } from 'react';
import { UIView, useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { Badge, Box, Button, Flex, Link as RadixLink, Text } from '@radix-ui/themes';
import { PageHeading } from '@sonatype/nexus-one-components';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { EntityDetailLayout } from 'MainRoot/nosc/entityDetail/EntityDetailLayout';
import { resolveEntityDetailContext } from 'MainRoot/nosc/entityDetail/resolveEntityDetailContext';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { useViolationDetailData } from 'MainRoot/nosc/violations/detail/useViolationDetailData';
import {
  VIOLATION_DETAIL_TAB_IDS,
  componentDisplayNameLabel,
  getMostRecentStageEntry,
  isSecurityPolicyCategory,
  tabFromViolationDetailStateName,
  violationDetailStateNameForTab,
} from 'MainRoot/nosc/violations/detail/violationDetailUtils';
import type { ViolationDetailTabId } from 'MainRoot/nosc/violations/detail/violationDetailTypes';

function tabLabel(label: string, count: ReactNode): ReactNode {
  if (count === null || count === undefined || count === false) {
    return label;
  }
  return (
    <Flex align="center" gap="2">
      {label}
      {count}
    </Flex>
  );
}

const VIOLATION_DETAIL_TABS = [
  {
    value: 'overview',
    label: 'Overview',
    testId: 'nosc-violation-detail-tab-overview',
  },
  {
    value: 'vulnerability',
    label: 'Vulnerability',
    testId: 'nosc-violation-detail-tab-vulnerability',
  },
  {
    value: 'waivers',
    label: 'Waivers',
    testId: 'nosc-violation-detail-tab-waivers',
  },
] as const;

function paramAsString(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}

export default function ViolationDetail(): ReactElement {
  const { params, state } = useCurrentStateAndParams();
  const { stateService } = useRouter();

  const violationId = paramAsString(params.id);
  const activeTab = tabFromViolationDetailStateName(state?.name);
  const detail = useViolationDetailData({ violationId });
  const activeWaiverCount = detail.activeWaivers.length;
  const showVulnerabilityTab = isSecurityPolicyCategory(detail.identity?.policyThreatCategory);
  const tabs = useMemo(
    () =>
      VIOLATION_DETAIL_TABS.filter((tab) => tab.value !== 'vulnerability' || showVulnerabilityTab).map((tab) =>
        tab.value === 'waivers'
          ? {
              ...tab,
              label: tabLabel(
                'Waivers',
                detail.waiversStatus === 'ready' ? (
                  <Badge color="gray" variant="soft" size="1" radius="full">
                    {activeWaiverCount}
                  </Badge>
                ) : null,
              ),
            }
          : tab
      ),
    [activeWaiverCount, detail.waiversStatus, showVulnerabilityTab]
  );

  useEffect(() => {
    if (activeTab !== 'vulnerability' || detail.identityStatus !== 'ready' || showVulnerabilityTab) return;
    stateService.go(violationDetailStateNameForTab('overview'), {
      id: violationId,
      type: params.type,
      sidebarReference: params.sidebarReference,
      sidebarId: params.sidebarId,
      page: params.page,
    });
  }, [
    activeTab,
    detail.identityStatus,
    params.page,
    params.sidebarId,
    params.sidebarReference,
    params.type,
    showVulnerabilityTab,
    stateService,
    violationId,
  ]);

  const handleTabChange = (next: string): void => {
    if (!violationId || !VIOLATION_DETAIL_TAB_IDS.includes(next as ViolationDetailTabId)) return;
    stateService.go(violationDetailStateNameForTab(next as ViolationDetailTabId), {
      id: violationId,
      type: params.type,
      sidebarReference: params.sidebarReference,
      sidebarId: params.sidebarId,
      page: params.page,
    });
  };

  const context = useMemo(() => {
    if (!detail.identity) return null;

    const latestStage = getMostRecentStageEntry(detail.identity.stageData);
    return resolveEntityDetailContext({
      current: 'violation',
      applicationPublicId: detail.identity.applicationPublicId,
      applicationName: detail.identity.applicationName,
      componentHash: detail.identity.hash,
      componentDisplayName: componentDisplayNameLabel(detail.identity.displayName) || undefined,
      policyViolationId: detail.identity.policyViolationId,
      policyName: detail.identity.policyName,
      vulnId: detail.vulnerabilitySummary?.identifier,
      stageId: latestStage?.stageId,
      scanId: latestStage?.scanId,
    });
  }, [detail.identity, detail.vulnerabilitySummary?.identifier]);

  const breadcrumb = useMemo(
    () => (
      <Flex
        align="center"
        gap="2"
        data-testid="nosc-violation-detail-breadcrumb"
        data-violation-id={violationId}
      >
        <RadixLink size="2" color="gray" href={stateService.href('nexusOneViolations')}>
          <Flex align="center" gap="1">
            <ActionIcons.Back size={14} />
            Violations
          </Flex>
        </RadixLink>
        <Text size="2" color="gray">
          /
        </Text>
        <Text size="2" weight="medium">
          {detail.identity?.policyName || violationId || 'Violation'}
        </Text>
      </Flex>
    ),
    [detail.identity?.policyName, stateService, violationId],
  );

  const header = useMemo(
    () => (
      <>
        {detail.identityStatus === 'loading' && (
          <LoadingSkeleton height={96} data-testid="nosc-violation-detail-header-loading" />
        )}
        {detail.identityStatus === 'error' && (
          <Flex
            direction="column"
            gap="3"
            align="start"
            p="4"
            data-testid="nosc-violation-detail-header-error"
            style={{ backgroundColor: 'var(--red-3)', borderRadius: 'var(--radius-3)' }}
          >
            <Text size="2" color="red">
              Failed to load violation <code>{violationId}</code>.
            </Text>
            <Button size="2" variant="soft" onClick={detail.retry} data-testid="nosc-violation-detail-header-retry">
              Retry
            </Button>
          </Flex>
        )}
        {detail.identityStatus === 'ready' && detail.identity && (
          <Flex direction="column" gap="2" data-testid="nosc-violation-detail-header">
            <PageHeading>{detail.identity.policyName}</PageHeading>
            <Flex align="center" gap="3" wrap="wrap">
              <Text size="2" color="gray">
                Threat level {detail.identity.threatLevel}
              </Text>
              <Text size="2" color="gray">
                {detail.identity.applicationName}
              </Text>
              <Text size="2" color="gray">
                {detail.identity.organizationName}
              </Text>
            </Flex>
          </Flex>
        )}
        {detail.identityStatus === 'idle' && (
          <Box data-testid="nosc-violation-detail-header-idle">
            <Text size="2" color="gray">
              Select a violation to view details.
            </Text>
          </Box>
        )}
      </>
    ),
    [detail.identity, detail.identityStatus, detail.retry, violationId],
  );

  return (
    <EntityDetailLayout
      breadcrumb={breadcrumb}
      header={header}
      context={context}
      tabs={tabs}
      activeTab={activeTab}
      onTabChange={handleTabChange}
      mainTestId="nosc-violation-detail-page"
      testIdPrefix="nosc-violation-detail"
    >
      <UIView />
    </EntityDetailLayout>
  );
}
