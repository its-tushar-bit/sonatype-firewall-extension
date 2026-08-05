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
import { nexusOneEstateComponentDetailStates } from 'MainRoot/nexus-one/nexusOneEstateComponentDetailStates';
import type { EstateComponentTab } from 'MainRoot/nosc/components/detail/estateComponentDetailHref';
import { estateComponentDetailStateNameForTab } from 'MainRoot/nosc/components/detail/estate/estateComponentDetailUtils';
import { NEXUS_ONE_COMPONENTS_STATE_NAME } from 'MainRoot/nosc/componentsList/componentsRoute';

/**
 * Router-aware harness for estate Component Detail nested-view tree (CLM-43961).
 */
export function createNexusOneEstateComponentDetailRouter(
  componentHash: string,
  tab: EstateComponentTab = 'overview',
): UIRouterReact {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);
  router.urlService.config.strictMode(false);
  router.stateRegistry.register({
    name: NEXUS_ONE_COMPONENTS_STATE_NAME,
    url: '/components?source&q&page',
  });
  nexusOneEstateComponentDetailStates().forEach((state) => router.stateRegistry.register(state));
  router.urlService.rules.initial({
    state: estateComponentDetailStateNameForTab(tab),
    params: { componentHash },
  });
  return router;
}

export function renderNexusOneEstateComponentDetail(
  componentHash: string,
  tab: EstateComponentTab = 'overview',
  renderOptions?: Parameters<typeof render>[1],
) {
  const router = createNexusOneEstateComponentDetailRouter(componentHash, tab);
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
