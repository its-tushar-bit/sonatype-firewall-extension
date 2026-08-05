/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  COMING_SOON_MODULES,
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
} from 'MainRoot/nosc/comingSoon';
import {
  CLEAN_PATH_OWNED_ELSEWHERE,
  embeddedHref,
  usesEmbeddedHrefPrimary,
} from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
import {
  VULNERABILITY_DETAIL_DEFAULT_TAB,
  VULNERABILITY_DETAIL_TAB_PATH_PATTERN,
} from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailUtils';

/**
 * Symmetric Classic <-> Nexus One in-hash path map.
 *
 * Nexus One routes live in the {@code nexus-one} HTML bundle
 * ({@code /assets/nexus-one/index.html}) without a {@code /preview/} prefix.
 * Classic routes stay in the classic bundle ({@code /assets/index.html}).
 */

export const NEXUS_ONE_DEFAULT_PATH = '/dashboard';

/** @deprecated Use {@link NEXUS_ONE_DEFAULT_PATH}. */
export const PREVIEW_DEFAULT_PATH = NEXUS_ONE_DEFAULT_PATH;

export const CLASSIC_DEFAULT_PATH = '/dashboard/violations';

function normalizeClassicPath(fullHref: string): string {
  if (fullHref.startsWith('/assets/#')) return fullHref.slice('/assets/#'.length);
  if (fullHref.startsWith('#')) return fullHref.slice(1);
  return fullHref;
}

// Emit clean embed primaries before stub aliases so Classic→Preview reverse
// lookup (Array.find on classic path) prefers /${slug} over /coming-soon/${slug},
// and prefers orgs-and-policies over the colliding policies stub on
// /management/view/organization/ROOT_ORGANIZATION_ID.
const COMING_SOON_ENTRIES: ReadonlyArray<readonly [string, string]> = [
  ...COMING_SOON_MODULE_ORDER.flatMap((slug) => {
    if (!usesEmbeddedHrefPrimary(slug)) return [];
    const classic = normalizeClassicPath(COMING_SOON_MODULES[slug].classicHref);
    return [
      [embeddedHref(slug), classic] as const,
      [comingSoonHref(slug), classic] as const,
    ];
  }),
  ...COMING_SOON_MODULE_ORDER.flatMap((slug) => {
    if (usesEmbeddedHrefPrimary(slug)) return [];
    // Hosted Repos (/repositories <-> /hostedRepos) is handled by SUBTREE_MAPPINGS
    // so deep links round-trip 1-1 — emit no Coming Soon map entries for those slugs.
    if (CLEAN_PATH_OWNED_ELSEWHERE.has(slug)) return [];
    const classic = normalizeClassicPath(COMING_SOON_MODULES[slug].classicHref);
    return [[comingSoonHref(slug), classic] as const];
  }),
];

