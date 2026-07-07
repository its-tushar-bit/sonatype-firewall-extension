/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { bundleIndexUrl, isNexusOneBundle } from 'MainRoot/util/urlUtil';

function normalizePath(path) {
  const trimmed = (path ?? '').trim();
  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
}

/** Deep-link to a dashboard tab from tiles. */
export function nexusOneDashboardHref(path) {
  const normalized = normalizePath(path);
  if (typeof window !== 'undefined' && isNexusOneBundle()) {
    return `#${normalized}`;
  }
  return bundleIndexUrl('nexus-one', normalized);
}

export function dashboardApplicationsHref() {
  return nexusOneDashboardHref('/dashboard/applications');
}

export function dashboardViolationsHref() {
  return nexusOneDashboardHref('/dashboard/violations');
}

export function dashboardWaiversHref() {
  return nexusOneDashboardHref('/dashboard/waivers');
}

export function dashboardComponentsHref() {
  return nexusOneDashboardHref('/dashboard/components');
}
