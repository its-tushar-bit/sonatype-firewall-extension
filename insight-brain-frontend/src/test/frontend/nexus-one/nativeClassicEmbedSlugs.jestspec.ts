/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  isNativeClassicEmbedSlug,
  NATIVE_CLASSIC_EMBED_SLUGS,
} from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';

describe('nativeClassicEmbedSlugs', () => {
  it('embeds Success Metrics, API, Legal, and Enterprise/Operational Reporting', () => {
    expect(NATIVE_CLASSIC_EMBED_SLUGS).toEqual(['success-metrics', 'api', 'legal', 'reports']);
  });

  it('classifies embed slugs', () => {
    expect(isNativeClassicEmbedSlug('success-metrics')).toBe(true);
    expect(isNativeClassicEmbedSlug('api')).toBe(true);
    expect(isNativeClassicEmbedSlug('legal')).toBe(true);
    expect(isNativeClassicEmbedSlug('reports')).toBe(true);
  });
});
