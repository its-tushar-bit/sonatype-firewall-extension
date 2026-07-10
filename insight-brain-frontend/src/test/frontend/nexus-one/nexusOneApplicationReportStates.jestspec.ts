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
  NEXUS_ONE_APPLICATION_REPORT_STATE,
  nexusOneApplicationReportStates,
} from 'MainRoot/nexus-one/nexusOneApplicationReportStates';
import { nexusOneApplicationReportHref } from 'MainRoot/nexus-one/nexusOneApplicationReportHref';

function createIsolatedRouter(): UIRouterReact {
  const isolated = new UIRouterReact();
  isolated.plugin(servicesPlugin);
  isolated.plugin(memoryLocationPlugin);
  isolated.urlService.config.strictMode(false);
  nexusOneApplicationReportStates().forEach((state) => isolated.stateRegistry.register(state));
  return isolated;
}

function registerReportStateOnSingleton(): void {
  if (!router.stateRegistry.get(NEXUS_ONE_APPLICATION_REPORT_STATE)) {
    nexusOneApplicationReportStates().forEach((state) => router.stateRegistry.register(state));
  }
}

describe('nexusOneApplicationReportStates', () => {
  it('registers the embedded report state with publicId and scanId params', () => {
    const isolated = createIsolatedRouter();
    const state = isolated.stateRegistry.get(NEXUS_ONE_APPLICATION_REPORT_STATE);
    expect(state).toBeDefined();
    expect(state?.url).toBe(
      '/applications/{publicId}/report/{scanId}?componentHash&tabId',
    );
  });

  it('lands on the embedded report state and builds href for stage clicks', async () => {
    const isolated = createIsolatedRouter();
    await isolated.stateService.go(NEXUS_ONE_APPLICATION_REPORT_STATE, {
      publicId: 'webgoat-app',
      scanId: 'scan-build-1',
    });
    expect(isolated.globals.$current.name).toBe(NEXUS_ONE_APPLICATION_REPORT_STATE);
    expect(isolated.globals.params.publicId).toBe('webgoat-app');
    expect(isolated.globals.params.scanId).toBe('scan-build-1');
    expect(
      isolated.stateService.href(NEXUS_ONE_APPLICATION_REPORT_STATE, {
        publicId: 'webgoat-app',
        scanId: 'scan-build-1',
      }),
    ).toBe('#/applications/webgoat-app/report/scan-build-1');
  });

  it('passes optional tabId query param through href', () => {
    const isolated = createIsolatedRouter();
    expect(
      isolated.stateService.href(NEXUS_ONE_APPLICATION_REPORT_STATE, {
        publicId: 'webgoat-app',
        scanId: 'scan-source-2',
        tabId: 'policy',
      }),
    ).toBe('#/applications/webgoat-app/report/scan-source-2?tabId=policy');
  });

  it('passes optional componentHash query param through href', () => {
    const isolated = createIsolatedRouter();
    expect(
      isolated.stateService.href(NEXUS_ONE_APPLICATION_REPORT_STATE, {
        publicId: 'webgoat-app',
        scanId: 'scan-build-1',
        componentHash: 'a2e3c6037a6a46bd8b769729c76cbb20',
      }),
    ).toBe(
      '#/applications/webgoat-app/report/scan-build-1?componentHash=a2e3c6037a6a46bd8b769729c76cbb20',
    );
  });
});

describe('nexusOneApplicationReportHref', () => {
  beforeAll(() => {
    registerReportStateOnSingleton();
  });

  it('delegates to the registered Nexus One report state', () => {
    expect(
      nexusOneApplicationReportHref({
        publicId: 'acme-consumer',
        scanId: 'a2e3c6037a6a46bd8b769729c76cbb20',
      }),
    ).toBe('#/applications/acme-consumer/report/a2e3c6037a6a46bd8b769729c76cbb20');
  });
});
