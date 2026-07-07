/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import { Badge, Box, Card, Flex, Heading, Tabs, Text, Theme } from '@radix-ui/themes';
import { PageHeading } from '@sonatype/nexus-one-components';
import { useDispatch, useSelector } from 'react-redux';
import { UIView, useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { ErrorBoundary } from 'react-error-boundary';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { loadFilter } from 'MainRoot/dashboard/filter/dashboardFilterActions';
import {
  selectApplicationResults,
  selectComponentResults,
  selectViolationResults,
  selectWaiversResults,
} from 'MainRoot/dashboard/dashboardSelectors';
import {
  loadApplicationResults,
  loadComponentResults,
  loadViolationResults,
  loadWaiverResults,
} from 'MainRoot/dashboard/results/dashboardResultsActions';
import { formatDashboardTabBadge } from 'MainRoot/nosc/dashboard/dashboardTabBadge';

import '@radix-ui/themes/styles.css';

/**
 * Nexus One Dashboard shell (CLM-39992 + CLM-39641 review follow-up).
 *
 * This is the parent route component for the abstract `nexusOneDashboard` state. UI-Router owns tab
 * navigation: each tab is a child state (`nexusOneDashboard.{overview,violations,components,
 * applications,waivers}`) and the active tab's content renders into the nested `<UIView />`. The page
 * no longer hand-rolls hash parsing / `history.pushState` / `hashchange` listeners — the previous
 * approach re-invented what UI-Router already provides (per Ross's review on this PR).
 *
 * Owns (shell-level, shared across all tabs):
 *   - The visible tab strip (with live badge counts read from the same Redux selectors the Classic
 *     tables read), driving navigation via `router.stateService.go(...)`.
 *   - A single `loadFilter()` dispatch on first mount so the shared `dashboardFilter` slice is loaded
 *     for every tab (runs once per shell mount, NOT per tab). We intentionally do NOT call
 *     `applyDefaultFilter()` — that action persists via PUT and would overwrite the user's active
 *     Classic dashboard filter on every Preview visit.
 *   - A `react-error-boundary` around the `<UIView />` so a thrown render error in one tab shows an
 *     inline fallback while the tab strip stays navigable (AT-D1-002). It resets on tab change.
 */

const TAB_IDS = ['overview', 'violations', 'components', 'applications', 'waivers'] as const;
type TabId = (typeof TAB_IDS)[number];

const DEFAULT_TAB: TabId = 'overview';

const DASHBOARD_STATE_PREFIX = 'nexusOneDashboard';

/** Derive the active tab id from the current UI-Router state name. The child states are named
 *  `nexusOneDashboard.{tab}`; the abstract parent (or an unrecognized state) falls back to Overview. */
function tabFromStateName(stateName: string | undefined): TabId {
  if (!stateName || !stateName.startsWith(DASHBOARD_STATE_PREFIX + '.')) {
    return DEFAULT_TAB;
  }
  const slug = stateName.slice(DASHBOARD_STATE_PREFIX.length + 1);
  return (TAB_IDS as readonly string[]).includes(slug) ? (slug as TabId) : DEFAULT_TAB;
}

interface TabErrorFallbackProps {
  readonly tabId: TabId;
  readonly message: string | null;
}

/** Inline fallback rendered by the per-tab {@link ErrorBoundary}. Keeps the `nosc-dashboard-{tab}-
 *  error-boundary` testid so tab-isolation assertions (AT-D1-002) stay stable. */
function TabErrorFallback({ tabId, message }: TabErrorFallbackProps): JSX.Element {
  return (
    <Card mt="4" data-testid={`nosc-dashboard-${tabId}-error-boundary`}>
      <Flex direction="column" gap="2" p="4" align="start">
        <Heading size="3" color="red">
          This tab failed to load.
        </Heading>
        <Text size="2" color="gray">
          {message ?? 'Unknown render error.'}
        </Text>
        <Text size="1" color="gray">
          Other tabs remain available — pick one from the strip above or the LeftNav.
        </Text>
      </Flex>
    </Card>
  );
}

/** Live badge count for a tab strip trigger. Renders nothing while the slice is still unloaded. */
function TabBadge({
  label,
  testId,
}: {
  readonly label: string | null;
  readonly testId: string;
}): JSX.Element | null {
  if (label === null) return null;
  return (
    <Badge variant="soft" color="gray" ml="2" data-testid={testId}>
      {label}
    </Badge>
  );
}

export default function PreviewDashboardPage(): JSX.Element {
  const { effectiveTheme } = useNoscTheme();
  const offsets = usePreviewShellOffsets();
  const dispatch = useDispatch();
  const router = useRouter();
  const { state } = useCurrentStateAndParams();
  const activeTab = tabFromStateName(state?.name);

  const violationsResults = useSelector(selectViolationResults);
  const componentsResults = useSelector(selectComponentResults);
  const applicationsResults = useSelector(selectApplicationResults);
  const waiversResults = useSelector(selectWaiversResults);
  const violationsBadge = formatDashboardTabBadge(violationsResults);
  const componentsBadge = formatDashboardTabBadge(componentsResults);
  const applicationsBadge = formatDashboardTabBadge(applicationsResults);
  const waiversBadge = formatDashboardTabBadge(waiversResults);

  // The active tab's own table dispatches its `load*Results()` on mount, so the
  // shell must NOT also eager-fetch that slice or the active tab double-fires the
  // request on first paint (`loadResults` has no in-flight guard). We read the
  // active tab through a ref so the post-`loadFilter` callback below sees the
  // current tab even if it changed while the filter request was in flight.
  const activeTabRef = useRef(activeTab);
  activeTabRef.current = activeTab;

  // One-shot `loadFilter()` dispatch on first shell mount so the shared `dashboardFilter` slice is
  // populated for ALL tabs (the filter drawer each tab mounts shows options instead of a spinner).
  // After a successful load, eagerly fetch the INACTIVE tab slices so strip badges reflect the
  // hydrated filter; the active tab is owned by its mounted table (see above). The ref guards
  // against React StrictMode's double-invoke.
  const filterLoadDispatchedRef = useRef(false);
  useEffect(() => {
    if (filterLoadDispatchedRef.current) return;
    filterLoadDispatchedRef.current = true;
    // `loadFilter()` handles its own failures (it dispatches `loadFilterFailed`
    // and resolves), so the returned promise never rejects — we branch on the
    // resulting `dashboardFilter.loadError` state instead of a `.catch()`.
    void dispatch(loadFilter()).then(() => {
      dispatch((_, getState) => {
        const filterState = getState().dashboardFilter;
        if (filterState?.loadError || filterState?.loadErrorFilterName) {
          return;
        }
        if (filterState?.loading || filterState?.needsAcknowledgement) {
          return;
        }
        const active = activeTabRef.current;
        // The landing grid does not render tab-strip badges — skip the four eager
        // classic-tab result loads so landing issues one metrics POST, not five.
        if (active === DEFAULT_TAB) {
          return;
        }
        if (active !== 'components') dispatch(loadComponentResults());
        if (active !== 'applications') dispatch(loadApplicationResults());
        if (active !== 'violations') dispatch(loadViolationResults());
        if (active !== 'waivers') dispatch(loadWaiverResults());
      });
    });
  }, [dispatch]);

  const handleTabChange = (next: string): void => {
    if (!(TAB_IDS as readonly string[]).includes(next)) return;
    router.stateService.go(`${DASHBOARD_STATE_PREFIX}.${next}`);
  };

  // The landing (overview) is the metric-card grid (CLM-40905) — a clean, tab-free view. The tab
  // strip only renders for the classic tabbed tables (violations/components/applications/waivers),
  // which stay reachable as a fallback. The child <UIView> still renders the active state's
  // component either way; on the landing it's wrapped without the tab chrome.
  const isLandingGrid = activeTab === DEFAULT_TAB;

  const tabbedUiView = (
    <ErrorBoundary
      resetKeys={[activeTab]}
      fallbackRender={({ error }) => (
        <TabErrorFallback
          tabId={activeTab}
          message={error instanceof Error ? error.message : String(error)}
        />
      )}
    >
      <UIView />
    </ErrorBoundary>
  );

  return (
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
    >
      <div
        style={{
          position: 'fixed',
          ...offsets,
          right: 0,
          bottom: 0,
          overflowY: 'auto',
          overflowX: 'hidden',
          backgroundColor: 'var(--gray-1)',
        }}
      >
        <Box p="6" data-testid="nosc-dashboard-page" data-active-tab={activeTab}>
          <Flex direction="column" gap="2" mb="4">
            <PageHeading>Dashboard</PageHeading>
            <Text size="2" color="gray">
              Monitor policy violations, components, applications, and waivers across your organization.
            </Text>
          </Flex>

          {isLandingGrid ? (
            tabbedUiView
          ) : (
            <Tabs.Root value={activeTab} onValueChange={handleTabChange} data-testid="nosc-dashboard-tabs">
              <Tabs.List size="2">
                <Tabs.Trigger value="overview" data-testid="nosc-dashboard-tab-overview">
                  Overview
                  {/* Overview is the metric-card grid landing (CLM-40905); no count badge here. */}
                </Tabs.Trigger>
                <Tabs.Trigger value="violations" data-testid="nosc-dashboard-tab-violations">
                  Violations
                  <TabBadge label={violationsBadge} testId="nosc-dashboard-tab-badge-violations" />
                </Tabs.Trigger>
                <Tabs.Trigger value="components" data-testid="nosc-dashboard-tab-components">
                  Components
                  <TabBadge label={componentsBadge} testId="nosc-dashboard-tab-badge-components" />
                </Tabs.Trigger>
                <Tabs.Trigger value="applications" data-testid="nosc-dashboard-tab-applications">
                  Applications
                  <TabBadge label={applicationsBadge} testId="nosc-dashboard-tab-badge-applications" />
                </Tabs.Trigger>
                <Tabs.Trigger value="waivers" data-testid="nosc-dashboard-tab-waivers">
                  Waivers
                  <TabBadge label={waiversBadge} testId="nosc-dashboard-tab-badge-waivers" />
                </Tabs.Trigger>
              </Tabs.List>

              <Tabs.Content value={activeTab} data-testid={`nosc-dashboard-tab-content-${activeTab}`}>
                {tabbedUiView}
              </Tabs.Content>
            </Tabs.Root>
          )}
        </Box>
      </div>
    </Theme>
  );
}
