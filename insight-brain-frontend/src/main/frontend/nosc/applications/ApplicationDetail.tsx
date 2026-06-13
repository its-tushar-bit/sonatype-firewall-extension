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
import { selectApplicationReportsState } from './applicationDetailSlice';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';

import { ApplicationDTO, TabId, TAB_IDS } from './applicationDetailTypes';
import {
  classicAppDetailHref,
  classicHref,
  extractScanId,
  flattenViolations,
  pickLatestReport,
  tabFromSlug,
  TAB_TO_URL,
} from './applicationDetailUtils';
import { InlineComingSoon } from './InlineComingSoon';
import { OverviewTab } from './OverviewTab';
import { PolicyFailuresTab } from './PolicyFailuresTab';
import { ComponentsTab } from './ComponentsTab';
import { AppWaiversTab } from './AppWaiversTab';
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
  const { params } = useCurrentStateAndParams();
  const { stateService } = useRouter();

  const publicId = typeof params.publicId === 'string' ? params.publicId : '';
  const activeTab: TabId = tabFromSlug(typeof params.tab === 'string' ? params.tab : undefined);

  /** Tab-click handler: navigate via UI-Router to the per-tab state so the URL
   *  and history update through the router (each tab is its own history entry,
   *  matching GitHub/GitLab tabbed-page UX). `activeTab` then re-derives from
   *  the new `{tab}` param. */
  const handleTabChange = (next: string): void => {
    if (!TAB_IDS.includes(next as TabId)) return;
    const nextTab = next as TabId;
    stateService.go('nexusOneApplicationsDetailTab', { publicId, tab: TAB_TO_URL[nextTab] });
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

  // Stages 2-4 (reports → policythreats + raw report) are fetched through
  // Redux (CLM-39709 review #7). The dependency chain is: reports need the
  // internal id; policythreats + raw need publicId + the scanId parsed from
  // the latest report. We derive `latestReport`/`scanId` from the reports
  // slice and feed scanId back into the hook, which owns all three dispatches.
  const reportsData = useSelector(selectApplicationReportsState).data;
  const latestReport = reportsData ? pickLatestReport(reportsData) : null;
  const scanId = latestReport ? extractScanId(latestReport) : null;

  const {
    reports,
    reportsStatus,
    policyThreats,
    policyStatus,
    rawReport,
    rawStatus,
    retryReports,
    retryPolicy,
    retryRaw,
  } = useApplicationDetailData({ applicationInternalId, publicId, scanId });

  const {
    violations,
    totalViolations,
    waivedViolations,
    openViolations,
    criticalCount,
    severeCount,
    moderateCount,
    maliciousCount,
    violationCountByHash,
  } = useMemo(() => {
    const flat = flattenViolations(policyThreats);
    let waived = 0;
    let critical = 0;
    let severe = 0;
    let moderate = 0;
    let malicious = 0;
    for (const v of flat) {
      if (/malicious/i.test(v.policyThreatCategory) || /malicious/i.test(v.policyName)) {
        malicious += 1;
      }
      if (v.waived) {
        waived += 1;
        continue;
      }
      const level = v.policyThreatLevel;
      if (level >= 8) critical += 1;
      else if (level >= 4) severe += 1;
      else if (level >= 2) moderate += 1;
    }
    const total = flat.length;
    const countsByHash: Record<string, number> = {};
    for (const c of policyThreats?.aaData ?? []) {
      if (!c.hash || c.hash === 'null') continue;
      const active = (c.allViolations ?? []).filter((v) => !v.waived && !v.legacyViolation);
      countsByHash[c.hash] = active.length;
    }
    return {
      violations: flat,
      totalViolations: total,
      waivedViolations: waived,
      openViolations: total - waived,
      criticalCount: critical,
      severeCount: severe,
      moderateCount: moderate,
      maliciousCount: malicious,
      violationCountByHash: countsByHash,
    };
  }, [policyThreats]);

  const componentCount = policyThreats?.aaData?.length ?? 0;

  const totalComponentsScanned = rawReport?.components?.length ?? 0;

  const overviewIsLoading = appTile.status === 'loading' || reportsStatus === 'loading' || policyStatus === 'loading';
  const overviewIsReady = appTile.status === 'ready';

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
                {policyStatus === 'ready' && (
                  <Badge size="1" color="gray" variant="soft" radius="full">
                    {totalViolations}
                  </Badge>
                )}
              </Flex>
            </Tabs.Trigger>
            <Tabs.Trigger value="components" data-testid="nosc-app-detail-tab-components">
              <Flex align="center" gap="2">
                Components
                {rawStatus === 'ready' && (
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

          {/* Overview */}
          <Tabs.Content value="overview" data-testid="nosc-app-detail-tab-content-overview">
            <OverviewTab
              appData={appTile.data}
              publicId={publicId}
              overviewIsLoading={overviewIsLoading}
              policyStatus={policyStatus}
              reportsStatus={reportsStatus}
              scanId={scanId}
              latestReport={latestReport}
              reports={reports}
              totalViolations={totalViolations}
              openViolations={openViolations}
              waivedViolations={waivedViolations}
              criticalCount={criticalCount}
              severeCount={severeCount}
              moderateCount={moderateCount}
              componentCount={componentCount}
              maliciousCount={maliciousCount}
              onRetryPolicy={retryPolicy}
              onRetryReports={retryReports}
            />
          </Tabs.Content>

          {/* Policy Failures */}
          <Tabs.Content value="policy-failures" data-testid="nosc-app-detail-tab-content-policy-failures">
            <PolicyFailuresTab
              violations={violations}
              loading={policyStatus === 'loading' || reportsStatus === 'loading'}
              errored={policyStatus === 'error'}
              onRetry={retryPolicy}
              showNoScanYet={!scanId && policyStatus !== 'loading' && reportsStatus === 'ready'}
            />
          </Tabs.Content>

          {/* Components — live data, no per-component detail page yet
              (deferred until Guide integration ships per CLM-39709
              review). Click-through goes to Classic Application
              Composition Report. */}
          <Tabs.Content value="components" data-testid="nosc-app-detail-tab-content-components">
            <ComponentsTab
              components={rawReport?.components ?? []}
              status={rawStatus}
              publicId={publicId}
              scanId={scanId}
              violationCountByHash={violationCountByHash}
              onRetry={retryRaw}
            />
          </Tabs.Content>

          {/* SBOMs — Coming Soon */}
          <Tabs.Content value="sboms" data-testid="nosc-app-detail-tab-content-sboms">
            <InlineComingSoon
              testId="nosc-app-detail-sboms-coming-soon"
              label="SBOMs"
              description="Generate, ingest, and download CycloneDX or SPDX SBOMs for this application. Coming to Nexus One soon."
              classicHref={classicHref(`/sbomManager/management/view/application/${encodeURIComponent(publicId)}`)}
            />
          </Tabs.Content>

          {/* Waivers — live data, scoped to this application via
              applicationIds=[{internalId}] in the dashboard query. */}
          <Tabs.Content value="waivers" data-testid="nosc-app-detail-tab-content-waivers">
            <AppWaiversTab
              applicationInternalId={applicationInternalId}
              publicId={publicId}
              waivers={waivers}
              loading={waiversLoading}
              error={waiversError}
              refetch={refetchWaivers}
            />
          </Tabs.Content>

          {/* Team Members — Coming Soon */}
          <Tabs.Content value="team-members" data-testid="nosc-app-detail-tab-content-team-members">
            <InlineComingSoon
              testId="nosc-app-detail-team-members-coming-soon"
              label="Team Members"
              description="See who can access, scan, and waive policy violations for this application — and adjust their roles."
              classicHref={classicAppDetailHref(publicId)}
            />
          </Tabs.Content>
        </Tabs.Root>
      </main>
    </Box>
  );
}
