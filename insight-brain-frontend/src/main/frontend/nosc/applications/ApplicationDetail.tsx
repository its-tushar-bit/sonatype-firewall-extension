/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo, type ReactElement, type ReactNode } from 'react';
import { useSelector } from 'react-redux';
import { Badge, Button, Flex, Link as RadixLink, Text } from '@radix-ui/themes';
import { PageHeading } from '@sonatype/nexus-one-components';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';
import { getApplicationUrl } from 'MainRoot/util/CLMLocation';
import { useApplicationDetailData } from './useApplicationDetailData';
import { selectApplicationPolicyThreatsState } from './applicationDetailSlice';
import { selectComponentCount, selectViolationSummary } from './applicationDetailSelectors';
import { UIView, useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { EntityDetailLayout } from 'MainRoot/nosc/entityDetail/EntityDetailLayout';
import { resolveEntityDetailContext } from 'MainRoot/nosc/entityDetail/resolveEntityDetailContext';
import { ApplicationDTO, TabId, TAB_IDS } from './applicationDetailTypes';
import {
  applicationDetailStateNameForTab,
  tabFromApplicationDetailStateName,
} from './applicationDetailUtils';
import {
  ApplicationDetailShellProvider,
  type ApplicationDetailShellContextValue,
} from './applicationDetailContext';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';

import '@radix-ui/themes/styles.css';

/**
 * Native Nexus One Application Detail page.
 *
 * Mounted at /applications/{publicId}. Uses EntityDetailLayout for shared
 * detail chrome while preserving the existing six-tab content and data pipeline.
 */

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

export default function ApplicationDetail(): ReactElement {
  const { params, state } = useCurrentStateAndParams();
  const { stateService } = useRouter();

  const publicId = typeof params.publicId === 'string' ? params.publicId : '';
  const activeTab: TabId = tabFromApplicationDetailStateName(state?.name);

  const handleTabChange = (next: string): void => {
    if (!TAB_IDS.includes(next as TabId)) return;
    stateService.go(applicationDetailStateNameForTab(next as TabId), { publicId });
  };

  const appTile = useTile<ApplicationDTO>(getApplicationUrl(publicId));
  const applicationInternalId = appTile.data?.id;

  const { retryReports, retryPolicy, retryRaw } = useApplicationDetailData({
    applicationInternalId,
    publicId,
  });

  const policyState = useSelector(selectApplicationPolicyThreatsState);
  const { totalViolations } = useSelector(selectViolationSummary);
  const componentCount = useSelector(selectComponentCount);
  const overviewIsReady = appTile.status === 'ready';

  const shellContext = useMemo<ApplicationDetailShellContextValue>(
    () => ({
      publicId,
      appData: appTile.data,
      appStatus: appTile.status,
      appRetry: appTile.retry,
      applicationInternalId,
      retryReports,
      retryPolicy,
      retryRaw,
    }),
    [
      publicId,
      appTile.data,
      appTile.status,
      appTile.retry,
      applicationInternalId,
      retryReports,
      retryPolicy,
      retryRaw,
    ],
  );

  const context = useMemo(() => {
    if (!overviewIsReady || !appTile.data) return null;
    return resolveEntityDetailContext({
      current: 'application',
      applicationPublicId: appTile.data.publicId || publicId,
      applicationName: appTile.data.name,
    });
  }, [appTile.data, overviewIsReady, publicId]);

  const tabs = useMemo(
    () => [
      {
        value: 'overview',
        label: 'Overview',
        testId: 'nosc-app-detail-tab-overview',
      },
      {
        value: 'policy-failures',
        label: tabLabel(
          'Violations',
          policyState.status === 'ready' ? (
            <Badge size="1" color="gray" variant="soft" radius="full">
              {totalViolations}
            </Badge>
          ) : null,
        ),
        testId: 'nosc-app-detail-tab-policy-failures',
      },
      {
        value: 'components',
        label: tabLabel(
          'Components',
          policyState.status === 'ready' ? (
            <Badge size="1" color="gray" variant="soft" radius="full">
              {componentCount}
            </Badge>
          ) : null,
        ),
        testId: 'nosc-app-detail-tab-components',
      },
      {
        value: 'sboms',
        label: 'SBOMs',
        testId: 'nosc-app-detail-tab-sboms',
      },
      {
        value: 'waivers',
        label: 'Waivers',
        testId: 'nosc-app-detail-tab-waivers',
      },
      {
        value: 'team-members',
        label: 'Team Members',
        testId: 'nosc-app-detail-tab-team-members',
      },
    ],
    [componentCount, policyState.status, totalViolations],
  );

  const breadcrumb = useMemo(
    () => (
      <Flex
        align="center"
        gap="2"
        data-testid="nosc-app-detail-breadcrumb"
        data-public-id={publicId}
      >
        <RadixLink size="2" color="gray" href={stateService.href('nexusOneApplications')}>
          <Flex align="center" gap="1">
            <ActionIcons.Back size={14} />
            Applications
          </Flex>
        </RadixLink>
        <Text size="2" color="gray">
          /
        </Text>
        <Text size="2" weight="medium">
          {appTile.data?.name || publicId}
        </Text>
      </Flex>
    ),
    [appTile.data?.name, publicId, stateService],
  );

  const header = useMemo(
    () => (
      <>
        {appTile.status === 'loading' && (
          <LoadingSkeleton height={96} data-testid="nosc-app-detail-header-loading" />
        )}
        {appTile.status === 'error' && (
          <Flex
            direction="column"
            gap="3"
            align="start"
            p="4"
            data-testid="nosc-app-detail-header-error"
            style={{ backgroundColor: 'var(--red-3)', borderRadius: 'var(--radius-3)' }}
          >
            <Text size="2" color="red">
              Failed to load application <code>{publicId}</code>.
            </Text>
            <Button size="2" variant="soft" onClick={appTile.retry} data-testid="nosc-app-detail-header-retry">
              Retry
            </Button>
          </Flex>
        )}
        {overviewIsReady && (
          <Flex direction="column" gap="2" data-testid="nosc-app-detail-header">
            <Flex align="center" gap="3">
              <DomainIcons.Applications size={28} color="var(--accent-9)" />
              <PageHeading data-testid="nosc-app-detail-name">{appTile.data?.name}</PageHeading>
            </Flex>
            <Flex align="center" gap="3" wrap="wrap">
              <Text size="2" color="gray">
                <Text style={{ fontFamily: 'var(--code-font-family)' }}>{appTile.data?.publicId}</Text>
              </Text>
              {appTile.data?.organizationName && (
                <>
                  <Text size="2" color="gray">
                    •
                  </Text>
                  <Flex align="center" gap="1">
                    <DomainIcons.Organizations size={14} color="var(--gray-10)" />
                    <Text size="2" color="gray">
                      {appTile.data.organizationName}
                    </Text>
                  </Flex>
                </>
              )}
            </Flex>
          </Flex>
        )}
      </>
    ),
    [
      appTile.data?.name,
      appTile.data?.organizationName,
      appTile.data?.publicId,
      appTile.retry,
      appTile.status,
      overviewIsReady,
      publicId,
    ],
  );

  return (
    <EntityDetailLayout
      breadcrumb={breadcrumb}
      header={header}
      context={context}
      tabs={tabs}
      activeTab={activeTab}
      onTabChange={handleTabChange}
      mainTestId="nosc-app-detail-page"
      testIdPrefix="nosc-app-detail"
    >
      <ApplicationDetailShellProvider value={shellContext}>
        <UIView />
      </ApplicationDetailShellProvider>
    </EntityDetailLayout>
  );
}
