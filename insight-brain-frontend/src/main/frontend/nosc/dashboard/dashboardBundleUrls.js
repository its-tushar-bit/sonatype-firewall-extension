/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { bundleIndexUrl, isNexusOneBundle } from 'MainRoot/util/urlUtil';
import { embeddedHref } from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
import { NEXUS_ONE_LEGAL_PATH } from 'MainRoot/nosc/legal/legalRoute';

function normalizePath(path) {
  const trimmed = (path ?? '').trim();
  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
}

/** Deep-link to a native Nexus One route from dashboard tiles/cards. */
export function nexusOneEntityHref(path) {
  const normalized = normalizePath(path);
  if (typeof window !== 'undefined' && isNexusOneBundle()) {
    return `#${normalized}`;
  }
  return bundleIndexUrl('nexus-one', normalized);
}

export function dashboardApplicationsHref() {
  return nexusOneEntityHref('/applications');
}

// Top-level `/violations` and `/components` redirect to dashboard tab views until
// native entity-list pages register at these paths (see nexus-one/routes.tsx).
export function dashboardViolationsHref() {
  return nexusOneEntityHref('/violations');
}

export function dashboardWaiversHref(options = {}) {
  if (options.expiresSoon) {
    return nexusOneEntityHref('/waivers?lifecycle=expiring');
  }
  return nexusOneEntityHref('/waivers');
}

export function dashboardComponentsHref() {
  return nexusOneEntityHref('/components');
}

// CLM-43206 Dashboard Deep Dives v1: prefer native NOUX when it exists.
export function dashboardVulnerabilitiesHref() {
  return nexusOneEntityHref('/vulnerabilities');
}

/**
 * CLM-44467: Legal deep-dive is ALP-aware.
 * With Advanced Legal Pack → Classic Obligations embed (same as LeftNav).
 * Without ALP → native LEGAL_VIOLATION list at {@code /legal-risk} (Classic Obligations 402s).
 *
 * @param {boolean} [advancedLegalPack=false]
 */
export function dashboardLegalHref(advancedLegalPack = false) {
  if (advancedLegalPack) {
    return nexusOneEntityHref(embeddedHref('legal'));
  }
  return nexusOneEntityHref(NEXUS_ONE_LEGAL_PATH);
}

// Orgs & Policies embeds Classic management in-shell at #/orgs-and-policies
// (NATIVE_CLASSIC_EMBED).
export function dashboardOrgsAndPoliciesHref() {
  return nexusOneEntityHref(embeddedHref('orgs-and-policies'));
}

// Bottom-row quick links — Success Metrics, Enterprise Reporting, and API stay
// in-shell Classic embeds (same destinations as LeftNav; CLM-43206).
export function dashboardSuccessMetricsHref() {
  return nexusOneEntityHref(embeddedHref('success-metrics'));
}

export function dashboardEnterpriseReportingHref() {
  return nexusOneEntityHref(embeddedHref('reports'));
}

export function dashboardApiHref() {
  return nexusOneEntityHref(embeddedHref('api'));
}
