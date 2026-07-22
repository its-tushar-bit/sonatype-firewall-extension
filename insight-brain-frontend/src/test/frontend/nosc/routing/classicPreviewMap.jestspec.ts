/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  toClassicEquivalent,
  toNexusOneEquivalent,
  CLASSIC_DEFAULT_PATH,
  NEXUS_ONE_DEFAULT_PATH,
} from 'MainRoot/nosc/routing/classicPreviewMap';
import {
  COMING_SOON_MODULES,
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
} from 'MainRoot/nosc/comingSoon/comingSoonModules';

/**
 * Mirrors Repo's same-page-toggle pattern: the toggle takes you to the equivalent
 * page on the other UI, never to the landing page. If no mapping exists, fall
 * back to the default (Dashboard).
 *
 * Both directions are pure functions — no side effects, no IO. Hash prefix is
 * tolerated and stripped because callers may pass `window.location.hash`
 * directly (with leading `#`) or already-trimmed paths.
 */
describe('classicPreviewMap', () => {
  describe('toNexusOneEquivalent (Classic -> Preview)', () => {
    it('maps the Classic Dashboard root to /dashboard', () => {
      expect(toNexusOneEquivalent('/dashboard/violations')).toBe('/dashboard');
    });

    it('maps generic /dashboard/<tab> URLs to /dashboard', () => {
      // Classic has /dashboard/violations, /dashboard/components,
      // /dashboard/waivers, etc. All of them collapse to the single
      // Preview Dashboard surface via the prefix-match in
      // CLASSIC_PREFIX_TO_PREVIEW.
      //
      // Exception: /dashboard/applications has its own dedicated mapping
      // (it's the canonical Classic destination for /applications)
      // so it does NOT collapse here. That's asserted in the next test.
      expect(toNexusOneEquivalent('/dashboard/components')).toBe('/dashboard');
      expect(toNexusOneEquivalent('/dashboard/violations')).toBe('/dashboard');
      expect(toNexusOneEquivalent('/dashboard/waivers')).toBe('/dashboard');
    });

    it('maps Classic application list to /applications', () => {
      // Classic has two app-list URLs that both round-trip to
      // /applications: the Dashboard's Applications tab (canonical
      // toggle target) and the Orgs & Policies management view (used by
      // search-result click-through with a publicId suffix).
      expect(toNexusOneEquivalent('/dashboard/applications')).toBe('/applications');
      expect(toNexusOneEquivalent('/management/view/application')).toBe('/applications');
    });

    it('maps Classic Preview-UI Settings to /ui-settings in the nexus-one bundle', () => {
      expect(toNexusOneEquivalent('/previewUiSettings')).toBe('/ui-settings');
    });

    it('maps Classic /hostedRepos to /repositories', () => {
      // Hosted Repos uses a native embedded mount, not a Coming Soon stub (CLM-42184).
      expect(toNexusOneEquivalent('/hostedRepos')).toBe('/repositories');
    });

    it('maps deep Classic Hosted Repos paths to /repositories 1-1, preserving the tail', () => {
      // The Hosted Repos drill-down hierarchy (manager -> repo -> components) shares
      // an identical sub-path structure in both bundles, so a deep link must round-trip
      // 1-1 rather than collapsing to the list page (CLM-42184).
      expect(toNexusOneEquivalent('/hostedRepos/mgr-123')).toBe('/repositories/mgr-123');
      expect(toNexusOneEquivalent('/hostedRepos/mgr-123/repo-456/components')).toBe(
        '/repositories/mgr-123/repo-456/components',
      );
      expect(toNexusOneEquivalent('/hostedRepos/mgr-123/repo-456/components?repositoryPublicId=my-repo')).toBe(
        '/repositories/mgr-123/repo-456/components?repositoryPublicId=my-repo',
      );
    });

    it('keeps the Success Metrics admin path identical on both bundles (CLM-42186)', () => {
      expect(toNexusOneEquivalent('/successMetricsConfiguration')).toBe('/successMetricsConfiguration');
    });

    it('keeps the Users admin path identical on both bundles (CLM-42465)', () => {
      expect(toNexusOneEquivalent('/users')).toBe('/users');
    });

    it('keeps /users sub-paths identical on both bundles, preserving the tail (CLM-42465)', () => {
      expect(toNexusOneEquivalent('/users/_new_')).toBe('/users/_new_');
      expect(toNexusOneEquivalent('/users/some-user-id')).toBe('/users/some-user-id');
      expect(toNexusOneEquivalent('/users/activity/some-user')).toBe('/users/activity/some-user');
    });

    it('keeps the User Activity admin path identical on both bundles (CLM-42465)', () => {
      expect(toNexusOneEquivalent('/user-activity')).toBe('/user-activity');
    });

    it('keeps the Base URL admin path identical on both bundles (CLM-42463)', () => {
      expect(toNexusOneEquivalent('/baseUrl')).toBe('/baseUrl');
    });

    it('keeps the System Notice admin path identical on both bundles', () => {
      expect(toNexusOneEquivalent('/systemNoticeConfiguration')).toBe('/systemNoticeConfiguration');
    });

    it('keeps the Administrators admin path identical on both bundles (CLM-42464)', () => {
      expect(toNexusOneEquivalent('/administrators')).toBe('/administrators');
    });

    it('falls back to NEXUS_ONE_DEFAULT_PATH for unmapped Classic URLs', () => {
      // `/orgsAndPolicies` (with the typo'd lowercase first letter)
      // and a fake nonsense path are both genuinely unmapped — they
      // should fall through to the default. NOTE: as new Coming Soon
      // stubs are added with their `classicHref` covering more
      // Classic URLs, those URLs naturally graduate from "unmapped" to
      // "round-trips to the new stub" — this is correct behavior. Use
      // truly-fictional paths in this assertion to avoid that drift.
      expect(toNexusOneEquivalent('/orgsAndPolicies')).toBe(NEXUS_ONE_DEFAULT_PATH);
      expect(toNexusOneEquivalent('/somenewpage')).toBe(NEXUS_ONE_DEFAULT_PATH);
      expect(toNexusOneEquivalent('/zzz-nonexistent-route-zzz')).toBe(NEXUS_ONE_DEFAULT_PATH);
    });

    it('strips the leading # if the caller passes window.location.hash directly', () => {
      expect(toNexusOneEquivalent('#/dashboard/violations')).toBe('/dashboard');
      expect(toNexusOneEquivalent('#dashboard/violations')).toBe('/dashboard');
    });

    it('handles empty / root paths by returning the default', () => {
      expect(toNexusOneEquivalent('')).toBe(NEXUS_ONE_DEFAULT_PATH);
      expect(toNexusOneEquivalent('/')).toBe(NEXUS_ONE_DEFAULT_PATH);
      expect(toNexusOneEquivalent('#')).toBe(NEXUS_ONE_DEFAULT_PATH);
    });

    it('returns the default if asked from an already-Preview URL (defensive)', () => {
      // Caller is on Classic and reading window.location.hash; this case
      // shouldn't happen in practice but guard against it returning a
      // nonsense double-preview path.
      expect(toNexusOneEquivalent('/dashboard')).toBe(NEXUS_ONE_DEFAULT_PATH);
    });
  });

  describe('toClassicEquivalent (Preview -> Classic)', () => {
    it('maps /dashboard back to Classic Dashboard root', () => {
      expect(toClassicEquivalent('/dashboard')).toBe('/dashboard/violations');
    });

    it('maps /applications back to /dashboard/applications', () => {
      // /management/view/application requires a {publicId} segment, so
      // clicking the toggle on /applications previously produced
      // an "Unknown Address" unrecoverable error. Toggle target is now
      // /dashboard/applications which is a valid list view of all apps.
      expect(toClassicEquivalent('/applications')).toBe('/dashboard/applications');
    });

    it('maps /ui-settings back to Classic admin settings', () => {
      expect(toClassicEquivalent('/ui-settings')).toBe('/previewUiSettings');
    });

    it('maps /repositories back to Classic /hostedRepos', () => {
      // Hosted Repos uses a native embedded mount, not a Coming Soon stub (CLM-42184).
      expect(toClassicEquivalent('/repositories')).toBe('/hostedRepos');
    });

    it('maps deep Hosted Repos paths back to Classic 1-1, preserving the tail', () => {
      // Switching bundles from a manager/repo/components page must land on the same
      // page in Classic, not collapse to the list page (CLM-42184).
      expect(toClassicEquivalent('/repositories/mgr-123')).toBe('/hostedRepos/mgr-123');
      expect(toClassicEquivalent('/repositories/mgr-123/repo-456/components')).toBe(
        '/hostedRepos/mgr-123/repo-456/components',
      );
      expect(toClassicEquivalent('/repositories/mgr-123/repo-456/components?repositoryPublicId=my-repo')).toBe(
        '/hostedRepos/mgr-123/repo-456/components?repositoryPublicId=my-repo',
      );
    });

    it('maps Success Metrics admin back to the same Classic path (CLM-42186)', () => {
      expect(toClassicEquivalent('/successMetricsConfiguration')).toBe('/successMetricsConfiguration');
    });

    it('maps Users admin back to the same Classic path (CLM-42465)', () => {
      expect(toClassicEquivalent('/users')).toBe('/users');
    });

    it('preserves /users sub-paths when toggling to Classic (CLM-42465)', () => {
      expect(toClassicEquivalent('/users/_new_')).toBe('/users/_new_');
      expect(toClassicEquivalent('/users/some-user-id')).toBe('/users/some-user-id');
      expect(toClassicEquivalent('/users/activity/some-user')).toBe('/users/activity/some-user');
    });

    it('maps User Activity admin back to the same Classic path (CLM-42465)', () => {
      expect(toClassicEquivalent('/user-activity')).toBe('/user-activity');
    });

    it('maps Base URL admin back to the same Classic path (CLM-42463)', () => {
      expect(toClassicEquivalent('/baseUrl')).toBe('/baseUrl');
    });

    it('maps System Notice admin back to the same Classic path', () => {
      expect(toClassicEquivalent('/systemNoticeConfiguration')).toBe('/systemNoticeConfiguration');
    });

    it('maps Administrators admin back to the same Classic path (CLM-42464)', () => {
      expect(toClassicEquivalent('/administrators')).toBe('/administrators');
    });

    it('falls back to CLASSIC_DEFAULT_PATH for unmapped Preview URLs', () => {
      // Phase-1.5 surfaces (search, platform-home, guide, firewall, sbom)
      // don't have Classic equivalents — fall back to the Classic landing.
      expect(toClassicEquivalent('/search')).toBe(CLASSIC_DEFAULT_PATH);
      expect(toClassicEquivalent('/home')).toBe(CLASSIC_DEFAULT_PATH);
      expect(toClassicEquivalent('/coming-soon/guide')).toBe(CLASSIC_DEFAULT_PATH);
    });

    it('strips the leading # if the caller passes window.location.hash directly', () => {
      expect(toClassicEquivalent('#/dashboard')).toBe('/dashboard/violations');
    });

    it('handles empty / root paths by returning the default', () => {
      expect(toClassicEquivalent('')).toBe(CLASSIC_DEFAULT_PATH);
      expect(toClassicEquivalent('/')).toBe(CLASSIC_DEFAULT_PATH);
    });

    it('returns the default if asked from an already-Classic URL (defensive)', () => {
      expect(toClassicEquivalent('/dashboard/violations')).toBe(CLASSIC_DEFAULT_PATH);
    });
  });

  describe('Application detail page round-trip (CLM-39709 / P1-F7c)', () => {
    it('maps /applications/{publicId} -> Classic application detail page', () => {
      expect(toClassicEquivalent('/applications/apple-java')).toBe(
        '/management/view/application/apple-java',
      );
    });

    it('maps Classic /management/view/application/{publicId} -> Preview detail page', () => {
      expect(toNexusOneEquivalent('/management/view/application/apple-java')).toBe(
        '/applications/apple-java',
      );
    });

    it('round-trips detail-page mapping for an arbitrary publicId', () => {
      const publicId = 'webgoat-server';
      const classicPath = toClassicEquivalent(`/applications/${publicId}`);
      expect(toNexusOneEquivalent(classicPath)).toBe(`/applications/${publicId}`);
    });

    it('preserves URL-encoded publicId segments verbatim', () => {
      // Encoding integrity matters: an app id with a slash arrives here
      // already encoded ('app%2Fwith%2Fslashes'). The mapping must not
      // double-encode or decode it.
      expect(toClassicEquivalent('/applications/app%2Fwith%2Fslashes')).toBe(
        '/management/view/application/app%2Fwith%2Fslashes',
      );
    });

    it('still maps the bare /applications list page back to /dashboard/applications', () => {
      // Detail mapping must NOT shadow the existing list-page mapping;
      // /applications (no publicId) still goes to the apps tab
      // on the Classic dashboard.
      expect(toClassicEquivalent('/applications')).toBe('/dashboard/applications');
    });
  });

  describe('Violation detail page round-trip (CLM-42256)', () => {
    it('maps /violations/{id} -> Classic violation detail page', () => {
      expect(toClassicEquivalent('/violations/violation-123')).toBe('/violation/violation-123');
    });

    it('maps Classic /violation/{id} -> Nexus One embedded detail page', () => {
      expect(toNexusOneEquivalent('/violation/violation-123')).toBe('/violations/violation-123');
    });

    it('round-trips detail-page mapping for an arbitrary violation id', () => {
      const id = 'a2e3c6037a6a46bd8b769729c76cbb20';
      const classicPath = toClassicEquivalent(`/violations/${id}`);
      expect(toNexusOneEquivalent(classicPath)).toBe(`/violations/${id}`);
    });

    it('still maps the bare /violations list page back to the Classic violations tab', () => {
      // Detail mapping must NOT shadow the list page; /violations (no id)
      // falls through to the Classic dashboard violations tab.
      expect(toClassicEquivalent('/violations')).toBe(CLASSIC_DEFAULT_PATH);
    });

    it('maps /violations/{id} with query params -> Classic violation detail page', () => {
      expect(toClassicEquivalent('/violations/abc?type=violation')).toBe('/violation/abc');
    });

    it('preserves URL-encoded violation id segments verbatim', () => {
      // Encoding integrity: an id arriving already encoded must not be
      // double-encoded or decoded by the mapping (mirrors the app-detail guard).
      expect(toClassicEquivalent('/violations/id%2Fwith%2Fslashes')).toBe('/violation/id%2Fwith%2Fslashes');
    });
  });

  describe('Administrators edit page identity mapping (CLM-42464)', () => {
    it('maps Nexus One /administrators/{roleId} -> Classic same path (identity)', () => {
      expect(toClassicEquivalent('/administrators/b9646757e98e486da7d730025f5245f8')).toBe(
        '/administrators/b9646757e98e486da7d730025f5245f8',
      );
    });

    it('maps Classic /administrators/{roleId} -> Nexus One same path (identity)', () => {
      expect(toNexusOneEquivalent('/administrators/b9646757e98e486da7d730025f5245f8')).toBe(
        '/administrators/b9646757e98e486da7d730025f5245f8',
      );
    });

    it('round-trips detail-page mapping for an arbitrary roleId', () => {
      const roleId = 'b9646757e98e486da7d730025f5245f8';
      const classicPath = toClassicEquivalent(`/administrators/${roleId}`);
      expect(toNexusOneEquivalent(classicPath)).toBe(`/administrators/${roleId}`);
    });

    it('still maps the bare /administrators list page back to the same Classic path', () => {
      // Detail mapping must NOT shadow the list-page identity entry;
      // /administrators (no roleId) still maps to /administrators on Classic.
      expect(toClassicEquivalent('/administrators')).toBe('/administrators');
    });

    it('preserves URL-encoded roleId segments verbatim', () => {
      expect(toClassicEquivalent('/administrators/role%2Fwith%2Fslashes')).toBe(
        '/administrators/role%2Fwith%2Fslashes',
      );
    });
  });

  describe('round-trip stability for known mappings', () => {
    // For pages that have Classic AND Preview equivalents, going one way and
    // back should land on a Classic URL that maps back to the same Preview URL.
    // Important for "switch to Preview, change my mind, switch back" UX.
    it.each([
      ['/dashboard'],
      ['/applications'],
      ['/ui-settings'],
      ['/repositories'],
      ['/repositories/mgr-123'],
      ['/repositories/mgr-123/repo-456/components'],
      ['/repositories/mgr-123/repo-456/components?repositoryPublicId=my-repo'],
      ['/successMetricsConfiguration'],
      ['/users'],
      ['/users/_new_'],
      ['/users/some-user-id'],
      ['/user-activity'],
      ['/baseUrl'],
      ['/systemNoticeConfiguration'],
      ['/administrators'],
    ])('nexus-one %s -> classic -> nexus-one returns to a path that maps back', (previewPath) => {
      const classicEquivalent = toClassicEquivalent(previewPath);
      const backToPreview = toNexusOneEquivalent(classicEquivalent);
      expect(backToPreview).toBe(previewPath);
    });
  });

  // CLM-39545 / P1-F15: every Coming Soon stub must map to its registered
  // Classic deep link. The map auto-derives entries from the
  // COMING_SOON_MODULES registry, so a contributor adding a new stub
  // should not need to also touch this map or this test.
  //
  // Round-trip stability is NOT asserted per stub because multiple stubs
  // legitimately share a Classic destination (Orgs, Policies, Repositories
  // all map to /management/view; Audit Log maps to /dashboard/violations).
  // Reverse mapping picks the first match, which is fine for the toggle UX.
  describe('Coming Soon stubs (P1-F15)', () => {
    // `repositories` graduated from a Coming Soon stub to a native embed (CLM-42184):
    // /coming-soon/repositories is intentionally filtered out of the map in favour of the
    // /repositories <-> /hostedRepos subtree mapping, so it no longer satisfies the
    // stub -> classicHref invariant asserted below. It's fully covered by the dedicated
    // /repositories <-> /hostedRepos tests earlier in this file.
    const COMING_SOON_STUB_SLUGS = COMING_SOON_MODULE_ORDER.filter((slug) => slug !== 'repositories');

    it.each(COMING_SOON_STUB_SLUGS)(
      'stub /coming-soon/%s maps to its registered Classic deep link',
      (slug) => {
        const previewPath = comingSoonHref(slug);
        const classicFull = COMING_SOON_MODULES[slug].classicHref;
        const expectedClassicPath = classicFull.replace(/^\/assets\/#/, '');
        expect(toClassicEquivalent(previewPath)).toBe(expectedClassicPath);
      },
    );

    // Reverse direction: every stub's Classic href must map BACK to a
    // Preview path (any Preview path — orgs/policies/repos all collapse
    // onto /management/view, so the reverse can land on any of them).
    it.each(COMING_SOON_STUB_SLUGS)(
      'stub /coming-soon/%s -> classic -> some nexus-one path (any is acceptable when destinations collide)',
      (slug) => {
        const previewPath = comingSoonHref(slug);
        const classicEquivalent = toClassicEquivalent(previewPath);
        const backToPreview = toNexusOneEquivalent(classicEquivalent);
        // Must land somewhere on the Preview side, not the Classic default.
        expect(
          backToPreview.startsWith('/coming-soon/') ||
            backToPreview === '/ui-settings' ||
            backToPreview === '/user-activity' ||
            backToPreview === '/dashboard' ||
            backToPreview === '/applications',
        ).toBe(true);
      },
    );
  });
});
