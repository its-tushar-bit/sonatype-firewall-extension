/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { bundleIndexUrl, isNexusOneBundle } from 'MainRoot/util/urlUtil';
import { embeddedHref } from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';

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

export function dashboardWaiversHref() {
  return nexusOneEntityHref('/waivers');
}

export function dashboardComponentsHref() {
  return nexusOneEntityHref('/components');
}

// CLM-43206 Dashboard Deep Dives v1: prefer native NOUX when it exists.
export function dashboardVulnerabilitiesHref() {
  return nexusOneEntityHref('/vulnerabilities');
}

// Legal embeds Classic Legal Dashboard in-shell at /legal (NATIVE_CLASSIC_EMBED).
export function dashboardLegalHref() {
  return nexusOneEntityHref(embeddedHref('legal'));
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
