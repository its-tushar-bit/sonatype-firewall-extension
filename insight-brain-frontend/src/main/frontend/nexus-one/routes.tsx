/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { ReactStateDeclaration, UIView } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import { PlatformHome } from 'MainRoot/nosc/platformHome/PlatformHome';
import {
  COMING_SOON_MODULES,
  COMING_SOON_MODULE_ORDER,
  ComingSoonModuleSlug,
  comingSoonHref,
  comingSoonStateName,
  ComingSoonRoute,
} from 'MainRoot/nosc/comingSoon';
import SuccessMetricsReportListContainer from 'MainRoot/labs/successMetrics/SuccessMetricsReportListContainer';
import SuccessMetricsReportContainer from 'MainRoot/labs/successMetrics/successMetricsReport/SuccessMetricsReportContainer';
import ApiPage from 'MainRoot/api/ApiPage';
import { ClassicComponentMount, mountClassicComponent } from 'MainRoot/nexus-one/ClassicComponentMount';
import { NATIVE_CLASSIC_EMBED_SLUGS } from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
import { NEXUS_ONE_DEFAULT_PATH } from 'MainRoot/nosc/routing/classicPreviewMap';
import PreviewUiSettingsPage from 'MainRoot/nosc/settings/PreviewUiSettingsPage';
import { nexusOneDashboardStates } from 'MainRoot/nexus-one/nexusOneDashboardStates';
import { nexusOneApplicationDetailStates } from 'MainRoot/nexus-one/nexusOneApplicationDetailStates';
import { SearchResultsPage } from 'MainRoot/nosc/searchResults/SearchResultsPage';
import PreviewApplicationsList from 'MainRoot/nosc/applications/ApplicationsList';
import {
  WaiversListPage as PreviewWaiversList,
  WaiverDetailPage as PreviewWaiverDetail,
} from 'MainRoot/nosc/waivers';
import { isAuthorized } from 'MainRoot/util/permissionService';

router.stateRegistry.register({
  name: 'root',
  url: '^',
  redirectTo: 'platformHome',
});

router.stateRegistry.register({
  name: 'home',
  url: '/',
  redirectTo: 'platformHome',
});

router.stateRegistry.register({
  name: 'platformHome',
  url: '/home',
  component: PlatformHome,
  data: {
    title: 'Nexus One',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneUiSettings',
  url: '/ui-settings',
  component: PreviewUiSettingsPage,
  data: {
    title: 'Nexus One UI Settings',
  },
  resolve: {
    isAuthorized: () => isAuthorized(['CONFIGURE_SYSTEM']),
  },
} as ReactStateDeclaration);

// Dashboard: abstract parent shell (tab strip + nested <UIView>) with one child state per tab.
nexusOneDashboardStates().forEach((state) => {
  router.stateRegistry.register(state);
});

router.stateRegistry.register({
  name: 'nexusOneApplications',
  url: '/applications',
  component: PreviewApplicationsList,
  data: {
    title: 'Nexus One — Applications',
  },
} as ReactStateDeclaration);

// Application detail: abstract parent shell + one child state per tab (CLM-40901).
nexusOneApplicationDetailStates().forEach((state) => {
  router.stateRegistry.register(state);
});

router.stateRegistry.register({
  name: 'nexusOneWaivers',
  url: '/waivers',
  component: PreviewWaiversList,
  data: {
    title: 'Nexus One — Waivers',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneWaiverDetail',
  // `?from` lets a caller (e.g. the Dashboard waivers tab) tell the detail page
  // where its back-link should return to.
  url: '/waivers/{ownerType}/{ownerId}/{waiverId}?from',
  component: PreviewWaiverDetail,
  data: {
    title: 'Nexus One — Waiver Detail',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneSearch',
  // `?q` carries the omnibar query; `?tab` selects the active results tab.
  url: '/search?q&tab',
  component: SearchResultsPage,
  data: {
    title: 'Nexus One — Search',
  },
} as ReactStateDeclaration);

function SuccessMetricsRoute(): JSX.Element {
  return (
    <main className="nx-page-main">
      <SuccessMetricsReportListContainer />
    </main>
  );
}

const NATIVE_CLASSIC_COMPONENTS: Partial<Record<ComingSoonModuleSlug, React.ComponentType>> = {
  'success-metrics': mountClassicComponent(SuccessMetricsRoute),
  api: mountClassicComponent(ApiPage),
};

if (NATIVE_CLASSIC_EMBED_SLUGS.length !== Object.keys(NATIVE_CLASSIC_COMPONENTS).length) {
  throw new Error('NATIVE_CLASSIC_EMBED_SLUGS must stay in sync with NATIVE_CLASSIC_COMPONENTS');
}

function LabsContainer(): JSX.Element {
  return (
    <ClassicComponentMount>
      <main className="nx-page-main">
        <UIView />
      </main>
    </ClassicComponentMount>
  );
}

router.stateRegistry.register({
  name: 'labs',
  abstract: true,
  url: '',
  component: LabsContainer,
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'labs.successMetricsReport',
  url: '/coming-soon/success-metrics/{successMetricsReportId}',
  component: SuccessMetricsReportContainer,
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'labs.successMetrics',
  url: '/_classic-aliases/labs/successMetrics',
  redirectTo: comingSoonStateName('success-metrics'),
  data: { title: 'Success Metrics' },
} as ReactStateDeclaration);

router.stateRegistry.register({ name: 'dashboard', abstract: true, url: '' });
router.stateRegistry.register({ name: 'dashboard.overview', abstract: true, url: '' });
router.stateRegistry.register({
  name: 'dashboard.overview.violations',
  url: '/_classic-aliases/dashboard/overview/violations',
  redirectTo: 'nexusOneDashboard.violations',
  data: { title: 'Dashboard' },
} as ReactStateDeclaration);

COMING_SOON_MODULE_ORDER.forEach((slug) => {
  const ClassicComponent = NATIVE_CLASSIC_COMPONENTS[slug];
  if (ClassicComponent) {
    const module = COMING_SOON_MODULES[slug];
    router.stateRegistry.register({
      name: comingSoonStateName(slug),
      url: comingSoonHref(slug),
      component: ClassicComponent,
      data: {
        title: module.label,
      },
    } as ReactStateDeclaration);
    return;
  }

  router.stateRegistry.register({
    name: comingSoonStateName(slug),
    url: comingSoonHref(slug),
    component: ComingSoonRoute,
    data: {
      title: 'Nexus One',
    },
  } as ReactStateDeclaration);
});

router.urlService.rules.otherwise(NEXUS_ONE_DEFAULT_PATH);
