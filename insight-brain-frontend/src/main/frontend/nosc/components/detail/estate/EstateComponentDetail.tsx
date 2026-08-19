/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactElement } from 'react';
import axios from 'axios';
import { Badge, Button, Flex, Link as RadixLink, Text } from '@radix-ui/themes';
import { PageHeading } from '@sonatype/nexus-one-components';
import { UIView, useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { EntityDetailLayout } from 'MainRoot/nosc/entityDetail/EntityDetailLayout';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import type { EstateComponentTab } from 'MainRoot/nosc/components/detail/estateComponentDetailHref';
import { NEXUS_ONE_COMPONENTS_STATE_NAME } from 'MainRoot/nosc/componentsList/componentsRoute';
import { EstateComponentDetailShellProvider } from './estateComponentDetailContext';
import type {
  EstateComponentBlastRadiusCounts,
  EstateComponentHdsStatus,
  EstateComponentPathSelection,
} from './estateComponentDetailContext';
import { fetchEstateComponentDetails } from './estateComponentDetailsApi';
import type { EstateComponentDetails } from './estateComponentDetailsApi';
import { EstateComponentPathSwitcher } from './EstateComponentPathSwitcher';
import type {
  EstateComponentPathSwitcherApplicationsUsage,
  EstateComponentPathSwitcherOrganizationsUsage,
} from './EstateComponentPathSwitcher';
import {
  ESTATE_COMPONENT_TAB_IDS,
  estateComponentDetailStateNameForTab,
  tabFromEstateComponentDetailStateName,
  truncatedComponentHash,
} from './estateComponentDetailUtils';
import {
  COMPONENT_USAGE_PAGE_SIZE,
  fetchComponentUsageApplications,
  fetchComponentUsageOrganizations,
} from './estateComponentUsageApi';
import { buildViolationsListRequest } from 'MainRoot/nosc/violations/violationsListApi';
import type { ViolationsListResponse } from 'MainRoot/nosc/violations/violationListTypes';
import { getViolationsListUrl } from 'MainRoot/util/CLMLocation';

const HEADER_COUNT_PAGE_SIZE = 1;

const ESTATE_COMPONENT_TABS = [
  { value: 'overview', label: 'Overview', testId: 'nosc-estate-component-tab-overview' },
  {
    value: 'vulnerabilities',
    label: 'Vulnerabilities',
    testId: 'nosc-estate-component-tab-vulnerabilities',
  },
  { value: 'violations', label: 'Policy Violations', testId: 'nosc-estate-component-tab-violations' },
  { value: 'applications', label: 'Applications', testId: 'nosc-estate-component-tab-applications' },
] as const;

function paramAsString(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

function formatCount(value: number | undefined, singular: string, plural: string): string {
  if (typeof value !== 'number') {
    return `${plural}: -`;
  }
  return `${value} ${value === 1 ? singular : plural}`;
}

function BlastRadiusCountBadges({ counts }: { readonly counts: EstateComponentBlastRadiusCounts }): ReactElement {
  return (
    <Flex gap="2" wrap="wrap" data-testid="nosc-estate-component-blast-radius-counts">
      <Badge size="2" color="gray" variant="soft" data-testid="nosc-estate-component-blast-radius-applications">
        {formatCount(counts.applications, 'App', 'Apps')}
      </Badge>
      <Badge size="2" color="gray" variant="soft" data-testid="nosc-estate-component-blast-radius-organizations">
        {formatCount(counts.organizations, 'Organization', 'Organizations')}
      </Badge>
      <Badge size="2" color="gray" variant="soft" data-testid="nosc-estate-component-blast-radius-violations">
        {formatCount(counts.violations, 'Violation', 'Violations')}
      </Badge>
    </Flex>
  );
}

async function fetchViolationCount(componentHash: string, signal: AbortSignal): Promise<number> {
  const { data } = await axios.post<ViolationsListResponse>(
    getViolationsListUrl(),
    buildViolationsListRequest({
      page: 0,
      pageSize: HEADER_COUNT_PAGE_SIZE,
      includeFacets: false,
      componentHash,
    }),
    { signal }
  );
  return data.total ?? 0;
}

/**
 * Estate (hash-primary) Component Detail shell (CLM-43961).
 * Mounted at /components/{componentHash}. Kitchen-sink tabs are intentionally omitted.
 */
export default function EstateComponentDetail(): ReactElement {
  const { params, state } = useCurrentStateAndParams();
  const { stateService } = useRouter();

  const componentHash = paramAsString(params.componentHash) || '';
  const activeTab: EstateComponentTab = tabFromEstateComponentDetailStateName(state?.name);
  const pathSelection = useMemo<EstateComponentPathSelection>(
    () => ({
      organizationId: paramAsString(params.organizationId),
      applicationId: paramAsString(params.applicationId),
      reportId: paramAsString(params.reportId),
    }),
    [params.applicationId, params.organizationId, params.reportId]
  );

  const [hdsStatus, setHdsStatus] = useState<EstateComponentHdsStatus>('loading');
  const [details, setDetails] = useState<EstateComponentDetails | null>(null);
  const [blastRadiusCounts, setBlastRadiusCounts] = useState<EstateComponentBlastRadiusCounts>({});
  const [organizationsUsage, setOrganizationsUsage] = useState<EstateComponentPathSwitcherOrganizationsUsage>({
    organizations: [],
    total: 0,
    status: 'loading',
  });
  const [applicationsUsage, setApplicationsUsage] = useState<EstateComponentPathSwitcherApplicationsUsage>({
    applications: [],
    total: 0,
    status: 'loading',
  });
  const [resolvedPathSelection, setResolvedPathSelection] = useState<EstateComponentPathSelection>(pathSelection);
  const hdsAbortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    setResolvedPathSelection(pathSelection);
  }, [componentHash, pathSelection]);

  const loadHds = useCallback(
    async (signal?: AbortSignal): Promise<void> => {
      if (!componentHash) {
        setDetails(null);
        setHdsStatus('error');
        return;
      }
      setHdsStatus('loading');
      try {
        const mapped = await fetchEstateComponentDetails(componentHash, signal);
        if (signal?.aborted) return;
        setDetails(mapped);
        setHdsStatus(mapped ? 'ready' : 'empty');
      } catch (err) {
        if (axios.isCancel(err) || signal?.aborted) return;
        console.error('Failed to load api/v2/components/details', { componentHash, err });
        setDetails(null);
        setHdsStatus('error');
      }
    },
    [componentHash]
  );

  const startHdsLoad = useCallback((): void => {
    hdsAbortRef.current?.abort();
    const controller = new AbortController();
    hdsAbortRef.current = controller;
    void loadHds(controller.signal);
  }, [loadHds]);

  useEffect(() => {
    startHdsLoad();
    return () => {
      hdsAbortRef.current?.abort();
      hdsAbortRef.current = null;
    };
  }, [startHdsLoad]);

  useEffect(() => {
    if (!componentHash) {
      setBlastRadiusCounts({});
      setOrganizationsUsage({ organizations: [], total: 0, status: 'error' });
      setApplicationsUsage({ applications: [], total: 0, status: 'error' });
      return undefined;
    }

    const controller = new AbortController();
    setBlastRadiusCounts({});
    setOrganizationsUsage({ organizations: [], total: 0, status: 'loading' });
    setApplicationsUsage({ applications: [], total: 0, status: 'loading' });

    // Pin URL Path ids so deep links resolve even when the target is past page 0.
    const organizationIncludeIds = pathSelection.organizationId
      ? [pathSelection.organizationId]
      : undefined;
    const applicationIncludeIds = pathSelection.applicationId
      ? [pathSelection.applicationId]
      : undefined;

    void Promise.allSettled([
      fetchComponentUsageOrganizations(componentHash, 0, COMPONENT_USAGE_PAGE_SIZE, controller.signal, {
        includeIds: organizationIncludeIds,
      }),
      fetchComponentUsageApplications(componentHash, 0, COMPONENT_USAGE_PAGE_SIZE, controller.signal, {
        includeIds: applicationIncludeIds,
        organizationId: pathSelection.organizationId,
      }),
      fetchViolationCount(componentHash, controller.signal),
    ]).then(([organizationsResult, applicationsResult, violationsResult]) => {
      if (controller.signal.aborted) return;
      setOrganizationsUsage(
        organizationsResult.status === 'fulfilled'
          ? {
              organizations: organizationsResult.value.organizations,
              total: organizationsResult.value.total,
              status: 'ready',
            }
          : { organizations: [], total: 0, status: 'error' }
      );
      setApplicationsUsage(
        applicationsResult.status === 'fulfilled'
          ? {
              applications: applicationsResult.value.applications,
              total: applicationsResult.value.total,
              status: 'ready',
            }
          : { applications: [], total: 0, status: 'error' }
      );
      setBlastRadiusCounts({
        organizations: organizationsResult.status === 'fulfilled' ? organizationsResult.value.total : undefined,
        applications: applicationsResult.status === 'fulfilled' ? applicationsResult.value.total : undefined,
        violations: violationsResult.status === 'fulfilled' ? violationsResult.value : undefined,
      });
    });

    return () => controller.abort();
    // Re-seed only on hash change; PathSwitcher owns subsequent search/pin fetches.
    // pathSelection is captured for the initial URL pin only.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [componentHash]);

  const displayName =
    details?.displayName?.trim() || details?.packageUrl?.trim() || truncatedComponentHash(componentHash);

  const pathParamsForNavigation = useCallback(
    (next: EstateComponentPathSelection) => ({
      componentHash,
      organizationId: next.organizationId,
      applicationId: next.applicationId,
      reportId: next.reportId,
    }),
    [componentHash]
  );

  const handlePathChange = useCallback(
    (next: EstateComponentPathSelection): void => {
      setResolvedPathSelection(next);
      stateService.go(
        state.name,
        pathParamsForNavigation(next),
        { inherit: false, notify: false, location: 'replace' }
      );
    },
    [pathParamsForNavigation, state.name, stateService]
  );

  const handleTabChange = (next: string): void => {
    if (!(ESTATE_COMPONENT_TAB_IDS as readonly string[]).includes(next)) return;
    stateService.go(estateComponentDetailStateNameForTab(next as EstateComponentTab), pathParamsForNavigation(resolvedPathSelection));
  };

  const shellContext = useMemo(
    () => ({
      componentHash,
      hdsStatus,
      details,
      displayName,
      blastRadiusCounts,
      pathSelection: resolvedPathSelection,
      retryHds: startHdsLoad,
    }),
    [blastRadiusCounts, componentHash, details, displayName, hdsStatus, resolvedPathSelection, startHdsLoad]
  );

  // Reuse PathSwitcher page-0 rows already in memory — no extra fetch for breadcrumb names.
  const pinnedApplication = useMemo(() => {
    const applicationId = resolvedPathSelection.applicationId?.trim();
    if (!applicationId) {
      return undefined;
    }
    return applicationsUsage.applications.find((app) => app.applicationId === applicationId);
  }, [applicationsUsage.applications, resolvedPathSelection.applicationId]);

  const breadcrumb = useMemo(() => {
    const applicationPublicId = pinnedApplication?.applicationPublicId?.trim();
    const applicationLabel =
      pinnedApplication?.applicationName?.trim()
      || applicationPublicId
      || resolvedPathSelection.applicationId;

    return (
      <Flex align="center" gap="2" data-testid="nosc-estate-component-breadcrumb">
        <RadixLink size="2" color="gray" href={stateService.href(NEXUS_ONE_COMPONENTS_STATE_NAME)}>
          <Flex align="center" gap="1">
            <ActionIcons.Back size={14} />
            Components
          </Flex>
        </RadixLink>
        {applicationPublicId && applicationLabel && (
          <>
            <Text size="2" color="gray">
              /
            </Text>
            <RadixLink
              size="2"
              color="gray"
              href={stateService.href('nexusOneApplicationsDetail.overview', {
                publicId: applicationPublicId,
              })}
              data-testid="nosc-estate-component-breadcrumb-application"
            >
              {applicationLabel}
            </RadixLink>
          </>
        )}
        <Text size="2" color="gray">
          /
        </Text>
        <Text size="2" weight="medium">
          {displayName}
        </Text>
      </Flex>
    );
  }, [displayName, pinnedApplication, resolvedPathSelection.applicationId, stateService]);

  const header = useMemo(
    () => (
      <Flex direction="column" gap="4">
        {hdsStatus === 'loading' && <LoadingSkeleton height={72} data-testid="nosc-estate-component-header-loading" />}
        {hdsStatus === 'error' && (
          <Flex direction="column" gap="3" align="start" data-testid="nosc-estate-component-header-error">
            <PageHeading data-testid="nosc-estate-component-name">{displayName}</PageHeading>
            <Text size="2" color="gray" style={{ fontFamily: 'var(--code-font-family)' }}>
              {componentHash}
            </Text>
            <Text size="2" color="red">
              Component catalog details could not be loaded. Policy Violations and Applications remain available.
            </Text>
            <Button size="2" variant="soft" onClick={startHdsLoad} data-testid="nosc-estate-component-header-retry">
              Retry details
            </Button>
          </Flex>
        )}
        {(hdsStatus === 'ready' || hdsStatus === 'empty') && (
          <Flex direction="column" gap="2" data-testid="nosc-estate-component-header">
            <PageHeading data-testid="nosc-estate-component-name">{displayName}</PageHeading>
            <Flex gap="2" wrap="wrap" align="center">
              {details?.format && (
                <Badge size="1" color="gray" variant="soft" data-testid="nosc-estate-component-ecosystem">
                  Ecosystem: {details.format}
                </Badge>
              )}
              {details?.matchState && (
                <Badge size="1" color="gray" variant="soft" data-testid="nosc-estate-component-match-state">
                  {details.matchState}
                </Badge>
              )}
              <Text size="2" color="gray" style={{ fontFamily: 'var(--code-font-family)' }}>
                {componentHash}
              </Text>
            </Flex>
          </Flex>
        )}
        <BlastRadiusCountBadges counts={blastRadiusCounts} />
        <EstateComponentPathSwitcher
          componentHash={componentHash}
          organizationsUsage={organizationsUsage}
          applicationsUsage={applicationsUsage}
          pathSelection={pathSelection}
          onPathChange={handlePathChange}
        />
      </Flex>
    ),
    [
      applicationsUsage,
      blastRadiusCounts,
      componentHash,
      details,
      displayName,
      handlePathChange,
      hdsStatus,
      organizationsUsage,
      pathSelection,
      resolvedPathSelection,
      startHdsLoad,
    ]
  );

  return (
    <EntityDetailLayout
      breadcrumb={breadcrumb}
      header={header}
      context={null}
      tabs={ESTATE_COMPONENT_TABS}
      activeTab={activeTab}
      onTabChange={handleTabChange}
      mainTestId="nosc-estate-component-detail-page"
      testIdPrefix="nosc-estate-component"
    >
      <EstateComponentDetailShellProvider value={shellContext}>
        <UIView />
      </EstateComponentDetailShellProvider>
    </EntityDetailLayout>
  );
}
