/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import {
  Badge,
  Box,
  Button,
  Flex,
  Link as RadixLink,
  Tabs,
  Text,
} from '@radix-ui/themes';
import { PageHeading } from '@sonatype/nexus-one-components';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';
import { getApplicationUrl } from 'MainRoot/util/CLMLocation';
import { useWaiversList } from 'MainRoot/nosc/waivers/useWaivers';
import { useApplicationDetailData } from './useApplicationDetailData';
import {
  selectApplicationPolicyThreatsState,
  selectApplicationRawReportState,
} from './applicationDetailSlice';
import {
  selectTotalComponentsScanned,
  selectViolationSummary,
} from './applicationDetailSelectors';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { UIView, useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { ErrorBoundary } from 'react-error-boundary';

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
 * P1-F7c (CLM-39709): Native Nexus One Application Detail page.
 *
 * Mounted at /applications/{publicId}. Renders the same six-section
 * layout as the apps/nexusone-ux-prototype `GuideApplicationDetailContent`
 * but wired to real IQ Server data. Shape decisions:
 *
 *   - Header: app name, publicId, organization (read from `GET
 *     /rest/application/{publicId}` — `ApplicationDTO`).
 *   - Overview tab: Policy Compliance + Risk & Trust Metrics + Scan
 *     Information cards on the left, Application Details + Quick Actions
 *     cards on the right. Numbers come from the latest non-stage-specific
 *     report's `policythreats.json`. The DTS/dtsTrend/security-events
 *     widgets in the prototype don't have IQ equivalents yet — they are
 *     replaced with what IQ does have (counts of waived, malicious-
 *     detected, total violations) so the visual contract is preserved
 *     without inventing fake data.
 *   - Policy Failures tab: client-side filter + sort + paginate over the
 *     full violation list from policythreats.json. No backend pagination
 *     because the endpoint already returns everything.
 *   - Components, SBOMs, Waivers, Team Members tabs: inline "Coming Soon"
 *     panel with a Continue-in-Classic deep link. (No Security Events tab
 *     — IQ has no equivalent data source today.)
 *
 * Sequential data dependencies are unavoidable here: we need the
 * application's internal `id` from `/rest/application/{publicId}` before
 * we can call `/api/v2/reports/applications/{id}` for the latest scanId,
 * and before that we can't call `/rest/report/.../policythreats.json`.
 * Each step has its own loading/error state so partial failure (e.g. an
 * app that's never been scanned) still renders the header + the cards
 * that work.
 *
 * No backend Java change. Endpoints used were verified live against the
 * dev IQ on 2026-05-14.
 */

export default function ApplicationDetail(): JSX.Element {
  const offsets = usePreviewShellOffsets();
  const { params, state } = useCurrentStateAndParams();
  const { stateService } = useRouter();

  const publicId = typeof params.publicId === 'string' ? params.publicId : '';
  const activeTab: TabId = tabFromApplicationDetailStateName(state?.name);

  /** Tab-click handler: navigate to the matching child state (CLM-40901). */
  const handleTabChange = (next: string): void => {
    if (!TAB_IDS.includes(next as TabId)) return;
    stateService.go(applicationDetailStateNameForTab(next as TabId), { publicId });
  };

  // Stage 1: load the application metadata so we have its internal `id`.
  const appTile = useTile<ApplicationDTO>(getApplicationUrl(publicId));

  const applicationInternalId = appTile.data?.id;

  // Live waivers scoped to this application — lifted from AppWaiversTab
  // to the parent so the Waivers tab trigger can show a count badge in
  // the same shape as the Violations tab (P1-F7d UX parity).
  const {
    loading: waiversLoading,
    error: waiversError,
    waivers,
    refetch: refetchWaivers,
  } = useWaiversList({
    applicationInternalId,
    includeAutoWaivers: true,
  });

  const { retryReports, retryPolicy, retryRaw } = useApplicationDetailData({
    applicationInternalId,
    publicId,
  });

  const policyState = useSelector(selectApplicationPolicyThreatsState);
  const rawState = useSelector(selectApplicationRawReportState);
  const { totalViolations } = useSelector(selectViolationSummary);
  const totalComponentsScanned = useSelector(selectTotalComponentsScanned);

  const overviewIsReady = appTile.status === 'ready';

  // Annotated + memoized so (a) a missing/renamed field is caught here at the
  // definition site rather than one call deep at the Provider boundary, and
  // (b) the Provider value keeps a stable identity across renders that don't
  // change its inputs — otherwise all tab-route consumers re-render every time.
  const shellContext = useMemo<ApplicationDetailShellContextValue>(
    () => ({
      publicId,
      appData: appTile.data,
      appStatus: appTile.status,
      appRetry: appTile.retry,
      applicationInternalId,
      waivers,
      waiversLoading,
      waiversError,
      refetchWaivers,
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
      waivers,
      waiversLoading,
      waiversError,
      refetchWaivers,
      retryReports,
      retryPolicy,
      retryRaw,
    ],
  );

  return (
    // The Radix Theme is provided once by NexusOneShellLayout; this page renders
    // its content into a fixed, scrollable <main> region below the shell chrome.
    <Box
      asChild
      p="6"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      <main data-testid="nosc-app-detail-page" data-public-id={publicId}>
        {/* Breadcrumb / back link */}
        <Flex align="center" gap="2" mb="3" data-testid="nosc-app-detail-breadcrumb">
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

        {/* Header */}
        {appTile.status === 'loading' && (
          <Box mb="5">
            <LoadingSkeleton height={96} data-testid="nosc-app-detail-header-loading" />
          </Box>
        )}
        {appTile.status === 'error' && (
          <Flex
            direction="column"
            gap="3"
            align="start"
            p="4"
            data-testid="nosc-app-detail-header-error"
            style={{ backgroundColor: 'var(--red-3)', borderRadius: 'var(--radius-3)', marginBottom: 24 }}
          >
            <Text size="2" color="red">
              Failed to load application <code>{publicId}</code>.
            </Text>
            <Button
              size="2"
              variant="soft"
              onClick={appTile.retry}
              data-testid="nosc-app-detail-header-retry"
            >
              Retry
            </Button>
          </Flex>
        )}
        {overviewIsReady && (
          <Flex direction="column" gap="2" mb="5" data-testid="nosc-app-detail-header">
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

        <Tabs.Root
          value={activeTab}
          onValueChange={handleTabChange}
          data-testid="nosc-app-detail-tabs"
        >
          <Tabs.List size="2">
            <Tabs.Trigger value="overview" data-testid="nosc-app-detail-tab-overview">
              Overview
            </Tabs.Trigger>
            <Tabs.Trigger value="policy-failures" data-testid="nosc-app-detail-tab-policy-failures">
              <Flex align="center" gap="2">
                Violations
                {policyState.status === 'ready' && (
                  <Badge size="1" color="gray" variant="soft" radius="full">
                    {totalViolations}
                  </Badge>
                )}
              </Flex>
            </Tabs.Trigger>
            <Tabs.Trigger value="components" data-testid="nosc-app-detail-tab-components">
              <Flex align="center" gap="2">
                Components
                {rawState.status === 'ready' && (
                  <Badge size="1" color="gray" variant="soft" radius="full">
                    {totalComponentsScanned}
                  </Badge>
                )}
              </Flex>
            </Tabs.Trigger>
            <Tabs.Trigger value="sboms" data-testid="nosc-app-detail-tab-sboms">
              SBOMs
            </Tabs.Trigger>
            <Tabs.Trigger value="waivers" data-testid="nosc-app-detail-tab-waivers">
              <Flex align="center" gap="2">
                Waivers
                {!waiversLoading && !waiversError && applicationInternalId && (
                  <Badge size="1" color="gray" variant="soft" radius="full">
                    {waivers.length}
                  </Badge>
                )}
              </Flex>
            </Tabs.Trigger>
            <Tabs.Trigger value="team-members" data-testid="nosc-app-detail-tab-team-members">
              Team Members
            </Tabs.Trigger>
          </Tabs.List>

          {/* Single Tabs.Content mounts only the active tab (UI-Router child inside).
              Tab-local useState (filter/sort) resets on switch; data-layer state lives in Redux. */}
          <Tabs.Content
            value={activeTab}
            data-testid={`nosc-app-detail-tab-content-${activeTab}`}
          >
            <ErrorBoundary
              resetKeys={[activeTab]}
              fallbackRender={({ error }) => (
                <Flex direction="column" gap="2" p="4" mt="4" data-testid="nosc-app-detail-tab-error">
                  <Text size="3" color="red" weight="medium">
                    This tab failed to load.
                  </Text>
                  <Text size="2" color="gray">
                    {error instanceof Error ? error.message : String(error)}
                  </Text>
                </Flex>
              )}
            >
              <ApplicationDetailShellProvider value={shellContext}>
                <UIView />
              </ApplicationDetailShellProvider>
            </ErrorBoundary>
          </Tabs.Content>
        </Tabs.Root>
      </main>
    </Box>
  );
}
