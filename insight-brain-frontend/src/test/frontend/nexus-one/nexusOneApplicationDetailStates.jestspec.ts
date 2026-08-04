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
import { nexusOneApplicationDetailStates } from 'MainRoot/nexus-one/nexusOneApplicationDetailStates';

function createRouter(): UIRouterReact {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);
  router.urlService.config.strictMode(false);
  router.stateRegistry.register({ name: 'nexusOneApplications', url: '/applications' });
  nexusOneApplicationDetailStates().forEach((state) => router.stateRegistry.register(state));
  return router;
}

describe('nexusOneApplicationDetailStates', () => {
  it('lands directly on the static child state for a known tab', async () => {
    const router = createRouter();
    await router.stateService.go('nexusOneApplicationsDetail.components', {
      publicId: 'webgoat-app',
    });
    expect(router.globals.$current.name).toBe('nexusOneApplicationsDetail.components');
    expect(
      router.stateService.href('nexusOneApplicationsDetail.components', { publicId: 'webgoat-app' }),
    ).toBe('#/applications/webgoat-app/components');
  });

  it('exposes the evaluations tab at its own bookmarkable url', async () => {
    const router = createRouter();
    await router.stateService.go('nexusOneApplicationsDetail.evaluations', {
      publicId: 'webgoat-app',
    });
    expect(router.globals.$current.name).toBe('nexusOneApplicationsDetail.evaluations');
    expect(
      router.stateService.href('nexusOneApplicationsDetail.evaluations', { publicId: 'webgoat-app' }),
    ).toBe('#/applications/webgoat-app/evaluations');
  });

  it('routes a known tab slug through the legacy wildcard to the static child state', async () => {
    const router = createRouter();
    await router.stateService.go('nexusOneApplicationsDetailTab', {
      publicId: 'webgoat-app',
      tab: 'components',
    });
    expect(router.globals.$current.name).toBe('nexusOneApplicationsDetail.components');
  });

  it('redirects unknown tab slugs through the legacy wildcard state', async () => {
    const router = createRouter();
    await router.stateService.go('nexusOneApplicationsDetailTab', {
      publicId: 'webgoat-app',
      tab: 'not-a-real-tab',
    });
    expect(router.globals.$current.name).toBe('nexusOneApplicationsDetail.overview');
    expect(router.globals.params.publicId).toBe('webgoat-app');
  });
});
