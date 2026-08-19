/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { comingSoonHref, type ComingSoonModuleSlug } from 'MainRoot/nosc/comingSoon';

/**
 * NOUX shell navigation helpers for entity-page CTAs.
 *
 * Shell contract: users are in Classic **or** NOUX. TopNav owns the only
 * Classic-aware toggle. Entity pages must stay on nexus-one hash routes
 * (including in-shell RSC embeds). Do **not** call
 * {@code bundleIndexUrl('classic', …)} from Quick Actions, detail footers,
 * or Coming Soon pages.
 */

/** Prefix an in-app path as a nexus-one hash href. */
export function nouxHashHref(path: string): string {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  return `#${normalized}`;
}

export interface NouxApplicationReportHrefParams {
  readonly publicId: string;
  readonly scanId: string;
  readonly componentHash?: string;
  readonly tabId?: string;
}

/**
 * In-shell application policy report ({@code nexusOneApplicationReport}).
 * Matches {@link nexusOneApplicationReportStates} URL shape.
 */
export function nouxApplicationReportHref(params: NouxApplicationReportHrefParams): string {
  const base = `/applications/${encodeURIComponent(params.publicId)}/report/${encodeURIComponent(params.scanId)}`;
  const qs = new URLSearchParams();
  if (params.componentHash) {
    qs.set('componentHash', params.componentHash);
  }
  if (params.tabId) {
    qs.set('tabId', params.tabId);
  }
  const query = qs.toString();
  return nouxHashHref(query ? `${base}?${query}` : base);
}

/** Coming Soon (or native RSC embed registered under the same slug) hash href. */
export function nouxComingSoonHref(slug: ComingSoonModuleSlug): string {
  return nouxHashHref(comingSoonHref(slug));
}

/** Estate waivers list. */
export function nouxWaiversListHref(): string {
  return '#/waivers';
}

/** Application-detail Waivers tab. */
export function nouxApplicationWaiversHref(publicId: string): string {
  return nouxHashHref(`/applications/${encodeURIComponent(publicId)}/waivers`);
}

/**
 * Orgs & Policies embed — application owner summary (policies, inheritance, access).
 * Already registered under the Nexus One {@code management.*} tree.
 */
export function nouxApplicationPoliciesHref(publicId: string): string {
  return nouxHashHref(`/management/view/application/${encodeURIComponent(publicId)}`);
}

/**
 * Orgs & Policies embed — application Source Control configuration.
 * {@code management.edit.application.edit-source-control}.
 */
export function nouxApplicationSourceControlHref(publicId: string): string {
  return nouxHashHref(`/management/edit/application/${encodeURIComponent(publicId)}/source-control`);
}

/** Enterprise / Operational Reporting Classic RSC embed ({@code /reports}). */
export function nouxReportsHref(): string {
  return '#/reports';
}
