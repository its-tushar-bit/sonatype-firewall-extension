/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * P1-F15: registry of "Coming Soon" stub modules for the Nexus One Preview UI.
 *
 * Each entry corresponds to a Classic IQ top-level concept that we haven't yet
 * built a native Nexus One module for. Until the native version ships, the
 * LeftNav surfaces the entry and clicking it lands the user on a
 * <ComingSoonPage>, which gives a one-click "Open in Classic IQ" escape hatch
 * to the working Classic equivalent.
 *
 * Single source of truth — consumed by:
 *   - nosc/shell/LeftNav.jsx to render the nav entries
 *   - nexus-one/routes.tsx to register the routes
 *   - nosc/routing/classicPreviewMap.ts to round-trip the symmetric toggle
 *   - tests in src/test/frontend/nosc/comingSoon/
 *
 * To add a new stub: append an entry below. No other change required for the
 * route to work end-to-end; the LeftNav, route registration, and toggle map
 * all read this registry.
 */

export interface ComingSoonModule {
  /** Display label shown in LeftNav and as the page heading. */
  readonly label: string;

  /** One-sentence description of what the module does. Rendered as the page
   *  subtitle. */
  readonly description: string;

  /** Classic IQ deep link that "Open in Classic IQ" jumps to. Must include
   *  the leading `/assets/#` because the Classic UI is hash-routed. */
  readonly classicHref: string;
}

/**
 * Slug → module metadata. Slug becomes the URL segment under `/coming-soon/`.
 *
 * The LeftNav render order is built explicitly in `buildNavItems`; it does not
 * derive from this map's key order or from `COMING_SOON_MODULE_ORDER`.
 */
