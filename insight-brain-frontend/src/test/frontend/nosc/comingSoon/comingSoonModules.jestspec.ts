/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  COMING_SOON_MODULES,
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
} from 'MainRoot/nosc/comingSoon/comingSoonModules';

/**
 * P1-F15 / CLM-39545: Registry-level invariants. These guard rails ensure
 * a new "Coming Soon" entry can never ship with TBD placeholder text, a
 * broken Classic href, or a misordered slug list.
 */
describe('comingSoonModules registry', () => {
  it('exports at least one module so the LeftNav has something to render', () => {
    expect(COMING_SOON_MODULE_ORDER.length).toBeGreaterThan(0);
  });

  it('every slug in COMING_SOON_MODULE_ORDER has a registry entry', () => {
    // ORDER is a subset of KEYS: every LeftNav-rendered slug must have
    // its metadata in the registry. KEYS may legitimately contain extras
    // (e.g. Epic 10 placeholders `applications` / `waivers` that have a
    // /preview/<slug> route + ComingSoonPage but are NOT rendered in the
    // LeftNav — the LeftNav has its own native Applications entry).
    const keysSet = new Set(Object.keys(COMING_SOON_MODULES));
    for (const slug of COMING_SOON_MODULE_ORDER) {
      expect(keysSet.has(slug)).toBe(true);
    }
  });

  it('COMING_SOON_MODULE_ORDER has no duplicates', () => {
    const orderSet = new Set(COMING_SOON_MODULE_ORDER);
    expect(orderSet.size).toBe(COMING_SOON_MODULE_ORDER.length);
  });

  describe.each(COMING_SOON_MODULE_ORDER)('entry "%s"', (slug) => {
    const mod = COMING_SOON_MODULES[slug];

    it('has a non-empty label', () => {
      expect(typeof mod.label).toBe('string');
      expect(mod.label.length).toBeGreaterThan(0);
    });

    it('has a non-empty description', () => {
      expect(typeof mod.description).toBe('string');
      expect(mod.description.length).toBeGreaterThan(0);
    });

    it('has a fully-qualified Classic deep link starting with /assets/#', () => {
      expect(typeof mod.classicHref).toBe('string');
      expect(mod.classicHref.startsWith('/assets/#')).toBe(true);
    });

    it('contains no TBD/TODO/FIXME placeholder text in any field', () => {
      const combined = `${mod.label} ${mod.description} ${mod.classicHref}`;
      expect(combined).not.toMatch(/\b(TBD|TODO|FIXME|XXX)\b/i);
    });
  });

  describe('comingSoonHref()', () => {
    it.each(COMING_SOON_MODULE_ORDER)('returns /coming-soon/%s', (slug) => {
      expect(comingSoonHref(slug)).toBe(`/coming-soon/${slug}`);
    });
  });
});