const NEXUS_ONE_TO_CLASSIC: ReadonlyArray<readonly [string, string]> = [
  // Native Waivers tab (nexusOneDashboard.waivers at /dashboard/waivers) toggles to the Classic
  // Waivers tab. Must precede /dashboard so it isn't swallowed by that prefix into Violations.
  ['/dashboard/waivers', '/dashboard/waivers'],
  ['/dashboard', '/dashboard/violations'],
  ['/applications', '/dashboard/applications'],
  ['/components', '/dashboard/components'],
  // Native Vulnerabilities list shares Classic CVE-search hash path.
  // Listed before COMING_SOON_ENTRIES so Classic `/vulnerabilities` toggles to
  // native Martha, not `/coming-soon/vulnerability-lookup`.
  ['/vulnerabilities', '/vulnerabilities'],
  ['/ui-settings', '/previewUiSettings'],
  ['/search', '/dashboard/violations'],
  // Identity entries: admin config pages share the same hash path on both
  // bundles. SHARED_PATHS isn't the right home for these — its
  // isSharedPath branch in toNexusOneEquivalent maps shared paths to
  // '/ui-settings', not the caller-supplied path.
  ['/successMetricsConfiguration', '/successMetricsConfiguration'],
  // LDAP list is at /ldap-servers on both bundles; the create/edit sub-pages
  // live under /ldap/* and are handled by SUBTREE_MAPPINGS below so deep links
  // (/ldap/create, /ldap/edit/{id}, /ldap/edit/{id}/userMapping) preserve their
  // tail on the toggle.
  ['/ldap-servers', '/ldap-servers'],
  ['/waivedComponentUpgradesConfiguration', '/waivedComponentUpgradesConfiguration'],
  ['/productlicense', '/productlicense'],
  ['/gettingStarted', '/gettingStarted'],
  // /users is handled by SUBTREE_MAPPINGS below so deep links (/users/_new_,
  // /users/{id}, /users/activity/{user}) preserve their tail on the toggle.
  ['/user-activity', '/user-activity'],
  ['/baseUrl', '/baseUrl'],
  ['/systemNoticeConfiguration', '/systemNoticeConfiguration'],
  ['/administrators', '/administrators'],
  ['/automaticApplicationsConfiguration', '/automaticApplicationsConfiguration'],
  // CLM-42957: Atlassian Crowd configuration page.
  ['/crowd', '/crowd'],
  // Reverse-mapping only: Classic firewall serves Atlassian Crowd at
  // /firewall/crowd under a firewall-only license (state
  // `firewall.atlassianCrowdConfiguration`, registered as a child of the
  // abstract `firewall` state in `firewall/route.js:770-778`). Users
  // toggling from Classic firewall must land on the NOUX `/crowd` embed.
  // Emission order does NOT matter for forward mapping — the identity
  // entry above wins on the nexus-key first-match. For reverse mapping
  // (`toNexusOneEquivalent`), this routes the firewall Classic path back
  // to the NOUX embed. CLM-42957.
  ['/crowd', '/firewall/crowd'],
  ['/mailConfig', '/mailConfig'],
  ['/saml', '/saml'],
  ['/userTokensConfiguration', '/userTokensConfiguration'],
  ['/advancedSearchConfig', '/advancedSearchConfig'],
  ['/proxyConfig', '/proxyConfig'],
  // Reverse-mapping only: Classic firewall serves Proxy at these paths under a
  // firewall-only license (state `firewall.proxyConfig`, registered as a child
  // of the abstract `firewall` state in `firewall/route.js:815-823`). Users
  // toggling from Classic firewall must land on the NOUX `/proxyConfig` embed.
  // Emission order does NOT matter for forward mapping — the identity entry
  // above wins on the nexus-key first-match. For reverse mapping
  // (`toNexusOneEquivalent`), first-match by classic-key routes each of these
  // distinct Classic paths back to the same `/proxyConfig`. CLM-42876.
  ['/proxyConfig', '/firewall/proxyConfig'],
  ['/proxyConfig', '/malware-defense/proxyConfig'],
  ...COMING_SOON_ENTRIES,
  // CLM-42962: Forward-only identity for the Automatic SCM Configuration embed.
  // Placed AFTER COMING_SOON_ENTRIES so that `toNexusOneEquivalent('/automaticSourceControlConfiguration')`
  // continues to prefer the Coming Soon entry (returns `/source-control`) — the
  // Classic→NOUX toggle keeps landing on the Coming Soon primary owned by the
  // `source-control` slug in NATIVE_CLASSIC_EMBED_SLUGS. This entry only affects
  // the reverse direction (`toClassicEquivalent('/automaticSourceControlConfiguration')`)
  // so users on the NOUX `/automaticSourceControlConfiguration` embed clicking the
  // top-nav "Switch to Classic UI" button land on the same Classic hash path
  // instead of falling through to CLASSIC_DEFAULT_PATH.
  ['/automaticSourceControlConfiguration', '/automaticSourceControlConfiguration'],
];

// Prefix matches use Array.find — keep more-specific paths before broader prefixes
// (e.g. /dashboard/applications before /dashboard/).
const CLASSIC_PREFIX_TO_NEXUS_ONE: ReadonlyArray<readonly [string, string]> = [
  ['/dashboard/applications', '/applications'],
  ['/dashboard/components', '/components'],
  ['/dashboard/waivers', '/dashboard/waivers'],
  ['/dashboard/', '/dashboard'],
  ['/management/view/application', '/applications'],
  ['/management/edit/application', '/applications'],
];

function stripHashPrefix(path: string): string {
  if (!path) return '';
  let p = path;
  if (p.startsWith('#')) p = p.slice(1);
  return p;
}

/**
 * Strips a query (`?…`) and/or fragment (`#…`) suffix so detail-page regexes match the path segment
 * only. Cuts at whichever delimiter appears first, so a `/violations/abc#section` (or `?foo`) never
 * leaks `#section`/`?foo` into the captured id and on into the Classic template. Callers already pass
 * hash-prefix-stripped paths (via {@link stripHashPrefix}); handling `#` here as well keeps the helper
 * self-contained and matches this docstring.
 */
function stripQuerySuffix(path: string): string {
  let end = path.length;
  const queryIndex = path.indexOf('?');
  if (queryIndex >= 0) {
    end = Math.min(end, queryIndex);
  }
  const fragmentIndex = path.indexOf('#');
  if (fragmentIndex >= 0) {
    end = Math.min(end, fragmentIndex);
  }
  return path.slice(0, end);
}

