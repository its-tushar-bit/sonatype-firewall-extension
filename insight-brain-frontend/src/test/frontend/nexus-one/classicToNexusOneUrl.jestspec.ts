/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { classicToNexusOneUrl } from 'MainRoot/nexus-one/classicToNexusOneUrl';

describe('classicToNexusOneUrl', () => {
  it('returns null until Epic 2 route table is populated', () => {
    expect(classicToNexusOneUrl('/dashboard')).toBeNull();
  });
});
