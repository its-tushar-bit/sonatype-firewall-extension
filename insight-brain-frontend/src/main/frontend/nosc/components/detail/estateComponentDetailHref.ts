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

export function estateComponentDetailHref(componentHash: string, tab: EstateComponentTab = 'overview'): string {
  const encoded = encodeURIComponent(componentHash);
  if (tab === 'overview') {
    return `#/components/${encoded}`;
  }
  return `#/components/${encoded}/${tab}`;
}