const SHARED_PATHS: ReadonlySet<string> = new Set(['/previewUiSettings']);

function isSharedPath(path: string): boolean {
  for (const shared of SHARED_PATHS) {
    if (path === shared || path.startsWith(shared + '/')) return true;
  }
  return false;
}

function isNexusOnePath(path: string): boolean {
  if (path === '/home' || path.startsWith('/home/')) return true;
  if (path === '/dashboard' || path.startsWith('/dashboard/')) return true;
  if (path === '/applications' || path.startsWith('/applications/')) return true;
  if (path === '/components' || path.startsWith('/components/')) return true;
  if (path === '/violations' || path.startsWith('/violations/')) return true;
  if (path === '/vulnerabilities' || path.startsWith('/vulnerabilities/')) return true;
  if (path === '/search' || path.startsWith('/search/')) return true;
  if (path === '/waivers' || path.startsWith('/waivers/')) return true;
  if (path === '/ui-settings' || path.startsWith('/ui-settings/')) return true;
  if (path === '/repositories' || path.startsWith('/repositories/')) return true;
  if (path === '/users' || path.startsWith('/users/')) return true;
  if (path === '/roles' || path.startsWith('/roles/')) return true;
  if (path === '/webhooks' || path.startsWith('/webhooks/')) return true;
  if (path === '/ldap' || path.startsWith('/ldap/')) return true;
  if (path === '/settings' || path.startsWith('/settings/')) return true;
  if (path.startsWith('/coming-soon/')) return true;
  return NEXUS_ONE_TO_CLASSIC.some(([nexus]) => path === nexus || path.startsWith(nexus + '/'));
}

function isClassicPath(path: string): boolean {
  return path !== '' && path !== '/' && !isNexusOnePath(path) && !isSharedPath(path);
}

interface DetailPageMapping {
  readonly nexusOneMatch: RegExp;
  readonly classicTemplate: (id: string) => string;
  readonly classicMatch: RegExp;
  readonly nexusOneTemplate: (id: string) => string;
}

const DETAIL_PAGE_MAPPINGS: ReadonlyArray<DetailPageMapping> = [
  {
    nexusOneMatch: /^\/applications\/([^/]+)\/?$/,
    classicTemplate: (id) => `/management/view/application/${id}`,
    classicMatch: /^\/management\/view\/application\/([^/]+)\/?$/,
    nexusOneTemplate: (id) => `/applications/${id}`,
  },
  {
    // Native violation detail (CLM-42256): Nexus One /violations/{id} <->
    // Classic sidebarView.violation at /violation/{id} (singular).
    nexusOneMatch: /^\/violations\/([^/]+)\/?$/,
    classicTemplate: (id) => `/violation/${id}`,
    classicMatch: /^\/violation\/([^/]+)\/?$/,
    nexusOneTemplate: (id) => `/violations/${id}`,
  },
  {
    // Native vulnerability detail: Classic CVE view <-> Nexus One tabbed detail.
    // Optional tab segment is derived from VULNERABILITY_DETAIL_TAB_IDS so new tabs
    // stay toggle-safe. Classic -> Nexus One lands on the default tab.
    // Encoded slash ids (e.g. sonatype-2024%2F12345) stay a single path segment.
    nexusOneMatch: new RegExp(
      `^/vulnerabilities/([^/]+)(?:/(?:${VULNERABILITY_DETAIL_TAB_PATH_PATTERN}))?/?$`,
    ),
    classicTemplate: (id) => `/vulnerabilities/${id}`,
    classicMatch: /^\/vulnerabilities\/([^/]+)\/?$/,
    nexusOneTemplate: (id) => `/vulnerabilities/${id}/${VULNERABILITY_DETAIL_DEFAULT_TAB}`,
  },
  {
    // Administrators edit (CLM-42464): identity mapping — both bundles share the
    // same /administrators/{roleId} path. A DETAIL_PAGE_MAPPING is needed rather
    // than a plain NEXUS_ONE_TO_CLASSIC entry because the roleId segment is
    // dynamic and identity-entry prefix-matching in toClassicEquivalent would
    // lose the roleId (returning /administrators instead of /administrators/id).
    nexusOneMatch: /^\/administrators\/([^/]+)\/?$/,
    classicTemplate: (id) => `/administrators/${id}`,
    classicMatch: /^\/administrators\/([^/]+)\/?$/,
    nexusOneTemplate: (id) => `/administrators/${id}`,
  },
];

function findDetailPageNexusOneToClassic(path: string): string | null {
  const matchPath = stripQuerySuffix(path);
  for (const m of DETAIL_PAGE_MAPPINGS) {
    const match = matchPath.match(m.nexusOneMatch);
    if (match) return m.classicTemplate(match[1]);
  }
  return null;
}

