/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { comingSoonHref } from 'MainRoot/nosc/comingSoon/comingSoonModules';
import {
  CLEAN_PATH_OWNED_ELSEWHERE,
  embeddedHref,
  isNativeClassicEmbedSlug,
  NATIVE_CLASSIC_EMBED_SLUGS,
  usesEmbeddedHrefPrimary,
} from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';

describe('nativeClassicEmbedSlugs', () => {
  it('embeds Success Metrics, API, Repositories, Legal, Orgs and Policies, and Enterprise/Operational Reporting', () => {
    expect(NATIVE_CLASSIC_EMBED_SLUGS).toEqual([
      'success-metrics',
      'api',
      'repositories',
      'legal',
      'orgs-and-policies',
      'reports',
    ]);
  });

  it('classifies embed slugs', () => {
    expect(isNativeClassicEmbedSlug('success-metrics')).toBe(true);
    expect(isNativeClassicEmbedSlug('api')).toBe(true);
    expect(isNativeClassicEmbedSlug('repositories')).toBe(true);
    expect(isNativeClassicEmbedSlug('legal')).toBe(true);
    expect(isNativeClassicEmbedSlug('orgs-and-policies')).toBe(true);
    expect(isNativeClassicEmbedSlug('reports')).toBe(true);
    expect(isNativeClassicEmbedSlug('organizations')).toBe(false);
  });

  it('CLEAN_PATH_OWNED_ELSEWHERE lists repositories and legal', () => {
    expect([...CLEAN_PATH_OWNED_ELSEWHERE].sort()).toEqual(['legal', 'repositories']);
  });

  it('usesEmbeddedHrefPrimary is true for every embed except CLEAN_PATH_OWNED_ELSEWHERE', () => {
    NATIVE_CLASSIC_EMBED_SLUGS.forEach((slug) => {
      expect(usesEmbeddedHrefPrimary(slug)).toBe(!CLEAN_PATH_OWNED_ELSEWHERE.has(slug));
    });
    expect(usesEmbeddedHrefPrimary('settings')).toBe(false);
  });

  describe('embeddedHref()', () => {
    it.each(['api', 'success-metrics', 'reports', 'orgs-and-policies'] as const)(
      'returns /%s',
      (slug) => {
        expect(embeddedHref(slug)).toBe(`/${slug}`);
      },
    );

    it('never returns a /coming-soon/ path', () => {
      expect(embeddedHref('api')).not.toMatch(/coming-soon/);
      expect(embeddedHref('api')).toBe('/api');
      expect(comingSoonHref('api')).toBe('/coming-soon/api');
    });

    it('throws for Coming Soon stubs and CLEAN_PATH_OWNED_ELSEWHERE slugs', () => {
      expect(() => embeddedHref('settings')).toThrow(/clean-primary embed slug/);
      expect(() => embeddedHref('repositories')).toThrow(/clean-primary embed slug/);
      expect(() => embeddedHref('legal')).toThrow(/clean-primary embed slug/);
    });
  });
});
