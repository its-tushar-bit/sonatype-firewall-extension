/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { ReactStateDeclaration } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import { PlatformHome } from 'MainRoot/nosc/platformHome/PlatformHome';
import {
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
  ComingSoonRoute,
} from 'MainRoot/nosc/comingSoon';
import { PreviewPagePlaceholder } from 'MainRoot/nosc/shell/PreviewPagePlaceholder';
import { NEXUS_ONE_DEFAULT_PATH } from 'MainRoot/nosc/routing/classicPreviewMap';
import PreviewUiSettingsPage from 'MainRoot/nosc/settings/PreviewUiSettingsPage';
import { nexusOneDashboardStates } from 'MainRoot/nexus-one/nexusOneDashboardStates';
import { AdvancedSearchComingSoon } from 'MainRoot/nosc/searchResults/AdvancedSearchComingSoon';
import { isAuthorized } from 'MainRoot/util/permissionService';

function readHashPath(): string {
  const rawHash = typeof window !== 'undefined' ? window.location.hash : '';
  return rawHash.startsWith('#') ? rawHash.slice(1) : rawHash;
}

function NexusOneRoutePlaceholder(): JSX.Element {
  return <PreviewPagePlaceholder route={readHashPath()} />;
}

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

const COMING_SOON_ENTITY_ROUTES: ReadonlyArray<{ readonly name: string; readonly url: string }> = [
  { name: 'nexusOneApplications', url: '/applications' },
  { name: 'nexusOneApplicationsDetail', url: '/applications/{publicId}' },
  { name: 'nexusOneWaivers', url: '/waivers' },
];

COMING_SOON_ENTITY_ROUTES.forEach(({ name, url }) => {
  router.stateRegistry.register({
    name,
    url,
    component: ComingSoonRoute,
    data: {
      title: 'Nexus One',
    },
  } as ReactStateDeclaration);
});

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

function slugToStateName(slug: string): string {
  const pascal = slug
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('');
  return 'nexusOneComingSoon' + pascal;
}

COMING_SOON_MODULE_ORDER.forEach((slug) => {
  router.stateRegistry.register({
    name: slugToStateName(slug),
    url: comingSoonHref(slug),
    component: ComingSoonRoute,
    data: {
      title: 'Nexus One',
    },
  } as ReactStateDeclaration);
});

router.urlService.rules.otherwise(NEXUS_ONE_DEFAULT_PATH);
