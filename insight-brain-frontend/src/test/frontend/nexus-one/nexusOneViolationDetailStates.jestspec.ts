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
import router from 'MainRoot/router/routerInstance';
import {
  NEXUS_ONE_VIOLATION_DETAIL_STATE,
  NEXUS_ONE_VIOLATION_DETAIL_DEFAULT_STATE,
  NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE,
  NEXUS_ONE_VIOLATION_DETAIL_VULNERABILITY_STATE,
  NEXUS_ONE_VIOLATION_DETAIL_WAIVERS_STATE,
  nexusOneViolationDetailStates,
} from 'MainRoot/nexus-one/nexusOneViolationDetailStates';
import { nexusOneViolationDetailHref } from 'MainRoot/nexus-one/nexusOneViolationDetailHref';

function createIsolatedRouter(): UIRouterReact {
  const isolated = new UIRouterReact();
  isolated.plugin(servicesPlugin);
  isolated.plugin(memoryLocationPlugin);
  isolated.urlService.config.strictMode(false);
  nexusOneViolationDetailStates().forEach((state) => isolated.stateRegistry.register(state));
  return isolated;
}

function registerDetailStateOnSingleton(): void {
  if (!router.stateRegistry.get(NEXUS_ONE_VIOLATION_DETAIL_STATE)) {
    nexusOneViolationDetailStates().forEach((state) => router.stateRegistry.register(state));
  }
}

describe('nexusOneViolationDetailStates', () => {
  it('registers an abstract parent state with id + deep-link params', () => {
    const isolated = createIsolatedRouter();
    const state = isolated.stateRegistry.get(NEXUS_ONE_VIOLATION_DETAIL_STATE);
    expect(state).toBeDefined();
    expect(state?.abstract).toBe(true);
    expect(state?.url).toBe('/violations/{id}?type&sidebarReference&sidebarId&page');
  });

  it('registers static tab child states under the parent', () => {
    const isolated = createIsolatedRouter();
    expect(isolated.stateRegistry.get(NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE)?.url).toBe('/overview');
    expect(isolated.stateRegistry.get(NEXUS_ONE_VIOLATION_DETAIL_VULNERABILITY_STATE)?.url).toBe('/vulnerability');
    expect(isolated.stateRegistry.get(NEXUS_ONE_VIOLATION_DETAIL_WAIVERS_STATE)?.url).toBe('/waivers');
  });

  it('redirects the bare parent url to the overview child', async () => {
    const isolated = createIsolatedRouter();
    await isolated.stateService.go(NEXUS_ONE_VIOLATION_DETAIL_DEFAULT_STATE, {
      id: 'violation-123',
    });
    expect(isolated.globals.$current.name).toBe(NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE);
    expect(isolated.globals.params.id).toBe('violation-123');
    expect(
      isolated.stateService.href(NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE, {
        id: 'violation-123',
      }),
    ).toBe('#/violations/violation-123/overview');
  });

  it('lands on the overview child by default and exposes the violation id param', async () => {
    const isolated = createIsolatedRouter();
    await isolated.stateService.go(NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE, {
      id: 'violation-123',
    });
    expect(isolated.globals.$current.name).toBe(NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE);
    expect(isolated.globals.params.id).toBe('violation-123');
    expect(
      isolated.stateService.href(NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE, {
        id: 'violation-123',
      }),
    ).toBe('#/violations/violation-123/overview');
  });

  it('preserves optional Classic sidebar deep-link query params across tab hrefs', () => {
    const isolated = createIsolatedRouter();
    expect(
      isolated.stateService.href(NEXUS_ONE_VIOLATION_DETAIL_WAIVERS_STATE, {
        id: 'violation-123',
        type: 'violation',
        sidebarReference: 'security',
        sidebarId: 'sidebar-1',
        page: '2',
      }),
    ).toBe('#/violations/violation-123/waivers?type=violation&sidebarReference=security&sidebarId=sidebar-1&page=2');
  });
});

describe('nexusOneViolationDetailHref', () => {
  // The href helper is bound to the app-global singleton router, so this suite must register the state
  // on that singleton (createIsolatedRouter can't be used here). Deregister in afterAll so the state
  // is not leaked onto the singleton for later tests sharing the same jest worker.
  beforeAll(() => {
    registerDetailStateOnSingleton();
  });

  afterAll(() => {
    router.stateRegistry.deregister(NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE);
    router.stateRegistry.deregister(NEXUS_ONE_VIOLATION_DETAIL_VULNERABILITY_STATE);
    router.stateRegistry.deregister(NEXUS_ONE_VIOLATION_DETAIL_WAIVERS_STATE);
    router.stateRegistry.deregister(NEXUS_ONE_VIOLATION_DETAIL_DEFAULT_STATE);
    router.stateRegistry.deregister(NEXUS_ONE_VIOLATION_DETAIL_STATE);
  });

  it('maps policyViolationId to the id path param and delegates to the state', () => {
    expect(
      nexusOneViolationDetailHref({
        policyViolationId: 'a2e3c6037a6a46bd8b769729c76cbb20',
      }),
    ).toBe('#/violations/a2e3c6037a6a46bd8b769729c76cbb20/overview');
  });

  it('forwards an optional deep-link param alongside the mapped id', () => {
    expect(
      nexusOneViolationDetailHref({
        policyViolationId: 'violation-123',
        type: 'violation',
      }),
    ).toBe('#/violations/violation-123/overview?type=violation');
  });

  it('serializes all four Classic deep-link query params together in URL-declared order', () => {
    // Locks the full deep-link contract the real drill-in (CLM-42259) produces: type,
    // sidebarReference, sidebarId, and page serialized together in the order the state URL declares
    // (/violations/{id}?type&sidebarReference&sidebarId&page).
    expect(
      nexusOneViolationDetailHref({
        policyViolationId: 'violation-123',
        type: 'violation',
        sidebarReference: 'security',
        sidebarId: 'sidebar-1',
        page: '2',
      }),
    ).toBe(
      '#/violations/violation-123/overview?type=violation&sidebarReference=security&sidebarId=sidebar-1&page=2',
    );
  });
});
