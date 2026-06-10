/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import {
  ItemType,
  RENDERED_ITEM_TYPES,
  SearchResultItemDTO,
  displayNameFor,
  isApplication,
  isApplicationCategory,
  isComponent,
  isComponentLabel,
  isOrganization,
  isPolicy,
  isRenderedType,
  isSbomMetadata,
  isVulnerability,
  reactKeyFor,
} from 'MainRoot/nosc/search/searchTypes';

function dto(itemType: ItemType, extra: Partial<SearchResultItemDTO> = {}): SearchResultItemDTO {
  return { itemType, resultIndex: 0, ...extra };
}

describe('searchTypes type guards', () => {
  it.each([
    ['APPLICATION', isApplication],
    ['ORGANIZATION', isOrganization],
    ['NON_VULNERABLE_COMPONENT', isComponent],
    ['SECURITY_VULNERABILITY', isVulnerability],
    ['POLICY', isPolicy],
    ['SBOM_METADATA', isSbomMetadata],
    ['APPLICATION_CATEGORY', isApplicationCategory],
    ['COMPONENT_LABEL', isComponentLabel],
  ] as const)(
    '%s guard returns true for matching itemType, false otherwise',
    (matchingType, guard) => {
      const allTypes: ItemType[] = [
        'APPLICATION',
        'ORGANIZATION',
        'NON_VULNERABLE_COMPONENT',
        'SECURITY_VULNERABILITY',
        'POLICY',
        'SBOM_METADATA',
        'APPLICATION_CATEGORY',
        'COMPONENT_LABEL',
      ];
      for (const t of allTypes) {
        expect(guard(dto(t))).toBe(t === matchingType);
      }
    },
  );
});

describe('isRenderedType', () => {
  it('returns true for the 6 entity types we render in the omnibar', () => {
    for (const t of RENDERED_ITEM_TYPES) {
      expect(isRenderedType(dto(t))).toBe(true);
    }
  });

  it('returns false for APPLICATION_CATEGORY and COMPONENT_LABEL (filter values, not destinations)', () => {
    expect(isRenderedType(dto('APPLICATION_CATEGORY'))).toBe(false);
    expect(isRenderedType(dto('COMPONENT_LABEL'))).toBe(false);
  });
});

describe('displayNameFor', () => {
  it.each([
    [dto('APPLICATION', { applicationName: 'Webgoat', applicationPublicId: 'webgoat-app' }), 'Webgoat'],
    [dto('APPLICATION', { applicationPublicId: 'webgoat-app' }), 'webgoat-app'],
    [dto('ORGANIZATION', { organizationName: 'Engineering' }), 'Engineering'],
    [dto('NON_VULNERABLE_COMPONENT', { componentName: 'log4j-core' }), 'log4j-core'],
    [dto('NON_VULNERABLE_COMPONENT', { componentHash: 'abc123' }), 'abc123'],
    [dto('SECURITY_VULNERABILITY', { vulnerabilityId: 'CVE-2021-44228' }), 'CVE-2021-44228'],
    [dto('POLICY', { policyName: 'Security-Critical' }), 'Security-Critical'],
    [dto('SBOM_METADATA', { applicationName: 'My SBOM' }), 'My SBOM'],
    [dto('SBOM_METADATA', { reportId: 'rpt-1' }), 'rpt-1'],
  ])('%# returns "%s"', (input, expected) => {
    expect(displayNameFor(input)).toBe(expected);
  });
});

describe('reactKeyFor', () => {
  it('produces stable, type-prefixed keys', () => {
    expect(reactKeyFor(dto('APPLICATION', { applicationId: 'app-1' }))).toBe('app:app-1');
    expect(reactKeyFor(dto('SECURITY_VULNERABILITY', { vulnerabilityId: 'CVE-2021-44228' }))).toBe(
      'vuln:CVE-2021-44228',
    );
    expect(reactKeyFor(dto('NON_VULNERABLE_COMPONENT', { componentHash: 'abc' }))).toBe(
      'comp:abc',
    );
  });
});
