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

const COMING_SOON_ENTRIES: ReadonlyArray<readonly [string, string]> =
  COMING_SOON_MODULE_ORDER.map(
    (slug) => [comingSoonHref(slug), normalizeClassicPath(COMING_SOON_MODULES[slug].classicHref)] as const,
  );

const NEXUS_ONE_TO_CLASSIC: ReadonlyArray<readonly [string, string]> = [
  ['/dashboard', '/dashboard/violations'],
  ['/applications', '/dashboard/applications'],
  ['/ui-settings', '/previewUiSettings'],
  ['/search', '/dashboard/violations'],
  // Identity entries: admin config pages share the same hash path on both
  // bundles. SHARED_PATHS isn't the right home for these — its
  // isSharedPath branch in toNexusOneEquivalent maps shared paths to
  // '/ui-settings', not the caller-supplied path.
  ['/successMetricsConfiguration', '/successMetricsConfiguration'],
  ['/baseUrl', '/baseUrl'],
  // Hosted Repos (/repositories <-> /hostedRepos) is handled by SUBTREE_MAPPINGS below
  // so deep links round-trip 1-1. It's excluded from the Coming Soon entries because
  // it's a native embed, not a stub (CLM-42184).
  ...COMING_SOON_ENTRIES.filter(([nexus]) => nexus !== '/coming-soon/repositories'),
];

// Prefix matches use Array.find — keep more-specific paths before broader prefixes
// (e.g. /dashboard/applications before /dashboard/).
const CLASSIC_PREFIX_TO_NEXUS_ONE: ReadonlyArray<readonly [string, string]> = [
  ['/dashboard/applications', '/applications'],
  ['/dashboard/', '/dashboard'],
  ['/management/view/application', '/applications'],
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
  if (path === '/violations' || path.startsWith('/violations/')) return true;
  if (path === '/search' || path.startsWith('/search/')) return true;
  if (path === '/waivers' || path.startsWith('/waivers/')) return true;
  if (path === '/ui-settings' || path.startsWith('/ui-settings/')) return true;
  if (path === '/repositories' || path.startsWith('/repositories/')) return true;
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
    // Embedded violation detail (CLM-42256): Nexus One /violations/{id} <->
    // Classic sidebarView.violation at /violation/{id} (singular).
    nexusOneMatch: /^\/violations\/([^/]+)\/?$/,
    classicTemplate: (id) => `/violation/${id}`,
    classicMatch: /^\/violation\/([^/]+)\/?$/,
    nexusOneTemplate: (id) => `/violations/${id}`,
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
