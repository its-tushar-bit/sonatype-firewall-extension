/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  UIRouter,
  UIRouterReact,
  UIView,
  memoryLocationPlugin,
  servicesPlugin,
} from '@uirouter/react';
import { Theme } from '@radix-ui/themes';
import { render } from 'TestRoot/SpecUtil';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { nexusOneApplicationDetailStates } from 'MainRoot/nexus-one/nexusOneApplicationDetailStates';
import {
  NEXUS_ONE_COMPONENT_DETAIL_STATE,
  nexusOneComponentDetailStates,
} from 'MainRoot/nexus-one/nexusOneComponentDetailStates';

export function createNexusOneComponentDetailRouter(
  publicId: string,
  componentHash: string,
  scanId?: string,
): UIRouterReact {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);
  router.urlService.config.strictMode(false);
  router.stateRegistry.register({ name: 'nexusOneApplications', url: '/applications' });
  nexusOneApplicationDetailStates().forEach((state) => router.stateRegistry.register(state));
  nexusOneComponentDetailStates().forEach((state) => router.stateRegistry.register(state));
  router.urlService.rules.initial({
    state: NEXUS_ONE_COMPONENT_DETAIL_STATE,
    params: { publicId, componentHash, scanId },
  });
  return router;
}

export function renderNexusOneComponentDetail(
  publicId: string,
  componentHash: string,
  scanId?: string,
  renderOptions?: Parameters<typeof render>[1],
) {
  const router = createNexusOneComponentDetailRouter(publicId, componentHash, scanId);
  const result = render(
    <Theme accentColor={BRAND_ACCENT} grayColor="slate" radius="medium" scaling="100%">
      <UIRouter router={router}>
        <UIView />
      </UIRouter>
    </Theme>,
    renderOptions,
  );
  return { router, ...result };
}
