/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { ApiError } from 'GuideRoot/api/apiFetch';
import type { OwnerAdapter } from 'GuideRoot/components/navigation/context-picker/OwnerAdapter';
import type {
  AncestorPathEntry,
  AppSummary,
  OrgAppsResult,
  OrgSummary,
  Owner,
  OwnerSearchType,
  SearchResult,
  TopOrgsResult,
} from 'GuideRoot/components/navigation/context-picker/types';

/**
 * In-memory {@link OwnerAdapter} used for UI development and component tests until the
 * GUIDE-3046 backend endpoints land (see `SelfHostedOwnerAdapter`). It mimics the endpoint
 * contracts — alphabetical sorting, per-org app counts, ancestor breadcrumbs, truncation
 * flags, and 404-as-null resolution — over a small fixed tree.
 *
 * The default {@link MOCK_OWNER_FIXTURE} deliberately exercises every branch the picker UI
 * must handle:
 * - an org with `appCount === 0` (Engineering) → directly selectable, no drill;
 * - orgs with `appCount > 0` (Payments, Payments/Frontend, …) → drill targets;
 * - `totalOrgCount > orgs.length` at the default limit of 20 → the "+ N more" hint;
 * - an org flagged as having a truncated app list (Data Platform);
 * - multi-level breadcrumbs (`Commerce / Storefront` on an org; `Payments / Frontend / checkout-app` on an app);
 * - a search query ("platform") that matches more orgs than the search limit → `truncated: true`;
 * - {@link MOCK_MISSING_OWNER_ID}, which resolves to `null` to exercise the rehydrate-to-root path.
 */

interface MockOrgNode {
  id: string;
  name: string;
  parentId: string | null;
  /** Forces `truncated: true` from getAppsForOrg regardless of the fixture app count. */
  appsTruncated?: boolean;
}

interface MockAppNode {
  id: string;
  publicId: string;
  name: string;
  orgId: string;
}

export interface MockOwnerData {
  orgs: MockOrgNode[];
  apps: MockAppNode[];
}

/** An owner id that is guaranteed absent from the fixture; {@link MockOwnerAdapter.resolveOwner} returns `null`. */
export const MOCK_MISSING_OWNER_ID = 'missing-owner';

/**
 * Default fixture — a realistic multi-level org tree (25 orgs, up to 3 levels deep):
 *
 *   Payments · Commerce · Platform Engineering · Security · Data & Analytics · Engineering
 *     Payments → Frontend, Billing
 *     Commerce → Storefront → Web Experience;  Commerce → Fulfillment
 *     Platform Engineering → Platform Core/Runtime/Storage/…/API, Data Platform
 *     Security → Application Security, Infrastructure Security
 *     Data & Analytics → Analytics, Machine Learning
 *
 * getTopOrgs(20) returns 20 of 25 → "+ 5 more". A search for "platform" matches 11 orgs
 * (> the 10 search limit) → truncated. Data Platform is flagged appsTruncated. Engineering has
 * no direct apps → directly selectable. {@link MOCK_MISSING_OWNER_ID} resolves to null.
 */
export const MOCK_OWNER_FIXTURE: MockOwnerData = {
  orgs: [
    // Top-level business units
    { id: 'payments', name: 'Payments', parentId: null },
    { id: 'commerce', name: 'Commerce', parentId: null },
    { id: 'platform', name: 'Platform Engineering', parentId: null },
    { id: 'security', name: 'Security', parentId: null },
    { id: 'data', name: 'Data & Analytics', parentId: null },
    { id: 'engineering', name: 'Engineering', parentId: null },
    // Payments
    { id: 'frontend', name: 'Frontend', parentId: 'payments' },
    { id: 'payments-billing', name: 'Billing', parentId: 'payments' },
    // Commerce (3-level: Commerce → Storefront → Web Experience)
    { id: 'commerce-storefront', name: 'Storefront', parentId: 'commerce' },
    { id: 'commerce-storefront-web', name: 'Web Experience', parentId: 'commerce-storefront' },
    { id: 'commerce-fulfillment', name: 'Fulfillment', parentId: 'commerce' },
    // Platform Engineering (a large org with many teams)
    { id: 'platform-core', name: 'Platform Core', parentId: 'platform' },
    { id: 'platform-runtime', name: 'Platform Runtime', parentId: 'platform' },
    { id: 'platform-storage', name: 'Platform Storage', parentId: 'platform' },
    { id: 'platform-networking', name: 'Platform Networking', parentId: 'platform' },
    { id: 'platform-identity', name: 'Platform Identity', parentId: 'platform' },
    { id: 'platform-observability', name: 'Platform Observability', parentId: 'platform' },
    { id: 'platform-delivery', name: 'Platform Delivery', parentId: 'platform' },
    { id: 'platform-tooling', name: 'Platform Tooling', parentId: 'platform' },
    { id: 'platform-api', name: 'Platform API', parentId: 'platform' },
    { id: 'data-platform', name: 'Data Platform', parentId: 'platform', appsTruncated: true },
    // Security
    { id: 'security-appsec', name: 'Application Security', parentId: 'security' },
    { id: 'security-infra', name: 'Infrastructure Security', parentId: 'security' },
    // Data & Analytics
    { id: 'data-analytics', name: 'Analytics', parentId: 'data' },
    { id: 'data-ml', name: 'Machine Learning', parentId: 'data' },
  ],
  apps: [
    { id: 'app-payment-service', publicId: 'payment-service', name: 'payment-service', orgId: 'payments' },
    { id: 'app-billing-gateway', publicId: 'billing-gateway', name: 'billing-gateway', orgId: 'payments' },
    { id: 'app-checkout', publicId: 'checkout-app', name: 'checkout-app', orgId: 'frontend' },
    { id: 'app-cart-ui', publicId: 'cart-ui', name: 'cart-ui', orgId: 'frontend' },
    { id: 'app-account-ui', publicId: 'account-ui', name: 'account-ui', orgId: 'frontend' },
    { id: 'app-invoicing', publicId: 'invoicing-service', name: 'invoicing-service', orgId: 'payments-billing' },
    { id: 'app-storefront-web', publicId: 'storefront-web', name: 'storefront-web', orgId: 'commerce-storefront-web' },
    { id: 'app-shipping', publicId: 'shipping-service', name: 'shipping-service', orgId: 'commerce-fulfillment' },
    { id: 'app-ingest', publicId: 'data-ingest', name: 'data-ingest', orgId: 'data-platform' },
    { id: 'app-warehouse', publicId: 'data-warehouse', name: 'data-warehouse', orgId: 'data-platform' },
  ],
};

