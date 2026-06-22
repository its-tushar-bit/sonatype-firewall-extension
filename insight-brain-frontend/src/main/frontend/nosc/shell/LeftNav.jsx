/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Box, Flex, IconButton, ScrollArea, Tooltip } from '@radix-ui/themes';
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { comingSoonHref } from 'MainRoot/nosc/comingSoon';
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
  selectIsReportListSupported,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsAdvancedSearchEnabled } from 'MainRoot/configuration/advancedSearch/advancedSearchSelectors';
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

/**
 * Nexus One Preview LeftNav.
 *
 * Mirrors Classic's `react/iqSidebarNav/IqSidebarNav.jsx` exactly: same
 * module list, same display order, same Redux-driven permissioning. The
 * only Nexus-One-only addition is **Applications** (the Preview entity
 * browser introduced in P1-F7), inserted between "Orgs and Policies"
 * and "Reports" because it is a sibling-weight entity browser.
 *
 * Per CLM-39640 user review: drop the Coming Soon stub registry and
 * the Home/Search/Settings/Waivers Nexus-One-only entries that were
 * present in the prior shape. The intent is module parity — a user
 * switching between Classic and Nexus One should see the same
 * navigable modules in the same order, plus the one new Applications
 * entry.
 *
 * Permissioning hooks the same Redux selectors `NavigationContainer.jsx`
 * uses for IqSidebarNav, so an unlicensed / SBOM-only / Firewall-only
 * tenant gets the same module visibility as in Classic. Applications
 * gates on `isLicensed && isOrgsAndAppsEnabled` (same gate as Reports),
 * which is the right read for the Phase-1 surface — it's an entity
 * browser of the apps the user already has access to.
 *
 * Hrefs all land inside the Nexus One Preview surface (`/preview/*`).
 * Per CLM-39640 review: clicking a Nexus One nav entry should keep
 * the user inside Nexus One — either landing on a native Preview page
 * (Dashboard, Applications, Advanced Search) or on a Coming Soon
 * stub that has a "Continue in Classic" escape hatch on the page
 * itself. The escape hatch belongs to the page, not the nav click.
 *
 * Stub-target mapping (Classic LeftNav module → /preview/* target):
 *   Dashboard            → /preview/dashboard            (native)
 *   Orgs and Policies    → /preview/organizations        (Coming Soon)
 *   Applications         → /preview/applications         (native)
 *   Reports              → /preview/reports              (Coming Soon)
 *   Success Metrics      → /preview/success-metrics      (Coming Soon)
 *   Vulnerability Lookup → /preview/vulnerability-lookup (Coming Soon;
 *                          follow-on PR replaces with native CVE detail)
 *   Advanced Search      → /preview/search               (live — mounts the
 *                          full SearchResultsPage; the omnibar handles
 *                          typeahead and deep-links into this page)
 *   Legal                → /preview/legal                (Coming Soon)
 *   Hosted Repos         → /preview/repositories         (Coming Soon)
 *   Enterprise/Operational Reporting → /preview/reports  (consolidated
 *                          with Reports — same conceptual surface)
 *   API                  → /preview/api                  (Coming Soon)
 */

