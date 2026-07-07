/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { bundleIndexUrl, isNexusOneBundle } from 'MainRoot/util/urlUtil';
import { comingSoonHref } from 'MainRoot/nosc/comingSoon';

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

// Cheap-tier cards (CLM-40927) deep-link to their Classic equivalents until native
// Nexus One pages exist, mirroring the LeftNav Classic deep-link convention.
export function dashboardVulnerabilitiesHref() {
  return bundleIndexUrl('classic', '/vulnerabilities');
}

export function dashboardLegalHref() {
  return bundleIndexUrl('classic', '/legal/dashboard');
}

export function dashboardOrgsAndPoliciesHref() {
  return bundleIndexUrl('classic', '/management/view/organization/ROOT_ORGANIZATION_ID');
}

// Bottom-row quick links — match the LeftNav Classic deep-link targets.
export function dashboardSuccessMetricsHref() {
  return nexusOneEntityHref(comingSoonHref('success-metrics'));
}

export function dashboardEnterpriseReportingHref() {
  return bundleIndexUrl('classic', '/enterpriseReporting');
}

export function dashboardApiHref() {
  return nexusOneEntityHref(comingSoonHref('api'));
}