// Most `classicHref` values were verified live against the dev IQ on
// 2026-05-14: each lands on a real UI-Router state (no "Unknown Address"
// unrecoverable errors). The `settings` stub added later is a placeholder
// pointing at the generic Classic dashboard (see the TODO marker below), not a
// per-domain analog.
// Mapping notes:
//
//   reports               → /operationalReporting (Operational Reporting page)
//   policies              → /management/view/organization/ROOT_ORGANIZATION_ID
//                           (Classic policies live under their owning org/app)
//   orgs-and-policies     → /management/view/organization/ROOT_ORGANIZATION_ID
//                           (root Orgs & Policies tree)
//   repositories          → /hostedRepos (Repository Managers — closest analog)
//   source-control        → /automaticSourceControlConfiguration
//   roles-permissions     → /roles
//   waiver-requests       → /dashboard/waiverRequests
//   audit-log             → /user-activity (User Activity — closest analog)
//   system-config         → /gettingStarted (entry-point of System Preferences)
//
// Added 2026-05-14 (CLM-39640 review): every Classic LeftNav module needs
// a Preview-side target so clicking a nav entry stays in Nexus One.
//
//   success-metrics       → /labs/successMetrics
//   vulnerability-lookup  → /vulnerabilities (legacy CVE-search stub; distinct
//                           from the native Martha `#/vulnerabilities` list.
//                           No LeftNav consumer today; retained for Classic
//                           escape-hatch / follow-on detail work)
//   legal                 → /legal/dashboard
//   api                   → /api
export const COMING_SOON_MODULES = {
  reports: {
    label: 'Reports',
    description: 'View, schedule, and download policy and SBOM scan reports.',
    classicHref: '/assets/#/operationalReporting',
  },
  'success-metrics': {
    label: 'Success Metrics',
    description: 'Track risk-reduction trends, MTTR, and progress against your application security program goals.',
    classicHref: '/assets/#/labs/successMetrics',
  },
  'vulnerability-lookup': {
    label: 'Vulnerability Lookup',
    description: 'Search the Sonatype catalog for CVE details, affected components, and remediation guidance.',
    classicHref: '/assets/#/vulnerabilities',
  },
  legal: {
    label: 'Legal',
    description: 'Surface license obligations, copyleft risk, and attribution data across your application portfolio.',
    classicHref: '/assets/#/legal/dashboard',
  },
  api: {
    label: 'API',
    description: 'Browse the REST API reference and copy ready-to-run examples for the IQ Server endpoints.',
    classicHref: '/assets/#/api',
  },
  // `policies` and `orgs-and-policies` intentionally share the Classic org
  // management page (Classic policies live under their owning org/app). They
  // are distinct entry points: `policies` backs the per-application "Configure
  // Policies" quick action (OverviewTab), while `orgs-and-policies` is the
  // top-level LeftNav entry.
  policies: {
    label: 'Policies',
    description: 'Define and manage the security, license, and quality policies that govern your applications.',
    classicHref: '/assets/#/management/view/organization/ROOT_ORGANIZATION_ID',
  },
  'orgs-and-policies': {
    label: 'Orgs & Policies',
    description: 'Manage organizations, applications, and hierarchical policy inheritance.',
    classicHref: '/assets/#/management/view/organization/ROOT_ORGANIZATION_ID',
  },
  repositories: {
    label: 'Repositories',
    description: 'View and configure the upstream repositories scanned by Firewall and Lifecycle.',
    classicHref: '/assets/#/hostedRepos',
  },
  'source-control': {
    label: 'Source Control',
    description: 'Configure source control integrations for automated SCM scanning and pull-request workflows.',
    classicHref: '/assets/#/automaticSourceControlConfiguration',
  },
  'roles-permissions': {
    label: 'Roles & Permissions',
    description: 'Define roles and assign permissions across the organization hierarchy.',
    classicHref: '/assets/#/roles',
  },
  'waiver-requests': {
    label: 'Waiver Requests',
    description: 'Review, approve, or deny waiver requests submitted by developers.',
    classicHref: '/assets/#/dashboard/waiverRequests',
  },
  'system-config': {
    label: 'System Configuration',
    description: 'Server-wide configuration — proxies, base URL, authentication providers, mail, and licensing.',
    classicHref: '/assets/#/gettingStarted',
  },
  guide: {
    label: 'Sonatype Guide',
    description: 'Guide AI coding assistants with open source intelligence.',
    classicHref: '/assets/#/dashboard/violations',
  },
  firewall: {
    label: 'Sonatype Repository Firewall',
    description: 'Reduce remediation with OSS malware protection.',
    classicHref: '/assets/#/dashboard/violations',
  },
  'sbom-manager': {
    label: 'Sonatype SBOM Manager',
    description: 'Automate software compliance and reporting.',
    classicHref: '/assets/#/dashboard/violations',
  },
  // TODO(CLM-42160): `settings` uses the generic Classic dashboard as a
  // placeholder classicHref until its native module is built.
  // `components` and `vulnerabilities` graduated to native list pages.
  settings: {
    label: 'Settings',
    description: 'Manage Nexus One preferences and configuration.',
    classicHref: '/assets/#/dashboard',
  },
} as const;

export type ComingSoonModuleSlug = keyof typeof COMING_SOON_MODULES;

/** Render order: explicit so the LeftNav doesn't depend on `Object.keys` semantics. */
export const COMING_SOON_MODULE_ORDER: readonly ComingSoonModuleSlug[] = [
  'reports',
  'success-metrics',
  'vulnerability-lookup',
  'legal',
  'api',
  'policies',
  'orgs-and-policies',
  'repositories',
  'source-control',
  'roles-permissions',
  'waiver-requests',
  'system-config',
  'guide',
  'firewall',
  'sbom-manager',
  'settings',
];

/** In-hash path for a Coming Soon stub in the nexus-one bundle. */
export function comingSoonHref(slug: ComingSoonModuleSlug): string {
  return `/coming-soon/${slug}`;
}

/** UI-Router state name for a Coming Soon stub (single source of truth, shared
 *  by route registration and any code navigating to a stub). */
export function comingSoonStateName(slug: ComingSoonModuleSlug): string {
  const pascal = slug
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('');
  return 'nexusOneComingSoon' + pascal;
}
