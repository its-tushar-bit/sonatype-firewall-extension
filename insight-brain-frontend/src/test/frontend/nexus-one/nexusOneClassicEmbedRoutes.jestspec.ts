/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
jest.mock('MainRoot/util/permissionService', () => ({
  isAuthorized: jest.fn(),
}));

import router from 'MainRoot/router/routerInstance';
import {
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
  comingSoonStateName,
  ComingSoonRoute,
} from 'MainRoot/nosc/comingSoon';
import { isNativeClassicEmbedSlug } from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
import { NEXUS_ONE_APPLICATION_REPORT_STATE } from 'MainRoot/nexus-one/nexusOneApplicationReportStates';
import { isAuthorized } from 'MainRoot/util/permissionService';
import { NEXUS_ONE_VIOLATION_DETAIL_STATE } from 'MainRoot/nexus-one/nexusOneViolationDetailStates';
import 'MainRoot/nexus-one/routes';

describe('nexusOneClassicEmbedRoutes', () => {
  it('registers native Classic mount for POC slugs and Coming Soon stubs for the rest', () => {
    COMING_SOON_MODULE_ORDER.forEach((slug) => {
      const state = router.stateRegistry.get(comingSoonStateName(slug));
      expect(state).toBeDefined();
      expect(state?.url).toBe(comingSoonHref(slug));

      if (isNativeClassicEmbedSlug(slug)) {
        expect(state?.component).not.toBe(ComingSoonRoute);
      } else {
        expect(state?.component).toBe(ComingSoonRoute);
      }
    });
  });

  it('registers the embedded application report state (CLM-41538)', () => {
    expect(router.stateRegistry.get(NEXUS_ONE_APPLICATION_REPORT_STATE)?.url).toBe(
      '/applications/{publicId}/report/{scanId}?componentHash&tabId',
    );
  });

  it('registers the embedded violation detail state (CLM-42256)', () => {
    expect(router.stateRegistry.get(NEXUS_ONE_VIOLATION_DETAIL_STATE)?.url).toBe(
      '/violations/{id}?type&sidebarReference&sidebarId&page',
    );
  });

  it('registers reporting deep-link states so embedded card links resolve in-shell', () => {
    const react2shell = router.stateRegistry.get('react2ShellReport');
    expect(react2shell?.url).toBe('/reports/react2shell');
    expect(react2shell?.component).toBeDefined();

    expect(router.stateRegistry.get('enterpriseReportingDashboardGroup')?.url).toBe(
      '/enterpriseReportingDashboard/{groupId}/{id}',
    );
    expect(router.stateRegistry.get('enterpriseReportingDashboard')?.url).toBe('/enterpriseReportingDashboard/{id}');

    expect(router.stateRegistry.get('enterpriseReporting')?.redirectTo).toBe(
      comingSoonStateName('reports'),
    );

    const twoSegmentMatch = router.urlService.match({ path: '/enterpriseReportingDashboard/security/sbom-scorecard' });
    expect(twoSegmentMatch?.rule?.state?.name).toBe('enterpriseReportingDashboardGroup');
  });

  it('registers Success Metrics detail and Classic alias states', () => {
    expect(router.stateRegistry.get('labs')?.abstract).toBe(true);
    expect(router.stateRegistry.get('labs.successMetricsReport')?.url).toBe(
      '/coming-soon/success-metrics/{successMetricsReportId}'
    );
    expect(router.stateRegistry.get('labs.successMetrics')?.redirectTo).toBe(comingSoonStateName('success-metrics'));
    expect(router.stateRegistry.get('dashboard.overview.violations')?.redirectTo).toBe('nexusOneDashboard.violations');
    expect(router.stateRegistry.get('nexusOneViolations')?.redirectTo).toBe('nexusOneDashboard.violations');
    expect(router.stateRegistry.get('nexusOneComponents')?.redirectTo).toBe('nexusOneDashboard.components');
  });

  // Guards the Classic-embed admin route's dirty-guard wiring: a typo in the
  // `data.isDirty` path array would silently disable the unsaved-changes
  // prompt, and the only other coverage is heavyweight Playwright suites.
  describe('successMetricsConfiguration Classic-embed admin route', () => {
    const state = () => router.stateRegistry.get('successMetricsConfiguration');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /successMetricsConfiguration', () => {
      expect(state()?.url).toBe('/successMetricsConfiguration');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('wires the dirty guard through the exact state-path array the router selector reads', () => {
      expect(state()?.data?.isDirty).toEqual(['successMetricsConfiguration', 'viewState', 'isDirty']);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });
});
