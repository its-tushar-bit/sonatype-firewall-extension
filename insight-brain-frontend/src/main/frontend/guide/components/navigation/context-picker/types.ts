/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Picker-owned domain types for the policy-context owner picker.
 *
 * These types are intentionally decoupled from the backend REST DTOs
 * (`/api/v2/policy-context/owners/*`). The backend surfaces `type` as
 * `"organization"` / `"application"`; the picker uses the shorter `'org'` /
 * `'app'` throughout. The mapping happens in the adapter layer
 * ({@link OwnerAdapter} implementations) — never import backend DTOs here.
 *
 * Keeping these types adapter-facing (not backend-facing) is what lets the whole
 * component be lifted into `@guide/ui-core` later without a rewrite: only the
 * adapter implementation changes, not these shapes.
 */

/** The two owner kinds the picker can select. Root is represented as `null`, not a type. */
export type OwnerType = 'org' | 'app';

/** Search-scope filter, mirrors the backend `type` query param (`all` | `org` | `app`). */
export type OwnerSearchType = 'all' | 'org' | 'app';

/**
 * A single ancestor in an owner's breadcrumb chain, ordered root-first and
 * excluding the owner itself. Used to render subtitles like `Org1 / Org11`.
 */
export interface AncestorPathEntry {
  id: string;
  name: string;
  type: OwnerType;
}

/**
 * A selectable owner (organization or application).
 *
 * `id` is the value persisted to localStorage and passed to
 * {@link OwnerAdapter.resolveOwner}. For organizations the backend's `id` and
 * `publicId` are identical; for applications they differ, so `publicId` is kept
 * for the downstream policy re-fetch (handled by the separate wiring story).
 *
 * `ancestorPath` is root-to-owner, exclusive of the owner itself.
 */
export interface Owner {
  id: string;
  publicId: string;
  name: string;
  type: OwnerType;
  ancestorPath: AncestorPathEntry[];
}

/** An organization row in Browse / search results. Carries the direct app count. */
export interface OrgSummary extends Owner {
  type: 'org';
  /**
   * Number of applications directly under this org the caller can evaluate.
   * `0` → the org is a directly-selectable leaf (no drill); `> 0` → a drill target.
   */
  appCount: number;
}

/** An application row in the drill-in / search results. */
export interface AppSummary extends Owner {
  type: 'app';
}

/** Result of {@link OwnerAdapter.getTopOrgs}. */
export interface TopOrgsResult {
  orgs: OrgSummary[];
  /**
   * Total orgs matching the caller's permission filter. May exceed `orgs.length`;
   * drives the "+ N more — search to find" hint when it does.
   */
  totalOrgCount: number;
}

/** Result of {@link OwnerAdapter.getAppsForOrg}. */
export interface OrgAppsResult {
  apps: AppSummary[];
  /** `true` when the org holds more permitted apps than were returned (capped by limit). */
  truncated: boolean;
}

/**
 * Result of {@link OwnerAdapter.searchOwners}.
 *
 * Flat list of matches (orgs and apps mixed, each carrying its own `type` and
 * `ancestorPath`); the modal groups them by type for the All / Orgs / Apps tabs.
 * `truncated` is the union of the backend's per-type truncation flags — the UI
 * uses it only to hint "more results — refine search", never to imply exact totals.
 */
export interface SearchResult {
  results: Owner[];
  truncated: boolean;
}
