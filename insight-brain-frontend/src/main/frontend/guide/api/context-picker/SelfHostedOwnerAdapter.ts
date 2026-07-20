/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { ApiError, apiFetch, GuideLicenseRevokedError } from 'GuideRoot/api/apiFetch';
import type { OwnerAdapter } from 'GuideRoot/components/navigation/context-picker/OwnerAdapter';
import type {
  AncestorPathEntry,
  AppSummary,
  OrgAppsResult,
  OrgSummary,
  Owner,
  OwnerSearchType,
  OwnerType,
  SearchResult,
  TopOrgsResult,
} from 'GuideRoot/components/navigation/context-picker/types';

/**
 * Self-hosted IQ implementation of {@link OwnerAdapter}, backed by the GUIDE-3046
 * `/api/v2/policy-context/owners/*` endpoints via {@link apiFetch}.
 *
 * This is the ONE file a future Guide SaaS host would replace (not move) with its own adapter.
 * It owns every backend-specific concern so the picker component stays host-agnostic:
 * - maps the REST `"organization"` / `"application"` type strings to the picker's `org` / `app`;
 * - folds the search endpoint's two-array response (`orgs` + `apps`) into a flat {@link SearchResult};
 * - cancels a superseded in-flight search with an `AbortController` (see {@link searchOwners});
 * - translates a 404 from {@link resolveOwner} into `null`.
 */

const OWNERS_BASE = '/api/v2/policy-context/owners';

/** Raw REST shapes — kept private to this file so backend DTOs never leak into picker types. */
interface RawPathEntry {
  id: string;
  name: string;
  type: string;
}

interface RawOrgSummary {
  id: string;
  publicId: string;
  name: string;
  type: string;
  ancestorPath: RawPathEntry[];
  appCount: number;
}

interface RawAppSummary {
  id: string;
  publicId: string;
  name: string;
  type: string;
  ancestorPath: RawPathEntry[];
}

interface RawTopOrgsResponse {
  orgs: RawOrgSummary[];
  totalOrgCount: number;
}

interface RawOrgAppsResponse {
  apps: RawAppSummary[];
  truncated: boolean;
}

interface RawSearchResponse {
  orgs: RawOrgSummary[];
  orgsTruncated: boolean;
  apps: RawAppSummary[];
  appsTruncated: boolean;
}

type RawOwnerSummary = RawOrgSummary | RawAppSummary;

function mapType(raw: string): OwnerType {
  if (raw === 'organization') {
    return 'org';
  }
  if (raw === 'application') {
    return 'app';
  }
  // Unknown type (backend typo, a new type, or GUIDE-3046 schema drift): warn so the mis-classification
  // is diagnosable, and fall back to 'app' so lists/rehydrate degrade gracefully instead of throwing.
  console.warn(`[SelfHostedOwnerAdapter] Unexpected owner type "${raw}"; treating as application.`);
  return 'app';
}

function mapAncestorPath(raw: RawPathEntry[]): AncestorPathEntry[] {
  return (raw ?? []).map((entry) => ({ id: entry.id, name: entry.name, type: mapType(entry.type) }));
}

function mapOrg(raw: RawOrgSummary): OrgSummary {
  return {
    id: raw.id,
    publicId: raw.publicId,
    name: raw.name,
    type: 'org',
    ancestorPath: mapAncestorPath(raw.ancestorPath),
    appCount: raw.appCount,
  };
}

function mapApp(raw: RawAppSummary): AppSummary {
  return {
    id: raw.id,
    publicId: raw.publicId,
    name: raw.name,
    type: 'app',
    ancestorPath: mapAncestorPath(raw.ancestorPath),
  };
}

function mapOwner(raw: RawOwnerSummary): Owner {
  // Route through mapType so an unexpected type is warned about here too — this backs resolveOwner,
  // i.e. the rehydrate-from-localStorage path — rather than being silently classified as an app.
  return mapType(raw.type) === 'org' ? mapOrg(raw as RawOrgSummary) : mapApp(raw as RawAppSummary);
}

export class SelfHostedOwnerAdapter implements OwnerAdapter {
  /**
   * Holds the in-flight search request so a subsequent {@link searchOwners} call can abort it.
   * Cancellation lives here (not in the component) so the picker never races setTimeout against
   * setState; the aborted call rejects with an `AbortError` the caller ignores.
   */
  private searchAbortController: AbortController | null = null;

  async getTopOrgs(limit: number): Promise<TopOrgsResult> {
    const raw = await apiFetch<RawTopOrgsResponse>(`${OWNERS_BASE}/top-orgs?limit=${limit}`);
    return { orgs: raw.orgs.map(mapOrg), totalOrgCount: raw.totalOrgCount };
  }

  async getAppsForOrg(orgId: string, limit: number, signal?: AbortSignal): Promise<OrgAppsResult> {
    const raw = await apiFetch<RawOrgAppsResponse>(
      `${OWNERS_BASE}/orgs/${encodeURIComponent(orgId)}/apps?limit=${limit}`,
      { signal }
    );
    return { apps: raw.apps.map(mapApp), truncated: raw.truncated };
  }

  async searchOwners(query: string, type: OwnerSearchType, limit: number): Promise<SearchResult> {
    // Abort a still-pending prior search before starting a new one.
    this.searchAbortController?.abort();
    const controller = new AbortController();
    this.searchAbortController = controller;

    const params = new URLSearchParams({ query, type, limit: String(limit) });
    try {
      const raw = await apiFetch<RawSearchResponse>(`${OWNERS_BASE}/search?${params.toString()}`, {
        signal: controller.signal,
      });
      const results: Owner[] = [...raw.orgs.map(mapOrg), ...raw.apps.map(mapApp)];
      return { results, truncated: raw.orgsTruncated || raw.appsTruncated };
    } finally {
      // Only clear if we are still the current request (a newer call may have replaced us).
      if (this.searchAbortController === controller) {
        this.searchAbortController = null;
      }
    }
  }

  cancelSearch(): void {
    this.searchAbortController?.abort();
    this.searchAbortController = null;
  }

  async resolveOwner(ownerId: string): Promise<Owner | null> {
    try {
      const raw = await apiFetch<RawOwnerSummary>(`${OWNERS_BASE}/${encodeURIComponent(ownerId)}`);
      return mapOwner(raw);
    } catch (error) {
      // A revoked-license error has its own recovery flow (LicenseGate) — never swallow it.
      if (error instanceof GuideLicenseRevokedError) {
        throw error;
      }
      // 404 = not found or lost permission (indistinguishable by design) → clear + fall back to root.
      if (error instanceof ApiError && error.status === 404) {
        return null;
      }
      throw error;
    }
  }
}
