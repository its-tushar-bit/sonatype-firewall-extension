/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nexusOneToClassicUrl } from 'MainRoot/nexus-one/nexusOneToClassicUrl';

describe('nexusOneToClassicUrl', () => {
  it('returns null until Epic 2 route table is populated', () => {
    expect(nexusOneToClassicUrl('/hello1')).toBeNull();
  });
});