function readHashPath() {
  const rawHash = typeof window !== 'undefined' ? window.location.hash : '';
  return rawHash.startsWith('#') ? rawHash.slice(1) : rawHash;
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

/** Active-route check — match exact path or descendant prefix. */
function isPathActive(currentPath, itemHref) {
  if (!itemHref) return false;
  return currentPath === itemHref || currentPath.startsWith(itemHref + '/');
}

/**
 * A single nav item row. Renders an anchor that navigates via hash
 * (works with IQ's existing ui-router setup without needing useSref).
 * When collapsed, wraps in a Radix Tooltip so the label remains
 * discoverable.
 *
 * `isSubEntry` is set on the four Dashboard-tab sub-entries added in
 * CLM-39992 / S2-PR-D-1. When expanded, sub-entries get a small left
 * indent and a smaller icon so the parent/child hierarchy is visible
 * at a glance. When the rail is collapsed they render exactly like
 * top-level entries (icon-only with tooltip) — there's no room for
 * indentation in the 64px collapsed width and the tooltip already
 * disambiguates "Violations" vs the parent "Dashboard".
 */
function NavItem({ id, label, Icon, href, isActive, isCollapsed, isSubEntry = false }) {
  const fullHref = bundleIndexUrl('nexus-one', href);
  const indentLeft = !isCollapsed && isSubEntry ? '20px' : '0px';
  const iconSize = isSubEntry ? 14 : 18;

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
        style={{ minHeight: isSubEntry ? '32px' : '40px', paddingLeft: `calc(var(--space-3) + ${indentLeft})` }}
      >
        <Icon size={iconSize} color={isActive ? 'var(--accent-11)' : 'var(--gray-11)'} />
        {!isCollapsed && (
          <span
            style={{
              fontSize: isSubEntry ? '13px' : '14px',
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
 * Build the visible nav-item list given the current Redux feature/license
 * flags. Mirrors the conditional rendering in Classic's IqSidebarNav so
 * an unlicensed/SBOM-only/Firewall-only tenant sees the same modules in
 * Nexus One as they do in Classic.
 *
 * Returns an array of `{ id, label, Icon, href }` rows in display order.
 */
function buildNavItems(flags) {
  const {
    isLicensed,
    isDashboardAvailable,
    isReportsListAvailable,
    isSuccessMetricsEnabled,
    isAdvancedSearchEnabled,
    isLegalEnabled,
    isApiPageEnabled,
    isOrgsAndAppsEnabled,
    isIntegratedEnterpriseReportingSupported,
    isHostedRepositoryEvaluationEnabled,
  } = flags;

  const items = [];

  if (isDashboardAvailable) {
    // Single Dashboard entry. The 4 tabs (Overview / Violations /
    // Components / Applications / Waivers) are reached via the
    // in-page tab strip on /preview/dashboard. LeftNav sub-entries
    // were removed per design feedback — they were redundant with the
    // tab strip and added vertical noise to the rail.
    items.push({
      id: 'dashboard',
      label: 'Dashboard',
      Icon: DomainIcons.Home,
      href: '/dashboard',
    });
  }
  if (isLicensed) {
    items.push({
      id: 'orgs-policies',
      label: 'Orgs and Policies',
      Icon: DomainIcons.Organizations,
      href: comingSoonHref('organizations'),
    });
  }
  // Applications: Nexus-One-only entity browser, inserted next to other
  // entity browsers. Same gate as Reports — `isLicensed && isOrgsAndAppsEnabled`.
  if (isLicensed && isOrgsAndAppsEnabled) {
    items.push({
      id: 'applications',
      label: 'Applications',
      Icon: DomainIcons.Applications,
      href: '/applications',
    });
  }
  if (isReportsListAvailable && isOrgsAndAppsEnabled) {
    items.push({
      id: 'reports',
      label: 'Reports',
      Icon: DomainIcons.ReportsBar,
      href: comingSoonHref('reports'),
    });
  }
  if (isSuccessMetricsEnabled && isOrgsAndAppsEnabled) {
    items.push({
      id: 'success-metrics',
      label: 'Success Metrics',
      Icon: DomainIcons.SuccessMetrics,
      href: comingSoonHref('success-metrics'),
    });
  }
  if (isLicensed) {
    items.push({
      id: 'vulnerability-lookup',
      label: 'Vulnerability Lookup',
      Icon: DomainIcons.VulnerabilityLookup,
      href: comingSoonHref('vulnerability-lookup'),
    });
  }
  if (isLicensed && isAdvancedSearchEnabled) {
    items.push({
      id: 'advanced-search',
      label: 'Advanced Search',
      Icon: DomainIcons.AdvancedSearch,
      href: '/search',
    });
  }
  if (isLicensed && isLegalEnabled) {
    items.push({
      id: 'legal',
      label: 'Legal',
      Icon: DomainIcons.Legal,
      href: comingSoonHref('legal'),
    });
  }
  if (isLicensed && isHostedRepositoryEvaluationEnabled) {
    items.push({
      id: 'hosted-repos',
      label: 'Hosted Repos',
      Icon: DomainIcons.HostedRepos,
      href: comingSoonHref('repositories'),
    });
  }
  if (isLicensed && isIntegratedEnterpriseReportingSupported) {
    items.push({
      id: 'enterprise-reporting',
      label: 'Enterprise Reporting',
      Icon: DomainIcons.EnterpriseReporting,
      // Consolidated with Reports — same conceptual surface; the
      // Reports stub will eventually grow to cover both.
      href: comingSoonHref('reports'),
    });
  }
  if (isLicensed && !isIntegratedEnterpriseReportingSupported) {
    items.push({
      id: 'operational-reporting',
      label: 'Operational Reporting',
      Icon: DomainIcons.OperationalReporting,
      // Consolidated with Reports — same conceptual surface.
      href: comingSoonHref('reports'),
    });
  }
  if (isApiPageEnabled) {
    items.push({
      id: 'api',
      label: 'API',
      Icon: DomainIcons.Api,
      href: comingSoonHref('api'),
    });
  }
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
  const isReportsListAvailable = useSelector(selectIsReportListSupported);
  const isSuccessMetricsEnabled = useSelector(selectIsSuccessMetricsEnabled);
  const isAdvancedSearchEnabled = useSelector(selectIsAdvancedSearchEnabled);
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
        isReportsListAvailable,
        isSuccessMetricsEnabled,
        isAdvancedSearchEnabled,
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
              <NavItem
                key={item.id}
                id={item.id}
                label={item.label}
                Icon={item.Icon}
                href={item.href}
                isActive={isPathActive(currentPath, item.href)}
                isCollapsed={isCollapsed}
                isSubEntry={item.isSubEntry}
              />
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
