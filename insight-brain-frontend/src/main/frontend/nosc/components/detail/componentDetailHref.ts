/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Native Nexus One Component detail hash (CLM-42767).
 * Hybrid Lifecycle URL: application + component, optional scan context.
 */
export function componentDetailHref(
  applicationPublicId: string,
  componentHash: string,
  scanId?: string | null,
): string {
  const base = `#/applications/${encodeURIComponent(applicationPublicId)}/components/${encodeURIComponent(componentHash)}`;
  if (scanId) {
    return `${base}?scanId=${encodeURIComponent(scanId)}`;
  }
  return base;
}
