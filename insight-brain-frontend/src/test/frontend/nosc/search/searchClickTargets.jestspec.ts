/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */

// Real plumbing, no module mocks: Nexus One destinations use the actual UI-Router
// singleton (with the relevant states registered), and Classic deep-links use the
// real bundleIndexUrl with a test base URL set. This exercises the real href output
// and keeps test-only fallbacks out of the implementation (Ross's review point).
import { clickHrefFor, enterSearchHref } from 'MainRoot/nosc/search/searchClickTargets';
import router from 'MainRoot/router/routerInstance';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { ItemType, SearchResultItemDTO } from 'MainRoot/nosc/search/searchTypes';
import { registerNexusOneApplicationDetailStatesForHref } from 'TestRoot/nosc/search/registerNexusOneApplicationDetailStatesForHref';

const CLASSIC = 'http://localhost/assets/index.html';

function registerState(name: string, url: string): void {
  if (!router.stateRegistry.get(name)) {
    router.stateRegistry.register({ name, url });
  }
}

beforeAll(() => {
  registerNexusOneApplicationDetailStatesForHref();
  registerState('nexusOneSearch', '/search?q');
  registerState('platformHome', '/home');
  _setBaseUrlForTesting('http://localhost');
});

function dto(itemType: ItemType, extra: Partial<SearchResultItemDTO> = {}): SearchResultItemDTO {
  return { itemType, resultIndex: 0, ...extra };
}

describe('clickHrefFor', () => {
  it('Application → nexusOneApplicationsDetail state URL', () => {
    expect(clickHrefFor(dto('APPLICATION', { applicationPublicId: 'webgoat-app' }))).toBe(
      '#/applications/webgoat-app',
    );
  });

  it('URL-encodes identity path params via the router', () => {
    expect(clickHrefFor(dto('APPLICATION', { applicationPublicId: 'app/with/slashes' }))).toBe(
      '#/applications/app%2Fwith%2Fslashes',
    );
  });

  it('Organization → Classic management view (context-path aware via bundleIndexUrl)', () => {
    expect(clickHrefFor(dto('ORGANIZATION', { organizationId: 'org-123' }))).toBe(
      `${CLASSIC}#/management/view/organization/org-123`,
    );
  });

  it('Vulnerability → Nexus One vulnerability detail on security-details', () => {
    expect(clickHrefFor(dto('SECURITY_VULNERABILITY', { vulnerabilityId: 'CVE-2021-44228' }))).toBe(
      '#/vulnerabilities/CVE-2021-44228/security-details',
    );
  });

  it('URL-encodes Sonatype-style slash ids in the vuln path param', () => {
    expect(clickHrefFor(dto('SECURITY_VULNERABILITY', { vulnerabilityId: 'sonatype-2024/12345' }))).toBe(
      '#/vulnerabilities/sonatype-2024%2F12345/security-details',
    );
  });

  it('Policy → Classic Orgs & Policies root (DTO lacks the owning org/app)', () => {
    expect(clickHrefFor(dto('POLICY', { policyId: 'pol-1' }))).toBe(
      `${CLASSIC}#/management/view/organization/ROOT_ORGANIZATION_ID`,
    );
  });

  it('Policy Violation → Classic violation-detail sidebar', () => {
    expect(clickHrefFor(dto('POLICY_VIOLATION', { policyViolationId: 'pv-1' }))).toBe(
      `${CLASSIC}#/violation/pv-1`,
    );
  });

  it('Waiver → Classic violation-detail sidebar (shares the violation deep-link)', () => {
    expect(clickHrefFor(dto('WAIVER', { policyViolationId: 'pv-1' }))).toBe(
      `${CLASSIC}#/violation/pv-1`,
    );
  });

  it('Component → Nexus One home until native component detail ships', () => {
    expect(clickHrefFor(dto('NON_VULNERABLE_COMPONENT', { componentHash: 'abc123' }))).toBe('#/home');
  });

  it('SBOM Metadata → Nexus One home until native SBOM detail ships', () => {
    expect(clickHrefFor(dto('SBOM_METADATA', { reportId: 'rpt-1' }))).toBe('#/home');
  });

  it('rows with missing identity fields fall back to Nexus One home (defensive)', () => {
    expect(clickHrefFor(dto('APPLICATION'))).toBe('#/home');
    expect(clickHrefFor(dto('SECURITY_VULNERABILITY'))).toBe('#/home');
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
});
