/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Box, Flex, IconButton, ScrollArea, Separator, Tooltip } from '@radix-ui/themes';
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { comingSoonHref } from 'MainRoot/nosc/comingSoon';
import { embeddedHref } from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
import {
  LEGAL_APPLICATIONS_DASHBOARD_URL,
  LEGAL_COMPONENTS_DASHBOARD_URL,
} from 'MainRoot/legal/dashboard/legalDashboardRouteData';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import { useLeftNavCollapsed } from 'MainRoot/nosc/shell/useLeftNavCollapsed';
import {
  LEFT_NAV_COLLAPSED_WIDTH_PX,
  LEFT_NAV_EXPANDED_WIDTH_PX,
  TOP_NAV_HEIGHT_PX,
} from 'MainRoot/nosc/shell/previewShellLayout';
import {
  selectIsAdvancedLegalPackSupported,
  selectIsApiPageSupported,
  selectIsDashboardSupported,
  selectIsHostedRepositoryEvaluationEnabled,
  selectIsIntegratedEnterpriseReportingSupported,
  selectIsOrgsAndAppsEnabled,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsSuccessMetricsEnabled } from 'MainRoot/configuration/successMetricsConfiguration/successMetricsConfigurationSelectors';
import {
  selectIsLicensed,
  selectIsSbomManagerOnlyLicense,
  selectIsFirewallOnlyLicense,
  selectLoadingProducts,
} from 'MainRoot/productFeatures/productLicenseSelectors';
import { selectIsLoggedIn } from 'MainRoot/user/userSessionSelectors';
import {
  selectIsStandaloneFirewall,
  selectIsStandaloneDeveloper,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';

const COLLAPSED_WIDTH = LEFT_NAV_COLLAPSED_WIDTH_PX + 'px';
const EXPANDED_WIDTH = LEFT_NAV_EXPANDED_WIDTH_PX + 'px';
const TOP_OFFSET = TOP_NAV_HEIGHT_PX + 'px';

// Active hrefs are extra paths (beyond an entry's own href) that keep it highlighted in the rail when in-page navigation or a
// redirect lands the user somewhere other than that href but still inside the entry's experience.
const LEGAL_ACTIVE_HREFS = Object.freeze([LEGAL_APPLICATIONS_DASHBOARD_URL, LEGAL_COMPONENTS_DASHBOARD_URL, '/legal']);
const REPORTING_ACTIVE_HREFS = Object.freeze(['/enterpriseReportingDashboard', '/reports/react2shell']);
// '/management' is a prefix match (see hrefMatches) covering the whole embedded Orgs and Policies
// tree (orgsAndPoliciesStates.ts). The entry redirects onto
// `/management/view/organization/ROOT_ORGANIZATION_ID`, so the rail must stay lit across all of
// `/management/...`, not just the entry href it advertises.
const MANAGEMENT_ACTIVE_HREFS = Object.freeze(['/management']);

/**
 * Nexus One Preview LeftNav.
 *
 * Item set and order match the Phase-1 final IA agreed with UX (ground
 * truth: `nexusone-ux-prototype`'s `getLifecycleV1NavItems()`), not
 * Classic's IqSidebarNav order.
 *
 * Permissioning hooks the same Redux selectors `NavigationContainer.jsx`
 * uses for IqSidebarNav, so an unlicensed / SBOM-only / Firewall-only
 * tenant gets the same module visibility as in Classic.
 *
 * Hrefs all land inside the Nexus One Preview surface (`/preview/*`).
 * Per CLM-39640 review: clicking a Nexus One nav entry should keep
 * the user inside Nexus One: either landing on a native Preview page,
 * a Classic page mounted in-shell (embedded Classic mount), or a
 * Coming Soon stub that has a "Continue in Classic" escape hatch on
 * the page itself. The escape hatch belongs to the page, not the nav
 * click.
 *
 * Per CLM-42168: Advanced Search and Vulnerability Lookup are no
 * longer separate LeftNav entries — Global Search (the top-nav
 * omnibar) and the unified search results surface at `/preview/search`
 * replace both as the single entry point.
 *
 * Stub-target mapping (LeftNav item → target), in display order:
 *   Dashboard            → /preview/dashboard            (native)
 *   Applications         → /preview/applications         (native)
 *   Components           → /preview/components            (native; Components list)
 *   Hosted Repos         → /repositories                 (native embedded
 *                          Classic mount)
 *   Legal                → /legal                        (native embed; redirects
 *                          to Legal Applications dashboard. Every reachable
 *                          deep link also mounts in-shell — application details,
 *                          component overview, attribution reports, and the
 *                          copyright/notice/license-file/license-details
 *                          families — none of it exits to Classic)
 *   Orgs & Policies      → /orgs-and-policies            (native embed; redirects
 *                          to the Classic root-org summary mounted in-shell.
 *                          Every reachable /management/* sub-route also mounts
 *                          in-shell — see MANAGEMENT_ACTIVE_HREFS)
 *   Violations           → /preview/violations            (native; PreviewViolationsList)
 *   Vulnerabilities      → /preview/vulnerabilities       (native; PreviewVulnerabilitiesList)
 *   Waivers              → /preview/waivers               (native)
 *   --- divider ---
 *   Success Metrics      → /success-metrics               (native Classic embed)
 *   Enterprise/Operational Reporting → /reports          (embedded Classic mount;
 *                          gate-switches between the Classic Enterprise and
 *                          Operational Reporting pages on
 *                          integrated-enterprise-reporting support)
 *   --- divider ---
 *   API                  → /api                           (embedded Classic
 *                          mount; native ApiPage, no Coming Soon stub)
 *   Settings             → /coming-soon/settings          (Coming Soon)
 */

function readHashPath() {
  const rawHash = typeof window !== 'undefined' ? window.location.hash : '';
  const withoutHash = rawHash.startsWith('#') ? rawHash.slice(1) : rawHash;
  const qIndex = withoutHash.indexOf('?');
  return qIndex === -1 ? withoutHash : withoutHash.slice(0, qIndex);
}

/** Hashchange-tracking hook returning the current `#path` segment. */
function useCurrentHashPath() {
  const [path, setPath] = useState(readHashPath());
  useEffect(() => {
    const onHashChange = () => setPath(readHashPath());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);
  return path;
}

function hrefMatches(currentPath, href) {
  return currentPath === href || currentPath.startsWith(href + '/');
}

/**
 * Active-route check — match the item's own href (exact or descendant prefix),
 * or any of its `activeHrefs` alternates. `activeHrefs` covers embedded-mount
 * items whose in-page navigation (e.g. tab switches) lands on a different path
 * than the nav entry's own href but should still keep the rail entry highlighted.
 */
function isPathActive(currentPath, item) {
  if (!item.href) return false;
  if (hrefMatches(currentPath, item.href)) return true;
  if (item.activeHrefs) {
    return item.activeHrefs.some((href) => hrefMatches(currentPath, href));
  }
  return false;
}

/**
 * A single nav item row. Renders an anchor that navigates via hash
 * (works with IQ's existing ui-router setup without needing useSref).
 * When collapsed, wraps in a Radix Tooltip so the label remains
 * discoverable.
 */
function NavItem({ id, label, Icon, href, isActive, isCollapsed }) {
  const fullHref = bundleIndexUrl('nexus-one', href);

  const content = (
    <a
      href={fullHref}
      data-testid={`nosc-leftnav-${id}`}
      aria-current={isActive ? 'page' : undefined}
      style={{
        display: 'block',
        textDecoration: 'none',
        color: 'inherit',
        borderRadius: 'var(--radius-3)',
        backgroundColor: isActive ? 'var(--accent-4)' : 'transparent',
        transition: 'background-color 100ms ease-out',
      }}
      onMouseEnter={(e) => {
        if (!isActive) e.currentTarget.style.backgroundColor = 'var(--gray-a3)';
      }}
      onMouseLeave={(e) => {
        if (!isActive) e.currentTarget.style.backgroundColor = 'transparent';
      }}
    >
      <Flex
        align="center"
        justify={isCollapsed ? 'center' : 'start'}
        gap="3"
        px="3"
        py="2"
        style={{ minHeight: '40px' }}
      >
        <Icon size={18} color={isActive ? 'var(--accent-11)' : 'var(--gray-11)'} />
        {!isCollapsed && (
          <span
            style={{
              fontSize: '14px',
              fontWeight: isActive ? 600 : 400,
              color: isActive ? 'var(--accent-12)' : 'var(--gray-12)',
            }}
          >
            {label}
          </span>
        )}
      </Flex>
    </a>
  );

  if (isCollapsed) {
    return (
      <Tooltip content={label} side="right">
        {content}
      </Tooltip>
    );
  }
  return content;
}

/**
 * Appends a divider-delimited group to the rail. The group's first item gets a
 * separator only when the group is non-empty AND something already precedes it.
 * This avoids two orphaned-divider cases: a divider inside an otherwise-empty
 * group, and a divider stranded at the top of the rail when every item above the
 * group is gated off (e.g. unlicensed + dashboard disabled, with Success Metrics
 * or API still enabled).
 */
function pushGroup(items, groupItems) {
  if (groupItems.length === 0) {
    return;
  }
  groupItems[0].separator = items.length > 0;
  items.push(...groupItems);
}

/**
 * Build the visible nav-item list given the current Redux feature/license
 * flags, matching the Phase-1 final IA order.
 *
 * Returns an array of `{ id, label, Icon, href, activeHrefs?, separator? }`
 * rows in display order. `separator: true` renders a divider immediately
 * above that item — placed on the item *after* a divider (rather than
 * tracked on the item before) so a feature-gated-off item above never
 * leaves an orphaned divider.
 */
function buildNavItems(flags) {
  const {
    isLicensed,
    isDashboardAvailable,
    isSuccessMetricsEnabled,
    isLegalEnabled,
    isApiPageEnabled,
    isOrgsAndAppsEnabled,
    isIntegratedEnterpriseReportingSupported,
    isHostedRepositoryEvaluationEnabled,
  } = flags;

  const items = [];

  if (isDashboardAvailable) {
    items.push({
      id: 'dashboard',
      label: 'Dashboard',
      Icon: DomainIcons.Home,
      href: '/dashboard',
    });
  }
  if (isLicensed && isOrgsAndAppsEnabled) {
    items.push({
      id: 'applications',
      label: 'Applications',
      Icon: DomainIcons.Applications,
      href: '/applications',
    });
  }
  // Components, Violations, and Waivers use native Preview list pages.
  if (isLicensed && isOrgsAndAppsEnabled) {
    items.push({
      id: 'components',
      label: 'Components',
      Icon: DomainIcons.Component,
      href: '/components',
    });
  }
  if (isLicensed && isHostedRepositoryEvaluationEnabled) {
    items.push({
      id: 'hosted-repos',
      label: 'Hosted Repos',
      Icon: DomainIcons.HostedRepos,
      href: '/repositories',
    });
  }
  if (isLicensed && isLegalEnabled) {
    items.push({
      id: 'legal',
      label: 'Legal',
      Icon: DomainIcons.Legal,
      href: embeddedHref('legal'),
      activeHrefs: LEGAL_ACTIVE_HREFS,
    });
  }
  if (isLicensed) {
    items.push({
      id: 'orgs-policies',
      label: 'Orgs & Policies',
      Icon: DomainIcons.Organizations,
      href: embeddedHref('orgs-and-policies'),
      activeHrefs: MANAGEMENT_ACTIVE_HREFS,
    });
  }
  if (isLicensed && isOrgsAndAppsEnabled) {
    items.push({
      id: 'violations',
      label: 'Violations',
      Icon: DomainIcons.Violations,
      href: '/violations',
    });
  }
  if (isLicensed && isOrgsAndAppsEnabled) {
    items.push({
      id: 'vulnerabilities',
      label: 'Vulnerabilities',
      Icon: DomainIcons.Vulnerability,
      href: '/vulnerabilities',
    });
  }
  if (isLicensed && isOrgsAndAppsEnabled) {
    items.push({
      id: 'waivers',
      label: 'Waivers',
      Icon: DomainIcons.Waivers,
      href: '/waivers',
    });
  }

  // Buffer the reporting group so pushGroup can place its leading divider correctly.
  const reportingGroupItems = [];
  if (isSuccessMetricsEnabled && isOrgsAndAppsEnabled) {
    reportingGroupItems.push({
      id: 'success-metrics',
      label: 'Success Metrics',
      Icon: DomainIcons.SuccessMetrics,
      href: embeddedHref('success-metrics'),
    });
  }
  if (isLicensed && isIntegratedEnterpriseReportingSupported) {
    reportingGroupItems.push({
      id: 'enterprise-reporting',
      label: 'Enterprise Reporting',
      Icon: DomainIcons.EnterpriseReporting,
      href: embeddedHref('reports'),
      activeHrefs: REPORTING_ACTIVE_HREFS,
    });
  }
  if (isLicensed && !isIntegratedEnterpriseReportingSupported) {
    reportingGroupItems.push({
      id: 'operational-reporting',
      label: 'Operational Reporting',
      Icon: DomainIcons.OperationalReporting,
      href: embeddedHref('reports'),
      activeHrefs: REPORTING_ACTIVE_HREFS,
    });
  }
  pushGroup(items, reportingGroupItems);

  // Buffer the settings group so pushGroup can place its leading divider correctly.
  const settingsGroupItems = [];
  if (isApiPageEnabled) {
    settingsGroupItems.push({
      id: 'api',
      label: 'API',
      Icon: DomainIcons.Api,
      href: embeddedHref('api'),
    });
  }
  if (isLicensed && isOrgsAndAppsEnabled) {
    settingsGroupItems.push({
      id: 'settings',
      label: 'Settings',
      Icon: DomainIcons.Settings,
      href: comingSoonHref('settings'),
    });
  }
  pushGroup(items, settingsGroupItems);
  return items;
}

/**
 * Preview Nexus One Left Navigation rail.
 *
 * Per UX-F3-001..007 of the F3 epic doc:
 *   001 - Dedicated left rail distinct from top nav  ✓
 *   002 - Icon + label rows (Lucide via DomainIcons facade)  ✓
 *   003 - Collapse toggle in footer; collapsed = icons only with tooltip  ✓
 *   004 - Persist collapsed state in localStorage[nosc.leftnav.collapsed]  ✓
 *   005 - RBAC-filtered via Redux selectors (CLM-39640 review)  ✓
 *   006 - Active-route highlighting via Radix accent token  ✓
 *   007 - Click-not-hover for nested items (no nested items in Phase-1)  ✓
 */
export default function LeftNav() {
  const [isCollapsed, setIsCollapsed] = useLeftNavCollapsed();
  const currentPath = useCurrentHashPath();

  // Read the same Redux selectors NavigationContainer.jsx feeds into
  // Classic's IqSidebarNav. Lets the Preview LeftNav respect the user's
  // actual entitlements out of the box.
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const isLicensed = useSelector(selectIsLicensed);
  const isDashboardAvailable = useSelector(selectIsDashboardSupported);
  const isSuccessMetricsEnabled = useSelector(selectIsSuccessMetricsEnabled);
  const isLegalEnabled = useSelector(selectIsAdvancedLegalPackSupported);
  const isApiPageEnabled = useSelector(selectIsApiPageSupported);
  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isIntegratedEnterpriseReportingSupported = useSelector(selectIsIntegratedEnterpriseReportingSupported);
  const isHostedRepositoryEvaluationEnabled = useSelector(selectIsHostedRepositoryEvaluationEnabled);
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const isProductsLoading = useSelector(selectLoadingProducts);
  const isStandaloneDeveloper = useSelector(selectIsStandaloneDeveloper);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const isSbomManager = useSelector(selectIsSbomManager);
  const isSbomManagerOnlyLicense = useSelector(selectIsSbomManagerOnlyLicense);
  const isFirewallOnlyLicense = useSelector(selectIsFirewallOnlyLicense);

  // Match Classic's bail-out logic: while products are still loading
  // OR for non-Lifecycle product surfaces (SBOM Manager, Standalone
  // Developer, Standalone Firewall), Classic returns a different
  // sidebar entirely. Phase 1 Nexus One only ships the Lifecycle
  // surface; for the other product surfaces, render an empty rail so
  // the layout stays consistent and the toggle still works.
  const renderEmpty =
    !isLoggedIn ||
    isProductFeaturesLoading ||
    isProductsLoading ||
    isSbomManagerOnlyLicense ||
    isSbomManager ||
    isStandaloneDeveloper ||
    isStandaloneFirewall ||
    isFirewallOnlyLicense;

  const items = renderEmpty
    ? []
    : buildNavItems({
        isLicensed,
        isDashboardAvailable,
        isSuccessMetricsEnabled,
        isLegalEnabled,
        isApiPageEnabled,
        isOrgsAndAppsEnabled,
        isIntegratedEnterpriseReportingSupported,
        isHostedRepositoryEvaluationEnabled,
      });

  return (
    <nav
      aria-label="Preview navigation"
      data-testid="nosc-leftnav"
      data-collapsed={isCollapsed}
      style={{
        position: 'fixed',
        top: TOP_OFFSET,
        bottom: 0,
        left: 0,
        width: isCollapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH,
        borderRight: '1px solid var(--gray-a5)',
        backgroundColor: 'var(--color-panel-solid)',
        transition: 'width 120ms ease',
        zIndex: 9,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <ScrollArea scrollbars="vertical" style={{ flexGrow: 1 }}>
        <Box p={isCollapsed ? '2' : '3'}>
          <Flex direction="column" gap="1">
            {items.map((item) => (
              <Box key={item.id}>
                {item.separator && <Separator my="2" data-testid="nosc-leftnav-separator" style={{ width: '100%' }} />}
                <NavItem
                  id={item.id}
                  label={item.label}
                  Icon={item.Icon}
                  href={item.href}
                  isActive={isPathActive(currentPath, item)}
                  isCollapsed={isCollapsed}
                />
              </Box>
            ))}
          </Flex>
        </Box>
      </ScrollArea>

      {/* Collapse toggle in footer */}
      <Box
        p="2"
        style={{
          borderTop: '1px solid var(--gray-a5)',
          display: 'flex',
          justifyContent: isCollapsed ? 'center' : 'flex-end',
        }}
      >
        <Tooltip content={isCollapsed ? 'Expand navigation' : 'Collapse navigation'} side="right">
          <IconButton
            size="2"
            variant="ghost"
            color="gray"
            onClick={() => setIsCollapsed(!isCollapsed)}
            aria-label={isCollapsed ? 'Expand navigation' : 'Collapse navigation'}
            data-testid="nosc-leftnav-collapse-toggle"
          >
            {isCollapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
          </IconButton>
        </Tooltip>
      </Box>
    </nav>
  );
}
