/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { estateComponentDetailHref } from 'MainRoot/nosc/components/detail/estateComponentDetailHref';

describe('estateComponentDetailHref', () => {
  it('builds overview URL for a hash', () => {
    expect(estateComponentDetailHref('abc123')).toBe('#/components/abc123');
  });

  it('encodes hash and appends tab segment', () => {
    expect(estateComponentDetailHref('a/b', 'violations')).toBe('#/components/a%2Fb/violations');
  });

  it('omits overview segment when tab is overview', () => {
    expect(estateComponentDetailHref('abc123', 'overview')).toBe('#/components/abc123');
  });

  it('builds each non-overview tab segment', () => {
    expect(estateComponentDetailHref('h1', 'legal')).toBe('#/components/h1/legal');
    expect(estateComponentDetailHref('h1', 'vulnerabilities')).toBe('#/components/h1/vulnerabilities');
    expect(estateComponentDetailHref('h1', 'applications')).toBe('#/components/h1/applications');
    expect(estateComponentDetailHref('h1', 'organizations')).toBe('#/components/h1/organizations');
  });
});
