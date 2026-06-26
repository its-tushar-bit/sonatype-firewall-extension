/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ComingSoonModuleSlug } from 'MainRoot/nosc/comingSoon';

/** CLM-41537 POC scope. Remaining ledger modules ship in CLM-41538. */
export const NATIVE_CLASSIC_EMBED_SLUGS: readonly ComingSoonModuleSlug[] = ['success-metrics', 'api'];

export function isNativeClassicEmbedSlug(slug: ComingSoonModuleSlug): boolean {
  return (NATIVE_CLASSIC_EMBED_SLUGS as readonly string[]).includes(slug);
}
