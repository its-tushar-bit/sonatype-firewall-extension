/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NEXUS_ONE_COMPONENTS_STATE_NAME,
  NEXUS_ONE_COMPONENTS_URL,
  componentsSourceToTab,
  componentsTabToSource,
} from 'MainRoot/nosc/componentsList/componentsRoute';

describe('componentsRoute', () => {
  it('declares the Components list URL with My Scan Data threat query param', () => {
    expect(NEXUS_ONE_COMPONENTS_STATE_NAME).toBe('nexusOneComponents');
    expect(NEXUS_ONE_COMPONENTS_URL).toBe(
      '/components?source&q&page&org&ecosystem&app&stage&threat',
    );
  });

  it('maps UI tabs to catalog source tokens', () => {
    expect(componentsTabToSource('myScanData')).toBe('local');
    expect(componentsTabToSource('catalog')).toBe('catalog');
    expect(componentsSourceToTab('catalog')).toBe('catalog');
    expect(componentsSourceToTab('local')).toBe('myScanData');
    expect(componentsSourceToTab(undefined)).toBe('myScanData');
  });
});
