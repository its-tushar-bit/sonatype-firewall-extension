/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nexusOneToClassicUrl } from 'MainRoot/nexus-one/nexusOneToClassicUrl';

describe('nexusOneToClassicUrl', () => {
  it('maps Nexus One dashboard to Classic dashboard root', () => {
    expect(nexusOneToClassicUrl('/dashboard')).toBe('/dashboard/violations');
  });

  it('maps the standalone Waivers page to the Classic waivers dashboard (CLM-43505)', () => {
    expect(nexusOneToClassicUrl('/waivers')).toBe('/dashboard/waivers');
  });
});
