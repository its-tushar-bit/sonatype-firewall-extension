/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { classicToNexusOneUrl } from 'MainRoot/nexus-one/classicToNexusOneUrl';

describe('classicToNexusOneUrl', () => {
  it('maps Classic dashboard to Nexus One dashboard', () => {
    expect(classicToNexusOneUrl('/dashboard/violations')).toBe('/dashboard');
  });
});
