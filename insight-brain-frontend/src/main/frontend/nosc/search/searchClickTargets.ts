/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  SearchRow,
  SearchSource,
  isApplication,
  isVulnerability,
  isViolation,
  isWaiver,
} from 'MainRoot/nosc/search/searchTypes';
import { DEFAULT_SEARCH_SOURCE } from 'MainRoot/nosc/search/searchDataSource';
import { violationSidebarHref } from 'MainRoot/nosc/applications/applicationDetailUtils';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';
import router from 'MainRoot/router/routerInstance';

/**
 * Maps a search result row to the URL the omnibar navigates to on click.
 *
 * The backend supplies an href on a row when it has a canonical destination; we
 * prefer it when present (and safe — see isSafeHref). Otherwise we resolve Nexus
 * One destinations through the UI-Router state registry (`router.stateService.href`)
 * so paths and params come from a single source of truth rather than hand-built
 * hash strings. Vulnerabilities use {@link vulnerabilityDetailHref} so clicks land
 * on a concrete tab (Security Details by default).
 *
 * Identifier semantics matter per entity type (the backend row `id` is not always
 * the route param a destination expects):
 *   - Application: `id` is the internal application id; the public id used by the
 *     detail route lives in `fields.applicationPublicId` (falling back to subtitle).
 *   - Waiver: `id` is the policy-waiver id, which is NOT a policy-violation id, so
 *     it cannot deep-link to the violation sidebar; waivers land on the waivers list.
 *
 * TODO(CLM-39549): as native Nexus One detail routes land for components /
 * violations / waivers, the backend href (or the fallbacks here) should point at
 * them so global-search clicks keep users inside the Nexus One UI.
 */

/**
 * Guards a backend-supplied href before navigation: only same-origin relative
 * paths, hash routes, and query strings are allowed. Every absolute form is
 * rejected — `javascript:` and other script-bearing schemes, protocol-relative
 * `//host` URLs, and `http(s)://host` URLs pointing at another origin. The row
 * href contract is app-relative, so an absolute URL can only be a tampered or
 * mistaken value. Falls through to the router-built destination when unsafe.
 */
function isSafeHref(href: string): boolean {
  const trimmed = href.trim();
  if (trimmed === '') return false;
  // Protocol-relative URLs (//evil.example/…) navigate off-origin; reject them
  // before the leading-slash check below would accept them.
  if (trimmed.startsWith('//')) return false;
  // Relative path, in-app hash route, or query string.
  return trimmed.startsWith('/') || trimmed.startsWith('#') || trimmed.startsWith('?');
}

/**
 * Public application id for the Nexus One detail route. Never returns the row's
 * internal `id` — that is not a publicId and would produce a dead link.
 */
function applicationPublicId(result: SearchRow): string | null {
  const fromFields = result.fields.applicationPublicId;
  if (typeof fromFields === 'string' && fromFields) return fromFields;
  if (result.subtitle) return result.subtitle;
  return null;
}

export function clickHrefFor(result: SearchRow): string {
  if (result.href && isSafeHref(result.href)) {
    return result.href;
  }

  if (isApplication(result) && result.id) {
    const publicId = applicationPublicId(result);
    if (!publicId) {
      // Same degrade as components: no usable public id → platform home, not a
      // broken detail URL built from the internal application id.
      return router.stateService.href('platformHome');
    }
    return router.stateService.href('nexusOneApplicationsDetail.overview', {
      publicId,
    });
  }

  if (isVulnerability(result) && result.id) {
    return vulnerabilityDetailHref({ vulnId: result.id });
  }

  // A violation row's id is a policy-violation id, so it deep-links to the Classic
  // violation-detail sidebar.
  if (isViolation(result) && result.id) {
    return violationSidebarHref(result.id);
  }

  // A waiver row carries only the policy-waiver id (not a violation id) and no
  // owner type, so it cannot build the violation sidebar or the native waiver
  // detail route; land on the Nexus One waivers list instead of a dead link.
  if (isWaiver(result)) {
    return router.stateService.href('nexusOneDashboard.waivers');
  }

  // TODO(CLM-39549): no native Nexus One component detail page exists yet;
  // land on the Nexus One home until those routes ship.
  return router.stateService.href('platformHome');
}

/**
 * `nexusOneSearch` state params for the query the user submitted without selecting
 * a row. When the active data source is the catalog, `source=catalog` is carried
 * too so the results page opens against the same corpus the omnibar was searching;
 * the default `local` source is omitted to keep the common URL clean.
 */
export function searchResultsStateParams(
  query: string,
  source: SearchSource = DEFAULT_SEARCH_SOURCE
): Record<string, string> {
  const trimmed = query.trim();
  const params: Record<string, string> = {};
  if (trimmed) params.q = trimmed;
  if (source !== DEFAULT_SEARCH_SOURCE) params.source = source;
  return params;
}

/**
 * URL of the Nexus One search route for a submitted query. Callers navigating
 * in-app should go through the router with {@link searchResultsStateParams}
 * instead; this builds the equivalent href for anchor targets.
 */
export function enterSearchHref(query: string, source: SearchSource = DEFAULT_SEARCH_SOURCE): string {
  return router.stateService.href('nexusOneSearch', searchResultsStateParams(query, source));
}
