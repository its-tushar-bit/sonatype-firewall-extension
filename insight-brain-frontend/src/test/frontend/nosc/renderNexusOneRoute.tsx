/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { ReactElement } from 'react';
import {
  ReactStateDeclaration,
  UIRouter,
  UIRouterReact,
  memoryLocationPlugin,
  servicesPlugin,
} from '@uirouter/react';
import { Theme } from '@radix-ui/themes';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { render } from 'TestRoot/SpecUtil';
import { nexusOneApplicationDetailStates } from 'MainRoot/nexus-one/nexusOneApplicationDetailStates';
import { nexusOneDashboardStates } from 'MainRoot/nexus-one/nexusOneDashboardStates';

/**
 * Mirrors the Radix Theme that {@code NexusOneShellLayout} provides in
 * production. Pages no longer wrap themselves in their own `<Theme>` (the shell
 * owns it), so the harness supplies one here for the standalone-mounted page.
 */
function NexusOneTestTheme({ children }: { children: React.ReactNode }): ReactElement {
  return (
    <Theme accentColor={BRAND_ACCENT} grayColor="slate" radius="medium" scaling="100%">
      {children}
    </Theme>
  );
}

/**
 * Router-aware test harness for Nexus One Applications / Waivers pages.
 *
 * These pages read their route params (publicId, tab, ownerType, ownerId,
 * waiverId, from) from UI-Router via `useCurrentStateAndParams` and navigate via
 * `stateService` — so they can't be mounted standalone with injected props. This
 * helper builds an isolated `UIRouterReact` (memory location, no `window`
 * coupling), registers the production state shapes, lands on the requested state
 * with params, and renders the component inside that router context. Returns the
 * router so tests can assert/drive navigation.
 */

// Mirrors production registrations in nexus-one/routes.tsx. Dashboard + application
// detail child states are shared with production via the exported state factories.
const STATES: ReactStateDeclaration[] = [
  { name: 'nexusOneApplications', url: '/applications?q&sort&page&stage&org&app&threat' },
  ...nexusOneApplicationDetailStates(),
  { name: 'nexusOneWaivers', url: '/waivers' },
  { name: 'nexusOneWaiverDetail', url: '/waivers/{ownerType}/{ownerId}/{waiverId}?from' },
  { name: 'nexusOneViolations', url: '/violations' },
  { name: 'platformHome', url: '/home' },
  ...nexusOneDashboardStates(),
];

export function createNexusOneRouter(
  stateName: string,
  params: Record<string, unknown> = {},
): UIRouterReact {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);
  router.urlService.config.strictMode(false);
  STATES.forEach((state) => router.stateRegistry.register(state));
  // No URL matches the memory plugin's initial '/', so the initial rule fires and
  // transitions to the requested state with params during <UIRouter> startup.
  router.urlService.rules.initial({ state: stateName, params });
  return router;
}

export function renderNexusOneRoute(
  ui: ReactElement,
  stateName: string,
  params: Record<string, unknown> = {},
  renderOptions?: Parameters<typeof render>[1],
) {
  const router = createNexusOneRouter(stateName, params);
  const result = render(
    <NexusOneTestTheme>
      <UIRouter router={router}>{ui}</UIRouter>
    </NexusOneTestTheme>,
    renderOptions,
  );
  return { router, ...result };
}

/**
 * Lightweight provider for components that only need a UIRouter *context* (e.g.
 * to call `stateService.href`) without driving a specific route — used by
 * dashboard-tab tests that embed the native WaiversTable.
 */
export function NexusOneRouterProvider({ children }: { children: React.ReactNode }): ReactElement {
  const [router] = React.useState(() => createNexusOneRouter('nexusOneWaivers'));
  return (
    <NexusOneTestTheme>
      <UIRouter router={router}>{children}</UIRouter>
    </NexusOneTestTheme>
  );
}
