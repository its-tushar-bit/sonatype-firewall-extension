/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Estate (hash-primary) Component Detail href helper (CLM-43961).
 * Used by estate detail tab/Overview self-links, My Scan Data list cards (CLM-43960),
 * and inbound NOSC entity links (CLM-44814).
 *
 * Path query pins stick only when both organizationId and applicationId are present.
 * A lone reportId/scanId is omitted — PathSwitcher would otherwise auto-select an
 * unrelated first org/app and overwrite the report.
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

/**
 * Map inbound NOSC scan context onto estate Path query params when the owning
 * org and app internal ids are already known (no lookups).
 * Policy-evaluation scan ids are the Path {@code reportId}.
 */
export function estateComponentPathFromScan(
  scanId?: string | null,
  extras?: Omit<EstateComponentPathContext, 'reportId'>
): EstateComponentPathContext | undefined {
  const organizationId = extras?.organizationId?.trim() || undefined;
  const applicationId = extras?.applicationId?.trim() || undefined;
  // Without both owners, Path auto-select discards reportId — omit rather than mislead.
  if (!organizationId || !applicationId) {
    return undefined;
  }
  return {
    organizationId,
    applicationId,
    reportId: scanId?.trim() || undefined,
  };
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
