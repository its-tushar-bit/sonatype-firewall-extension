/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Estate (hash-primary) Component Detail href helper (CLM-43961).
 * Distinct from app-scoped {@link componentDetailHref}.
 * <p>
 * List card → estate detail deep-links are intentionally deferred to CLM-43960
 * ({@code #16861}); this helper is for in-page tab / Overview self-links until then.
 */
export type EstateComponentTab =
  | 'overview'
  | 'legal'
  | 'violations'
  | 'applications'
  | 'organizations';

export function estateComponentDetailHref(
  componentHash: string,
  tab: EstateComponentTab = 'overview',
): string {
  const encoded = encodeURIComponent(componentHash);
  if (tab === 'overview') {
    return `#/components/${encoded}`;
  }
  return `#/components/${encoded}/${tab}`;
}
