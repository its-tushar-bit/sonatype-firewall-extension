/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import { PlatformHome } from 'MainRoot/nosc/platformHome/PlatformHome';
import {
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
  comingSoonStateName,
  ComingSoonRoute,
} from 'MainRoot/nosc/comingSoon';
import { NEXUS_ONE_DEFAULT_PATH } from 'MainRoot/nosc/routing/classicPreviewMap';
import PreviewUiSettingsPage from 'MainRoot/nosc/settings/PreviewUiSettingsPage';
import { nexusOneDashboardStates } from 'MainRoot/nexus-one/nexusOneDashboardStates';
import { nexusOneApplicationDetailStates } from 'MainRoot/nexus-one/nexusOneApplicationDetailStates';
import { AdvancedSearchComingSoon } from 'MainRoot/nosc/searchResults/AdvancedSearchComingSoon';
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

// Dashboard: an abstract parent shell (tab strip + nested <UIView>) with one child state per tab.
// UI-Router owns tab navigation now — the page no longer hand-rolls hash parsing / pushState. The
// state declarations are shared with the jest router harness via `nexusOneDashboardStates()`.
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
  // `?q` carries the omnibar query so the destination (and the Classic escape
  // hatch) can read the user's term instead of dropping it.
  url: '/search?q',
  component: AdvancedSearchComingSoon,
  data: {
    title: 'Nexus One — Search',
  },
} as ReactStateDeclaration);

COMING_SOON_MODULE_ORDER.forEach((slug) => {
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
