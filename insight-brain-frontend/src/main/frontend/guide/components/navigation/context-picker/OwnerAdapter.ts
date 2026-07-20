/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import type { Owner, OrgAppsResult, OwnerSearchType, SearchResult, TopOrgsResult } from './types';

/**
 * Data-access seam for the policy-context owner picker.
 *
 * The picker component consumes an `OwnerAdapter` via {@link useOwnerAdapter} and
 * never instantiates one or talks to the backend directly. This mirrors the
 * `NavigationAdapter` precedent in `@guide/ui-core` (one interface, host-specific
 * implementations): self-hosted IQ provides {@code SelfHostedOwnerAdapter}; a
 * future Guide SaaS host would provide its own, swapping only the implementation.
 *
 * Implementations own all backend concerns: mapping the REST `organization` /
 * `application` type strings to the picker's `org` / `app`, folding the search
 * endpoint's two-array response into a flat {@link SearchResult}, and translating
 * HTTP errors (notably 404 → `null` in {@link resolveOwner}).
 */
export interface OwnerAdapter {
  /**
   * Top `limit` organizations the caller can evaluate, sorted alphabetically,
   * with direct app counts and ancestor breadcrumbs. Fetched once on modal open.
   */
  getTopOrgs(limit: number): Promise<TopOrgsResult>;

  /**
   * Applications directly under `orgId` that the caller can evaluate, sorted
   * alphabetically. Rejects (backend 404) when the org is unknown or the caller
   * lacks permission — the two are intentionally indistinguishable.
   *
   * Accepts an optional `AbortSignal` so the caller can cancel a superseded
   * drill (the aborted promise rejects with an `AbortError` the caller ignores).
   * Unlike {@link searchOwners}, the controller is owned by the caller rather
   * than the adapter: apps are cached client-side and a cache hit supersedes an
   * in-flight fetch without calling the adapter, so the abort must be triggered
   * where the supersession happens (the picker), not on the next adapter call.
   */
  getAppsForOrg(orgId: string, limit: number, signal?: AbortSignal): Promise<OrgAppsResult>;

  /**
   * Global substring search across orgs and apps.
   *
   * Callers must pass a query of at least the backend's minimum length (3 chars);
   * shorter queries are rejected by the backend with 400 and should be gated in
   * the UI before calling.
   *
   * Cancellation of a superseded in-flight request is the implementation's
   * responsibility (via `AbortController`): calling `searchOwners` again aborts
   * the prior call, whose promise rejects with an `AbortError` the caller ignores.
   * Callers use {@link cancelSearch} to abort when there is no successor search.
   */
  searchOwners(query: string, type: OwnerSearchType, limit: number): Promise<SearchResult>;

  /**
   * Aborts the current in-flight {@link searchOwners} request, if any. Used when a search is
   * abandoned without a following one — e.g. the picker unmounts or leaves the search view — so
   * the request does not run to completion. A no-op when nothing is in flight.
   */
  cancelSearch(): void;

  /**
   * Resolves a single owner by id (apps also by public id) so a persisted
   * selection can be rehydrated with its breadcrumb path. Returns `null` when the
   * owner no longer exists or the caller lost permission (backend 404) — the
   * caller then clears the stored selection and falls back to root.
   */
  resolveOwner(ownerId: string): Promise<Owner | null>;
}