const byNameCaseInsensitive = (a: { name: string }, b: { name: string }) =>
  a.name.localeCompare(b.name, undefined, { sensitivity: 'accent' });

export class MockOwnerAdapter implements OwnerAdapter {
  private readonly orgsById: Map<string, MockOrgNode>;

  constructor(private readonly data: MockOwnerData = MOCK_OWNER_FIXTURE) {
    this.orgsById = new Map(data.orgs.map((o) => [o.id, o]));
  }

  getTopOrgs(limit: number): Promise<TopOrgsResult> {
    const sorted = [...this.data.orgs].sort(byNameCaseInsensitive);
    const orgs = sorted.slice(0, limit).map((o) => this.toOrgSummary(o));
    return Promise.resolve({ orgs, totalOrgCount: this.data.orgs.length });
  }

  getAppsForOrg(orgId: string, limit: number, signal?: AbortSignal): Promise<OrgAppsResult> {
    if (signal?.aborted) {
      return Promise.reject(new DOMException('Aborted', 'AbortError'));
    }
    const org = this.orgsById.get(orgId);
    if (!org) {
      // Mirror the real adapter: an unknown/deleted org surfaces as an ApiError 404 (SelfHostedOwnerAdapter
      // gets this from apiFetch), so callers exercise the same error shape in dev and tests as in prod.
      return Promise.reject(new ApiError(`Organization not found: ${orgId}`, 404, 'Not Found'));
    }
    const all = this.data.apps.filter((a) => a.orgId === orgId).sort(byNameCaseInsensitive);
    const apps = all.slice(0, limit).map((a) => this.toAppSummary(a));
    const truncated = Boolean(org.appsTruncated) || all.length > limit;
    return Promise.resolve({ apps, truncated });
  }

  searchOwners(query: string, type: OwnerSearchType, limit: number): Promise<SearchResult> {
    const q = query.trim().toLowerCase();
    const includeOrgs = type === 'all' || type === 'org';
    const includeApps = type === 'all' || type === 'app';

    const matchedOrgs = includeOrgs
      ? this.data.orgs.filter((o) => o.name.toLowerCase().includes(q)).sort(byNameCaseInsensitive)
      : [];
    const matchedApps = includeApps
      ? this.data.apps.filter((a) => a.name.toLowerCase().includes(q)).sort(byNameCaseInsensitive)
      : [];

    const truncated = matchedOrgs.length > limit || matchedApps.length > limit;
    const results: Owner[] = [
      ...matchedOrgs.slice(0, limit).map((o) => this.toOrgSummary(o)),
      ...matchedApps.slice(0, limit).map((a) => this.toAppSummary(a)),
    ];
    return Promise.resolve({ results, truncated });
  }

  cancelSearch(): void {
    // No-op: the mock resolves searches synchronously, so there is never an in-flight request.
  }

  resolveOwner(ownerId: string): Promise<Owner | null> {
    const org = this.orgsById.get(ownerId);
    if (org) {
      return Promise.resolve(this.toOrgSummary(org));
    }
    const app = this.data.apps.find((a) => a.id === ownerId || a.publicId === ownerId);
    if (app) {
      return Promise.resolve(this.toAppSummary(app));
    }
    return Promise.resolve(null);
  }

  private toOrgSummary(org: MockOrgNode): OrgSummary {
    const appCount = this.data.apps.filter((a) => a.orgId === org.id).length;
    return {
      id: org.id,
      publicId: org.id,
      name: org.name,
      type: 'org',
      ancestorPath: this.ancestorPathForOrg(org),
      appCount,
    };
  }

  private toAppSummary(app: MockAppNode): AppSummary {
    const org = this.orgsById.get(app.orgId);
    const ancestorPath: AncestorPathEntry[] = org
      ? [...this.ancestorPathForOrg(org), { id: org.id, name: org.name, type: 'org' }]
      : [];
    return {
      id: app.id,
      publicId: app.publicId,
      name: app.name,
      type: 'app',
      ancestorPath,
    };
  }

  /** Root-to-parent chain (exclusive of the org itself), ordered root-first. */
  private ancestorPathForOrg(org: MockOrgNode): AncestorPathEntry[] {
    const chain: AncestorPathEntry[] = [];
    let current = org.parentId ? this.orgsById.get(org.parentId) : undefined;
    while (current) {
      chain.unshift({ id: current.id, name: current.name, type: 'org' });
      current = current.parentId ? this.orgsById.get(current.parentId) : undefined;
    }
    return chain;
  }
}
