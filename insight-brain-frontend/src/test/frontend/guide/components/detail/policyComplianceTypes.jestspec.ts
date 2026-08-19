/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  stageLabel,
  threatAccent,
  waiverScopeLabel,
  ROOT_ORGANIZATION_ID,
} from 'GuideRoot/components/detail/policyComplianceTypes';

describe('stageLabel', () => {
  it('capitalizes the stage type id and suffixes "Stage"', () => {
    expect(stageLabel('release')).toBe('Release Stage');
    expect(stageLabel('build')).toBe('Build Stage');
  });
});

describe('threatAccent', () => {
  it('maps IQ policy threat-level bands to Radix accents', () => {
    expect(threatAccent(0)).toBe('blue');     // none
    expect(threatAccent(1)).toBe('indigo');   // low
    expect(threatAccent(2)).toBe('yellow');   // moderate
    expect(threatAccent(3)).toBe('yellow');   // moderate
    expect(threatAccent(4)).toBe('orange');   // severe
    expect(threatAccent(7)).toBe('orange');   // severe
    expect(threatAccent(8)).toBe('red');      // critical
    expect(threatAccent(10)).toBe('red');     // critical
  });
});

describe('waiverScopeLabel', () => {
  it('shows a friendly label for the root organization', () => {
    expect(waiverScopeLabel({ scopeOwnerId: ROOT_ORGANIZATION_ID, scopeOwnerType: 'organization' }))
      .toBe('Root Organization');
  });

  it('capitalizes the owner type for non-root scopes', () => {
    expect(waiverScopeLabel({ scopeOwnerId: 'abc123', scopeOwnerType: 'application' }))
      .toBe('Application');
  });

  it('falls back to "Inherited" when no owner type is present', () => {
    expect(waiverScopeLabel({ scopeOwnerId: 'abc123' })).toBe('Inherited');
  });
});