function findDetailPageClassicToNexusOne(path: string): string | null {
  const matchPath = stripQuerySuffix(path);
  for (const m of DETAIL_PAGE_MAPPINGS) {
    const match = matchPath.match(m.classicMatch);
    if (match) return m.nexusOneTemplate(match[1]);
  }
  return null;
}

/**
 * Subtrees that exist in both bundles with an identical sub-path structure,
 * differing only by their base segment. Unlike {@link NEXUS_ONE_TO_CLASSIC}
 * (single-path -> single-path), these preserve everything after the base — path
 * segments AND query string — so deep links round-trip 1-1. E.g. Nexus One
 * `/repositories/{mgrId}/{repoId}/components?repositoryPublicId=x`
 * <-> Classic `/hostedRepos/{mgrId}/{repoId}/components?repositoryPublicId=x`. CLM-42184.
 */
const SUBTREE_MAPPINGS: ReadonlyArray<{ readonly nexusOne: string; readonly classic: string }> = [
  { nexusOne: '/repositories', classic: '/hostedRepos' },
  // Users shares identical sub-path structure in both bundles (/users/_new_,
  // /users/{id}, /users/activity/{user}), so sub-paths round-trip 1-1.
  { nexusOne: '/users', classic: '/users' },
  // Roles shares identical sub-path structure in both bundles (/roles/_new_,
  // /roles/{roleId}), so sub-paths round-trip 1-1.
  { nexusOne: '/roles', classic: '/roles' },
  // Webhooks shares identical sub-path structure in both bundles (/webhooks/list,
  // /webhooks/create, /webhooks/{id}), so sub-paths round-trip 1-1. CLM-42961.
  { nexusOne: '/webhooks', classic: '/webhooks' },
  // LDAP create/edit pages share identical /ldap/* structure in both bundles
  // (/ldap/create, /ldap/edit/{id}, /ldap/edit/{id}/userMapping), so sub-paths
  // round-trip 1-1. The LDAP list itself sits at /ldap-servers (see above).
  { nexusOne: '/ldap', classic: '/ldap' },
];

/** Maps a Nexus One subtree path to its Classic equivalent, preserving the tail. */
function toClassicSubtree(path: string): string | null {
  for (const { nexusOne, classic } of SUBTREE_MAPPINGS) {
    if (path === nexusOne || path.startsWith(nexusOne + '/')) {
      return classic + path.slice(nexusOne.length);
    }
  }
  return null;
}

/** Maps a Classic subtree path to its Nexus One equivalent, preserving the tail. */
function toNexusOneSubtree(path: string): string | null {
  for (const { nexusOne, classic } of SUBTREE_MAPPINGS) {
    if (path === classic || path.startsWith(classic + '/')) {
      return nexusOne + path.slice(classic.length);
    }
  }
  return null;
}

export function toNexusOneEquivalent(classicPath: string): string {
  const path = stripHashPrefix(classicPath);
  if (path === '' || path === '/') return NEXUS_ONE_DEFAULT_PATH;
  if (path === '/previewUiSettings') return '/ui-settings';
  if (isSharedPath(path)) return '/ui-settings';
  if (path === '/dashboard') return NEXUS_ONE_DEFAULT_PATH;

  const detail = findDetailPageClassicToNexusOne(path);
  if (detail) return detail;

  const subtree = toNexusOneSubtree(path);
  if (subtree) return subtree;

  const exact = NEXUS_ONE_TO_CLASSIC.find(([, classic]) => classic === path);
  if (exact) return exact[0];

  const prefix = CLASSIC_PREFIX_TO_NEXUS_ONE.find(([cp]) => path === cp || path.startsWith(cp));
  if (prefix) return prefix[1];

  return NEXUS_ONE_DEFAULT_PATH;
}

/** @deprecated Use {@link toNexusOneEquivalent}. */
export const toPreviewEquivalent = toNexusOneEquivalent;

export function toClassicEquivalent(nexusOnePath: string): string {
  const path = stripHashPrefix(nexusOnePath);
  if (path === '' || path === '/') return CLASSIC_DEFAULT_PATH;
  if (path === '/ui-settings') return '/previewUiSettings';
  if (isSharedPath(path)) return path;
  if (isClassicPath(path)) return CLASSIC_DEFAULT_PATH;

  const detail = findDetailPageNexusOneToClassic(path);
  if (detail) return detail;

  const subtree = toClassicSubtree(path);
  if (subtree) return subtree;

  const entry = NEXUS_ONE_TO_CLASSIC.find(([nexus]) => path === nexus || path.startsWith(nexus + '/'));
  if (entry) return entry[1];

  return CLASSIC_DEFAULT_PATH;
}
