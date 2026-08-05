/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  UIRouterReact,
  memoryLocationPlugin,
  servicesPlugin,
} from '@uirouter/react';
import { nexusOneEstateComponentDetailStates } from 'MainRoot/nexus-one/nexusOneEstateComponentDetailStates';
import { NEXUS_ONE_COMPONENTS_STATE_NAME } from 'MainRoot/nosc/componentsList/componentsRoute';

function createRouter(): UIRouterReact {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);
  router.urlService.config.strictMode(false);
  router.stateRegistry.register({
    name: NEXUS_ONE_COMPONENTS_STATE_NAME,
    url: '/components?source&q&page',
  });
  nexusOneEstateComponentDetailStates().forEach((state) => router.stateRegistry.register(state));
  return router;
}

describe('nexusOneEstateComponentDetailStates', () => {
  it('lands on the overview child for a component hash', async () => {
    const router = createRouter();
    await router.stateService.go('nexusOneEstateComponentDetail.overview', {
      componentHash: 'deadbeef',
    });
    expect(router.globals.$current.name).toBe('nexusOneEstateComponentDetail.overview');
    expect(
      router.stateService.href('nexusOneEstateComponentDetail.overview', {
        componentHash: 'deadbeef',
      }),
    ).toBe('#/components/deadbeef');
  });

  it('lands on each tab child with the expected URL', async () => {
    const router = createRouter();
    await router.stateService.go('nexusOneEstateComponentDetail.violations', {
      componentHash: 'abc123',
    });
    expect(router.globals.$current.name).toBe('nexusOneEstateComponentDetail.violations');
    expect(
      router.stateService.href('nexusOneEstateComponentDetail.violations', {
        componentHash: 'abc123',
      }),
    ).toBe('#/components/abc123/violations');
  });

  it('keeps the components list URL distinct from estate detail', async () => {
    const router = createRouter();
    await router.stateService.go(NEXUS_ONE_COMPONENTS_STATE_NAME, { source: 'local' });
    expect(router.globals.$current.name).toBe(NEXUS_ONE_COMPONENTS_STATE_NAME);
    expect(router.stateService.href(NEXUS_ONE_COMPONENTS_STATE_NAME, { source: 'local' })).toContain(
      '#/components',
    );
  });
});
