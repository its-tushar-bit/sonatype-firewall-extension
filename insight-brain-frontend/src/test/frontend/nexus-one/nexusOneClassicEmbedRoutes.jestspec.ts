/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import {
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
  comingSoonStateName,
  ComingSoonRoute,
} from 'MainRoot/nosc/comingSoon';
import { isNativeClassicEmbedSlug } from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
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

  it('registers Success Metrics detail and Classic alias states', () => {
    expect(router.stateRegistry.get('labs')?.abstract).toBe(true);
    expect(router.stateRegistry.get('labs.successMetricsReport')?.url).toBe(
      '/coming-soon/success-metrics/{successMetricsReportId}',
    );
    expect(router.stateRegistry.get('labs.successMetrics')?.redirectTo).toBe(
      comingSoonStateName('success-metrics'),
    );
    expect(router.stateRegistry.get('dashboard.overview.violations')?.redirectTo).toBe(
      'nexusOneDashboard.violations',
    );
  });
});
