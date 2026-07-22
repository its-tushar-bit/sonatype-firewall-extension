/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIRouterReact, memoryLocationPlugin, servicesPlugin } from '@uirouter/react';
import {
  NEXUS_ONE_COMPONENT_DETAIL_STATE,
  nexusOneComponentDetailStates,
} from 'MainRoot/nexus-one/nexusOneComponentDetailStates';
import { componentDetailHref } from 'MainRoot/nosc/components/detail/componentDetailHref';

function createIsolatedRouter(): UIRouterReact {
  const isolated = new UIRouterReact();
  isolated.plugin(servicesPlugin);
  isolated.plugin(memoryLocationPlugin);
  isolated.urlService.config.strictMode(false);
  nexusOneComponentDetailStates().forEach((state) => isolated.stateRegistry.register(state));
  return isolated;
}

describe('nexusOneComponentDetailStates', () => {
  it('registers the hybrid application + component URL with optional scanId', () => {
    const isolated = createIsolatedRouter();
    const state = isolated.stateRegistry.get(NEXUS_ONE_COMPONENT_DETAIL_STATE);
    expect(state).toBeDefined();
    expect(state?.url).toBe('/applications/{publicId}/components/{componentHash}?scanId');
  });

  it('exposes publicId, componentHash, and scanId params', async () => {
    const isolated = createIsolatedRouter();
    await isolated.stateService.go(NEXUS_ONE_COMPONENT_DETAIL_STATE, {
      publicId: 'demo-app',
      componentHash: 'abc123',
      scanId: 'scan-1',
    });
    expect(isolated.globals.$current.name).toBe(NEXUS_ONE_COMPONENT_DETAIL_STATE);
    expect(isolated.globals.params.publicId).toBe('demo-app');
    expect(isolated.globals.params.componentHash).toBe('abc123');
    expect(isolated.globals.params.scanId).toBe('scan-1');
    expect(
      isolated.stateService.href(NEXUS_ONE_COMPONENT_DETAIL_STATE, {
        publicId: 'demo-app',
        componentHash: 'abc123',
        scanId: 'scan-1',
      }),
    ).toBe('#/applications/demo-app/components/abc123?scanId=scan-1');
  });
});

describe('componentDetailHref', () => {
  it('builds the native component detail hash without scanId', () => {
    expect(componentDetailHref('demo-app', 'abc123')).toBe(
      '#/applications/demo-app/components/abc123',
    );
  });

  it('appends scanId when provided', () => {
    expect(componentDetailHref('demo-app', 'abc123', 'scan-1')).toBe(
      '#/applications/demo-app/components/abc123?scanId=scan-1',
    );
  });

  it('percent-encodes path segments', () => {
    expect(componentDetailHref('a/b', 'h ash', 's can')).toBe(
      '#/applications/a%2Fb/components/h%20ash?scanId=s%20can',
    );
  });
});
