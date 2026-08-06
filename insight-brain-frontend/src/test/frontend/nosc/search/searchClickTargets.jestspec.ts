/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */

// Real plumbing, no module mocks: Nexus One destinations use the actual UI-Router
// singleton (with the relevant states registered), and Classic deep-links use the
// real bundleIndexUrl with a test base URL set. This exercises the real href output
// and keeps test-only fallbacks out of the implementation.
import { clickHrefFor, enterSearchHref } from 'MainRoot/nosc/search/searchClickTargets';
import router from 'MainRoot/router/routerInstance';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { SearchEntityType, SearchRow } from 'MainRoot/nosc/search/searchTypes';
import { registerNexusOneApplicationDetailStatesForHref } from 'TestRoot/nosc/search/registerNexusOneApplicationDetailStatesForHref';

const CLASSIC = 'http://localhost/assets/index.html';

function registerState(name: string, url: string): void {
  if (!router.stateRegistry.get(name)) {
    router.stateRegistry.register({ name, url });
  }
}

beforeAll(() => {
  registerNexusOneApplicationDetailStatesForHref();
  registerState('nexusOneSearch', '/search?q&tab&source');
  registerState('platformHome', '/home');
  registerState('nexusOneDashboard', '/dashboard');
  registerState('nexusOneDashboard.waivers', '/waivers');
  _setBaseUrlForTesting('http://localhost');
});

function row(type: SearchEntityType, extra: Partial<SearchRow> = {}): SearchRow {
  return {
    id: 'id-1',
    type,
    source: 'local',
    title: 'Title',
    subtitle: '',
    href: null,
    fields: {},
    ...extra,
  };
}

describe('clickHrefFor', () => {
  it('prefers a backend-provided href when it is same-origin relative', () => {
    expect(clickHrefFor(row('COMPONENT', { href: '/relative/path' }))).toBe('/relative/path');
    expect(clickHrefFor(row('COMPONENT', { href: '#/hash/route' }))).toBe('#/hash/route');
    expect(clickHrefFor(row('COMPONENT', { href: '?q=term' }))).toBe('?q=term');
  });

  it('ignores an unsafe backend href and resolves the router destination instead', () => {
    // javascript: and off-scheme hrefs are dropped; falls through to the router path.
    expect(clickHrefFor(row('COMPONENT', { href: 'javascript:alert(1)' }))).toBe('#/home');
    expect(clickHrefFor(row('COMPONENT', { href: 'data:text/html,<script>' }))).toBe('#/home');
    expect(clickHrefFor(row('COMPONENT', { href: '   ' }))).toBe('#/home');
    // Protocol-relative URLs navigate off-origin; treat as unsafe.
    expect(clickHrefFor(row('COMPONENT', { href: '//evil.example/path' }))).toBe('#/home');
  });

  it('rejects an absolute off-origin href', () => {
    // The row href contract is app-relative, so an absolute URL can only be a
    // tampered value; an https: scheme does not make another origin a safe target.
    expect(clickHrefFor(row('COMPONENT', { href: 'https://attacker.example' }))).toBe('#/home');
    expect(clickHrefFor(row('COMPONENT', { href: 'https://attacker.example/steal?t=1' }))).toBe('#/home');
    expect(clickHrefFor(row('COMPONENT', { href: 'http://attacker.example/path' }))).toBe('#/home');
    expect(clickHrefFor(row('COMPONENT', { href: 'HTTPS://Attacker.Example/path' }))).toBe('#/home');
  });

  it('Application → detail route uses the public id from fields, not the internal row id', () => {
    // The backend sets an application row id to the internal applicationId and puts
    // the public id in fields.applicationPublicId (and subtitle).
    expect(
      clickHrefFor(
        row('APPLICATION', {
          id: 'internal-app-id-42',
          subtitle: 'webgoat-public',
          fields: { applicationPublicId: 'webgoat-public' },
        }),
      ),
    ).toBe('#/applications/webgoat-public');
  });

  it('Application → falls back to subtitle when fields.applicationPublicId is absent', () => {
    expect(
      clickHrefFor(row('APPLICATION', { id: 'internal-app-id-42', subtitle: 'webgoat-public' })),
    ).toBe('#/applications/webgoat-public');
  });

  it('Application → platform home when no public id is available (never uses internal id)', () => {
    expect(
      clickHrefFor(row('APPLICATION', { id: 'internal-app-id-42', subtitle: '', fields: {} })),
    ).toBe('#/home');
  });

  it('URL-encodes the public-id path param via the router', () => {
    expect(
      clickHrefFor(
        row('APPLICATION', { id: 'x', fields: { applicationPublicId: 'app/with/slashes' } }),
      ),
    ).toBe('#/applications/app%2Fwith%2Fslashes');
  });

  it('Vulnerability → Nexus One vulnerability detail on security-details from the row id', () => {
    expect(clickHrefFor(row('VULNERABILITY', { id: 'CVE-2021-44228' }))).toBe(
      '#/vulnerabilities/CVE-2021-44228/security-details',
    );
  });

  it('URL-encodes Sonatype-style slash ids in the vuln path param', () => {
    expect(clickHrefFor(row('VULNERABILITY', { id: 'sonatype-2024/12345' }))).toBe(
      '#/vulnerabilities/sonatype-2024%2F12345/security-details',
    );
  });

  it('Violation → Classic violation-detail sidebar from the row id', () => {
    expect(clickHrefFor(row('VIOLATION', { id: 'pv-1' }))).toBe(`${CLASSIC}#/violation/pv-1`);
  });

  it('Waiver → Nexus One waivers list (row carries a waiver id, not a violation id)', () => {
    // A waiver row's id is the policy-waiver id, which is NOT a policy-violation id,
    // so it must not deep-link to the violation sidebar; it lands on the waivers list.
    expect(clickHrefFor(row('WAIVER', { id: 'waiver-1' }))).toBe('#/dashboard/waivers');
  });

  it('Component → Nexus One home until native component detail ships', () => {
    expect(clickHrefFor(row('COMPONENT', { id: 'abc123' }))).toBe('#/home');
  });
});

describe('enterSearchHref', () => {
  it('routes to the Nexus One search state carrying the q param', () => {
    expect(enterSearchHref('log4j')).toBe('#/search?q=log4j');
  });

  it('URL-encodes the query', () => {
    expect(enterSearchHref('CVE-2021-44228 critical')).toBe('#/search?q=CVE-2021-44228%20critical');
  });

  it('routes to the Nexus One search state with no query for empty input', () => {
    expect(enterSearchHref('')).toBe('#/search');
    expect(enterSearchHref('   ')).toBe('#/search');
  });

  it('omits source for the default local data source', () => {
    expect(enterSearchHref('log4j', 'local')).toBe('#/search?q=log4j');
  });

  it('carries source=catalog when the catalog data source is active', () => {
    expect(enterSearchHref('log4j', 'catalog')).toBe('#/search?q=log4j&source=catalog');
  });
});
