/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import { Badge, Box, Card, Flex, Heading, Tabs, Text, Theme } from '@radix-ui/themes';
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
 *     for every tab (runs once per shell mount, NOT per tab).
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

/** Live badge count for a tab strip trigger. Reads `results.length` from the same Redux selectors the
 *  Classic tables consume. Renders nothing when the slice has no `results` array yet (initial / loading
 *  / errored) so the trigger doesn't flash a "0" before the first fetch completes. */
function TabBadge({
  count,
  testId,
}: {
  readonly count: number | null;
  readonly testId: string;
}): JSX.Element | null {
  if (count === null) return null;
  return (
    <Badge variant="soft" color="gray" ml="2" data-testid={testId}>
      {count}
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

  // Tab-strip badge counts. The Redux slice initializes `results` to `null`; a successful fetch
  // replaces it with an array. Treat the null state as "nothing to show yet" so the badge is hidden
  // until there's a real number to display (avoids a "0" flash on first mount before the table loads).
  const violationsResults = useSelector(selectViolationResults);
  const componentsResults = useSelector(selectComponentResults);
  const applicationsResults = useSelector(selectApplicationResults);
  const waiversResults = useSelector(selectWaiversResults);
  const violationsCount: number | null = Array.isArray(violationsResults?.results)
    ? violationsResults.results.length
    : null;
  const componentsCount: number | null = Array.isArray(componentsResults?.results)
    ? componentsResults.results.length
    : null;
  const applicationsCount: number | null = Array.isArray(applicationsResults?.results)
    ? applicationsResults.results.length
    : null;
  const waiversCount: number | null = Array.isArray(waiversResults?.results)
    ? waiversResults.results.length
    : null;

  // One-shot `loadFilter()` dispatch on first shell mount so the shared `dashboardFilter` slice is
  // populated for ALL tabs (the filter drawer each tab mounts shows options instead of a spinner).
  // The shell mounts once per dashboard visit, so this runs once regardless of which tab is active.
  // The ref guards against React StrictMode's double-invoke; the action is also idempotent.
  const filterLoadDispatchedRef = useRef(false);
  useEffect(() => {
    if (filterLoadDispatchedRef.current) return;
    filterLoadDispatchedRef.current = true;
    dispatch(loadFilter());
  }, [dispatch]);

  const handleTabChange = (next: string): void => {
    if (!(TAB_IDS as readonly string[]).includes(next)) return;
    router.stateService.go(`${DASHBOARD_STATE_PREFIX}.${next}`);
  };

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
            <Heading size="6">Dashboard</Heading>
            <Text size="2" color="gray">
              Monitor policy violations, components, applications, and waivers across your organization.
            </Text>
          </Flex>

          <Tabs.Root value={activeTab} onValueChange={handleTabChange} data-testid="nosc-dashboard-tabs">
            <Tabs.List size="2">
              <Tabs.Trigger value="overview" data-testid="nosc-dashboard-tab-overview">
                Overview
                {/* No badge: Overview is a tile grid (Apps Scanned, Severity, Legal Obligations, Top
                 *  Policy Violations, Risk Over Time), not a violation list, so a violations count here
                 *  is misleading. Re-add with an Overview-specific metric only if design asks. */}
              </Tabs.Trigger>
              <Tabs.Trigger value="violations" data-testid="nosc-dashboard-tab-violations">
                Violations
                <TabBadge count={violationsCount} testId="nosc-dashboard-tab-badge-violations" />
              </Tabs.Trigger>
              <Tabs.Trigger value="components" data-testid="nosc-dashboard-tab-components">
                Components
                <TabBadge count={componentsCount} testId="nosc-dashboard-tab-badge-components" />
              </Tabs.Trigger>
              <Tabs.Trigger value="applications" data-testid="nosc-dashboard-tab-applications">
                Applications
                <TabBadge count={applicationsCount} testId="nosc-dashboard-tab-badge-applications" />
              </Tabs.Trigger>
              <Tabs.Trigger value="waivers" data-testid="nosc-dashboard-tab-waivers">
                Waivers
                <TabBadge count={waiversCount} testId="nosc-dashboard-tab-badge-waivers" />
              </Tabs.Trigger>
            </Tabs.List>

            <Tabs.Content value={activeTab} data-testid={`nosc-dashboard-tab-content-${activeTab}`}>
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
            </Tabs.Content>
          </Tabs.Root>
        </Box>
      </div>
    </Theme>
  );
}
