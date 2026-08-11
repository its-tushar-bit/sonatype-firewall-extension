/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Estate (hash-primary) Component Detail href helper (CLM-43961).
 * Distinct from app-scoped {@link componentDetailHref}.
 * Used by estate detail tab/Overview self-links and My Scan Data list cards (CLM-43960).
 */
export type EstateComponentTab = 'overview' | 'vulnerabilities' | 'violations' | 'applications';

export type EstateComponentPathContext = {
  readonly organizationId?: string;
  readonly applicationId?: string;
  readonly reportId?: string;
};

function appendPathContextQuery(href: string, pathContext?: EstateComponentPathContext): string {
  if (!pathContext) {
    return href;
  }
  const params = new URLSearchParams();
  const organizationId = pathContext.organizationId?.trim();
  const applicationId = pathContext.applicationId?.trim();
  const reportId = pathContext.reportId?.trim();
  if (organizationId) {
    params.set('organizationId', organizationId);
  }
  if (applicationId) {
    params.set('applicationId', applicationId);
  }
  if (reportId) {
    params.set('reportId', reportId);
  }
  const query = params.toString();
  return query ? `${href}?${query}` : href;
}

export function estateComponentDetailHref(
  componentHash: string,
  tab: EstateComponentTab = 'overview',
  pathContext?: EstateComponentPathContext
): string {
  const encoded = encodeURIComponent(componentHash);
  const base = tab === 'overview' ? `#/components/${encoded}` : `#/components/${encoded}/${tab}`;
  return appendPathContextQuery(base, pathContext);
}
