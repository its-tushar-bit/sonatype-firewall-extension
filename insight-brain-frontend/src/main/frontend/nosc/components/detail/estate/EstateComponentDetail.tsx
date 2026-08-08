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
import type { EstateComponentHdsStatus } from './estateComponentDetailContext';
import { fetchEstateComponentDetails } from './estateComponentDetailsApi';
import type { EstateComponentDetails } from './estateComponentDetailsApi';
import { EstateComponentPathSwitcher } from './EstateComponentPathSwitcher';
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
import type { ComponentUsageApplicationRow } from './estateComponentUsageApi';
import { buildViolationsListRequest } from 'MainRoot/nosc/violations/violationsListApi';
import type { ViolationsListResponse } from 'MainRoot/nosc/violations/violationListTypes';
import { getViolationsListUrl } from 'MainRoot/util/CLMLocation';

const HEADER_COUNT_PAGE_SIZE = 1;

const ESTATE_COMPONENT_TABS = [
  { value: 'overview', label: 'Overview', testId: 'nosc-estate-component-tab-overview' },
  { value: 'legal', label: 'Legal', testId: 'nosc-estate-component-tab-legal' },
  { value: 'violations', label: 'Policy Violations', testId: 'nosc-estate-component-tab-violations' },
  { value: 'applications', label: 'Applications', testId: 'nosc-estate-component-tab-applications' },
  {
    value: 'organizations',
    label: 'Organizations',
    testId: 'nosc-estate-component-tab-organizations',
  },
] as const;

function paramAsString(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}

type BlastRadiusCounts = {
  readonly applications?: number;
  readonly organizations?: number;
  readonly violations?: number;
};

type ApplicationsUsageState = {
  readonly applications: ReadonlyArray<ComponentUsageApplicationRow>;
  readonly total: number;
  readonly status: 'loading' | 'ready' | 'error';
};

function formatCount(value: number | undefined, singular: string, plural: string): string {
  if (typeof value !== 'number') {
    return `${plural}: -`;
  }
  return `${value} ${value === 1 ? singular : plural}`;
}

function EstateComponentBlastRadiusCounts({ counts }: { readonly counts: BlastRadiusCounts }): ReactElement {
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

  const [hdsStatus, setHdsStatus] = useState<EstateComponentHdsStatus>('loading');
  const [details, setDetails] = useState<EstateComponentDetails | null>(null);
  const [blastRadiusCounts, setBlastRadiusCounts] = useState<BlastRadiusCounts>({});
  const [applicationsUsage, setApplicationsUsage] = useState<ApplicationsUsageState>({
    applications: [],
    total: 0,
    status: 'loading',
  });
  const hdsAbortRef = useRef<AbortController | null>(null);

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
      setApplicationsUsage({ applications: [], total: 0, status: 'error' });
      return undefined;
    }

    const controller = new AbortController();
    setBlastRadiusCounts({});
    setApplicationsUsage({ applications: [], total: 0, status: 'loading' });

    void Promise.allSettled([
      fetchComponentUsageApplications(componentHash, 0, COMPONENT_USAGE_PAGE_SIZE, controller.signal),
      fetchComponentUsageOrganizations(componentHash, 0, HEADER_COUNT_PAGE_SIZE, controller.signal).then(
        (response) => response.total
      ),
      fetchViolationCount(componentHash, controller.signal),
    ]).then(([applicationsResult, organizationsResult, violationsResult]) => {
      if (controller.signal.aborted) return;
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
        applications: applicationsResult.status === 'fulfilled' ? applicationsResult.value.total : undefined,
        organizations: organizationsResult.status === 'fulfilled' ? organizationsResult.value : undefined,
        violations: violationsResult.status === 'fulfilled' ? violationsResult.value : undefined,
      });
    });

    return () => controller.abort();
  }, [componentHash]);

  const displayName =
    details?.displayName?.trim() || details?.packageUrl?.trim() || truncatedComponentHash(componentHash);

  const handleTabChange = (next: string): void => {
    if (!(ESTATE_COMPONENT_TAB_IDS as readonly string[]).includes(next)) return;
    stateService.go(estateComponentDetailStateNameForTab(next as EstateComponentTab), {
      componentHash,
    });
  };

  const shellContext = useMemo(
    () => ({
      componentHash,
      hdsStatus,
      details,
      displayName,
      retryHds: startHdsLoad,
    }),
    [componentHash, details, displayName, hdsStatus, startHdsLoad]
  );

  const breadcrumb = useMemo(
    () => (
      <Flex align="center" gap="2" data-testid="nosc-estate-component-breadcrumb">
        <RadixLink size="2" color="gray" href={stateService.href(NEXUS_ONE_COMPONENTS_STATE_NAME)}>
          <Flex align="center" gap="1">
            <ActionIcons.Back size={14} />
            Components
          </Flex>
        </RadixLink>
        <Text size="2" color="gray">
          /
        </Text>
        <Text size="2" weight="medium">
          {displayName}
        </Text>
      </Flex>
    ),
    [displayName, stateService]
  );

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
              Component catalog details could not be loaded. Policy Violations, Applications, and Organizations remain
              available.
            </Text>
            <Button size="2" variant="soft" onClick={startHdsLoad} data-testid="nosc-estate-component-header-retry">
              Retry details
            </Button>
          </Flex>
        )}
        {(hdsStatus === 'ready' || hdsStatus === 'empty') && (
          <Flex direction="column" gap="2" data-testid="nosc-estate-component-header">
            <PageHeading data-testid="nosc-estate-component-name">{displayName}</PageHeading>
            <Flex gap="3" wrap="wrap" align="center">
              <Text size="2" color="gray" style={{ fontFamily: 'var(--code-font-family)' }}>
                {componentHash}
              </Text>
              {details?.format && (
                <Badge size="1" color="gray" variant="soft">
                  {details.format}
                </Badge>
              )}
              {details?.matchState && (
                <Badge size="1" color="gray" variant="soft">
                  {details.matchState}
                </Badge>
              )}
            </Flex>
          </Flex>
        )}
        <EstateComponentBlastRadiusCounts counts={blastRadiusCounts} />
        <EstateComponentPathSwitcher componentHash={componentHash} applicationsUsage={applicationsUsage} />
      </Flex>
    ),
    [
      applicationsUsage,
      blastRadiusCounts,
      componentHash,
      details?.format,
      details?.matchState,
      displayName,
      hdsStatus,
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
