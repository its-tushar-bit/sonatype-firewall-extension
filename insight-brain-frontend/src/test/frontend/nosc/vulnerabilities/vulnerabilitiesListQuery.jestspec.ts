/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildVulnerabilitiesListRouteParams,
  parseVulnerabilitiesListParams,
  rawVulnerabilitiesListParamsSnapshot,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListQuery';
import { createDefaultVulnerabilitiesFilterState } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';

describe('vulnerabilitiesListQuery', () => {
  it('parses tab, search, page, sort, and filter tokens', () => {
    const state = parseVulnerabilitiesListParams({
      tab: 'catalog',
      q: ' log4j ',
      page: '3',
      sort: 'lowest',
      severity: 'Critical,HIGH,bogus,unknown',
      cvss: '7.0-10',
      ecosystem: 'Maven,npm',
    });

    expect(state.tab).toBe('catalog');
    expect(state.search).toBe('log4j');
    expect(state.page).toBe(2);
    expect(state.orderBy).toBe('cvssScore');
    expect(Array.from(state.filters.severities).sort()).toEqual(['critical', 'high', 'unknown']);
    expect(state.filters.cvssRange).toEqual([7, 10]);
    expect(Array.from(state.filters.ecosystems).sort()).toEqual(['maven', 'npm']);
  });

  it('serializes only non-default filter state', () => {
    const filters = createDefaultVulnerabilitiesFilterState();
    expect(
      buildVulnerabilitiesListRouteParams({
        tab: 'myScanData',
        search: '',
        page: 0,
        orderBy: '-cvssScore',
        filters,
      }),
    ).toEqual({
      tab: undefined,
      q: undefined,
      page: undefined,
      sort: undefined,
      severity: undefined,
      cvss: undefined,
      ecosystem: undefined,
      org: undefined,
      app: undefined,
      stage: undefined,
    });

    expect(
      buildVulnerabilitiesListRouteParams({
        tab: 'catalog',
        search: 'cve',
        page: 1,
        orderBy: 'cvssScore',
        filters: {
          ...createDefaultVulnerabilitiesFilterState(),
          severities: new Set(['critical']),
          ecosystems: new Set(['maven']),
          organizations: new Set(['org-1']),
          applications: new Set(['app-1']),
          stages: new Set(['build']),
          cvssRange: [7.5, 9.8],
        },
      }),
    ).toEqual({
      tab: 'catalog',
      q: 'cve',
      page: '2',
      sort: 'lowest',
      severity: 'critical',
      cvss: '7.5-9.8',
      ecosystem: 'maven',
      org: 'org-1',
      app: 'app-1',
      stage: 'build',
    });
  });

  it('parses scope filters verbatim, since ids are opaque (CLM-43211)', () => {
    const state = parseVulnerabilitiesListParams({
      org: 'Org-A,org-b',
      app: 'App-1',
      stage: 'build,release',
    });

    // Unlike severity/ecosystem, these are not case-folded — the index matches the id verbatim.
    expect(Array.from(state.filters.organizations).sort()).toEqual(['Org-A', 'org-b']);
    expect(Array.from(state.filters.applications)).toEqual(['App-1']);
    expect(Array.from(state.filters.stages).sort()).toEqual(['build', 'release']);
  });

  it('falls back to full CVSS range for malformed tokens', () => {
    expect(parseVulnerabilitiesListParams({ cvss: 'abc' }).filters.cvssRange).toEqual([0, 10]);
    expect(parseVulnerabilitiesListParams({ cvss: '4-3' }).filters.cvssRange).toEqual([3, 4]);
  });

  it('raw snapshot differs from canonical route params when junk tokens are present', () => {
    const junk = { severity: 'critical,bogus', cvss: 'abc' };
    const cleaned = buildVulnerabilitiesListRouteParams(parseVulnerabilitiesListParams(junk));
    expect(rawVulnerabilitiesListParamsSnapshot(junk)).not.toBe(JSON.stringify(cleaned));
    expect(rawVulnerabilitiesListParamsSnapshot(junk)).toContain('bogus');
    expect(rawVulnerabilitiesListParamsSnapshot(junk)).toContain('abc');
    expect(JSON.stringify(cleaned)).toContain('critical');
    expect(JSON.stringify(cleaned)).not.toContain('bogus');
    expect(JSON.stringify(cleaned)).not.toContain('abc');
  });

  it('round-trips ecosystem tokens that contain commas via percent-encoding', () => {
    const serialized = buildVulnerabilitiesListRouteParams({
      tab: 'myScanData',
      search: '',
      page: 0,
      orderBy: '-cvssScore',
      filters: {
        ...createDefaultVulnerabilitiesFilterState(),
        ecosystems: new Set(['acme,corp', 'npm']),
      },
    });

    expect(serialized.ecosystem).toBe('acme%2Ccorp,npm');

    const parsed = parseVulnerabilitiesListParams({ ecosystem: serialized.ecosystem });
    expect(Array.from(parsed.filters.ecosystems).sort()).toEqual(['acme,corp', 'npm']);
  });
});
