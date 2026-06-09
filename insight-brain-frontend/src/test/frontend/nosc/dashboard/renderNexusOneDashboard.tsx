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
import { render } from 'TestRoot/SpecUtil';
import { nexusOneDashboardStates } from 'MainRoot/nexus-one/nexusOneDashboardStates';

/**
 * Router-aware test harness for the Nexus One Dashboard (CLM-39992 / CLM-39641 review).
 *
 * The dashboard is now a UI-Router nested-view tree (abstract `nexusOneDashboard` parent shell + a
 * child state per tab), so it can no longer be mounted standalone with an `initialTab` prop — the
 * active tab comes from the router. This helper builds an isolated `UIRouterReact` instance with the
 * `memoryLocationPlugin` (no `window.location` coupling), registers the exact production state
 * declarations, starts it at the requested tab, and renders the `<UIView>` tree inside SpecUtil's
 * Redux provider. Returns the router so tests can assert/drive navigation.
 */
export type DashboardTab = 'overview' | 'violations' | 'components' | 'applications' | 'waivers';

export function createNexusOneDashboardRouter(initialTab: DashboardTab = 'overview'): UIRouterReact {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);
  router.urlService.config.strictMode(false);
  nexusOneDashboardStates().forEach((state) => router.stateRegistry.register(state));
  router.urlService.rules.initial({ state: `nexusOneDashboard.${initialTab}` });
  // NB: do NOT call router.start() here — the <UIRouter router={...}> component starts it as the
  // final step of its own initialization. Calling it here too throws StartMethodCalledMoreThanOnce.
  return router;
}

export function renderNexusOneDashboard(
  initialTab: DashboardTab = 'overview',
  renderOptions?: Parameters<typeof render>[1],
) {
  const router = createNexusOneDashboardRouter(initialTab);
  const result = render(
    <UIRouter router={router}>
      <UIView />
    </UIRouter>,
    renderOptions,
  );
  return { router, ...result };
}
